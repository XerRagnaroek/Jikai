/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.xerragnaroek.jikai.jikai;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.xerragnaroek.jikai.anime.alrh.ALRHManager;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

public class JikaiManager extends Manager<Jikai> {
	final JikaiDataManager jdm = new JikaiDataManager();
	final ALRHManager alrhm = new ALRHManager();
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
		sm.remove(id);
	}
}
