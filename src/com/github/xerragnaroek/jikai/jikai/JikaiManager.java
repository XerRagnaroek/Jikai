
package com.github.xerragnaroek.jikai.jikai;

import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.list.ALRHManager;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Manager;
import com.github.xerragnaroek.jikai.util.prop.MapProperty;

import net.dv8tion.jda.api.entities.Guild;

public class JikaiManager extends Manager<Jikai> {
	final JikaiDataManager jdm = new JikaiDataManager();
	final ALRHManager alrhm = new ALRHManager();
	private MapProperty<ZoneId, Set<Long>> timeZones = new MapProperty<>();

	public JikaiManager() {
		super(Jikai.class);
	}

	@Override
	public void init() {
		JikaiLocaleManager.loadLocales();
		AnimeDB.init();
		AnimeDB.waitUntilLoaded();
		AnimeDB.startUpdateThread(true);
		JikaiUserManager.init();
		JikaiIO.load();
		jdm.getGuildIds().forEach(this::registerNew);
		alrhm.init();
		ScheduleManager.init();
		Core.CUR_SEASON.onChange((ov, nv) -> updateJikaisSeasonChanged(nv));
		JikaiUserManager.getInstance().cachePrivateChannels();
		log.info("Jikai initialized!");
	}

	@Override
	public Jikai registerNew(long id) {
		if (Core.JDA.getGuildById(id) != null) {
			return super.registerNew(id);
		}
		return null;
	}

	private void updateJikaisSeasonChanged(String newSeason) {
		log.info("Season has changed to '" + newSeason + "', updating Jikias");
		BotUtils.sendToAllAnimeChannels("The season has changed! We're now in\n **" + newSeason + "**!\n");
	}

	@Override
	protected Jikai makeNew(long gId) {
		return new Jikai(gId, this);
	}

	public void startSaveThread(long delay) {
		JikaiIO.startSaveThread(delay, TimeUnit.MINUTES);
	}

	public JikaiDataManager getJDM() {
		return jdm;
	}

	public ALRHManager getALHRM() {
		return alrhm;
	}

	public boolean isKnownGuild(long gId) {
		return impls.keySet().contains(gId);
	}

	public boolean isKnownGuild(Guild g) {
		return impls.keySet().contains(g.getIdLong());
	}

	public Set<Long> getGuildIds() {
		return impls.keySet();
	}

	public void addTimeZone(ZoneId id, long gId) {
		timeZones.compute(id, (zid, s) -> {
			s = s == null ? new HashSet<>() : s;
			s.add(gId);
			return s;
		});
	}

	public void removeTimeZone(ZoneId id, long gId) {
		timeZones.compute(id, (zid, s) -> {
			s.remove(gId);
			return s.isEmpty() ? null : s;
		});
	}

	public Set<ZoneId> getUsedTimeZones() {
		return new HashSet<>(timeZones.keySet());
	}

	public Set<Jikai> getJikaisWithTimeZone(ZoneId z) {
		if (timeZones.containsKey(z)) {
			return timeZones.get(z).stream().map(this::get).collect(Collectors.toSet());
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public void remove(long id) {
		super.remove(id);
		jdm.remove(id);
		alrhm.remove(id);
	}
}
