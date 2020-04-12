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
package com.github.xerragnaroek.jikai.jikai;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.text.StringEscapeUtils;
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
import com.github.xerragnaroek.jikai.anime.alrh.ALRHData;
import com.github.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Destroyable;
import com.github.xerragnaroek.jikai.util.prop.IntegerProperty;
import com.github.xerragnaroek.jikai.util.prop.Property;

@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder({ "guild_id", "completed_setup", "commands_enabled", "trigger", "timezone", "exec_command_count", "list_channel_id", "schedule_channel_id", "anime_channel_id", "info_channel_id", "last_mentioned", "alrh_data" })
public class JikaiData implements Destroyable {
	private Property<Long> aniChId = new Property<>(0l);
	private AtomicBoolean changed = new AtomicBoolean(false);
	private Property<Boolean> completedSetup = new Property<>(false);
	private Property<Boolean> comsEnabled = new Property<>(true);
	private Path fileLoc;
	private long gId;
	private Property<Long> infoChId = new Property<>(0l);
	private Property<Long> listChId = new Property<>(0l);
	private final Logger log;
	private Property<Long> schedChId = new Property<>(0l);
	private List<Long> schedMsgIds;
	private Property<String> trigger = new Property<>();
	private Property<ZoneId> zone = new Property<>();
	private IntegerProperty execComs = new IntegerProperty(0);
	private BotData bd = JM.getJDM().getBotData();

	public JikaiData(long guildId, boolean save) {
		log = LoggerFactory.getLogger(JikaiData.class + "#" + guildId);
		gId = guildId;
		fileLoc = Paths.get(String.format("./data/%s.json", guildId));
		setTrigger(bd.getDefaultTrigger());
		setTimeZone(bd.getDefaultTimeZone());
		changed.set(save);
		log.info("Made configuration for {}", guildId);
	}

	public Property<Long> animeChannelIdProperty() {
		return aniChId;
	}

	@JsonProperty("commands_enabled")
	public boolean areCommandsEnabled() {
		return comsEnabled.get();
	}

	public Property<Boolean> comsEnabledProperty() {
		return comsEnabled;
	}

	@JsonProperty("alrh_data")
	public Set<ALRHData> getALRHData() {
		ALRHandler alrh = JM.get(gId).getALRHandler();
		if (alrh != null) {
			return alrh.getData();
		} else {
			return Collections.emptySet();
		}

	}

	@JsonProperty("anime_channel_id")
	public long getAnimeChannelId() {
		return aniChId.get();
	}

	@JsonProperty("guild_id")
	public long getGuildId() {
		return gId;
	}

	@JsonProperty("info_channel_id")
	public long getInfoChannelId() {
		return infoChId.get();
	}

	@JsonProperty("list_channel_id")
	public long getListChannelId() {
		return listChId.get();
	}

	@JsonProperty("schedule_channel_id")
	public long getScheduleChannelId() {
		return schedChId.get();
	}

	@JsonProperty("exec_command_count")
	public int getExecutedCommandCount() {
		return execComs.get();
	}

	@JsonProperty("schedule_message_ids")
	public List<Long> getScheduleMessageIds() {
		return schedMsgIds;
	}

	@JsonIgnore
	public ZoneId getTimeZone() {
		return zone.get();
	}

	@JsonProperty("timezone")
	public String getTimeZoneString() {
		return zone.get().getId();
	}

	@JsonProperty("trigger")
	public String getTrigger() {
		return trigger.get();
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

	public Property<Long> infoChannelIdProperty() {
		return infoChId;
	}

	public Property<Long> listChannelIdProperty() {
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

	public Property<Long> scheduleChannelIdProperty() {
		return schedChId;
	}

	public long setAnimeChannelId(long id) {
		return setData(aniChId, id, "anime_channel_id");
	}

	public boolean setCommandsEnabled(boolean enabled) {
		return Objects.requireNonNullElse(setData(comsEnabled, enabled, "commands_enabled"), false);
	}

	public long setInfoChannelId(long id) {
		return setData(infoChId, id, "info_channel_id");
	}

	public int setExecutedCommandCount(int c) {
		return setData(execComs, c, "exec_command_count");
	}

	public long setListChannelId(long id) {
		return setData(listChId, id, "list_channel_id");
	}

	public long setScheduleChannelId(long id) {
		return setData(schedChId, id, "schedule_channel_id");
	}

	public List<Long> setScheduleMessageIds(List<Long> ids) {
		List<Long> tmp = schedMsgIds;
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
		trigger = StringEscapeUtils.escapeJson(trigger);
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
		ALRHandler h = JM.get(gId).getALRHandler();
		if (h != null && h.isInitialized() && h.hasUpdateFlagAndReset()) {
			log.debug("ALHRData changed");
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
	public static JikaiData of(@JsonProperty("exec_command_count") Property<Integer> execComs, @JsonProperty("guild_id") long gId, @JsonProperty("trigger") Property<String> trig, @JsonProperty("anime_channel_id") Property<Long> aniChId, @JsonProperty("list_channel_id") Property<Long> listChId, @JsonProperty("timezone") String zone, @JsonProperty("alrh_data") Set<ALRHData> data, @JsonProperty("completed_setup") Property<Boolean> setupCompleted, @JsonProperty("commands_enabled") Property<Boolean> comsEnabled, @JsonProperty("info_channel_id") Property<Long> icId, @JsonProperty("schedule_channel_id") Property<Long> schId, @JsonProperty("schedule_message_ids") List<Long> schedMsgIds) {
		JikaiData gd = new JikaiData(gId, false);
		setIfNonNull(gd.trigger, trig);
		setIfNonNull(gd.aniChId, aniChId);
		setIfNonNull(gd.listChId, listChId);
		if (zone != null) {
			gd.zone = Property.of(ZoneId.of(zone));
		}
		setIfNonNull(gd.completedSetup, setupCompleted);
		setIfNonNull(gd.comsEnabled, comsEnabled);
		setIfNonNull(gd.infoChId, icId);
		setIfNonNull(gd.schedChId, schId);
		gd.schedMsgIds = schedMsgIds;
		setIfNonNull(gd.execComs, execComs);
		JM.getALHRM().addToInitMap(gId, data);
		return gd;
	}

	public boolean hasExplicitCommandSetting() {
		return comsEnabled.hasNonNullValue();
	}

	public int incrementAndGetExecComs() {
		log.info("exec_command_count updated to '{}'", execComs.incrementAndGet());
		hasChanged();
		return execComs.get();
	}

	private static <T> void setIfNonNull(Property<T> gdp, Property<T> prop) {
		if (prop != null) {
			gdp.set(prop.get());
		}
	}

	@Override
	public void destroy() {
		aniChId.destroy();
		completedSetup.destroy();
		comsEnabled.destroy();
		infoChId.destroy();
		listChId.destroy();
		schedChId.destroy();
		schedMsgIds.clear();
		trigger.destroy();
		execComs.destroy();
	}
}
