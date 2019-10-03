package com.xerragnaroek.bot.timer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.base.AnimeDayTime;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.util.BotUtils;
import com.xerragnaroek.bot.util.Manager;

public class RTKManager extends Manager<ReleaseTimeKeeper> {

	public RTKManager() {
		super(ReleaseTimeKeeper.class);
	}

	private static int dayThreshold = 0;
	private static int hourThreshold = 4;
	private static long updateRate = 60;
	private final Logger log = LoggerFactory.getLogger(RTKManager.class);
	private Map<String, Map<String, String>> initMap = new TreeMap<>();
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private boolean threadRunning = false;

	public static ReleaseTime whenWillAnimeAir(ZonedDateTime curTime, AnimeDayTime adt, ZoneId tz) {
		long diff = curTime.until(adt.getZonedDateTime(), ChronoUnit.MILLIS);
		ReleaseTime time = new ReleaseTime(diff);
		LoggerFactory.getLogger(RTKManager.class).debug("TimeZone={}; {} airs in {}", tz, adt.getAnime().title, time);
		return time;
	}

	@Override
	public void init() {
		log.debug("Initializing RTKs");
		if (initMap != null && !initMap.isEmpty()) {
			initMap.forEach((str, map) -> {
				registerNew(str).setLastMentionedMap(mapZDTs(map));
			});
			initMap.clear();
			initMap = null;
		}
	}

	private Map<String, ZonedDateTime> mapZDTs(Map<String, String> map) {
		Map<String, ZonedDateTime> tmp = new TreeMap<>();
		if (map != null) {
			map.forEach((str, zdt) -> tmp.put(str, ZonedDateTime.parse(zdt)));
		}
		return tmp;
	}

	static boolean meetsTresholds(ReleaseTime time, ZonedDateTime lastMentioned) {
		//airs today and is less or equal than %hourThreshold% away
		if (time.days() == dayThreshold && time.hours() <= hourThreshold) {
			return true;
		} else {
			//hasn't been mentioned today, allows for a daily update
			return !BotUtils.isToday(lastMentioned);
		}
	}

	public void addToInitMap(String gId, Map<String, String> map) {
		initMap.put(gId, map);
	}

	public void setUpdateRate(long rate) {
		updateRate = rate;
	}

	public void setDayThreshold(int thresh) {
		dayThreshold = thresh;
	}

	public void setHourThreshold(int thresh) {
		hourThreshold = thresh;
	}

	public void startReleaseUpdateThread() {
		if (!threadRunning) {
			log.info("Started release update thread");
			exec.scheduleAtFixedRate(() -> {
				impls.forEach((id, rtk) -> {
					if (Core.GDM.get(id).hasCompletedSetup()) {
						ForkJoinPool.commonPool().execute(() -> rtk.updateAnimes(false, false));
					}
				});
			}, updateRate, updateRate, TimeUnit.MINUTES);
			threadRunning = true;
		}
	}

	@Override
	protected ReleaseTimeKeeper makeNew(String gId) {
		return new ReleaseTimeKeeper(gId);
	}

}
