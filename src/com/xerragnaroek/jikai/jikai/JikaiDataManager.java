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
package com.xerragnaroek.jikai.jikai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class JikaiDataManager extends Manager<JikaiData> {

	private BotData botData = new BotData(false);

	public JikaiDataManager() {
		super(JikaiData.class);
	}

	public BotData getBotData() {
		return botData;
	}

	public Set<Long> getGuildIds() {
		Set<Long> copy = new TreeSet<>(impls.keySet());
		return copy;
	}

	public boolean hasCompletedSetup(Guild g) {
		JikaiData gd = get(g);

		return gd != null && gd.hasCompletedSetup();
	}

	public boolean isKnownGuild(long gId) {
		return getGuildIds().contains(gId);
	}

	@Override
	protected JikaiData makeNew(long gId) {
		return new JikaiData(gId, true);
	}

	private void initBotData() {
		botData = new BotData(true);
	}

	void loadData(Path p) {
		ObjectMapper mapper = new ObjectMapper();
		JikaiData jd = readFromPath(p, mapper, JikaiData.class);
		if (jd != null) {
			impls.put(jd.getGuildId(), jd);
			Jikai.addTimeZone(jd.getTimeZone());
		}
	}

	void loadBotData(Path loc) {
		ObjectMapper mapper = new ObjectMapper();
		botData = readFromPath(loc, mapper, BotData.class);
		Jikai.addTimeZone(botData.getDefaultTimeZone());
	}

	private <T> T readFromPath(Path p, ObjectMapper mapper, Class<T> clazz) {
		try {
			return mapper.readValue(Files.readString(p), clazz);
		} catch (IOException e) {
			BotUtils.logAndSendToDev(log, "Failed reading json from " + p, e);
		}
		return null;
	}

	/**
	 * Save all data. Gets called every 10 minutes.
	 */
	void save(boolean now) {
		log.debug("Saving data...");
		AtomicInteger saved = new AtomicInteger(0);
		impls.values().forEach(gd -> {
			if (gd.save(now)) {
				saved.incrementAndGet();
			}
		});
		if (botData.save()) {
			saved.incrementAndGet();
		}
		if (saved.get() != 0) {
			log.info("Saved {} JikaiData", saved.get());
		} else {
			log.debug("No data had to be saved");
		}
	}

	public Set<JikaiData> data() {
		return Collections.synchronizedSet(new HashSet<>(impls.values()));
	}

	@Override
	public void init() {

	}

	@Override
	public void remove(long id) {
		try {
			Files.deleteIfExists(Paths.get(Core.DATA_LOC.toString(), "/" + id + ".json"));
		} catch (IOException e) {
			Core.ERROR_LOG.error("Couldn't delete obsolete data file!", e);
		}
		super.remove(id);
	}
}
