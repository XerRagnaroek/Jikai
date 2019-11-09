package com.xerragnaroek.jikai.jikai;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.xerragnaroek.jikai.anime.alrh.ALRHManager;
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.xerragnaroek.jikai.commands.CommandHandlerManager;
import com.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

public class JikaiManager extends Manager<Jikai> {
	final JikaiDataManager jdm = new JikaiDataManager();
	final ALRHManager alrhm = new ALRHManager();
	final CommandHandlerManager chm = new CommandHandlerManager();
	final ScheduleManager sm = new ScheduleManager();

	public JikaiManager() {
		super(Jikai.class);
	}

	@Override
	public void init() {
		AnimeDB.init();
		AnimeDB.waitUntilLoaded();
		JikaiIO.load();
		jdm.getGuildIds().forEach(this::registerNew);
		chm.init();
		alrhm.init();
		sm.init();
		log.info("Jikai initialized!");
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

	public CommandHandlerManager getCHM() {
		return chm;
	}

	public ScheduleManager getSM() {
		return sm;
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

	@Override
	public void remove(long id) {
		super.remove(id);
		jdm.remove(id);
		alrhm.remove(id);
		chm.remove(id);
		sm.remove(id);
	}
}
