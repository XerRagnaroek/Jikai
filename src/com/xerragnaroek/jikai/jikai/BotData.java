package com.xerragnaroek.jikai.jikai;

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
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.util.prop.IntegerProperty;
import com.xerragnaroek.jikai.util.prop.Property;

public class BotData {

	private String defTrigger = "!";
	private ZoneId defZone = ZoneId.of("Europe/Berlin");
	private Property<String> curSeasonHash = new Property<>("");
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
		return curSeasonHash.get();
	}

	@JsonProperty("anime_base_version")
	private int getAnimeBaseVersion() {
		return AnimeDB.getAnimeDBVersion();
	}

	public boolean hasSeasonSearchHash() {
		return curSeasonHash.hasNonNullValue();
	}

	public String setCurrentSeasonHash(String hash) {
		String tmp = curSeasonHash.get();
		curSeasonHash.set(hash);
		changed.set(true);
		return tmp;
	}

	boolean save() {
		if (changed.get()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			try {
				Files.writeString(fileLoc, mapper.writeValueAsString(this));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@JsonCreator
	public static BotData makeBotData(@JsonProperty("trigger") String trigger, @JsonProperty("timezone") String tz, @JsonProperty("current_season_hash") Property<String> hash, @JsonProperty("anime_base_version") IntegerProperty version) {
		BotData b = new BotData();
		b.defTrigger = trigger;
		b.defZone = ZoneId.of(tz);
		b.curSeasonHash = hash;
		AnimeDB.setDBVersionProperty(version);
		return b;
	}
}
