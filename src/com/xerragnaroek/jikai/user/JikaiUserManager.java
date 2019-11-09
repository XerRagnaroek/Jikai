package com.xerragnaroek.jikai.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.util.BotUtils;

public class JikaiUserManager {

	private Map<Long, JikaiUser> user = new ConcurrentHashMap<>();
	private Map<String, Set<JikaiUser>> subscriptionMap = new ConcurrentHashMap<>();
	private JikaiUserUpdater juu = new JikaiUserUpdater();
	private final Logger log = LoggerFactory.getLogger(JikaiUserManager.class);

	public JikaiUserManager() {}

	public JikaiUser registerUser(long id) {
		JikaiUser ju = new JikaiUser(id);
		registerImpl(ju);
		if (!ju.isSetupCompleted()) {
			JikaiUserSetup.runSetup(ju);
		}
		log.debug("Registered new JikaiUser");
		return ju;
	}

	public int userAmount() {
		return user.size();
	}

	private void registerImpl(JikaiUser ju) {
		handleSubscriptions(ju);
		handleTimeZoneChange(ju);
		juu.registerUser(ju);
		user.put(ju.getId(), ju);
	}

	public JikaiUser getUser(long id) {
		return user.get(id);
	}

	private void handleSubscriptions(JikaiUser ju) {
		ju.subscribedAnimesProperty().onAdd(title -> {
			subscriptionMap.compute(title, (t, s) -> {
				if (s == null) {
					s = Collections.synchronizedSet(new HashSet<>());
				}
				s.add(ju);
				return s;
			});
		});
		ju.subscribedAnimesProperty().onRemove(title -> {
			subscriptionMap.computeIfPresent(title, (t, s) -> {
				s.remove(ju);
				return s.isEmpty() ? null : s;
			});
		});
	}

	private void handleTimeZoneChange(JikaiUser ju) {
		ju.timeZoneProperty().onChange((oz, nz) -> {
			Jikai.removeTimeZone(oz);
			if (nz != null) {
				Jikai.addTimeZone(nz);
			}
		});
	}

	public void removeUser(long id) {
		JikaiUser ju = user.remove(id);
		if (ju != null) {
			ju.destroy();
		}
	}

	public void save() throws IOException {
		Path loc = Paths.get(Core.DATA_LOC.toString(), "/user.db");
		Files.write(loc, BotUtils.collectionToIterableStr(user.values()));
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

	private JikaiUser loadUser(String str) {
		/**
		 * This regex will find all commas, which are not inside quotes. Also works if sections are
		 * not quoted. - by Luke Sheppard (regexr.com)
		 */
		String[] data = str.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*(?![^\\\"]*\\\"))");
		if (data.length < 6) {
			throw new IllegalArgumentException("Expected data length 6, got " + data.length + " instead");
		}
		for (int i = 0; i < data.length; i++) {
			data[i] = StringUtils.substringBetween(data[i], "\"");
		}
		JikaiUser ju = new JikaiUser(Long.parseLong(data[0]));
		registerImpl(ju);
		ju.setTitleType(TitleType.values()[Integer.parseInt(data[1])]);
		ju.setTimeZone(ZoneId.of(data[2]));
		ju.setUpdateDaily(data[3].equals("1"));
		String tmp = StringUtils.substringBetween(data[4], "[", "]");
		if (tmp != null && !tmp.isEmpty()) {
			Stream.of(tmp.split(",")).mapToLong(Long::parseLong).forEach(ju::addPreReleaseNotificaionStep);
		}
		tmp = StringUtils.substringBetween(data[5], "[", "]");
		if (tmp != null && !tmp.isEmpty()) {
			Stream.of(tmp.split(",")).mapToInt(Integer::parseInt).mapToObj(AnimeDB::getAnimeByNumber).forEach(a -> ju.subscribeAnime(a.title));
		}
		ju.setSetupCompleted(true);
		log.info("Loaded JikaiUser: '{}'", ju);
		ju.loading = false;
		return ju;
	}
}
