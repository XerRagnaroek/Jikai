package com.github.xerragnaroek.jikai.anime.ani;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.MediaListStatus;
import com.github.xerragnaroek.jasa.UserListEntry;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
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
		Core.EXEC.execute(() -> als.syncLists());
	}

	public void registerUser(JikaiUser ju) {
		IntegerProperty idP = ju.aniIdProperty();
		int id = idP.get();
		if (id > 0) {
			addUserToMap(ju, id);
		}
		idP.onChange((o, n) -> {
			if (n > 0) {
				addUserToMap(ju, id);
				if (o > 0) {
					removeOldId(ju, o);
				}
				log.debug("Updated aniId");
			}
		});
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
		if (!userMap.isEmpty()) {
			log.debug("Syncing {} lists", userMap.size());
			try {
				userMap.values().stream().flatMap(Set::stream).forEach(this::syncAniListsWithSubs);
				AnimeDB.getJASA().fetchUserListEntries(new ArrayList<Integer>(userMap.keySet()), MediaListStatus.PLANNING, MediaListStatus.CURRENT).filter(ule -> ule.getMediaStatus() != null && !ule.getMediaStatus().equals("FINISHED")).forEach(this::handleUserListEntry);
			} catch (Exception e) {
				BotUtils.logAndSendToDev(log, "Exception syncing lists!", e);
			}
		}
	}

	private void handleUserListEntry(UserListEntry ule) {
		log.debug("Handling UserListEntry: userId: {}; mediaId: {}", ule.getUserId(), ule.getMediaId());
		if (AnimeDB.getAnime(ule.getMediaId()) != null) {
			userMap.get(ule.getUserId()).forEach(ju -> ju.subscribeAnime(ule.getMediaId(), ju.getLocale().getString("ju_sub_add_cause_ani")));
		}
	}

	public void syncSubsWithAniList(JikaiUser ju) throws AniException, IOException {
		log.debug("Syncing subs with ani for {}", ju.getId());
		AnimeDB.getJASA().fetchUserListEntries(Arrays.asList(ju.getAniId()), MediaListStatus.PLANNING, MediaListStatus.CURRENT).filter(ule -> ule.getMediaStatus() != null && !ule.getMediaStatus().equals("FINISHED")).forEach(this::handleUserListEntry);
	}

	public void syncAniListsWithSubs(JikaiUser ju) {
		if (ju.getAniId() > 0 && JikaiUserAniTokenManager.hasToken(ju)) {
			log.debug("Syncing subs with ani for {}", ju.getId());
			String token = JikaiUserAniTokenManager.getAniToken(ju).getAccessToken();
			ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).filter(Objects::nonNull).forEach(a -> {
				try {
					try {
						// epic multi try blocks pog
						AnimeDB.getJASA().getMediaListEntryIdForUserFromAniId(ju.getAniId(), a.getId());
					} catch (AniException e) {
						// 404 means it's not part of any lists, thus add them. Otherwise rethrow the exception and do
						// nothing
						if (e.getStatusCode() == 404) {
							log.debug("{} isn't in any list yet for {}", a.getTitleRomaji(), ju.getId());
							if (a.isNotYetReleased()) {
								AnimeDB.getJASA().addToUserPlanningList(token, a.getId());
								log.debug("{} added to planning list for {}", a.getTitleRomaji(), ju.getId());
							} else if (a.isReleasing()) {
								AnimeDB.getJASA().addToUserCurrentList(token, a.getId());
								log.debug("{} added to watching list for {}", a.getTitleRomaji(), ju.getId());
							}
						} else {
							throw e;
						}
					}
				} catch (IOException | AniException e) {
					BotUtils.logAndSendToDev(log, String.format("Failed adding anime %s,%s to ju %s anilist!", a.getTitleRomaji(), a.getId(), ju.getId()), e);
				}
			});
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
