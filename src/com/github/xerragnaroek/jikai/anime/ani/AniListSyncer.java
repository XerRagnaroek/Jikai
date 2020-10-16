package com.github.xerragnaroek.jikai.anime.ani;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jasa.UserListEntry;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.prop.IntegerProperty;

/**
 * 
 */
public class AniListSyncer {
	private static AniListSyncer als;
	private final Logger log = LoggerFactory.getLogger(AniListSyncer.class);

	private AniListSyncer() {}

	private Map<Integer, Set<JikaiUser>> userMap = Collections.synchronizedMap(new HashMap<>());

	public static AniListSyncer getInstance() {
		als = als == null ? new AniListSyncer() : als;
		return als;
	}

	public static void init() {
		getInstance();
		als.log.info("Initializing...");
		JikaiUserManager.getInstance().users().forEach(als::registerUser);
		als.syncLists();
	}

	public void registerUser(JikaiUser ju) {
		MDC.put("id", String.valueOf(ju.getId()));
		log.debug("Registering user");
		IntegerProperty idP = ju.aniIdProperty();
		int id = idP.get();
		if (id > 0) {
			addUserToMap(ju, id);
		}
		idP.onChange((o, n) -> {
			if (n > 0) {
				MDC.put("id", String.valueOf(ju.getId()));
				addUserToMap(ju, id);
				if (o > 0) {
					removeOldId(ju, o);
				}
				log.debug("Updated aniId");
				MDC.remove("id");
			}
		});
		MDC.remove("id");
	}

	private void addUserToMap(JikaiUser ju, int id) {
		userMap.compute(id, (i, s) -> {
			s = s == null ? new HashSet<>() : s;
			s.add(ju);
			log.debug("User added to map, aniId: {}", id);
			return s;
		});
	}

	private void removeOldId(JikaiUser ju, int id) {
		userMap.compute(id, (i, s) -> {
			s.remove(ju);
			log.debug("Removed old id mapping, aniId: {}", id);
			return s.isEmpty() ? null : s;
		});

	}

	private void syncLists() {
		JASA jasa = new JASA();
		if (!userMap.isEmpty()) {
			log.debug("Syncing {} lists", userMap.size());
			jasa.fetchUserListEntries(new ArrayList<Integer>(userMap.keySet())).filter(ule -> ule.getMediaStatus() != null && !ule.getMediaStatus().equals("FINISHED")).forEach(this::handleUserListEntry);
		}
	}

	private void handleUserListEntry(UserListEntry ule) {
		log.debug("Handling UserListEntry: userId: {}; mediaId: {}", ule.getUserId(), ule.getMediaId());
		if (AnimeDB.getAnime(ule.getMediaId()) != null) {
			userMap.get(ule.getUserId()).forEach(ju -> ju.subscribeAnime(ule.getMediaId(), ju.getLocale().getString("ju_sub_add_cause_ani")));
		}
	}

	public static void startSyncThread(long minutes) {
		getInstance();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime halfHour = now.withMinute(30);
		halfHour = halfHour.isBefore(now) ? halfHour.plusHours(1) : halfHour;
		long dif = Duration.between(now, halfHour).toSeconds();
		als.log.info("Starting sync thread, first running in {} seconds, running every {} minutes", dif, minutes);
		Core.EXEC.scheduleAtFixedRate(als::syncLists, dif, minutes * 60, TimeUnit.SECONDS);
	}
}
