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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class GuildDataManager {
	static final Map<ZoneId, List<String>> usedZones = Collections.synchronizedMap(new HashMap<>());
	private static BotData botData;
	private static Map<String, GuildData> data = new HashMap<>();
	private static final Logger log = LoggerFactory.getLogger(GuildDataManager.class);
	private static final ScheduledExecutorService saver = Executors.newSingleThreadScheduledExecutor();
	private static Map<GuildDataKey, Set<BiConsumer<String, String>>> strConsumers =
			Collections.synchronizedMap(new HashMap<>());
	private static Set<BiConsumer<String, ZoneId>> zoneCons = Collections.synchronizedSet(new HashSet<>());

	static {
		initBotData();
		registerOnUniversalTimeZoneChanged(GuildDataManager::addTimeZone);
	}

	public static BotData getBotConfig() {
		return botData;
	}

	public static GuildData getDataForGuild(Guild g) {
		return getDataForGuild(g.getId());
	}

	/**
	 * Gets the id's associated data or creates a new one if none is loaded.
	 * 
	 * @param id
	 *            - the guild's id
	 * @return the associated data
	 */
	public static GuildData getDataForGuild(String id) {
		log.debug("Fetching config for id {}", id);
		if (data.containsKey(id)) {
			return data.get(id);
		} else {
			log.debug("No config found, creating new one");
			GuildData c = new GuildData(id, true);
			data.put(id, c);
			return c;
		}
	}

	public static Set<String> getGuildIds() {
		Set<String> copy = new TreeSet<String>(data.keySet());
		return copy;
	}

	public static Set<ZoneId> getUsedTimeZones() {
		return new HashSet<>(usedZones.keySet());
	}

	public static boolean hasCompletedSetup(Guild g) {
		return getDataForGuild(g).hasCompletedSetup();
	}

	public static void init() {
		loadData();
		log.debug("Started thread to save configurations every 10 minutes");
	}

	public static boolean isKnownGuild(String gId) {
		return getGuildIds().contains(gId);
	}

	public static GuildData registerNewGuild(Guild g) {
		log.info("Registering new guild {}#{}", g.getName(), g.getId());
		return registerNewGuild(g.getId());
	}

	public static GuildData registerNewGuild(String gId) {
		GuildData c = new GuildData(gId, true);
		data.put(gId, c);
		return c;
	}

	public static void registerOnUniversalTimeZoneChanged(BiConsumer<String, ZoneId> con) {
		zoneCons.add(con);
		data.values().forEach(gd -> gd.timeZoneProperty().onChange((ov, nv) -> con.accept(gd.getGuildId(), nv)));
		log.debug("Registered a new universal consumer for TimeZone");
	}

	public static void startSaveThread(long delay, TimeUnit unit) {
		saver.scheduleAtFixedRate(GuildDataManager::saveConfigs, delay, delay, unit);
	}

	public static Map<ZoneId, List<String>> timeZoneGuildMap() {
		Map<ZoneId, List<String>> tmp = new HashMap<>();
		tmp.putAll(usedZones);
		return tmp;
	}

	/**
	 * Get the ZoneID from the given String, add it to the set and the AnimeBase
	 * 
	 * @param zone
	 *            - the TimeZone's id
	 */
	private static void addTimeZone(String gId, ZoneId zone) {
		usedZones.compute(zone, (k, v) -> {
			if (v == null) {
				v = new LinkedList<String>();
			}
			v.add(gId);
			return v;
		});
		log.debug("Added zone {}", zone);
	}

	private static void initBotData() {
		botData = new BotData(true);
		usedZones.put(botData.getDefaultTimeZone(), new LinkedList<>());
	}

	private static void loadData() {
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
							data.put(gd.getGuildId(), gd);
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

	private static void putBiConInStrMap(GuildDataKey gdk, BiConsumer<String, String> con) {
		strConsumers.compute(gdk, (key, set) -> {
			if (set == null) {
				set = new HashSet<>();
			}
			set.add(con);
			return set;
		});
	}

	private static <T> T readFromPath(Path p, ObjectMapper mapper, Class<T> clazz) {
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
	private static void saveConfigs() {
		log.debug("Saving configs...");
		AtomicInteger saved = new AtomicInteger(0);
		data.values().forEach(gd -> {
			if (gd.save()) {
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
