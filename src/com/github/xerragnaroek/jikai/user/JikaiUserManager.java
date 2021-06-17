
package com.github.xerragnaroek.jikai.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.ani.AniListSyncer;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.MapProperty;

import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Result;

public class JikaiUserManager {

	private Map<Long, JikaiUser> user = new ConcurrentHashMap<>();
	private Map<Integer, Set<JikaiUser>> subscriptionMap = new ConcurrentHashMap<>();
	private MapProperty<ZoneId, Set<JikaiUser>> timeZoneMap = new MapProperty<>();
	private JikaiUserUpdater juu;
	private final Logger log = LoggerFactory.getLogger(JikaiUserManager.class);

	private static JikaiUserManager instance;

	private JikaiUserManager() {}

	public static void init() {
		instance = new JikaiUserManager();
		instance.juu = new JikaiUserUpdater();
	}

	public static JikaiUserManager getInstance() {
		return instance;
	}

	public JikaiUser registerNewUser(long id, Jikai j) {
		JikaiUser ju = new JikaiUser(id);
		registerUser(ju);
		if (!ju.isSetupCompleted()) {
			// JikaiUserSetupOld.runSetup(ju, j);
			JikaiUserSetup.runSetup(ju, j);
		}
		log.debug("Registered new JikaiUser");
		return ju;
	}

	public boolean isKnownJikaiUser(long id) {
		return user.containsKey(id);
	}

	public int userAmount() {
		return user.size();
	}

	public void registerUser(JikaiUser ju) {
		ju.init();
		handleSubscriptions(ju);
		handleTimeZoneChange(ju);
		juu.registerUser(ju);
		user.put(ju.getId(), ju);
		AniListSyncer.getInstance().registerUser(ju);
		ju.titleLanguageProperty().onChange((o, n) -> {
			if (!Core.INITIAL_LOAD.get()) {
				BotUtils.switchTitleLangRole(ju, o, n);
			}
		});
		log.debug("Registered JUser {}", ju.getId());
		// AniListSyncer.getInstance().registerUser(ju);
	}

	public void loadUser(JikaiUser j) {
		JikaiUser ju = new JikaiUser(j.getId());
		if (ju.getUser() == null) {
			log.info("User was invalid");
			removeUser(ju.getId());
		}
		registerUser(ju);
		ju.copy(j);
		ju.setSetupCompleted(true);
		ju.loading = false;
		log.info("Loaded JUser {}", ju.getId());
		// log.debug("Removed {} invalid anime", ju.getSubscribedAnime().removeInvalidAnime());
	}

	public JikaiUser getUser(long id) {
		return user.get(id);
	}

	public void handleSubscriptions(JikaiUser ju) {
		ju.getSubscribedAnime().onAdd((id, cause, linked) -> {
			subscriptionMap.compute(id, (t, s) -> {
				if (s == null) {
					s = Collections.synchronizedSet(new HashSet<>());
				}
				s.add(ju);
				return s;
			});
		});
		ju.getSubscribedAnime().onRemove((id, cause) -> {
			subscriptionMap.computeIfPresent(id, (t, s) -> {
				s.remove(ju);
				return s.isEmpty() ? null : s;
			});
		});
	}

	private void handleTimeZoneChange(JikaiUser ju) {
		ju.timeZoneProperty().onChange((ov, nv) -> {
			if (ov != null) {
				log.debug("Deleting old timezone mapping for {}", ju.getId());
				timeZoneMap.compute(ov, (z, s) -> {
					s.remove(ju);
					return s.isEmpty() ? null : s;
				});
			}
			if (nv == null) {
				if (timeZoneMap.containsKey(ov)) {
					timeZoneMap.compute(ov, (k, s) -> {
						s.remove(ju);
						log.debug("Removed {} from the timezone map!", ju.getId());
						return s.isEmpty() ? null : s;
					});
				}
			} else {
				timeZoneMap.compute(nv, (z, s) -> {
					s = s == null ? Collections.synchronizedSet(new HashSet<>()) : s;
					s.add(ju);
					return s;
				});
			}
		});
	}

	public void removeUser(long id) {
		log.debug("Removing user '{}'", id);
		JikaiUser ju = user.remove(id);
		if (ju != null) {
			ju.destroy();
			EpisodeTracker.removeTracker(ju);
		}
	}

	public void save() throws IOException {
		Path loc = Paths.get(Core.DATA_LOC.toString(), "/user.db");
		Files.write(loc, BotUtils.collectionToIterableStr(user.values().stream().filter(JikaiUser::isSetupCompleted).collect(Collectors.toList())));
	}

