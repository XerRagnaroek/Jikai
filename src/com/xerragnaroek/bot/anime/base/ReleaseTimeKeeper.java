package com.xerragnaroek.bot.anime.base;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.alrh.ALRHManager;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.entities.Guild;

public class ReleaseTimeKeeper {

	private static int dayThreshold = 1;
	private static int hourThreshold = 4;
	private static final Logger log = LoggerFactory.getLogger(ReleaseTimeKeeper.class);

	public static ReleaseTime whenWillAnimeAir(ZonedDateTime curTime, AnimeDayTime adt, ZoneId tz) {
		long diff = curTime.until(adt.getZonedDateTime(), ChronoUnit.MILLIS);
		ReleaseTime time = new ReleaseTime(diff);
		log.debug("TimeZone={}; {} airs in {}", tz, adt.getAnime().title, time);
		return time;
	}

	public static void updateAnimes(boolean ignoreThresholds, boolean allAnimes) {
		log.info("Sending anime updates");
		GuildDataManager.timeZoneGuildMap().forEach((zone, gList) -> {
			log.debug("Sending to {} guilds for ZoneId {}", gList.size(), zone);
			gList.forEach(id -> {
				Guild g = Core.getJDA().getGuildById(id);
				updateAnimesForGuild(g, ignoreThresholds, allAnimes);
			});
		});
	}

	private static void updateReactedAnimes(ZonedDateTime now, Guild g, ZoneId zone, boolean ignoreThresholds) {
		Set<String> reactedAnimes = ALRHManager.getAnimeListReactionHandlerForGuild(g).getReactedAnimes();
		AnimeBase.getSeasonalAnimesAdjusted(zone).forEach(adt -> {
			String title = adt.getAnime().title;
			if (reactedAnimes.contains(title)) {
				log.debug("{} is a reacted to anime", title);
				ReleaseTime time = ReleaseTimeKeeper.whenWillAnimeAir(now, adt, zone);
				if (meetsTresholds(time) || ignoreThresholds) {
					log.debug("{} met threshold with {}", title, time);
					RoleMentioner.mentionUpdate(g, adt.getAnime(), time);
				}
			}
		});
	}

	private static void updateAllAnimes(ZonedDateTime now, Guild g, ZoneId zone, boolean ignoreThresholds) {
		AnimeBase.getSeasonalAnimesAdjusted(zone).forEach(adt -> {
			String title = adt.getAnime().title;
			ReleaseTime time = ReleaseTimeKeeper.whenWillAnimeAir(now, adt, zone);
			if (meetsTresholds(time) || ignoreThresholds) {
				log.debug("{} met threshold with {}", title, time);
				RoleMentioner.mentionUpdate(g, adt.getAnime(), time);
			}
		});
	}

	public static void updateAnimesForGuild(Guild g, boolean ignoreThresholds, boolean allAnimes) {
		ZoneId zone = GuildDataManager.getDataForGuild(g).getTimeZone();
		updateAnimesForGuildZone(g, zone, ignoreThresholds, allAnimes);
	}

	private static void updateAnimesForGuildZone(Guild g, ZoneId zone, boolean ignoreThresholds, boolean allAnimes) {
		ZonedDateTime now = ZonedDateTime.now(zone);
		if (allAnimes) {
			updateAllAnimes(now, g, zone, ignoreThresholds);
		} else {
			updateReactedAnimes(now, g, zone, ignoreThresholds);
		}
	}

	private static boolean meetsTresholds(ReleaseTime time) {
		if (time.days() == 0 && time.days() <= hourThreshold) {
			return true;
		} else {
			return time.days() == dayThreshold;
		}
	}
}
