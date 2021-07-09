
package com.github.xerragnaroek.jikai.jikai;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.list.ALRHData;
import com.github.xerragnaroek.jikai.anime.list.ALRHandler;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.prop.IntegerProperty;
import com.github.xerragnaroek.jikai.util.prop.LongProperty;
import com.github.xerragnaroek.jikai.util.prop.MapProperty;
import com.github.xerragnaroek.jikai.util.prop.Property;
// @JsonPropertyOrder({ "guild_id", "completed_setup", "commands_enabled", "prefix", "timezone",
// "language", "exec_command_count", "list_channel_romaji_id", "list_channel_native_id",
// "list_channel_english_id", "schedule_channel_id", "anime_channel_id", "info_channel_id",
// "command_channel_id", "last_mentioned", "msg_id_title_romaji", "alrh_data_romaji",
// "season_msg_romaji", "msg_id_title_english", "alrh_data_english", "season_msg_english",
// "msg_id_title_native", "alrh_data_native", "season_msg_native" })

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder(alphabetic = true)
public class JikaiData {
	@JsonProperty("anime_channel_id")
	private LongProperty aniChId = new LongProperty(0l);
	@JsonIgnore
	private AtomicBoolean changed = new AtomicBoolean(false);
	@JsonProperty("completed_setup")
	private Property<Boolean> completedSetup = new Property<>(false);
	@JsonProperty("commands_enabled")
	private Property<Boolean> comsEnabled = new Property<>(true);
	@JsonIgnore
	private Path fileLoc;
	@JsonProperty("guild_id")
	private long gId;
	@JsonProperty("info_channel_id")
	private LongProperty infoChId = new LongProperty(0l);
	@JsonProperty("list_channel_romaji_id")
	private LongProperty listChRomajiId = new LongProperty(0l);
	@JsonProperty("list_channel_native_id")
	private LongProperty listChNativeId = new LongProperty(0l);
	@JsonProperty("list_channel_english_id")
	private LongProperty listChEnglishId = new LongProperty(0l);
	@JsonProperty("list_channel_adult_id")
	private LongProperty listChAdultId = new LongProperty(0l);
	@JsonProperty("command_channel_id")
	private LongProperty commandChId = new LongProperty(0l);
	@JsonIgnore
	private final Logger log;
	@JsonProperty("schedule_channel_id")
	private LongProperty schedChId = new LongProperty(0l);
	private Property<String> prefix = new Property<>();
	@JsonIgnore
	private Property<ZoneId> zone = new Property<>();
	@JsonProperty("exec_command_count")
	private IntegerProperty execComs = new IntegerProperty(0);
	@JsonIgnore
	private Property<JikaiLocale> locale = new Property<>(JikaiLocaleManager.getEN());
	@JsonIgnore
	private BotData bd = JM.getJDM().getBotData();
	@JsonProperty("adult_role")
	private LongProperty adultRole = new LongProperty(0l);
	@JsonProperty("title_language_roles")
	private Map<TitleLanguage, Long> titleLanguageRoles = new MapProperty<>();
	@JsonProperty("user_role")
	private LongProperty userRole = new LongProperty(0l);

	public JikaiData(long guildId, boolean save) {
		log = LoggerFactory.getLogger(JikaiData.class + "#" + guildId);
		gId = guildId;
		fileLoc = Paths.get(String.format("./data/guilds/%s.json", guildId));
		zone.onChange((old, n) -> {
			if (old != null) {
				JM.removeTimeZone(old, guildId);
			}
			JM.addTimeZone(n, guildId);
		});
		setPrefix(bd.getDefaultPrefix());
		setTimeZone(bd.getDefaultTimeZone());
		changed.set(save);
		log.info("Made configuration for {}", guildId);
	}

	public LongProperty animeChannelIdProperty() {
		return aniChId;
	}

	// @JsonProperty("commands_enabled")
	public boolean areCommandsEnabled() {
		return comsEnabled.get();
	}

	public Property<Boolean> comsEnabledProperty() {
		return comsEnabled;
	}

	@JsonIgnore
	public Set<ALRHData> getALRHData(TitleLanguage lang) {
		ALRHandler alrh = JM.get(gId).getALRHandler(lang);
		if (alrh != null) {
			return alrh.getData();
		} else {
			return Collections.emptySet();
		}
	}

	@JsonProperty("alrh_data_romaji")
	public Set<ALRHData> getALRHDataRomaji() {
		return getALRHData(TitleLanguage.ROMAJI);
	}

	@JsonProperty("alrh_data_english")
	public Set<ALRHData> getALRHDataEnglish() {
		return getALRHData(TitleLanguage.ENGLISH);
	}

