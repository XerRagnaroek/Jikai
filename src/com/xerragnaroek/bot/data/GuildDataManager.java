package com.xerragnaroek.bot.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerragnaroek.bot.util.BotUtils;
import com.xerragnaroek.bot.util.Initilizable;
import com.xerragnaroek.bot.util.Manager;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class GuildDataManager extends Manager<GuildData> implements Initilizable {

	final Map<ZoneId, List<String>> usedZones = Collections.synchronizedMap(new HashMap<>());
	private BotData botData;
	private final ScheduledExecutorService saver = Executors.newSingleThreadScheduledExecutor();
	private Set<BiConsumer<String, ZoneId>> zoneCons = Collections.synchronizedSet(new HashSet<>());

	public GuildDataManager() {
		super(GuildData.class);
	}

	@Override
	public void init() {
		initBotData();
		registerOnUniversalTimeZoneChanged(this::addTimeZone);
		loadData();
	}

	public BotData getBotData() {
		return botData;
	}

	public Set<String> getGuildIds() {
		Set<String> copy = new TreeSet<String>(impls.keySet());
		return copy;
	}

	public Set<ZoneId> getUsedTimeZones() {
		return new HashSet<>(usedZones.keySet());
	}

	public boolean hasCompletedSetup(Guild g) {
		GuildData gd = get(g);

		return gd != null && gd.hasCompletedSetup();
	}

	public boolean isKnownGuild(String gId) {
		return getGuildIds().contains(gId);
	}

	public void registerOnUniversalTimeZoneChanged(BiConsumer<String, ZoneId> con) {
		zoneCons.add(con);
		impls.values().forEach(gd -> gd.timeZoneProperty().onChange((ov, nv) -> con.accept(gd.getGuildId(), nv)));
		log.debug("Registered a new universal consumer for TimeZone");
	}

	public void startSaveThread(long delay, TimeUnit unit) {
		saver.scheduleAtFixedRate(this::saveConfigs, delay, delay, unit);
	}

	public Map<ZoneId, List<String>> timeZoneGuildMap() {
		Map<ZoneId, List<String>> tmp = new HashMap<>();
		tmp.putAll(usedZones);
		return tmp;
	}

	@Override
	protected GuildData makeNew(String gId) {
		return new GuildData(gId, true);
	}

	/**
	 * Get the ZoneID from the given String, add it to the set and the AnimeBase
	 * 
	 * @param zone
	 *            - the TimeZone's id
	 */
	private void addTimeZone(String gId, ZoneId zone) {
		usedZones.compute(zone, (k, v) -> {
			if (v == null) {
				v = new LinkedList<String>();
			}
			v.add(gId);
			return v;
		});
		log.debug("Added zone {}", zone);
	}

	private void initBotData() {
		botData = new BotData(true);
		usedZones.put(botData.getDefaultTimeZone(), new LinkedList<>());
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
						usedZones.put(botData.getDefaultTimeZone(), new LinkedList<>());
					} else {
						GuildData gd = readFromPath(path, mapper, GuildData.class);
						if (gd != null) {
							impls.put(gd.getGuildId(), gd);
							addTimeZone(gd.getGuildId(), gd.getTimeZone());
						}
					}
				}).count();
				log.info("Loaded {} configuration(s)", count);
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				botData = new BotData(true);
				usedZones.put(botData.getDefaultTimeZone(), new LinkedList<>());
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
	 * Save all configurations. Gets called every 10 minutes.
	 */
	private void saveConfigs() {
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

}
