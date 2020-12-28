package com.github.xerragnaroek.jikai.anime.db;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

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

	private AnimeReleaseTracker() {}

	public static AnimeReleaseTracker getInstance() {
		return instance == null ? (instance = new AnimeReleaseTracker()) : instance;
	}

	/**
	 * @return the difference in hours. dif > 0 = releases later, dif < 0 = releases earlier
	 */
	public long addAnime(Anime a) {
		log.debug("Adding new anime \"{}\",", a.getTitleRomaji(), a.getId());
		long[] wrapper = new long[1];
		releaseTimes.compute(a.getId(), (id, q) -> {
			if (q == null) {
				q = new CircularFifoQueue<>(queueMax);
			}
			long nextEpAirsAt = a.getNextEpisodesAirsAt();
			long dif = 0;
			if (q.size() > 2) {
				Iterator<Long> it = q.iterator();
				long lastEp = it.next();
				long curEp = nextEpAirsAt;
				long curPeriod = LocalDateTime.ofEpochSecond(lastEp, 0, ZoneOffset.UTC).until(LocalDateTime.ofEpochSecond(curEp, 0, ZoneOffset.UTC), ChronoUnit.HOURS);
				log.debug("Current release period is {} hours", curPeriod);
				while (it.hasNext()) {
					lastEp = curEp;
					curEp = it.next();
					long p = LocalDateTime.ofEpochSecond(lastEp, 0, ZoneOffset.UTC).until(LocalDateTime.ofEpochSecond(curEp, 0, ZoneOffset.UTC), ChronoUnit.HOURS);
					if ((dif = curEp - p) == 0) {
						// not a new release period
						log.debug("Not a new period.");
						break;
					}
				}
				if (dif != 0) {
					log.debug("Current period differs by {} hours", dif);
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

	public void setMap(Map<Integer, Queue<Long>> map) {
		TreeMap<Integer, Queue<Long>> tmp = new TreeMap<>();
		tmp.putAll(map);
		releaseTimes = Collections.synchronizedMap(tmp);
	}
}
