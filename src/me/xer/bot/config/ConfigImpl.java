package me.xer.bot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.xerragnaroek.xlog.XLogger;

/**
 * Concrete implementation of the Configuration.
 * 
 * @author XerRagnarök
 *
 */
class ConfigImpl {
	private final HashMap<ConfigOption, String> config = new HashMap<>();
	private final Path fileLoc = Paths.get("./config.ini");
	private static final XLogger log = XLogger.getInstance();

	/**
	 * Package only cause it mine >:)
	 */
	ConfigImpl() {
		initConfig();
	}

	/**
	 * Load that config, boy. Will load default values and save them if no config file is present.
	 */
	private void initConfig() {
		log.log("Loading config");
		defaultConfig();
		if (Files.exists(fileLoc)) {
			log.log("Config file exists, loading...");
			loadConfig();
		} else {
			saveConfig();
		}
	}

	/**
	 * Load the config file.
	 */
	private void loadConfig() {
		try {
			Files.lines(fileLoc).forEach(line -> {
				String[] tmp = line.split("=");
				if (tmp.length == 2) {
					ConfigOption co;
					config.put((co = ConfigOption.valueOf(tmp[0])), tmp[1]);
					log.logf("%s=%s", co, tmp[1]);
				}
			});
		} catch (IOException e) {
			log.logException(e, "Failed loading config");
		}
	}

	/**
	 * Load the default config.
	 */
	private void defaultConfig() {
		config.put(ConfigOption.TRIGGER, "!");
	}

	/**
	 * Save the config.
	 */
	private void saveConfig() {
		try {
			Files.write(fileLoc, stringifyConfig());
			log.log("Saved config to " + fileLoc.toAbsolutePath());
		} catch (IOException e) {
			log.logException(e, "Failed saving config");
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
	String getOption(ConfigOption co) {
		return config.get(co);
	}

	/**
	 * Set the value of a {@link ConfigOption}
	 */
	void setOption(ConfigOption co, String val) {
		config.put(co, val);
		log.logf("Changed %s from %s to %s", co, getOption(co), val);
		saveConfig();
	}

}
