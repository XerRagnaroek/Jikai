
package com.github.xerragnaroek.jikai.jikai;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;

public class BotData {

	private String defPrefix = "!";
	private ZoneId defZone = ZoneId.of("Europe/Berlin");
	private JikaiLocale defLocale = JikaiLocaleManager.getEN();
	private Color defCol = new Color(240, 240, 240);
	private String jikaiImgUrl = "https://cdn.discordapp.com/attachments/725017810186666066/725017816121475092/jikai.png";
	private final Logger log = LoggerFactory.getLogger(BotData.class);
	private final Path loc;

	BotData() {
		loc = Path.of(Core.DATA_LOC.toString(), "BOT.json");
	}

	@JsonProperty("prefix")
	public String getDefaultPrefix() {
		return defPrefix;
	}

	@JsonIgnore
	public ZoneId getDefaultTimeZone() {
		return defZone;
	}

	@JsonProperty("timezone")
	public String getDefaultTimeZoneString() {
		return defZone.getId();
	}

	@JsonProperty("language")
	public String getDefaultLocaleIdentifier() {
		return defLocale.getIdentifier();
	}

	@JsonIgnore
	public JikaiLocale getDefaultLocale() {
		return defLocale;
	}

	@JsonIgnore
	public Color getJikaiColor() {
		return defCol;
	}

	@JsonProperty("color")
	public int getJikaiRGB() {
		return defCol.getRGB();
	}

	@JsonProperty("jikai_img_url")
	public String getJikaiImgUrl() {
		return jikaiImgUrl;
	}

	public void save() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		try {
			Files.writeString(loc, mapper.writeValueAsString(this));
			log.info("Successfully saved BotData to {}", loc);
		} catch (IOException e) {
			Core.logThrowable(e);
		}
	}

	@JsonCreator
	public static BotData makeBotData(@JsonProperty("prefix") String prefix, @JsonProperty("timezone") String tz, @JsonProperty("language") String lang, @JsonProperty("color") int col, @JsonProperty("jikai_img_url") String url) {
		BotData b = new BotData();
		if (prefix != null && !prefix.isEmpty()) {
			b.defPrefix = prefix;
		}
		if (tz != null && !tz.isEmpty()) {
			try {
				b.defZone = ZoneId.of(tz);
			} catch (Exception e) {
				Core.ERROR_LOG.error("Invalid zone id in BOT.json", e);
			}
		}

		if (lang != null && !lang.isEmpty()) {
			if (JikaiLocaleManager.getInstance().hasLocale(lang)) {
				b.defLocale = JikaiLocaleManager.getInstance().getLocale(lang);
			} else {
				Core.ERROR_LOG.error("Invalid locale in BOT.json: '{}'", lang);
			}
		}
		if (col != 0) {
			b.defCol = new Color(col);
		}

		if (url != null && !url.isEmpty()) {
			b.jikaiImgUrl = url;
		}
		return b;
	}
}
