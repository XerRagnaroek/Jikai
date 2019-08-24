package me.xer.bot.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handles the executing of registered consumers on an option change.
 * 
 * @author XerRagnarök
 *
 */
public class ConfigChangeHandler {
	private final Map<ConfigOption, List<Consumer<String>>> consumer = new HashMap<>();

	ConfigChangeHandler() {}

	/**
	 * Will execute con when co changes.
	 * 
	 * @param co
	 * @param con
	 */
	void registerOnOptionChange(ConfigOption co, Consumer<String> con) {
		consumer.compute(co, (k, v) -> {
			if (v == null) {
				v = new LinkedList<>();
			}
			v.add(con);
			return v;
		});
	}

	/**
	 * Triggered when a ConfigOption is changed. Runs all consumer synchronously.
	 * 
	 * @param co
	 * @param newVal
	 */
	void configOptionChanged(ConfigOption co, String newVal) {
		if (consumer.containsKey(co)) {
			consumer.get(co).forEach(con -> con.accept(newVal));
		}
	}
}
