
package com.github.xerragnaroek.jikai.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
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

	public JikaiUser registerUser(long id, Jikai j) {
		JikaiUser ju = new JikaiUser(id);
		registerImpl(ju);
		AniListSyncer.getInstance().registerUser(ju);
		if (!ju.isSetupCompleted()) {
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

	private void registerImpl(JikaiUser ju) {
		handleSubscriptions(ju);
		handleTimeZoneChange(ju);
		juu.registerUser(ju);
		user.put(ju.getId(), ju);
		// AniListSyncer.getInstance().registerUser(ju);
	}

	public JikaiUser getUser(long id) {
		return user.get(id);
	}

	private void handleSubscriptions(JikaiUser ju) {
		ju.getSubscribedAnime().onAdd((id, cause) -> {
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
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading users", e);
		}
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
		if (data.length < 9) {
			throw new IllegalArgumentException("Expected data length 8, got " + data.length + " instead");
		}
		for (int i = 0; i < data.length; i++) {
			data[i] = StringUtils.substringBetween(data[i], "\"");
		}
		JikaiUser ju = new JikaiUser(Long.parseLong(data[0]));
		registerImpl(ju);
		ju.setAniId(Integer.parseInt(data[1]));
		ju.setTitleLanguage(TitleLanguage.values()[Integer.parseInt(data[2])]);
		ju.setTimeZone(ZoneId.of(data[3]));
		ju.setLocale(JikaiLocaleManager.getInstance().getLocale(data[4]));
		ju.setUpdateDaily(data[5].equals("1"));
		ju.setSentWeeklySchedule(data[6].equals("1"));
		String tmp = StringUtils.substringBetween(data[7], "[", "]");
		if (tmp != null && !tmp.isEmpty()) {
			Stream.of(tmp.split(",")).mapToInt(Integer::parseInt).forEach(ju::addPreReleaseNotificaionStep);
		}
		tmp = StringUtils.substringBetween(data[8], "[", "]");
		if (tmp != null && !tmp.isEmpty()) {
			Stream.of(tmp.split(", ")).mapToInt(Integer::parseInt).mapToObj(AnimeDB::getAnime).filter(Objects::nonNull).forEach(a -> ju.subscribeAnime(a.getId(), "Startup load"));
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

}
