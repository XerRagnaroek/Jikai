package com.github.xerragnaroek.jikai.anime.db;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.util.BoundedArrayList;

/**
 * 
 */
public class AnimeReleaseTracker {

	private static AnimeReleaseTracker instance;
	// id -> last ep unix time
	private Map<Integer, BoundedArrayList<Long>> releaseTimes = Collections.synchronizedMap(new TreeMap<>());
	private int listMax = 3;

	private final Logger log = LoggerFactory.getLogger(AnimeReleaseTracker.class);

	private AnimeReleaseTracker() {
		AnimeDB.runOnDBUpdate(au -> au.getRemovedAnime().forEach(this::removeAnime));
	}

	public static AnimeReleaseTracker getInstance() {
		return instance == null ? (instance = new AnimeReleaseTracker()) : instance;
	}

	/**
	 * @return the difference in minutes. dif > 0 = releases later, dif < 0 = releases earlier
	 */
	public long addAnime(Anime a) {
		log.debug("Adding new anime \"{}\",{}", a.getTitleRomaji(), a.getId());
		long[] wrapper = new long[] { 0 };
		releaseTimes.compute(a.getId(), (id, l) -> {
			if (l == null) {
				l = new BoundedArrayList<>(listMax);
				log.debug("First entry!");
			}
			long nextEpAirsAt = a.getNextEpisodesAirsAt();
			if (l.contains(nextEpAirsAt)) {
				// already in queue, no reason to mess around
				return l;
			}
			long dif = 0;
			if (l.size() > 2) {
				// last release time is also the last in the list, list goes oldest -> newest so we need to iterate
				// backwards over it to go newest -> oldest
				ReverseListIterator<Long> it = l.reverseListIterator();
				long lastEp = it.next();
				long curEp = nextEpAirsAt;
				long curPeriod = LocalDateTime.ofEpochSecond(lastEp, 0, ZoneOffset.UTC).until(LocalDateTime.ofEpochSecond(curEp, 0, ZoneOffset.UTC), ChronoUnit.MINUTES);
				log.debug("Current release period is {} minutes", curPeriod);
				while (it.hasNext()) {
					long newer = curEp;
					long older = it.next();
					long oldPeriod = LocalDateTime.ofEpochSecond(older, 0, ZoneOffset.UTC).until(LocalDateTime.ofEpochSecond(newer, 0, ZoneOffset.UTC), ChronoUnit.MINUTES);
					if ((dif = curPeriod - oldPeriod) == 0) {
						// not a new release period
						log.debug("Not a new period.");
						break;
					}
				}
				if (dif != 0) {
					log.debug("Current period differs by {} minutes", dif);
				}
			}
			wrapper[0] = dif;
			l.add(nextEpAirsAt);
			return l;
		});
		return wrapper[0];
	}

	public void addAllAnime(Collection<Anime> col) {
		col.forEach(this::addAnime);
	}

	public Map<Integer, BoundedArrayList<Long>> getMap() {
		TreeMap<Integer, BoundedArrayList<Long>> tmp = new TreeMap<>();
		tmp.putAll(releaseTimes);
		return tmp;
	}

	public void loadMap(Map<Integer, BoundedArrayList<Long>> map) {
		Set<Integer> loadedIds = AnimeDB.getLoadedAnime().stream().map(Anime::getId).collect(Collectors.toSet());
		map.keySet().retainAll(loadedIds);
		map.forEach((i, l) -> l.setMaxCapacity(listMax));
		log.debug("Loaded {} entries", map.size());
		map.forEach((i, l) -> {
			if (releaseTimes.containsKey(i)) {
				BoundedArrayList<Long> curL = releaseTimes.get(i);
				if (curL != null && !curL.isEmpty()) {
					long latest = curL.get(curL.size() - 1);
					if (latest != l.get(l.size() - 1)) {
						l.add(latest);
					}
					releaseTimes.put(i, l);
				}
			} else {
				releaseTimes.put(i, l);
			}
		});
	}

	public void removeAnime(Anime a) {
		releaseTimes.remove(a.getId());
		log.debug("Removed anime {},{}", a.getTitleRomaji(), a.getId());
	}
}
