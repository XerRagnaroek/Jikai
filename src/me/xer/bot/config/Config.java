package me.xer.bot.config;

import java.util.function.Consumer;

import com.github.xerragnaroek.xlog.XLogger;

/**
 * Business class for the Configuration.
 * 
 * @author XerRagnarök
 *
 */
public class Config {
	private static ConfigImpl config;
	private static final ConfigChangeHandler ccH = new ConfigChangeHandler();
	private static final XLogger log = XLogger.getInstance();

	/**
	 * Get the value of a ConfigOption.
	 */
	public static String getOption(ConfigOption co) {
		return config.getOption(co);
	}

	/**
	 * Set the value of a ConfigOption
	 */
	public static synchronized void setOption(ConfigOption co, String val) {
		config.setOption(co, val);
		ccH.configOptionChanged(co, val);
	}

	public static synchronized void registerOnOptionChange(ConfigOption co, Consumer<String> con) {
		ccH.registerOnOptionChange(co, con);
	}

	public static void initConfig() {
		if (config == null) {
			config = new ConfigImpl();
		}

	}

}
