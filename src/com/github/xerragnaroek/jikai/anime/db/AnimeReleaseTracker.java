package com.github.xerragnaroek.jikai.anime.db;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;

/**
 * 
 */
public class AnimeReleaseTracker {

	private static AnimeReleaseTracker instance;
	// id -> last ep unix time
	private Map<Integer, Queue<Long>> releaseTimes = Collections.synchronizedMap(new TreeMap<>());
	private int queueMax = 3;

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
		releaseTimes.compute(a.getId(), (id, q) -> {
			if (q == null) {
				q = new CircularFifoQueue<>(queueMax);
				log.debug("First entry!");
			}
			long nextEpAirsAt = a.getNextEpisodesAirsAt();
			if (q.contains(nextEpAirsAt)) {
				// already in queue, no reason to mess around
				return q;
			}
			long dif = 0;
			if (q.size() > 2) {
				Iterator<Long> it = q.iterator();

				long lastEp = it.next();
				long curEp = nextEpAirsAt;
				long curPeriod = LocalDateTime.ofEpochSecond(lastEp, 0, ZoneOffset.UTC).until(LocalDateTime.ofEpochSecond(curEp, 0, ZoneOffset.UTC), ChronoUnit.MINUTES);
				log.debug("Current release period is {} minutes", curPeriod);
				while (it.hasNext()) {
					lastEp = curEp;
					curEp = it.next();
					long p = LocalDateTime.ofEpochSecond(lastEp, 0, ZoneOffset.UTC).until(LocalDateTime.ofEpochSecond(curEp, 0, ZoneOffset.UTC), ChronoUnit.MINUTES);
					if ((dif = curEp - p) == 0) {
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
			q.add(nextEpAirsAt);
			return q;
		});
		return wrapper[0];
	}

	public void addAllAnime(Collection<Anime> col) {
		col.forEach(this::addAnime);
	}

	public Map<Integer, Queue<Long>> getMap() {
		TreeMap<Integer, Queue<Long>> tmp = new TreeMap<>();
		tmp.putAll(releaseTimes);
		return tmp;
	}

	public void loadMap(Map<Integer, Queue<Long>> map) {
		Set<Integer> loadedIds = AnimeDB.getLoadedAnime().stream().map(Anime::getId).collect(Collectors.toSet());
		map.keySet().retainAll(loadedIds);
		TreeMap<Integer, Queue<Long>> tmp = new TreeMap<>();
		map.forEach((i, q) -> {
			Queue<Long> tmpQ = new CircularFifoQueue<>(queueMax);
			tmpQ.addAll(q);
			tmp.put(i, tmpQ);
		});
		log.debug("Loaded {} entries", tmp.size());
		tmp.forEach((i, q) -> {
			if (releaseTimes.containsKey(i)) {
				Queue<Long> curQ = releaseTimes.get(i);
				if (curQ != null && !curQ.isEmpty()) {
					long latest = curQ.peek();
					if (latest != q.peek()) {
						q.add(latest);
					}
					releaseTimes.put(i, q);
				}
			} else {
				releaseTimes.put(i, q);
			}
		});
	}

	public void removeAnime(Anime a) {
		releaseTimes.remove(a.getId());
		log.debug("Removed anime {},{}", a.getTitleRomaji(), a.getId());
	}
}
