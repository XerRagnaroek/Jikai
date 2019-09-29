package com.xerragnaroek.bot.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class BotData {

	private String defTrigger = "!";
	private ZoneId defZone = ZoneId.of("Europe/Berlin");
	private String curSeasonHash;
	private int abVersion;
	private AtomicBoolean changed = new AtomicBoolean(false);
	private final Path fileLoc = Paths.get("./data/BOT.json");
	private final Logger log = LoggerFactory.getLogger(BotData.class);

	BotData(boolean save) {
		changed.set(save);
	}

	BotData() {
		this(false);
	}

	@JsonProperty("trigger")
	public String getDefaultTrigger() {
		return defTrigger;
	}

	@JsonIgnore
	public ZoneId getDefaultTimeZone() {
		return defZone;
	}

	@JsonProperty("timezone")
	public String getDefaultTimeZoneString() {
		return defZone.getId();
	}

	@JsonProperty("current_season_hash")
	public String getCurrentSeasonHash() {
		return curSeasonHash;
	}

	@JsonProperty("anime_base_version")
	public int getAnimeBaseVersion() {
		return abVersion;
	}

	public String setCurrentSeasonHash(String hash) {
		String tmp = curSeasonHash;
		curSeasonHash = hash;
		changed.set(true);
		return tmp;
	}

	public int incrementAnimeBaseVersion() {
		int tmp = abVersion;
		abVersion++;
		log.debug("Updated AnimeBase version to {}", abVersion);
		changed.set(true);
		return tmp;
	}

	void save() {
		if (changed.get()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			try {
				Files.writeString(fileLoc, mapper.writeValueAsString(this));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@JsonCreator
	public static BotData makeBotData(@JsonProperty("trigger") String trigger, @JsonProperty("timezone") String tz,
			@JsonProperty("current_season_hash") String hash, @JsonProperty("anime_base_version") int version) {
		BotData b = new BotData();
		b.defTrigger = trigger;
		b.defZone = ZoneId.of(tz);
		b.curSeasonHash = hash;
		b.abVersion = version;
		return b;
	}
}
