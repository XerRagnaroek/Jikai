package com.xerragnaroek.jikai.timer;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Manager;

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
		if (diff < 0) {
			adt.updateZDTToNextAirDate();
			diff = curTime.until(adt.getZonedDateTime(), ChronoUnit.MILLIS);
		}
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
		if (hourThreshold == 0 && dayThreshold == 0) {
			return false;
		}
		if ((time.days() <= dayThreshold && time.hours() <= hourThreshold) || (hourThreshold == 0 && time.days() <= dayThreshold)) {
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

	public long getUpdateRate() {
		return updateRate;
	}

	public int getHourThreshold() {
		return hourThreshold;
	}

	public int getDayThreshold() {
		return dayThreshold;
	}

	public void startReleaseUpdateThread() {
		if (!threadRunning) {
			LocalTime now = LocalTime.now();
			LocalTime nextHour = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
			long minDif = now.until(nextHour, ChronoUnit.MINUTES);
			exec.scheduleAtFixedRate(() -> {
				impls.forEach((id, rtk) -> {
					if (Core.GDM.get(id).hasCompletedSetup()) {
						CompletableFuture.runAsync(() -> rtk.updateAnimes(false, false)).whenComplete((v, e) -> {
							if (e != null) {
								BotUtils.logAndSendToDev(Core.ERROR_LOG, "", e);
							}
						});
					}
				});
			}, minDif, updateRate, TimeUnit.MINUTES);
			threadRunning = true;
			log.info("Started release update thread. First update will be at " + nextHour.toString() + " in " + minDif + " minutes");
		}
	}

	@Override
	protected ReleaseTimeKeeper makeNew(String gId) {
		return new ReleaseTimeKeeper(gId);
	}

}
