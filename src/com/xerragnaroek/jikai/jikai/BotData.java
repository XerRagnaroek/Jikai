/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
