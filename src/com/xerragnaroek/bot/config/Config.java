package com.xerragnaroek.bot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of the Configuration.
 * 
 * @author XerRagnarök
 *
 */
public class Config {
	public static final String BOT_CONFIG = "BOT";
	private final HashMap<ConfigOption, String> config = new HashMap<>();
	private final Path fileLoc;
	private final Logger log;
	private final String guildId;
	private AtomicBoolean hasChanged = new AtomicBoolean(false);
	private Map<ConfigOption, List<Consumer<String>>> consumers = new HashMap<>();

	/**
	 * Package only cause it mine >:)
	 */
	Config(String id) {
		guildId = id;
		log = LoggerFactory.getLogger(Config.class.getName() + "#" + guildId);
		fileLoc = Paths.get(String.format("./configs/config_%s.ini", guildId));

	}

	/**
	 * Load that config, boy. Will load default values and save them if no config file is present.
	 */
	void initConfig() {
		log.info("Loading config [{}]", guildId);
		//defaultConfig();
		if (Files.exists(fileLoc)) {
			loadConfig();
		}
		hasChanged.set(true);
	}

	/**
	 * Load the config file.
	 */
	private void loadConfig() {
		log.debug("Loading config...");
		try {
			Files.lines(fileLoc).forEach(line -> {
				String[] tmp = line.split("=");
				if (tmp.length == 2) {
					ConfigOption co;
					setOption((co = ConfigOption.valueOf(tmp[0])), tmp[1]);
					log.debug("{}={}", co, tmp[1]);
				}
			});
		} catch (IOException e) {
			log.error("Failed loading config", e);
		}
	}

	/**
	 * Save the config but only if it changed.
	 */
	synchronized void saveConfig() {
		if (hasChanged.get()) {
			try {
				Files.write(fileLoc, stringifyConfig());
				log.info("Saved config to " + fileLoc.toAbsolutePath());
			} catch (IOException e) {
				log.error("Failed saving config", e);
			}
			hasChanged.set(false);
		}
	}

	/**
	 * Utility method that turns the config map into an ini-fied string.
	 */
	private List<String> stringifyConfig() {
		List<String> l = new ArrayList<>(config.size());
		for (ConfigOption key : config.keySet()) {
			l.add(key + "=" + config.get(key));
		}
		return l;
	}

	/**
	 * Get the Value of a {@link ConfigOption}
	 */
	public String getOption(ConfigOption co) {
		String val;
		if (!guildId.equals(BOT_CONFIG)) {
			return ((val = config.get(co)) == null) ? ConfigManager.getBotConfig().getOption(co) : val;
		} else {
			return config.get(co);
		}
	}

	/**
	 * Set the value of a {@link ConfigOption}
	 */
	public void setOption(ConfigOption co, String val) {
		if (this.guildId.equals(Config.BOT_CONFIG) || !co.isBotOnly()) {
			config.compute(co, (cop, v) -> {
				if (v == null || !v.equals(val)) {
					hasChanged.set(true);
					log.debug("Changed {} from {} to {}", co, v, val);
					runConsumers(co, val);
					return val;
				} else {
					log.debug("{} was already set to {}", co, v);
					return v;
				}
			});
		}
	}

	public void registerOptionChangedConsumer(ConfigOption co, Consumer<String> con) {
		consumers.compute(co, (key, list) -> {
			if (list == null) {
				list = new LinkedList<>();
			}
			list.add(con);
			return list;
		});
		log.debug("Registered a new consumer for {}", co);
	}

	void setConsumers(Map<ConfigOption, List<Consumer<String>>> cons) {
		consumers = cons;
	}

	private void runConsumers(ConfigOption co, String newVal) {
		if (consumers.containsKey(co)) {
			log.debug("Running consumers for {}", co);
			consumers.get(co).forEach(con -> con.accept(newVal));
		}
	}

	boolean hasChanged() {
		return hasChanged.get();
	}

	public String getGuildId() {
		return guildId;
	}

}
