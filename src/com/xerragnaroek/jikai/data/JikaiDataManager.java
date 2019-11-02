package com.xerragnaroek.jikai.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Initilizable;
import com.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class JikaiDataManager extends Manager<JikaiData> implements Initilizable {

	private BotData botData;
	private final ScheduledExecutorService saver = Executors.newSingleThreadScheduledExecutor();

	public JikaiDataManager() {
		super(JikaiData.class);
	}

	@Override
	public void init() {
		initBotData();
		loadData();
	}

	public BotData getBotData() {
		return botData;
	}

	public Set<String> getGuildIds() {
		Set<String> copy = new TreeSet<String>(impls.keySet());
		return copy;
	}

	public boolean hasCompletedSetup(Guild g) {
		JikaiData gd = get(g);

		return gd != null && gd.hasCompletedSetup();
	}

	public boolean isKnownGuild(String gId) {
		return getGuildIds().contains(gId);
	}

	public void startSaveThread(long delay, TimeUnit unit) {
		saver.scheduleAtFixedRate(this::save, delay, delay, unit);
	}

	@Override
	protected JikaiData makeNew(String gId) {
		return new JikaiData(gId, true);
	}

	private void initBotData() {
		botData = new BotData(true);
	}

	private void loadData() {
		log.info("Loading configurations...");
		Path loc = Paths.get("./data/");
		try {
			if (Files.exists(loc)) {
				ObjectMapper mapper = new ObjectMapper();
				long count = Files.walk(loc).filter(Files::isRegularFile).peek(path -> {
					if (path.endsWith("BOT.json")) {
						botData = readFromPath(path, mapper, BotData.class);
						Jikai.addTimeZone(botData.getDefaultTimeZone());
					} else {
						JikaiData jd = readFromPath(path, mapper, JikaiData.class);
						if (jd != null) {
							impls.put(jd.getGuildId(), jd);
							Jikai.addTimeZone(jd.getTimeZone());
						}
					}
				}).count();
				log.info("Loaded {} configuration(s)", count);
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				botData = new BotData(true);
				Jikai.addTimeZone(botData.getDefaultTimeZone());
				Files.createDirectory(loc);
			}
		} catch (IOException e) {
			BotUtils.logAndSendToDev(log, "Failed loading the configurations", e);
		}
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
	private void save() {
		log.debug("Saving configs...");
		AtomicInteger saved = new AtomicInteger(0);
		impls.values().forEach(gd -> {
			if (gd.save(false)) {
				saved.incrementAndGet();
			}
		});
		if (botData.save()) {
			saved.incrementAndGet();
		}
		if (saved.get() != 0) {
			log.info("Saved {} GuildData", saved.get());
		} else {
			log.debug("No data had to be saved");
		}
	}

	public void saveNow() {
		impls.values().forEach(gd -> {
			gd.save(true);
		});
		log.info("Saved data");
	}

	public Set<JikaiData> data() {
		return Collections.synchronizedSet(new HashSet<>(impls.values()));
	}

}
