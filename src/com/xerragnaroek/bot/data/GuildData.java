package com.xerragnaroek.bot.data;

import static com.xerragnaroek.bot.core.Core.ALRHM;
import static com.xerragnaroek.bot.core.Core.GDM;
import static com.xerragnaroek.bot.core.Core.RTKM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xerragnaroek.bot.anime.alrh.ALRHData;
import com.xerragnaroek.bot.anime.alrh.ALRHandler;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.timer.ReleaseTimeKeeper;
import com.xerragnaroek.bot.util.BotUtils;
import com.xerragnaroek.bot.util.Property;

@JsonInclude(Include.NON_EMPTY)
public class GuildData {
	private Property<String> aniChId;
	private AtomicBoolean changed = new AtomicBoolean(false);
	private boolean completedSetup = false;
	private Property<Boolean> comsEnabled;
	private Path fileLoc;
	private String gId;
	private Property<String> infoChId;
	private Property<String> listChId;
	private final Logger log;
	private Property<String> trigger;
	private Property<ZoneId> zone;

	public GuildData(String guildId, boolean save) {
		log = LoggerFactory.getLogger(GuildData.class + "#" + guildId);
		gId = guildId;
		fileLoc = Paths.get(String.format("./data/%s.json", guildId));
		changed.set(save);
		log.info("Loaded configuration for {}", guildId);
	}

	public Property<String> animeChannelIdProperty() {
		return aniChId;
	}

	@JsonProperty("commands_enabled")
	public boolean areCommandsEnabled() {
		if (hasExplicitCommandSetting()) {
			return comsEnabled.get();
		} else {
			return Core.CHM.areCommandsEnabledByDefault();
		}
	}

	public Property<Boolean> comsEnabledProperty() {
		return comsEnabled;
	}

	@JsonProperty("alrh_data")
	public Set<ALRHData> getALRHData() {
		return ALRHM.get(gId).getData();
	}

	@JsonProperty("anime_channel_id")
	public String getAnimeChannelId() {
		return aniChId.get();
	}

	@JsonIgnore
	public Map<String, ZonedDateTime> getAnimesLastMentioned() {
		return RTKM.get(gId).getLastMentionedMap();
	}

	@JsonProperty("last_mentioned")
	public Map<String, String> getAnimesLastMentionedString() {
		Map<String, String> tmp = new TreeMap<>();
		RTKM.get(gId).getLastMentionedMap().forEach((id, zdt) -> tmp.put(id, zdt.toString()));
		return tmp;
	}

	@JsonProperty("guild_id")
	public String getGuildId() {
		return gId;
	}

	@JsonProperty("info_channel_id")
	public String getInfoChannelId() {
		return infoChId.get();
	}

	@JsonProperty("list_channel_id")
	public String getListChannelId() {
		return listChId.get();
	}

	@JsonIgnore
	public ZoneId getTimeZone() {
		return Objects.requireNonNullElse(zone.get(), GDM.getBotConfig().getDefaultTimeZone());
	}

	@JsonProperty("timezone")
	public String getTimeZoneString() {
		if (zone != null) {
			return zone.get().getId();
		} else {
			return GDM.getBotConfig().getDefaultTimeZoneString();
		}
	}

	@JsonProperty("trigger")
	public String getTrigger() {
		return Objects.requireNonNullElse(trigger.get(), GDM.getBotConfig().getDefaultTrigger());
	}

	@JsonProperty("completed_setup")
	public boolean hasCompletedSetup() {
		return completedSetup;
	}

	public boolean hasExplicitCommandSetting() {
		return comsEnabled.hasNonNullValue();
	}

	public Property<String> infoChannelIdProperty() {
		return infoChId;
	}

	public Property<String> listChannelIdProperty() {
		return listChId;
	}

