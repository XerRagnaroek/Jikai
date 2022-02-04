package com.github.xerragnaroek.jikai.user;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

/**
 * 
 */
public class EpisodeTrackerManager implements ButtonInteractor {
	private static Map<Long, EpisodeTracker> tracker = Collections.synchronizedMap(new TreeMap<>());
	private static EpisodeTrackerManager etm;

	public static void init() {
		etm = new EpisodeTrackerManager();
		Core.getEventListener().registerButtonInteractor(etm);
	}

	public static EpisodeTracker getTracker(JikaiUser ju) {
		return tracker.compute(ju.getId(), (key, t) -> t == null ? new EpisodeTracker(ju) : t);
	}

	public static void removeTracker(JikaiUser ju) {
		tracker.remove(ju.getId());
	}

	/*
	 * public static void loadOld(Set<Long> ids) {
	 * Logger log = LoggerFactory.getLogger(EpisodeTracker.class);
	 * log.debug("Loading old rmids");
	 * ids.forEach(l -> {
	 * Core.EXEC.execute(() -> {
	 * log.debug("Checking {}", l);
	 * JikaiUserManager.getInstance().users().forEach(ju -> {
	 * try {
	 * Message m = ju.getUser().openPrivateChannel().complete().retrieveMessageById(l).complete();
	 * // since we're now here that means the message was for this user
	 * String title = m.getEmbeds().get(0).getTitle();
	 * title = title.substring(2, title.length() - 2);
	 * Anime a = AnimeDB.getAnime(title, ju.getTitleLanguage());
	 * if (a == null) {
	 * for (TitleLanguage tt : TitleLanguage.values()) {
	 * if (tt != ju.getTitleLanguage()) {
	 * a = AnimeDB.getAnime(title, tt);
	 * }
	 * }
	 * }
	 * log.debug("Found anime: {}", a);
	 * if (a != null) {
	 * String[] split = ju.getLocale().getString("ju_eb_notify_release_desc").split("%episodes%");
	 * int episode = Integer.parseInt(m.getEmbeds().get(0).getDescription().replace(split[0],
	 * "").replace(split[1], "").split("/")[0].trim());
	 * getTracker(ju).registerEpisodeDetailed(a.getId(), l, episode);
	 * }
	 * } catch (ErrorResponseException e) {
	 * log.debug("Message wasn't for user {}", ju.getId());
	 * }
	 * });
	 * });
	 * });
	 * }
	 */

	public static Map<Long, Map<Integer, Map<Long, Integer>>> getSavableMap() {
		Map<Long, Map<Integer, Map<Long, Integer>>> map = new TreeMap<>();
		tracker.forEach((l, t) -> map.put(l, t.episodes));
		return map;
	}

	public static void loadOld(Map<Long, Map<Integer, Map<Long, Integer>>> map) {
		map.forEach((l, m) -> {
			JikaiUser ju = JikaiUserManager.getInstance().getUser(l);
			if (ju != null) {
				EpisodeTracker et = getTracker(ju);
				m.forEach((aniId, idEpMap) -> {
					idEpMap.forEach((msgId, epNum) -> et.registerEpisodeDetailed(aniId, msgId, epNum));
				});
			}
		});
	}

	public static void load(Collection<EpisodeTracker> ets) {
		ets.stream().filter(Objects::nonNull).forEach(et -> tracker.put(et.getJikaiUserId(), et));
	}

	public static Map<Long, EpisodeTracker> getEpisodeTracker() {
		return tracker;
	}

	public static int size() {
		return tracker.size();
	}

	@Override
	public String getIdentifier() {
		return "ept";
	}

	@Override
	public void handleButtonClick(String[] data, ButtonClickEvent event) {
		JikaiUser ju;
		if ((ju = JikaiUserManager.getInstance().getUser(event.getUser().getIdLong())) != null) {
			getTracker(ju).handleButtonClick(data, event);
		}
	}
}