	@JsonProperty("alrh_data_native")
	public Set<ALRHData> getALRHDataNative() {
		return getALRHData(TitleLanguage.NATIVE);
	}

	@JsonIgnore
	public Map<Long, String> getMessageIdTitleMap(TitleLanguage lang) {
		ALRHandler alrh = JM.get(gId).getALRHandler(lang);
		if (alrh != null) {
			return alrh.getMessageIdTitleMap();
		} else {
			return Collections.emptyMap();
		}
	}

	@JsonProperty("msg_id_title_romaji")
	public Map<Long, String> getMessageIdTitleMapRomaji() {
		return getMessageIdTitleMap(TitleLanguage.ROMAJI);
	}

	@JsonProperty("msg_id_title_english")
	public Map<Long, String> getMessageIdTitleMapEnglish() {
		return getMessageIdTitleMap(TitleLanguage.ENGLISH);
	}

	@JsonProperty("msg_id_title_native")
	public Map<Long, String> getMessageIdTitleMapNative() {
		return getMessageIdTitleMap(TitleLanguage.NATIVE);
	}

	@JsonIgnore
	public Pair<String, Long> getSeasonMsg(TitleLanguage lang) {
		ALRHandler alrh = JM.get(gId).getALRHandler(lang);
		if (alrh != null) {
			return alrh.getSeasonMsg();
		} else {
			return null;
		}
	}

	@JsonProperty("season_msg_romaji")
	public Pair<String, Long> getSeasonMsgRomaji() {
		return getSeasonMsg(TitleLanguage.ROMAJI);
	}

	@JsonProperty("season_msg_english")
	public Pair<String, Long> getSeasonMsgEnglish() {
		return getSeasonMsg(TitleLanguage.ENGLISH);
	}

	@JsonProperty("season_msg_native")
	public Pair<String, Long> getSeasonMsgNative() {
		return getSeasonMsg(TitleLanguage.NATIVE);
	}

	// @JsonProperty("anime_channel_id")
	public long getAnimeChannelId() {
		return aniChId.get();
	}

	// @JsonProperty("guild_id")
	public long getGuildId() {
		return gId;
	}

	// @JsonProperty("info_channel_id")
	public long getInfoChannelId() {
		return infoChId.get();
	}

	@JsonIgnore
	public long getListChannelId(TitleLanguage lang) {
		long id = 0;
		switch (lang) {
			case ROMAJI -> id = getListChannelRomajiId();
			case ENGLISH -> id = getListChannelEnglishId();
			case NATIVE -> id = getListChannelNativeId();
		}
		return id;
	}

	// @JsonProperty("list_channel_romaji_id")
	public long getListChannelRomajiId() {
		return listChRomajiId.get();
	}

	// @JsonProperty("list_channel_native_id")
	public long getListChannelNativeId() {
		return listChNativeId.get();
	}

	// @JsonProperty("list_channel_english_id")
	public long getListChannelEnglishId() {
		return listChEnglishId.get();
	}

	// @JsonProperty("list_channel_adult_id")
	public long getListChannelAdultId() {
		return listChAdultId.get();
	}

	// @JsonProperty("schedule_channel_id")
	public long getScheduleChannelId() {
		return schedChId.get();
	}

	// @JsonProperty("command_channel_id")
	public long getCommandChannelId() {
		return commandChId.get();
	}

	// @JsonProperty("exec_command_count")
	public int getExecutedCommandCount() {
		return execComs.get();
	}

	@JsonIgnore
	public JikaiLocale getLocale() {
		return locale.get();
	}

	@JsonProperty("language")
	public String getLocaleIdentifier() {
		return locale.get().getIdentifier();
	}

	@JsonIgnore
	public ZoneId getTimeZone() {
		return zone.get();
	}

	@JsonProperty("timezone")
	public String getTimeZoneString() {
		return zone.get().getId();
	}

	// @JsonProperty("prefix")
	public String getPrefix() {
		return prefix.get();
	}

	@JsonProperty("completed_setup")
	public boolean hasCompletedSetup() {
		return completedSetup != null && completedSetup.hasNonNullValue() && completedSetup.get();
	}

	public boolean hasScheduleChannelId() {
		return schedChId.get() > 0;
	}

	public boolean hasListChannelId() {
		return listChRomajiId.get() > 0;
	}

	public boolean hasInfoChannelId() {
		return infoChId.get() > 0;
	}

	public boolean hasAnimeChannelId() {
		return aniChId.get() > 0;
	}

	public boolean hasCommandChannelId() {
		return commandChId.get() > 0;
	}

	public LongProperty infoChannelIdProperty() {
		return infoChId;
	}