	public String setAnimeChannelId(String id) {
		String tmp = aniChId.get();
		aniChId.set(id);
		log.info("AnimeChannelId was changed from '{}' to '{}'", tmp, id);
		hasChanged();
		return tmp;
	}

	public boolean setCommandsEnabled(boolean enabled) {
		boolean tmp = enabled;
		comsEnabled.set(enabled);
		log.info("Commands for guild {} have been {}", gId, (enabled ? "enabled" : "disabled"));
		hasChanged();
		return tmp;
	}

	public void setInfoChannelId(String id) {
		infoChId.set(id);
	}

	public String setListChannelId(String id) {
		String tmp = listChId.get();
		listChId.set(id);
		log.info("ListChannelId was changed from '{}' to '{}'", tmp, id);
		hasChanged();
		return tmp;
	}

	public void setSetupCompleted(boolean setup) {
		completedSetup = setup;
	}

	public ZoneId setTimeZone(ZoneId z) {
		ZoneId tmp = zone.get();
		zone.set(z);
		log.info("TimeZone was changed from '{}' to '{}'", tmp, z);
		hasChanged();
		return tmp;
	}

	public String setTrigger(String trigger) {
		trigger = StringEscapeUtils.escapeJava(trigger);
		String tmp = this.trigger.get();
		this.trigger.set(trigger);
		log.info("Trigger was changed from '{}' to '{}'", tmp, trigger);
		hasChanged();
		return tmp;
	}

	public Property<ZoneId> timeZoneProperty() {
		return zone;
	}

	public Property<String> triggerProperty() {
		return trigger;
	}

	boolean save() {
		if (updatesAvailable()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			try {
				Files.writeString(fileLoc, mapper.writeValueAsString(this));
				log.info("Successfully saved data to {}", fileLoc);
				changed.set(false);
				return true;
			} catch (IOException e) {
				BotUtils.logAndSendToDev(log, "Failed saving data to " + fileLoc, e);
			}
		}
		return false;
	}

	private void hasChanged() {
		changed.set(true);
	}

	private boolean updatesAvailable() {
		boolean tmp = false;
		log.debug("Seeing if any data changed:");
		if (changed.get()) {
			log.debug("GuildData changed");
			tmp = true;
		}
		ALRHandler h = ALRHM.get(gId);
		if (h.isInitialized() && h.hasUpdateFlagAndReset()) {
			log.debug("ALHRData changed");
			tmp = true;
		}
		ReleaseTimeKeeper rtk = RTKM.get(gId);
		if (rtk != null && rtk.hasUpdateFlagAndReset()) {
			log.debug("ReleaseTimeKeeper changed");
			tmp = true;
		}
		return tmp;
	}

	@JsonCreator
	public static GuildData of(@JsonProperty("guild_id") String gId, @JsonProperty("trigger") Property<String> trig,
			@JsonProperty("anime_channel_id") Property<String> aniChId,
			@JsonProperty("list_channel_id") Property<String> listChId, @JsonProperty("timezone") String zone,
			@JsonProperty("alrh_data") Set<ALRHData> data,
			@JsonProperty("last_mentioned") Map<String, String> lastMentioned,
			@JsonProperty("completed_setup") boolean setupCompleted,
			@JsonProperty("commands_enabled") Property<Boolean> comsEnabled,
			@JsonProperty("info_channel_id") Property<String> icId) {
		GuildData gd = new GuildData(gId, false);
		gd.trigger = trig;
		gd.aniChId = aniChId;
		gd.listChId = listChId;
		gd.zone = Property.of(ZoneId.of(zone));
		gd.completedSetup = setupCompleted;
		if (comsEnabled == null) {
			comsEnabled = new Property<Boolean>();
		}
		gd.comsEnabled = comsEnabled;
		if (icId == null) {
			icId = new Property<String>();
		}
		gd.infoChId = icId;
		ALRHM.addToInitMap(gId, data);
		RTKM.addToInitMap(gId, lastMentioned);
		return gd;
	}

}
