package com.xerragnaroek.bot.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GuildData storage object.
 * 
 * @author XerRagnarök
 *
 */
public class GuildData {
	public static final String BOT = "BOT";
	private final Map<GuildDataKey, String> data = Collections.synchronizedMap(new HashMap<>());
	private Map<GuildDataKey, List<Consumer<String>>> consumers = new HashMap<>();
	private final Path fileLoc;
	private final String guildId;
	private AtomicBoolean hasChanged = new AtomicBoolean(false);
	private final Logger log;

	/**
	 * Package only cause it mine >:)
	 */
	GuildData(String id) {
		guildId = id;
		log = LoggerFactory.getLogger(GuildData.class.getName() + "#" + guildId);
		fileLoc = Paths.get(String.format("./data/%s.ini", guildId));

	}

	/**
	 * Get the Value of a {@link GuildDataKey}
	 */
	public String get(GuildDataKey co) {
		String val;
		if (!guildId.equals(BOT)) {
			return ((val = data.get(co)) == null) ? GuildDataManager.getBotConfig().get(co) : val;
		} else {
			return data.get(co);
		}
	}

	public String getGuildId() {
		return guildId;
	}

	public void registerDataChangedConsumer(GuildDataKey co, Consumer<String> con) {
		consumers.compute(co, (key, list) -> {
			if (list == null) {
				list = new LinkedList<>();
			}
			list.add(con);
			return list;
		});
		log.debug("Registered a new consumer for {}", co);
	}

	/**
	 * Set the value of a {@link GuildDataKey}
	 */
	public void set(GuildDataKey co, String val) {
		if (this.guildId.equals(GuildData.BOT) || !co.isBotOnly()) {
			data.compute(co, (cop, v) -> {
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

	public void clear(GuildDataKey co) {
		data.remove(co);
		hasChanged.set(true);
		log.debug("Cleared {}", co);
	}

	boolean hasChanged() {
		return hasChanged.get();
	}

	/**
	 * Load that data, boy. Will load default values and save them if no data file is present.
	 */
	void init() {
		log.info("Loading config [{}]", guildId);
		//defaultConfig();
		if (Files.exists(fileLoc)) {
			loadData();
		}
		hasChanged.set(true);
	}

	/**
	 * Save the data but only if it changed.
	 */
	synchronized void saveData() {
		if (hasChanged.get()) {
			try {
				Files.write(fileLoc, stringifyData());
				log.info("Saved config to " + fileLoc.toAbsolutePath());
			} catch (IOException e) {
				log.error("Failed saving config", e);
			}
			hasChanged.set(false);
		}
	}

	void setConsumers(Map<GuildDataKey, List<Consumer<String>>> cons) {
		consumers = cons;
	}

	/**
	 * Load from the data file
	 */
	private void loadData() {
		log.debug("Loading data...");
		try {
			Files.lines(fileLoc).forEach(line -> {
				String[] tmp = line.split("=");
				if (tmp.length == 2) {
					GuildDataKey co;
					set((co = GuildDataKey.valueOf(tmp[0])), tmp[1]);
					log.debug("{}={}", co, tmp[1]);
				}
			});
		} catch (IOException e) {
			log.error("Failed loading config", e);
		}
	}

	private void runConsumers(GuildDataKey co, String newVal) {
		if (consumers.containsKey(co)) {
			log.debug("Running consumers for {}", co);
			consumers.get(co).forEach(con -> con.accept(newVal));
		}
	}

	/**
	 * Utility method that turns the data map into an ini-fied string.
	 */
	private List<String> stringifyData() {
		List<String> l = new ArrayList<>(data.size());
		for (GuildDataKey key : data.keySet()) {
			l.add(key + "=" + data.get(key));
		}
		return l;
	}

}