	public void load(Path loc) {
		try {
			Files.lines(loc).forEach(str -> {
				JikaiUser ju = loadUser(str);
				if (ju.getUser() == null) {
					log.info("User was invalid");
					removeUser(ju.getId());
				}
			});
			setUpLinks();
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading users", e);
		}
	}

	public void setUpLinks() {
		log.debug("Setting up links...");
		user.values().forEach(ju -> {
			ju.getLinkedUsers().stream().map(this::getUser).forEach(u -> u.linkToUser(ju.getId()));
			log.debug("Links established for {}", ju.getId());
		});
		log.debug("{} links established", user.size());
	}

	public Set<JikaiUser> users() {
		return new HashSet<>(user.values());
	}

	public Map<Integer, Set<JikaiUser>> subscriptionMap() {
		return new ConcurrentHashMap<>(subscriptionMap);
	}

	public Set<JikaiUser> getJUSubscribedToAnime(Anime a) {
		Set<JikaiUser> jums = subscriptionMap.get(a.getId());
		return jums == null ? Collections.emptySet() : jums;
	}

	private JikaiUser loadUser(String str) {
		/**
		 * This regex will find all commas, which are not inside quotes. Also works if sections are
		 * not quoted. - by Luke Sheppard (regexr.com)
		 */
		String[] data = str.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*(?![^\\\"]*\\\"))");
		/*
		 * if (data.length < 10) {
		 * throw new IllegalArgumentException("Expected data length 9, got " + data.length + " instead");
		 * }
		 */
		for (int i = 0; i < data.length; i++) {
			data[i] = StringUtils.substringBetween(data[i], "\"");
		}
		JikaiUser ju = new JikaiUser(Long.parseLong(data[0]));
		registerUser(ju);
		ju.setAniId(Integer.parseInt(data[1]));
		ju.setTitleLanguage(TitleLanguage.values()[Integer.parseInt(data[2])]);
		ju.setTimeZone(ZoneId.of(data[3]));
		ju.setLocale(JikaiLocaleManager.getInstance().getLocale(data[4]));
		ju.setUpdateDaily(data[5].equals("1"));
		ju.setSendWeeklySchedule(data[6].equals("1"));
		String tmp = StringUtils.substringBetween(data[7], "[", "]");
		if (tmp != null && !tmp.isEmpty()) {
			Stream.of(tmp.split(",")).mapToInt(Integer::parseInt).forEach(ju::addPreReleaseNotificaionStep);
		}
		tmp = StringUtils.substringBetween(data[8], "[", "]");
		if (tmp != null && !tmp.isEmpty()) {
			Stream.of(tmp.split(", ")).map(Integer::parseInt).map(AnimeDB::getAnime).filter(Objects::nonNull).forEach(a -> ju.subscribeAnime(a.getId(), "Startup load"));
		}
		if (data.length > 9) {
			tmp = StringUtils.substringBetween(data[9], "[", "]");
			if (tmp != null && !tmp.isEmpty()) {
				Stream.of(tmp.split(", ")).map(Long::parseLong).forEach(ju::linkUser);
			}
		}
		ju.setSetupCompleted(true);
		log.info("Loaded JikaiUser: '{}'", ju);
		ju.loading = false;
		return ju;
	}

	public JikaiUserUpdater getUserUpdater() {
		return juu;
	}

	public Set<JikaiUser> getJikaiUsersWithTimeZone(ZoneId z) {
		Set<JikaiUser> set = new HashSet<>();
		if (timeZoneMap.containsKey(z)) {
			set.addAll(timeZoneMap.get(z));
		}
		return set;
	}

	public MapProperty<ZoneId, Set<JikaiUser>> timeZoneMapProperty() {
		return timeZoneMap;
	}

	public void cachePrivateChannels() {
		Collection<JikaiUser> jus = user.values();
		log.debug("Caching {} PrivateChannels", jus.size());
		List<RestAction<Result<PrivateChannel>>> rests = jus.stream().map(JikaiUser::getUser).map(u -> u.openPrivateChannel()).map(RestAction::mapToResult).collect(Collectors.toList());
		RestAction.allOf(rests).submit().thenAccept(l -> {
			int success = 0;
			List<Result<PrivateChannel>> failures = new ArrayList<>();
			for (Result<PrivateChannel> r : l) {
				if (r.isSuccess()) {
					success++;
				} else {
					failures.add(r);
				}
			}
			log.debug("Cached {} of {} PrivateChannels, failed {}", success, l.size(), failures.size());
			failures.forEach(r -> log.error("Failed openeing PrivateChannel", r.getFailure()));
		});
	}
}
