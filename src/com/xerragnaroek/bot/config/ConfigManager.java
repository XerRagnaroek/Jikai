package com.xerragnaroek.bot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.AnimeBase;

import net.dv8tion.jda.api.entities.Guild;

/**
 * This class manages the different servers' configs.
 * 
 * @author XerRagnaroek
 *
 */
public class ConfigManager {
	private static Map<String, Config> configs = new HashMap<>();
	private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
	private static final ScheduledExecutorService saver = Executors.newSingleThreadScheduledExecutor();
	static final Set<ZoneId> usedZones = new HashSet<>();

	/**
	 * Gets the id's associated config or creates a new one if none is loaded.
	 * 
	 * @param id
	 *            - the guild's id
	 * @return the associated Config
	 */
	public static Config getConfigForGuild(String id) {
		log.debug("Fetching config for id {}", id);
		if (configs.containsKey(id)) {
			return configs.get(id);
		} else {
			log.debug("No config found, creating new one");
			Config c = new Config(id);
			configs.put(id, c);
			return c;
		}
	}

	public static Config getConfigForGuild(Guild g) {
		return getConfigForGuild(g.getId());
	}

	public static void init() {
		log.info("Loading configurations...");
		Path loc = Paths.get("./configs/");
		try {
			if (Files.exists(loc)) {
				long count = Files.walk(loc).filter(Files::isRegularFile).map(ConfigManager::extractId).peek(id -> {
					configs.put(id, makeConfigFromId(id));
				}).count();
				log.info("Loaded {} configuration(s)", count);
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				configs.put(Config.BOT_CONFIG, makeBotConfig());
				Files.createDirectory(loc);
			}
		} catch (IOException e) {
			log.error("Failed loading the configurations", e);
		}
		saver.scheduleAtFixedRate(ConfigManager::saveConfigs, 10, 10, TimeUnit.MINUTES);
		log.debug("Started thread to save configurations every 10 minutes");
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
	 * Get the ZoneID from the given String, add it to the set and the AnimeBase
	 * 
	 * @param zone
	 *            - the TimeZone's id
	 */
	private static void updateTimeZone(String zone) {
		ZoneId z = ZoneId.of(zone);
		usedZones.add(z);
		AnimeBase.addTimeZone(z);
		log.debug("Added zone {}", zone);
	}

	/**
	 * Makes the bot's default config. Also adds the default timezone to the list of used timezones.
	 */
	private static Config makeBotConfig() {
		Config bot = new Config(Config.BOT_CONFIG);
		bot.setOption(ConfigOption.TIMEZONE, "Europe/Berlin");
		bot.setOption(ConfigOption.TRIGGER, "!");
		usedZones.add(ZoneId.of("Europe/Berlin"));
		return bot;
	}

	private static Config makeConfigFromId(String id) {
		Config c = new Config(id);
		c.registerOptionChangedConsumer(ConfigOption.TIMEZONE, ConfigManager::updateTimeZone);
		c.initConfig();
		return c;
	}

	/**
	 * Save all configurations. Gets called every 10 minutes.
	 */
	private static void saveConfigs() {
		log.debug("Saving configs...");
		configs.values().forEach(Config::saveConfig);
		log.info("Saved configurations!");
	}

	public static Config getBotConfig() {
		return getConfigForGuild(Config.BOT_CONFIG);
	}

	public static Set<ZoneId> getUsedTimeZones() {
		return new HashSet<>(usedZones);
	}
}
