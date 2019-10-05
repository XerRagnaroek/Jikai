package com.xerragnaroek.bot.timer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.db.AnimeBase;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.data.UpdatableData;

import net.dv8tion.jda.api.entities.Guild;

public class ReleaseTimeKeeper implements UpdatableData {
	private final String gId;
	private Map<String, ZonedDateTime> lastMentioned;
	private final Logger log;
	private final AtomicBoolean changed = new AtomicBoolean(false);

	ReleaseTimeKeeper(String gId) {
		this.gId = gId;
		lastMentioned = new TreeMap<>();
		log = LoggerFactory.getLogger(ReleaseTimeKeeper.class + "#" + gId);
	}

	private void updateReactedAnimes(ZonedDateTime now, Guild g, ZoneId zone, boolean ignoreThresholds) {
		Set<String> reactedAnimes = Core.ALRHM.get(g).getReactedAnimes();
		AnimeBase.getSeasonalAnimesAdjusted(zone).forEach(adt -> {
			String title = adt.getAnime().title;
			if (reactedAnimes.contains(title)) {
				log.debug("{} is a reacted to anime", title);
				ReleaseTime time = RTKManager.whenWillAnimeAir(now, adt, zone);
				if (RTKManager.meetsTresholds(time, lastMentioned.get(title)) || ignoreThresholds) {
					log.debug("{} met threshold with {}", title, time);
					RoleMentioner.mentionUpdate(g, adt, time, m -> addToLastMentioned(title, now));
				}
			}
		});
	}

	private void addToLastMentioned(String title, ZonedDateTime time) {
		lastMentioned.put(title, time);
		changed.set(true);
	}

	private void updateAllAnimes(ZonedDateTime now, Guild g, ZoneId zone, boolean ignoreThresholds) {
		AnimeBase.getSeasonalAnimesAdjusted(zone).forEach(adt -> {
			String title = adt.getAnime().title;
			ReleaseTime time = RTKManager.whenWillAnimeAir(now, adt, zone);
			if (RTKManager.meetsTresholds(time, lastMentioned.get(title)) || ignoreThresholds) {
				log.debug("{} met threshold with {}", title, time);
				RoleMentioner.mentionUpdate(g, adt, time, m -> addToLastMentioned(title, now));
			} else {
				log.debug("{} didn't meet threshold with {}", title, time);
			}
		});
	}

	public void updateAnimes(boolean ignoreThresholds, boolean allAnimes) {
		log.info("Sending anime release times to guild {}{}{}", gId, (ignoreThresholds ? " ignoring thresholds" : ""), (allAnimes ? " for all animes" : ""));
		ZoneId zone = Core.GDM.get(gId).getTimeZone();
		Guild g = Core.JDA.getGuildById(gId);
		updateAnimesZone(g, zone, ignoreThresholds, allAnimes);
	}

	private void updateAnimesZone(Guild g, ZoneId zone, boolean ignoreThresholds, boolean allAnimes) {
		ZonedDateTime now = ZonedDateTime.now(zone);
		if (allAnimes) {
			updateAllAnimes(now, g, zone, ignoreThresholds);
		} else {
			updateReactedAnimes(now, g, zone, ignoreThresholds);
		}
	}

	@Override
	public boolean hasUpdateFlagAndReset() {
		return changed.getAndSet(false);
	}

	public Map<String, ZonedDateTime> getLastMentionedMap() {
		return new TreeMap<>(lastMentioned);
	}

	void setLastMentionedMap(Map<String, ZonedDateTime> map) {
		lastMentioned = map;
	}

}
