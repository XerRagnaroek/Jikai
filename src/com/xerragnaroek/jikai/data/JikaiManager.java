package com.xerragnaroek.jikai.data;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.xerragnaroek.jikai.anime.alrh.ALRHManager;
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.xerragnaroek.jikai.commands.CommandHandlerManager;
import com.xerragnaroek.jikai.timer.RTKManager;
import com.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

public class JikaiManager extends Manager<Jikai> {
	final JikaiDataManager jdm = new JikaiDataManager();
	final ALRHManager alrhm = new ALRHManager();
	final CommandHandlerManager chm = new CommandHandlerManager();
	final RTKManager rtkm = new RTKManager();
	final ScheduleManager sm = new ScheduleManager();

	public JikaiManager() {
		super(Jikai.class);
	}

	@Override
	public void init() {
		jdm.init();
		jdm.getGuildIds().forEach(this::registerNew);
		AnimeDB.init();
		AnimeDB.waitUntilLoaded();
		chm.init();
		rtkm.init();
		alrhm.init();
		sm.init();
		rtkm.startReleaseUpdateThread();
	}

	@Override
	protected Jikai makeNew(String gId) {
		return new Jikai(gId, this);
	}

	public void startSaveThread(long delay) {
		jdm.startSaveThread(delay, TimeUnit.MINUTES);
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

	public RTKManager getRTKM() {
		return rtkm;
	}

	public ScheduleManager getSM() {
		return sm;
	}

	public boolean isKnownGuild(String gId) {
		return impls.keySet().contains(gId);
	}

	public boolean isKnownGuild(Guild g) {
		return impls.keySet().contains(g.getId());
	}

	public Set<String> getGuildIds() {
		return impls.keySet();
	}
}
