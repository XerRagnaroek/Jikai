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
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class GuildDataManager {
	static final Map<ZoneId, List<String>> usedZones = Collections.synchronizedMap(new HashMap<>());
	private static Map<GuildDataKey, List<BiConsumer<String, String>>> consumers = Collections.synchronizedMap(new HashMap<>());
	private static Map<String, GuildData> data = new HashMap<>();
	private static final Logger log = LoggerFactory.getLogger(GuildDataManager.class);
	private static final ScheduledExecutorService saver = Executors.newSingleThreadScheduledExecutor();

	static {
		registerUniversalOptionChangedConsumer(GuildDataKey.TIMEZONE, GuildDataManager::addTimeZone);
	}

	public static GuildData getBotConfig() {
		return getDataForGuild(GuildData.BOT);
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
			GuildData c = makeConfigFromId(id);
			data.put(id, c);
			return c;
		}
	}

	public static Set<String> getGuildIds() {
		Set<String> copy = new TreeSet<String>(data.keySet());
		copy.remove(GuildData.BOT);
		return copy;
	}

	public static Set<ZoneId> getUsedTimeZones() {
		return new HashSet<>(usedZones.keySet());
	}

	public static Map<ZoneId, List<String>> timeZoneGuildMap() {
		Map<ZoneId, List<String>> tmp = new HashMap<>();
		tmp.putAll(usedZones);
		return tmp;
	}

	public static void init() {
		log.info("Loading configurations...");
		Path loc = Paths.get("./data/");
		try {
			if (Files.exists(loc)) {
				long count = Files.walk(loc).filter(Files::isRegularFile).map(GuildDataManager::extractId).peek(id -> {
					data.put(id, makeConfigFromId(id));
				}).count();
				log.info("Loaded {} configuration(s)", count);
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				data.put(GuildData.BOT, makeBotConfig());
				Files.createDirectory(loc);
			}
		} catch (IOException e) {
			log.error("Failed loading the configurations", e);
		}
		saver.scheduleAtFixedRate(GuildDataManager::saveConfigs, 10, 10, TimeUnit.SECONDS);
		log.debug("Started thread to save configurations every 10 minutes");
	}

	/**
	 * 
	 * @param co
	 *            - the GuildDataKey
	 * @param con
	 *            - BiConsumer for the GuildId and the new Value
	 */
	public static void registerUniversalOptionChangedConsumer(GuildDataKey co, BiConsumer<String, String> con) {
		consumers.compute(co, (key, list) -> {
			if (list == null) {
				list = new LinkedList<>();
			}
			list.add(con);
			return list;
		});
		data.values().forEach(c -> c.registerDataChangedConsumer(co, con));
		log.debug("Registered a new universal consumer for {}", co);
	}

	/**
	 * Utility method
	 */
	private static String extractId(Path p) {
		//config file names are config_ID.ini
		//this replaced everything in front of and including '_' and after including a '.'
		return p.getFileName().toString().replaceAll("(.*_|\\..*)", "");
	}

	/**
	 * Makes the bot's default config. Also adds the default timezone to the list of used timezones.
	 */
	private static GuildData makeBotConfig() {
		GuildData bot = new GuildData(GuildData.BOT);
		bot.set(GuildDataKey.TIMEZONE, "Europe/Berlin");
		bot.set(GuildDataKey.TRIGGER, "!");
		usedZones.put(ZoneId.of("Europe/Berlin"), new LinkedList<String>());
		return bot;
	}

	private static GuildData makeConfigFromId(String id) {
		GuildData c = new GuildData(id);
		c.setConsumers(consumers);
		c.init();
		return c;
	}

	/**
	 * Save all configurations. Gets called every 10 minutes.
	 */
	private static void saveConfigs() {
		log.debug("Saving configs...");
		data.values().forEach(GuildData::saveData);
		log.info("Saved configurations!");
	}

	/**
	 * Get the ZoneID from the given String, add it to the set and the AnimeBase
	 * 
	 * @param zone
	 *            - the TimeZone's id
	 */
	private static void addTimeZone(String gId, String zone) {
		ZoneId z = ZoneId.of(zone);
		if (!gId.equals(GuildData.BOT)) {
			usedZones.compute(z, (k, v) -> {
				if (v == null) {
					v = new LinkedList<String>();
				}
				v.add(gId);
				return v;
			});
			log.debug("Added zone {}", zone);
		}
	}

	public static boolean isKnownGuild(String gId) {
		return getGuildIds().contains(gId);
	}
}
