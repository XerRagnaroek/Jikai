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
import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;

public class RTKManager {

	private static int dayThreshold = 1;
	private static int hourThreshold = 4;
	private static final Logger log = LoggerFactory.getLogger(RTKManager.class);
	private static final Map<String, ReleaseTimeKeeper> keeper = new TreeMap<>();
	private static Map<String, Map<String, String>> initMap = new TreeMap<>();
	private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private static boolean threadRunning = false;

	public static ReleaseTime whenWillAnimeAir(ZonedDateTime curTime, AnimeDayTime adt, ZoneId tz) {
		long diff = curTime.until(adt.getZonedDateTime(), ChronoUnit.MILLIS);
		ReleaseTime time = new ReleaseTime(diff);
		log.debug("TimeZone={}; {} airs in {}", tz, adt.getAnime().title, time);
		return time;
	}

	public static ReleaseTimeKeeper getKeeperForGuild(String gId) {
		log.debug("Getting RTK for guild {}", gId);
		ReleaseTimeKeeper rtk = keeper.get(gId);
		return rtk;
	}

	public static ReleaseTimeKeeper getKeeperForGuild(Guild g) {
		return getKeeperForGuild(g.getId());
	}

	public static void init() {
		log.debug("Initializing RTKs");
		if (initMap != null && !initMap.isEmpty()) {
			initMap.forEach((str, map) -> {
				registerNewRTK(str).setLastMentionedMap(mapZDTs(map));
			});
			initMap.clear();
			initMap = null;
		}
	}

	private static Map<String, ZonedDateTime> mapZDTs(Map<String, String> map) {
		Map<String, ZonedDateTime> tmp = new TreeMap<>();
		if (map != null) {
			map.forEach((str, zdt) -> tmp.put(str, ZonedDateTime.parse(zdt)));
		}
		return tmp;
	}

	public static ReleaseTimeKeeper registerNewRTK(String gId) {
		ReleaseTimeKeeper rtk;
		if (!keeper.containsKey(gId)) {
			log.debug("Registered new RTK for guild {}", gId);
			rtk = new ReleaseTimeKeeper(gId);
			keeper.put(gId, rtk);
		} else {
			rtk = keeper.get(gId);
		}
		return rtk;
	}

	static boolean meetsTresholds(ReleaseTime time, ZonedDateTime lastMentioned) {
		//airs today and is less or equal than %hourThreshold% away
		if (time.days() == 0 && time.days() <= hourThreshold) {
			return true;
		} else {
			//hasn't been mentioned today, allows for a daily update
			return !BotUtils.isToday(lastMentioned);
		}
	}

	public static void addToInitMap(String gId, Map<String, String> map) {
		initMap.put(gId, map);
	}

	public static void startReleaseUpdateThread() {
		if (!threadRunning) {
			log.info("Started release update thread");
			exec.scheduleAtFixedRate(() -> {
				keeper.forEach((id, rtk) -> {
					if (GuildDataManager.getDataForGuild(id).hasCompletedSetup()) {
						ForkJoinPool.commonPool().execute(() -> rtk.updateAnimes(false, false));
					}
				});
			}, 30, 30, TimeUnit.SECONDS);
			threadRunning = true;
		}
	}

}
