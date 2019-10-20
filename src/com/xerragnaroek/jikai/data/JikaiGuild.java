package com.xerragnaroek.jikai.data;

import static com.xerragnaroek.jikai.core.Core.ALRHM;
import static com.xerragnaroek.jikai.core.Core.GDM;
import static com.xerragnaroek.jikai.core.Core.RTKM;
import static com.xerragnaroek.jikai.core.Core.SM;

import com.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.xerragnaroek.jikai.anime.schedule.Scheduler;
import com.xerragnaroek.jikai.timer.ReleaseTimeKeeper;

public class JikaiGuild {

	private final GuildData gd;
	private final ALRHandler alrh;
	private final ReleaseTimeKeeper rtk;
	private final Scheduler sch;

	public JikaiGuild(String g) {
		gd = GDM.get(g);
		gd.setJikaiGuild(this);
		alrh = ALRHM.get(g);
		alrh.setJikaiGuild(this);
		rtk = RTKM.get(g);
		rtk.setJikaiGuild(this);
		sch = SM.get(g);
		sch.setJikaiGuild(this);
	}
}
