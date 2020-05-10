
package com.github.xerragnaroek.jikai.jikai;

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
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class JikaiDataManager extends Manager<JikaiData> {

	private BotData botData = new BotData(true);

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
		}
	}

	void loadBotData(Path loc) {
		ObjectMapper mapper = new ObjectMapper();
		botData = readFromPath(loc, mapper, BotData.class);
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
