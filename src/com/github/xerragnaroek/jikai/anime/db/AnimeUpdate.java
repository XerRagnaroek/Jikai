package com.github.xerragnaroek.jikai.anime.db;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

/**
 * Compares the old anime with the new ones and categorises the changes into removed, new and
 * postponed anime
 * 
 * @author XerRagnaroek
 */
public class AnimeUpdate {
	private List<Anime> removed;
	private List<Anime> newA;
	private List<Pair<Anime, Long>> changed = new ArrayList<>();
	private List<Anime> nextEp = new ArrayList<>();
	private List<Pair<Anime, Long>> changedPeriod = new ArrayList<>();
	private final Logger log = LoggerFactory.getLogger(AnimeUpdate.class);

	private AnimeUpdate() {}

	private void listChanges(List<Anime> oldA, List<Anime> newAnime) {
		handleRemoved(newAnime, oldA);
		newA = newAnime.stream().filter(a -> !oldA.contains(a)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<>())));
		newA.forEach(a -> log.debug("New anime: {}", a.getTitleRomaji()));
		handleReleaseChanged(newAnime, oldA);
		handleNextEp(newAnime, oldA);
		// handleReleasePeriod(newAnime);
		String msg = String.format("Updated AnimeDB. removed: %d, new: %d, postponed: %d, next episode: %d, different release period: %d", removed.size(), newA.size(), changed.size(), nextEp.size(), changedPeriod.size());
		log.info(msg);
		BotUtils.sendToAllInfoChannels(msg);
	}

	private void handleRemoved(List<Anime> newA, List<Anime> old) {
		List<Anime> removedOldA = old.stream().filter(a -> !newA.contains(a)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<>())));
		removed = newA.stream().filter(a -> a.getStatus().equals("FINISHED")).collect(Collectors.toList());
		removedOldA.forEach(a -> {
			if (!removed.stream().anyMatch(an -> an.getId() == a.getId())) {
				removed.add(a);
			}
		});
		removed.forEach(a -> log.debug("{} has been removed or finshed! status: {}", a.getTitleRomaji(), a.getStatus()));
	}

	private void handleReleaseChanged(List<Anime> newA, List<Anime> old) {
		newA.forEach(a -> {
			if (old.contains(a)) {
				Anime oldA = old.get(old.indexOf(a));
				if (oldA.hasDataForNextEpisode() && a.hasDataForNextEpisode() && oldA.getNextEpisodeNumber() == a.getNextEpisodeNumber()) {
					long delay = a.getNextEpisodesAirsAt() - oldA.getNextEpisodesAirsAt();
					if (delay != 0) {
						changed.add(Pair.of(a, delay));
						log.info("{} has been {} by {}!", a.getTitleRomaji(), (delay > 0 ? "postponed" : "advanced"), BotUtils.formatSeconds(delay, JikaiLocaleManager.getEN()));
					}
				}
			}
		});
	}

	private void handleNextEp(List<Anime> newA, List<Anime> old) {
		newA.forEach(a -> {
			if (old.contains(a)) {
				Anime oldA = old.get(old.indexOf(a));
				if (oldA.hasDataForNextEpisode() && a.hasDataForNextEpisode() && a.getNextEpisodeNumber() > oldA.getNextEpisodeNumber()) {
					nextEp.add(a);
					log.info("{} has a new episode coming up!", a.getTitleRomaji());
				}
			}
		});
	}

	private void handleReleasePeriod(List<Anime> newA) {
		/*
		 * newA.forEach(a -> {
		 * if (old.contains(a) && nextEp.contains(a)) {
		 * releasePeriods.compute(a, (an, q) -> {
		 * Duration dif = getDifference(old.get(old.indexOf(a)), a);
		 * int dayDif = (int) dif.toDays();
		 * if (q == null) {
		 * q = new CircularFifoQueue<>(2);
		 * q.offer(dayDif);
		 * } else {
		 * if (q.isAtFullCapacity() && !q.contains(dayDif)) {
		 * log.debug("{} has a changed release period: {} instead of {}", a.getTitleRomaji(), dayDif, q);
		 * q.offer(dayDif);
		 * changedPeriod.add(Pair.of(a, dif.toSeconds()));
		 * }
		 * }
		 * return q;
		 * });
		 * }
		 * });
		 */
		AnimeReleaseTracker art = AnimeReleaseTracker.getInstance();
		newA.forEach(a -> {
			long dif = 0;
			if ((dif = art.addAnime(a)) != 0) {
				changedPeriod.add(Pair.of(a, dif));
			}
		});
	}

	private Duration getDifference(Anime old, Anime newA) {
		return Duration.between(old.getNextEpisodeDateTime(ZoneId.systemDefault()).get(), newA.getNextEpisodeDateTime(ZoneId.systemDefault()).get());
	}

	public List<Anime> getRemovedAnime() {
		return removed;
	}

	public boolean hasRemovedAnime() {
		return !removed.isEmpty();
	}

	public List<Anime> getNewAnime() {
		return newA;
	}

	public boolean hasNewAnime() {
		return !newA.isEmpty();
	}

	public List<Pair<Anime, Long>> getChangedReleaseAnime() {
		return changed;
	}

	public List<Anime> getAnimeNextEp() {
		return nextEp;
	}

	public boolean hasAnimeNextEp() {
		return !nextEp.isEmpty();
	}

	public List<Pair<Anime, Long>> getAnimeChangedPeriod() {
		return changedPeriod;
	}

	public boolean hasAnimeChangedPeriod() {
		return !changedPeriod.isEmpty();
	}

	public boolean hasChangedReleaseAnime() {
		return changed != null && !changed.isEmpty();
	}

	public boolean hasChange() {
		return hasAnimeNextEp() || hasNewAnime() || hasChangedReleaseAnime() || hasRemovedAnime() || hasAnimeChangedPeriod();
	}

	public static AnimeUpdate categoriseUpdates(List<Anime> oldA, List<Anime> newA) {
		AnimeUpdate au = new AnimeUpdate();
		au.listChanges(oldA, newA);
		return au;
	}
}
