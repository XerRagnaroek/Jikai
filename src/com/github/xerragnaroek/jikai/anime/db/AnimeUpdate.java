package com.github.xerragnaroek.jikai.anime.db;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.AnimeDate;
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
	private List<Anime> oldA;
	private List<Anime> newA;
	private List<Anime> removed = new LinkedList<>();
	private List<Anime> removedFiltered;
	private List<Anime> newAnime;
	private List<Anime> newAnimeFiltered;
	private List<Pair<Anime, Long>> changed = new LinkedList<>();
	private List<Pair<Anime, Long>> changedFiltered;
	private List<Anime> nextEp = new ArrayList<>();
	private List<Anime> nextEpFiltered;
	private List<Anime> removedButStillValid = new LinkedList<>();
	private List<Anime> removedButStillValidFiltered;
	private List<Anime> finished = new LinkedList<>();
	private List<Anime> finishedFiltered;
	private List<Anime> hiatus = new LinkedList<>();
	private List<Anime> hiatusFiltered;
	private List<Anime> cancelled = new LinkedList<>();
	private List<Anime> cancelledFiltered;
	private List<Anime> infoChanged = new LinkedList<>();
	private List<Anime> infoChangedFiltered;
	private final Logger log = LoggerFactory.getLogger(AnimeUpdate.class);
	private int size;

	private AnimeUpdate() {}

	private void listChanges(List<Anime> oldAn, List<Anime> newAn) {
		newA = new ArrayList<>(newAn.size());
		newA.addAll(newAn);
		size = newA.size();
		oldA = new ArrayList<>(oldAn.size());
		oldA.addAll(oldAn);
		handleRemoved();
		handleFinished();
		handleCancelled();
		handleInfoChanged();
		handleHiatus();
		handleReleaseChanged();
		handleNextEp();
		handleNewAnime();
		// handleReleasePeriod(newAnime);
		List<String> strings = new LinkedList<>();
		if (hasRemovedAnime()) {
			strings.add("Removed: " + removed.size());
		}
		String msg = "Refreshed AnimeDB\nSize of this update: " + size + "\n" + formatUpdate();
		log.info(msg);
		BotUtils.sendToAllInfoChannels(msg);
	}

	private String formatUpdate() {
		if (hasChange()) {
			List<String> strings = new LinkedList<>();
			if (hasNewAnime()) {
				strings.add("New: " + newAnime.size());
			}
			if (hasFinishedAnime()) {
				strings.add("Finished: " + finished.size());
			}
			if (hasAnimeNextEp()) {
				strings.add("Next Episode: " + nextEp.size());
			}
			if (hasChangedReleaseAnime()) {
				strings.add("Changed Release: " + changed.size());
			}
			if (hasRemovedButStillValid()) {
				strings.add("Not in new data but still valid: " + removedButStillValid.size());
			}
			if (hasCancelledAnime()) {
				strings.add("Cancelled: " + cancelled.size());
			}
			if (hasHiatusAnime()) {
				strings.add("Hiatus: " + hiatus.size());
			}
			if (hasInfoChangedAnime()) {
				strings.add("Info Changed: " + infoChanged.size());
			}
			if (hasRemovedAnime()) {
				strings.add("Removed: " + removed.size());
			}
			return String.join("\n", strings);
		}
		return "No change!";

	}

	private void handleNewAnime() {
		newAnime = newA.stream().filter(a -> !oldA.contains(a)).filter(a -> a.isReleasing() || a.isNotYetReleased()).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<>())));
		newAnime.forEach(a -> log.debug("New anime: {}", a.getTitleRomaji()));
	}

	private void handleRemoved() {
		Set<Anime> removedOldA = oldA.stream().filter(a -> !newA.contains(a)).collect(Collectors.toCollection(() -> new TreeSet<>()));
		// removed = newA.stream().filter(a ->
		// a.getStatus().equals("FINISHED")).collect(Collectors.toList());
		removedOldA.forEach(a -> {
			if (a.hasDataForNextEpisode()) {
				LocalDateTime ldt = a.getNextEpisodeDateTime(ZoneId.systemDefault()).get();
				if (ldt.isAfter(LocalDateTime.now())) {
					removedButStillValid.add(a);
					log.debug("{} isn't in the data but still has a valid next ep on {}!", a.getTitleRomaji(), ldt);
				}
			} else {
				removed.add(a);
			}
			oldA.remove(a);
		});
		removed.forEach(a -> log.debug("{}{} has been removed! status: {}", a.getTitleRomaji(), a.getId(), a.getStatus()));
	}

	private void handleFinished() {
		finished = newA.stream().filter(a -> {
			if (oldA.contains(a)) {
				Anime oldAn = oldA.get(oldA.indexOf(a));
				if (!oldAn.isFinished() & a.isFinished()) {
					return true;
				}
				if (!a.hasDataForNextEpisode() && oldA.contains(a) && (oldAn = oldA.get(oldA.indexOf(a))).hasDataForNextEpisode()) {
					AnimeDate end = a.getEndDate();
					return (oldAn.getNextEpisodeNumber() + 1 == a.getEpisodes()) || (end.hasAll() && end.toZDT(ZoneId.systemDefault()).toLocalDate().isEqual(LocalDate.now()));
				}

			}
			return false;
		}).peek(a -> log.debug("{},{} has finished!", a.getTitleRomaji(), a.getId())).collect(Collectors.toList());
		finished.forEach(this::remAnime);
	}

	private void handleCancelled() {
		cancelled = newA.stream().filter(Anime::isCancelled).filter(a -> !isSameAsOld(a, an -> an.isCancelled())).peek(a -> log.debug("{},{} has been cancelled!", a.getTitleRomaji(), a.getId())).collect(Collectors.toList());
		cancelled.forEach(this::remAnime);
		log.debug("{} anime have been cancelled!", cancelled.size());
	}

	private void handleHiatus() {
		hiatus = newA.stream().filter(Anime::isOnHiatus).filter(a -> !isSameAsOld(a, an -> an.isOnHiatus())).peek(a -> log.debug("{},{} has been put on hiatus!", a.getTitleRomaji(), a.getId())).collect(Collectors.toList());
		hiatus.forEach(this::remAnime);
		log.debug("{} anime have been put on hiatus!", hiatus.size());
	}

	private boolean isSameAsOld(Anime newAn, Function<Anime, Boolean> check) {
		if (oldA.contains(newAn)) {
			Anime old = oldA.get(oldA.indexOf(newAn));
			return check.apply(newAn) == check.apply(old);
		}
		return false;
	}

	private void remAnime(Anime a) {
		newA.remove(a);
		oldA.remove(a);
	}

	private void handleReleaseChanged() {
		newA.stream().filter(Anime::isReleasing).forEach(a -> {
			if (oldA.contains(a)) {
				Anime oldAn = oldA.get(oldA.indexOf(a));
				if (oldAn.hasDataForNextEpisode() && a.hasDataForNextEpisode() && oldAn.getNextEpisodeNumber() == a.getNextEpisodeNumber()) {
					long delay = a.getNextEpisodesAirsAt() - oldAn.getNextEpisodesAirsAt();
					if (delay != 0) {
						changed.add(Pair.of(a, delay));
						log.debug("{} has been {} by {}!", a.getTitleRomaji(), (delay > 0 ? "postponed" : "advanced"), BotUtils.formatSeconds(delay, JikaiLocaleManager.getEN()));
					}
				} else if (oldAn.hasDataForNextEpisode() && !a.hasDataForNextEpisode()) {
					changed.add(Pair.of(a, 0l));
					log.debug("{} has been postponed for an unknown amount of time!", a.getTitleRomaji());
				}
			}
		});
		changed.forEach(p -> remAnime(p.getLeft()));
	}

	private void handleNextEp() {
		newA.forEach(a -> {
			if (oldA.contains(a)) {
				Anime oldAn = oldA.get(oldA.indexOf(a));
				if ((!oldAn.hasDataForNextEpisode() && a.hasDataForNextEpisode()) || (oldAn.hasDataForNextEpisode() && a.hasDataForNextEpisode() && a.getNextEpisodeNumber() > oldAn.getNextEpisodeNumber())) {
					nextEp.add(a);
					log.debug("{} has a new episode coming up! Old ep: {}, new ep: {}", a.getTitleRomaji(), oldAn.getNextEpisodeNumber(), a.getNextEpisodeNumber());
				}
			}
		});
	}

	private void handleInfoChanged() {
		newA.forEach(a -> {
			if (oldA.contains(a)) {
				if (!a.sameInfo(oldA.get(oldA.indexOf(a)))) {
					infoChanged.add(a);
					log.debug("{} has changed info!", a.getTitleRomaji());
				}
			}
		});
	}

	private Duration getDifference(Anime old, Anime newA) {
		return Duration.between(old.getNextEpisodeDateTime(ZoneId.systemDefault()).get(), newA.getNextEpisodeDateTime(ZoneId.systemDefault()).get());
	}

	public void withFilter(Predicate<Anime> filter) {
		removedFiltered = removed.stream().filter(filter).collect(Collectors.toList());
		newAnimeFiltered = newAnime.stream().filter(filter).collect(Collectors.toList());
		changedFiltered = changed.stream().filter(p -> filter.test(p.getLeft())).collect(Collectors.toList());
		nextEpFiltered = nextEp.stream().filter(filter).collect(Collectors.toList());
		removedButStillValidFiltered = removedButStillValid.stream().filter(filter).collect(Collectors.toList());
		finishedFiltered = finished.stream().filter(filter).collect(Collectors.toList());
		hiatusFiltered = hiatus.stream().filter(filter).collect(Collectors.toList());
		cancelledFiltered = cancelled.stream().filter(filter).collect(Collectors.toList());
		infoChangedFiltered = infoChanged.stream().filter(filter).collect(Collectors.toList());
	}

	public List<Anime> getRemovedAnime() {
		return removedFiltered == null ? removed : removedFiltered;
	}

	public boolean hasRemovedAnime() {
		return removedFiltered == null ? !removed.isEmpty() : !removedFiltered.isEmpty();
	}

	public List<Anime> getRemovedButStillValid() {
		return removedButStillValidFiltered == null ? removedButStillValid : removedButStillValidFiltered;
	}

	public boolean hasRemovedButStillValid() {
		return removedButStillValidFiltered == null ? !removedButStillValid.isEmpty() : !removedButStillValidFiltered.isEmpty();
	}

	public List<Anime> getNewAnime() {
		return newAnimeFiltered == null ? newAnime : newAnimeFiltered;
	}

	public boolean hasNewAnime() {
		return newAnimeFiltered == null ? !newAnime.isEmpty() : !newAnimeFiltered.isEmpty();
	}

	/**
	 * Delay > 0 = postponed.
	 * 
	 * @return
	 */
	public List<Pair<Anime, Long>> getChangedReleaseAnime() {
		return changedFiltered == null ? changed : changedFiltered;
	}

	public boolean hasChangedReleaseAnime() {
		return changedFiltered == null ? (changed != null && !changed.isEmpty()) : !changedFiltered.isEmpty();
	}

	public List<Anime> getAnimeNextEp() {
		return nextEpFiltered == null ? nextEp : nextEpFiltered;
	}

	public boolean hasAnimeNextEp() {
		return nextEpFiltered == null ? !nextEp.isEmpty() : !nextEpFiltered.isEmpty();
	}

	public List<Anime> getFinishedAnime() {
		return finishedFiltered == null ? finished : finishedFiltered;
	}

	public boolean hasFinishedAnime() {
		return finishedFiltered == null ? !finished.isEmpty() : !finishedFiltered.isEmpty();
	}

	public List<Anime> getCancelledAnime() {
		return cancelledFiltered == null ? cancelled : cancelledFiltered;
	}

	public boolean hasCancelledAnime() {
		return cancelledFiltered == null ? !cancelled.isEmpty() : !cancelledFiltered.isEmpty();
	}

	public List<Anime> getHiatusAnime() {
		return hiatusFiltered == null ? hiatus : hiatusFiltered;
	}

	public boolean hasHiatusAnime() {
		return hiatusFiltered == null ? !hiatus.isEmpty() : !hiatusFiltered.isEmpty();
	}

	public List<Anime> getInfoChangedAnime() {
		return infoChangedFiltered == null ? infoChanged : infoChangedFiltered;
	}

	public boolean hasInfoChangedAnime() {
		return infoChangedFiltered == null ? !infoChanged.isEmpty() : !infoChangedFiltered.isEmpty();
	}

	public boolean hasChange() {
		return hasAnimeNextEp() || hasNewAnime() || hasChangedReleaseAnime() || hasRemovedAnime() || hasFinishedAnime() || hasCancelledAnime() || hasHiatusAnime() || hasInfoChangedAnime();
	}

	public static AnimeUpdate categoriseUpdates(List<Anime> oldA, List<Anime> newA) {
		AnimeUpdate au = new AnimeUpdate();
		au.listChanges(oldA, newA);
		return au;
	}
}
