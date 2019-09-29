package com.xerragnaroek.bot.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

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

@JsonInclude(Include.NON_NULL)
public class GuildData {
	private String gId;
	private String trigger;
	private String aniChId;
	private String listChId;
	private ZoneId zone;
	private Set<ALRHData> alrhData;
	private AtomicBoolean changed = new AtomicBoolean(false);
	private Map<GuildDataKey, Set<BiConsumer<String, String>>> strConsumer = new HashMap<>();
	private Set<BiConsumer<String, ZoneId>> zoneCons = new HashSet<>();
	private Set<BiConsumer<String, Set<ALRHData>>> alrhdCons = new HashSet<>();
	private Path fileLoc;
	private final Logger log;

	public GuildData(String guildId, boolean save) {
		log = LoggerFactory.getLogger(GuildData.class + "#" + guildId);
		gId = guildId;
		alrhData = new TreeSet<>();
		fileLoc = Paths.get(String.format("./data/%s.json", guildId));
		changed.set(save);
		log.info("Loaded configuration for {}", guildId);
	}

	@JsonProperty("trigger")
	public String getTrigger() {
		return Objects.requireNonNullElse(trigger, GuildDataManager.getBotConfig().getDefaultTrigger());
	}

	@JsonProperty("anime_channel_id")
	public String getAnimeChannelId() {
		return aniChId;
	}

	@JsonProperty("list_channel_id")
	public String getListChannelId() {
		return listChId;
	}

	@JsonProperty("guild_id")
	public String getGuildId() {
		return gId;
	}

	@JsonIgnore
	public ZoneId getTimeZone() {
		return Objects.requireNonNullElse(zone, GuildDataManager.getBotConfig().getDefaultTimeZone());
	}

	@JsonProperty("timezone")
	public String getTimeZoneString() {
		if (zone != null) {
			return zone.getId();
		} else {
			return GuildDataManager.getBotConfig().getDefaultTimeZoneString();
		}
	}

	@JsonProperty("alrh_data")
	public Set<ALRHData> getALRHData() {
		return new TreeSet<>(alrhData);
	}

	public String setTrigger(String trigger) {
		String tmp = this.trigger;
		this.trigger = trigger;
		log.info("Trigger was changed from '{}' to '{}'", tmp, trigger);
		hasChanged();
		runStringConsumer(GuildDataKey.TRIGGER, trigger);
		return tmp;
	}

	public String setAnimeChannelId(String id) {
		String tmp = aniChId;
		aniChId = id;
		log.info("AnimeChannelId was changed from '{}' to '{}'", tmp, id);
		hasChanged();
		runStringConsumer(GuildDataKey.ANIME_CHANNEL, id);
		return tmp;
	}

	public String setListChannelId(String id) {
		String tmp = listChId;
		listChId = id;
		log.info("ListChannelId was changed from '{}' to '{}'", tmp, id);
		hasChanged();
		runStringConsumer(GuildDataKey.LIST_CHANNEL, id);
		return tmp;
	}

	public ZoneId setTimeZone(ZoneId z) {
		ZoneId tmp = zone;
		zone = z;
		log.info("TimeZone was changed from '{}' to '{}'", tmp, z);
		hasChanged();
		runTimeZoneConsumer(z);
		return tmp;
	}

	public void setALRHData(Set<ALRHData> data) {
		alrhData = data;
		log.info("Data was changed");
		hasChanged();
		runALRHDataConsumer(data);
	}

	private void hasChanged() {
		changed.set(true);
	}

	private void runStringConsumer(GuildDataKey gdk, String newValue) {
		if (strConsumer != null && strConsumer.containsKey(gdk)) {
			log.debug("Running string consumer for {}", gdk);
			strConsumer.get(gdk).forEach(con -> con.accept(gId, newValue));
		}
	}

	private void runTimeZoneConsumer(ZoneId newZ) {
		if (zoneCons != null) {
			log.debug("Running TimeZone consumer");
			zoneCons.forEach(con -> con.accept(gId, newZ));
		}
	}

	private void runALRHDataConsumer(Set<ALRHData> newData) {
		if (alrhdCons != null) {
			log.debug("Running ALRHData consumer");
			alrhdCons.forEach(con -> con.accept(gId, newData));
		}
	}

	public void registerOnTriggerChange(BiConsumer<String, String> con) {
		registerOnStringOptionChangeConsumer(GuildDataKey.TRIGGER, con);
	}

	public void registerOnAnimeChannelIdChange(BiConsumer<String, String> con) {
		registerOnStringOptionChangeConsumer(GuildDataKey.ANIME_CHANNEL, con);
	}

	public void registerOnListChannelIdChange(BiConsumer<String, String> con) {
		registerOnStringOptionChangeConsumer(GuildDataKey.LIST_CHANNEL, con);
	}

	public void registerOnTimeZoneChange(BiConsumer<String, ZoneId> con) {
		if (zoneCons == null) {
			zoneCons = Collections.synchronizedSet(new HashSet<>());
		}
		zoneCons.add(con);
		log.debug("Registered a new TimeZone consumer");
	}

	public void registerOnALRHDataChange(BiConsumer<String, Set<ALRHData>> con) {
		if (alrhdCons == null) {
			alrhdCons = Collections.synchronizedSet(new HashSet<>());
		}
		alrhdCons.add(con);
		log.debug("Registered a new ALRHData consumer");
	}

	void registerOnStringOptionChangeConsumer(GuildDataKey gdk, BiConsumer<String, String> con) {
		strConsumer.compute(gdk, (k, list) -> {
			list = (list == null) ? new HashSet<>() : list;
			list.add(con);
			return list;
		});
		log.debug("Registered a new consumer for {}", gdk);
	}

	void save() {
		if (changed.get()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			try {
				Files.writeString(fileLoc, mapper.writeValueAsString(this));
				log.info("Successfully saved data to {}", fileLoc);
				changed.set(false);
			} catch (IOException e) {
				log.error("Failed saving data to [}", fileLoc, e);
			}
		}
	}

	@JsonCreator
	public static GuildData makeGuildData(@JsonProperty("guild_id") String gId, @JsonProperty("trigger") String trig,
			@JsonProperty("anime_channel_id") String aniChId, @JsonProperty("list_channel_id") String listChId,
			@JsonProperty("timezone") String zone, @JsonProperty("alrh_data") Set<ALRHData> data) {
		GuildData gd = new GuildData(gId, false);
		gd.trigger = trig;
		gd.aniChId = aniChId;
		gd.listChId = listChId;
		gd.zone = ZoneId.of(zone);
		gd.alrhData = data;
		return gd;
	}
}