	public LongProperty listChannelRomajiIdProperty() {
		return listChRomajiId;
	}

	public LongProperty listChannelNativeIdProperty() {
		return listChNativeId;
	}

	public LongProperty listChannelEnglishIdProperty() {
		return listChEnglishId;
	}

	public LongProperty listChannelAdultIdProperty() {
		return listChAdultId;
	}

	public LongProperty scheduleChannelIdProperty() {
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

	public long setListChannelId(long id, TitleLanguage lang) {
		long oldId = 0;
		switch (lang) {
			case ROMAJI -> oldId = setData(listChRomajiId, id, "list_channel_romaji_id");
			case ENGLISH -> oldId = setData(listChEnglishId, id, "list_channel_english_id");
			case NATIVE -> oldId = setData(listChNativeId, id, "list_channel_native_id");
		}
		return oldId;
	}

	public long setListChannelAdultId(long id) {
		long old = listChAdultId.get();
		setData(listChAdultId, id, "list_channel_adult_id");
		return old;
	}

	public long setScheduleChannelId(long id) {
		return setData(schedChId, id, "schedule_channel_id");
	}

	public void setCommandChannelId(long idLong) {
		commandChId.set(idLong);
	}

	public boolean setSetupCompleted(boolean setup) {
		return setData(completedSetup, setup, "completed_setup");
	}

	public ZoneId setTimeZone(ZoneId z) {
		return setData(zone, z, "timezone");
	}

	public String setPrefix(String prefix) {
		// prefix = StringEscapeUtils.escapeJava(prefix);
		prefix = prefix.split("\\n")[0];
		if (prefix.length() > 3 && StringUtils.isAlphanumericSpace(prefix)) {
			prefix = prefix.trim() + " ";
		}
		return setData(this.prefix, prefix, "prefix");
	}

	public JikaiLocale setLocale(JikaiLocale loc) {
		return setData(locale, loc, "language");
	}

	public Property<ZoneId> timeZoneProperty() {
		return zone;
	}

	public Property<String> prefixProperty() {
		return prefix;
	}

	public long getAdultRoleId() {
		return adultRole.get();
	}

	public void setAdultRoleId(long id) {
		setData(adultRole, id, "adult_role");
	}

	public long getTitleLanguageRole(TitleLanguage tl) {
		return titleLanguageRoles.get(tl);
	}

	public Map<TitleLanguage, Long> getTitleLanguageRoles() {
		return new HashMap<>(titleLanguageRoles);
	}

	public void setTitleLanguageRole(TitleLanguage tl, long id) {
		titleLanguageRoles.put(tl, id);
	}

	public void setJikaiUserRole(long id) {
		setData(userRole, id, "user_role");
	}

	public long getJikaiUserRole() {
		return userRole.get();
	}

	private void hasChanged() {
		changed.set(true);
	}

	private boolean updatesAvailable() {
		boolean tmp = false;
		log.debug("Seeing if any data changed:");
		if (changed.get()) {
			log.debug("JikaiData changed");
			tmp = true;
		}
		for (TitleLanguage lang : TitleLanguage.values()) {
			ALRHandler h = JM.get(gId).getALRHandler(lang);
			if (h != null && h.isInitialized() && h.hasUpdateFlagAndReset()) {
				log.debug("ALHRData {} changed", lang);
				tmp = true;
			}
		}
		return tmp;
	}

	private <T> T setData(Property<T> field, T newData, String name) {
		T tmp = field.get();
		field.set(newData);
		log.debug(name + " changed '{}'->'{}'", tmp, newData);
		hasChanged();
		return tmp;
	}

	/*
	 * @JsonCreator
	 * public static JikaiData of(@JsonProperty("language") String
	 * lang, @JsonProperty("exec_command_count") Property<Integer> execComs, @JsonProperty("guild_id")
	 * long gId, @JsonProperty("prefix") Property<String> pre, @JsonProperty("anime_channel_id")
	 * LongProperty aniChId, @JsonProperty("list_channel_romaji_id") LongProperty
	 * listChRomajiId, @JsonProperty("list_channel_english_id") LongProperty
	 * listChEnglishId, @JsonProperty("list_channel_native_id") LongProperty
	 * listChNativeId, @JsonProperty("list_channel_adult_id") LongProperty
	 * listChAdultId, @JsonProperty("timezone") String zone, @JsonProperty("alrh_data_romaji")
	 * Set<ALRHData> dataRomaji, @JsonProperty("alrh_data_english") Set<ALRHData>
	 * dataEnglish, @JsonProperty("alrh_data_native") Set<ALRHData>
	 * dataNative, @JsonProperty("completed_setup") Property<Boolean>
	 * setupCompleted, @JsonProperty("commands_enabled") Property<Boolean>
	 * comsEnabled, @JsonProperty("info_channel_id") LongProperty
	 * icId, @JsonProperty("schedule_channel_id") LongProperty
	 * schId, @JsonProperty("command_channel_id") LongProperty
	 * comChId, @JsonProperty("msg_id_title_romaji") Map<Long, String>
	 * msgIdTitleMapRomaji, @JsonProperty("msg_id_title_english") Map<Long, String>
	 * msgIdTitleMapEnglish, @JsonProperty("msg_id_title_native") Map<Long, String>
	 * msgIdTitleMapNative, @JsonProperty("season_msg_romaji") Pair<String, Long>
	 * seasonMsgRomaji, @JsonProperty("season_msg_english") Pair<String, Long>
	 * seasonMsgEnglish, @JsonProperty("season_msg_native") Pair<String, Long> seasonMsgNative) {
	 * JikaiData jd = new JikaiData(gId, false);
	 * if (zone != null) {
	 * jd.zone = Property.of(ZoneId.of(zone));
	 * }
	 * setIfNonNull(jd.prefix, pre);
	 * setIfNonNull(jd.aniChId, aniChId);
	 * setIfNonNull(jd.listChRomajiId, listChRomajiId);
	 * setIfNonNull(jd.listChEnglishId, listChEnglishId);
	 * setIfNonNull(jd.listChNativeId, listChNativeId);
	 * setIfNonNull(jd.listChAdultId, listChAdultId);
	 * setIfNonNull(jd.completedSetup, setupCompleted);
	 * setIfNonNull(jd.comsEnabled, comsEnabled);
	 * setIfNonNull(jd.infoChId, icId);
	 * setIfNonNull(jd.schedChId, schId);
	 * setIfNonNull(jd.commandChId, comChId);
	 * setIfNonNull(jd.execComs, execComs);
	 * setIfNonNull(jd.locale, new Property<>(JikaiLocaleManager.getInstance().getLocale(lang)));
	 * JM.getALHRM().addToInitMap(gId, dataRomaji, msgIdTitleMapRomaji, seasonMsgRomaji,
	 * TitleLanguage.ROMAJI);
	 * JM.getALHRM().addToInitMap(gId, dataEnglish, msgIdTitleMapEnglish, seasonMsgEnglish,
	 * TitleLanguage.ENGLISH);
	 * JM.getALHRM().addToInitMap(gId, dataNative, msgIdTitleMapNative, seasonMsgNative,
	 * TitleLanguage.NATIVE);
	 * return jd;
	 * }
	 */

	@JsonCreator
	public static JikaiData of(@JsonProperty("language") String lang, @JsonProperty("guild_id") long gId, @JsonProperty("timezone") String zone, @JsonProperty("alrh_data_romaji") Set<ALRHData> dataRomaji, @JsonProperty("alrh_data_english") Set<ALRHData> dataEnglish, @JsonProperty("alrh_data_native") Set<ALRHData> dataNative, @JsonProperty("msg_id_title_romaji") Map<Long, String> msgIdTitleMapRomaji, @JsonProperty("msg_id_title_english") Map<Long, String> msgIdTitleMapEnglish, @JsonProperty("msg_id_title_native") Map<Long, String> msgIdTitleMapNative, @JsonProperty("season_msg_romaji") Pair<String, Long> seasonMsgRomaji, @JsonProperty("season_msg_english") Pair<String, Long> seasonMsgEnglish, @JsonProperty("season_msg_native") Pair<String, Long> seasonMsgNative) {
		JikaiData jd = new JikaiData(gId, false);
		setIfNonNull(jd.locale, new Property<>(JikaiLocaleManager.getInstance().getLocale(lang)));
		JM.getALHRM().addToInitMap(gId, dataRomaji, msgIdTitleMapRomaji, seasonMsgRomaji, TitleLanguage.ROMAJI);
		JM.getALHRM().addToInitMap(gId, dataEnglish, msgIdTitleMapEnglish, seasonMsgEnglish, TitleLanguage.ENGLISH);
		JM.getALHRM().addToInitMap(gId, dataNative, msgIdTitleMapNative, seasonMsgNative, TitleLanguage.NATIVE);
		return jd;
	}

	public boolean hasExplicitCommandSetting() {
		return comsEnabled.hasNonNullValue();
	}

	public int incrementAndGetExecComs() {
		log.info("exec_command_count updated to '{}'", execComs.incrementAndGet());
		hasChanged();
		return execComs.get();
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

	private static <T> void setIfNonNull(Property<T> gdp, Property<T> prop) {
		if (prop != null) {
			gdp.set(prop.get());
		}
	}

}
