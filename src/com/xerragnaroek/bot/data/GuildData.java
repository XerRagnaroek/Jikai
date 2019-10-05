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
import java.util.List;
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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xerragnaroek.bot.anime.alrh.ALRHData;
import com.xerragnaroek.bot.anime.alrh.ALRHandler;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.timer.ReleaseTimeKeeper;
import com.xerragnaroek.bot.util.BotUtils;
import com.xerragnaroek.bot.util.Property;

@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder({ "guild_id", "completed_setup", "commands_enabled", "trigger", "timezone", "list_channel_id", "schedule_channel_id", "anime_channel_id", "info_channel_id", "last_mentioned", "alrh_data" })
public class GuildData {
	private Property<String> aniChId = new Property<>();
	private AtomicBoolean changed = new AtomicBoolean(false);
	private Property<Boolean> completedSetup = new Property<>(false);
	private Property<Boolean> comsEnabled = new Property<>();
	private Path fileLoc;
	private String gId;
	private Property<String> infoChId = new Property<>();
	private Property<String> listChId = new Property<>();
	private final Logger log;
	private Property<String> schedChId = new Property<>();
	private List<String> schedMsgIds;
	private Property<String> trigger = new Property<>();
	private Property<ZoneId> zone = new Property<>();

	public GuildData(String guildId, boolean save) {
		log = LoggerFactory.getLogger(GuildData.class + "#" + guildId);
		gId = guildId;
		fileLoc = Paths.get(String.format("./data/%s.json", guildId));
		BotData bc = Core.GDM.getBotData();
		setTrigger(bc.getDefaultTrigger());
		setTimeZone(bc.getDefaultTimeZone());
		setCommandsEnabled(Core.CHM.areCommandsEnabledByDefault());
		changed.set(save);
		log.info("Made configuration for {}", guildId);
	}

	public Property<String> animeChannelIdProperty() {
		return aniChId;
	}

	@JsonProperty("commands_enabled")
	public boolean areCommandsEnabled() {
		if (comsEnabled.hasNonNullValue()) {
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

	@JsonProperty("schedule_channel_id")
	public String getScheduleChannelId() {
		return schedChId.get();
	}

	@JsonProperty("schedule_message_ids")
	public List<String> getScheduleMessageIds() {
		return schedMsgIds;
	}

	@JsonIgnore
	public ZoneId getTimeZone() {
		return Objects.requireNonNullElse(zone.get(), GDM.getBotData().getDefaultTimeZone());
	}

	@JsonProperty("timezone")
	public String getTimeZoneString() {
		if (zone.hasNonNullValue()) {
			return zone.get().getId();
		} else {
			return GDM.getBotData().getDefaultTimeZoneString();
		}
	}

	@JsonProperty("trigger")
	public String getTrigger() {
		return Objects.requireNonNullElse(trigger.get(), GDM.getBotData().getDefaultTrigger());
	}

	@JsonProperty("completed_setup")
	public boolean hasCompletedSetup() {
		return completedSetup != null && completedSetup.hasNonNullValue() && completedSetup.get();
	}

	public boolean hasScheduleMessageIds() {
		return schedMsgIds != null;
	}

	public boolean hasScheduleChannelId() {
		return schedChId.hasNonNullValue();
	}

	public boolean hasListChannelId() {
		return listChId.hasNonNullValue();
	}

	public boolean hasInfoChannelId() {
		return infoChId.hasNonNullValue();
	}

	public boolean hasAnimeChannelId() {
		return aniChId.hasNonNullValue();
	}

	public Property<String> infoChannelIdProperty() {
		return infoChId;
	}

	public Property<String> listChannelIdProperty() {
		return listChId;
	}

	public synchronized boolean save(boolean now) {
		if (updatesAvailable() || now) {
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

	public Property<String> scheduleChannelIdProperty() {
		return schedChId;
	}

	public String setAnimeChannelId(String id) {
		return setData(aniChId, id, "anime_channel_id");
	}

	public boolean setCommandsEnabled(boolean enabled) {
		return Objects.requireNonNullElse(setData(comsEnabled, enabled, "commands_enabled"), false);
	}

	public String setInfoChannelId(String id) {
		return setData(infoChId, id, "info_channel_id");
	}

	public String setListChannelId(String id) {
		return setData(listChId, id, "list_channel_id");
	}

	public String setScheduleChannelId(String id) {
		return setData(schedChId, id, "schedule_channel_id");
	}

	public List<String> setScheduleMessageIds(List<String> ids) {
		List<String> tmp = schedMsgIds;
		schedMsgIds = ids;
		hasChanged();
		log.info("schedule_message_ids changed '{}' -> '{}'", tmp, schedMsgIds);
		return tmp;
	}

	public boolean setSetupCompleted(boolean setup) {
		return setData(completedSetup, setup, "completed_setup");
	}

	public ZoneId setTimeZone(ZoneId z) {
		return setData(zone, z, "timezone");
	}

	public String setTrigger(String trigger) {
		trigger = StringEscapeUtils.escapeJava(trigger);
		return setData(this.trigger, trigger, "trigger");
	}

	public Property<ZoneId> timeZoneProperty() {
		return zone;
	}

	public Property<String> triggerProperty() {
		return trigger;
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

	private <T> T setData(Property<T> field, T newData, String name) {
		T tmp = field.get();
		field.set(newData);
		log.info(name + " changed '{}'->'{}'", tmp, newData);
		hasChanged();
		return tmp;
	}

	@JsonCreator
	public static GuildData of(@JsonProperty("guild_id") String gId, @JsonProperty("trigger") Property<String> trig, @JsonProperty("anime_channel_id") Property<String> aniChId, @JsonProperty("list_channel_id") Property<String> listChId, @JsonProperty("timezone") String zone, @JsonProperty("alrh_data") Set<ALRHData> data, @JsonProperty("last_mentioned") Map<String, String> lastMentioned, @JsonProperty("completed_setup") Property<Boolean> setupCompleted, @JsonProperty("commands_enabled") Property<Boolean> comsEnabled, @JsonProperty("info_channel_id") Property<String> icId, @JsonProperty("schedule_channel_id") Property<String> schId, @JsonProperty("schedule_message_ids") List<String> schedMsgIds) {
		GuildData gd = new GuildData(gId, false);
		gd.trigger = trig;
		gd.aniChId = aniChId;
		gd.listChId = listChId;
		gd.zone = Property.of(ZoneId.of(zone));
		gd.completedSetup = setupCompleted;
		if (comsEnabled != null) {
			gd.comsEnabled = comsEnabled;
		}
		if (icId == null) {
			icId = new Property<String>();
		}
		gd.infoChId = icId;
		gd.schedChId = schId;
		gd.schedMsgIds = schedMsgIds;
		ALRHM.addToInitMap(gId, data);
		RTKM.addToInitMap(gId, lastMentioned);
		return gd;
	}

	public boolean hasExplicitCommandSetting() {
		return comsEnabled.hasNonNullValue();
	}

}
