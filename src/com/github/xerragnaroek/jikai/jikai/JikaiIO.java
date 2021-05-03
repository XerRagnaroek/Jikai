
package com.github.xerragnaroek.jikai.jikai;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.EpisodeTracker;
import com.github.xerragnaroek.jikai.user.ExportKeyHandler;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

public class JikaiIO {
	private final static Logger log = LoggerFactory.getLogger(JikaiIO.class);

	public static void save(boolean now) {
		JM.getJDM().save(now);
		try {
			saveJikaiUsers();
			JikaiUserManager.getInstance().save();
			ScheduleManager.saveSchedule();
			// saveReleaseMessageIds();
			// saveAnimeReleaseTracker();
			saveExportKeyHandler();
			saveEpisodeTracker();
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed saving", e);
		}
	}

	public static void load() {
		log.info("Loading data...");
		Path loc = Core.DATA_LOC;
		try {
			if (Files.exists(loc)) {
				Files.list(loc).forEach(path -> {
					log.info("Found file {}", path.toAbsolutePath());
					String fileName = path.getFileName().toString();

					switch (fileName) {
						case "BOT.json" -> JM.getJDM().loadBotData(path);
						// case "user.db" -> JikaiUserManager.getInstance().load(path);
						case "users.json" -> loadJikaiUsers(path);
						case "schedules.json" -> ScheduleManager.loadSchedules(path);
						case "guilds" -> loadGuilds(path);
						case "keys.json" -> loadExportKeyHandler(path);
						// case "art.json" -> loadAnimeReleaseTracker(path);
					}
				});
				// loadReleaseMessages(Path.of(loc.toString(), "/rmids.txt"));
				loadEpisodeTracker();
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				Files.createDirectory(loc);
			}
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading the data!", e);
		}
	}

	private static void loadGuilds(Path path) {
		JikaiDataManager jdm = JM.getJDM();
		try {
			Files.list(path).forEach(jdm::loadData);
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading guild data!", e);
		}
	}

	/*
	 * private static void loadReleaseMessages(Path path) {
	 * try {
	 * ReleaseMessageReactionHandler.getRMRH().setReleaseMessageIds(Files.lines(path).map(Long::
	 * parseLong).collect(Collectors.toSet()));
	 * } catch (IOException e) {
	 * Core.ERROR_LOG.error("Failed loading the release message ids!", e);
	 * }
	 * }
	 * private static void saveReleaseMessageIds() throws IOException {
	 * Set<String> ids =
	 * ReleaseMessageReactionHandler.getRMRH().getReleaseMessageIds().stream().map(String::valueOf).
	 * collect(Collectors.toSet());
	 * Path loc = Paths.get(Core.DATA_LOC.toString(), "/rmids.txt");
	 * log.debug("Writing {} release message ids to '{}'", ids.size(), loc.toAbsolutePath());
	 * Files.write(loc, ids, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	 * }
	 */
	private static void loadEpisodeTracker() throws IOException {
		Path rmids = Path.of(Core.DATA_LOC.toString(), "/rmids.txt");
		if (Files.exists(rmids)) {
			EpisodeTracker.loadOld(Files.lines(rmids).map(Long::parseLong).collect(Collectors.toSet()));
			Files.delete(rmids);
		} else {
			Path ets = Path.of(Core.DATA_LOC.toString(), "/et.json");
			if (Files.exists(ets)) {
				TypeReference<Map<Long, Map<Integer, Map<Long, Integer>>>> ref = new TypeReference<>() {};
				Map<Long, Map<Integer, Map<Long, Integer>>> map = new ObjectMapper().readValue(Files.readString(ets), ref);
				EpisodeTracker.load(map);
			}
		}
	}

	public static void startSaveThread(long delay, TimeUnit unit) {
		Core.EXEC.scheduleAtFixedRate(() -> JikaiIO.save(false), delay, delay, unit);
	}

	private static void saveEpisodeTracker() throws JsonProcessingException, IOException {
		Map<Long, Map<Integer, Map<Long, Integer>>> ets = EpisodeTracker.getSavableMap();
		Path loc = Paths.get(Core.DATA_LOC.toString(), "/et.json");
		log.debug("Saving EpisodeTracker, size {}, to {}", ets.size(), loc.toAbsolutePath());
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		Files.writeString(loc, mapper.writeValueAsString(ets), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}

	private static void saveExportKeyHandler() throws JsonProcessingException, IOException {
		Map<Long, String> map = ExportKeyHandler.getInstance().getKeyMap();
		Path loc = Path.of(Core.DATA_LOC.toString(), "/keys.json");
		log.debug("Saving SubscriptionExportHandler, size {}, to {}", map.size(), loc.toAbsolutePath());
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		// mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
		// DefaultTyping.OBJECT_AND_NON_CONCRETE);
		Files.writeString(loc, mapper.writeValueAsString(map), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}

	private static void loadExportKeyHandler(Path path) {
		TypeReference<TreeMap<Long, String>> ref = new TypeReference<TreeMap<Long, String>>() {};
		try {
			Map<Long, String> map = new ObjectMapper().readValue(Files.readString(path), ref);
			log.debug("Loaded map(size {}) for subscription export handler", map.size());
			ExportKeyHandler.loadMap(map);
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading the subscription export handler!", e);
		}
	}

	private static void saveJikaiUsers() throws JsonProcessingException, IOException {
		Path loc = Path.of(Core.DATA_LOC.toString(), "/users.json");
		Set<JikaiUser> user = JikaiUserManager.getInstance().users().stream().filter(JikaiUser::isSetupCompleted).collect(Collectors.toSet());
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		Files.writeString(loc, mapper.writeValueAsString(user), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		log.debug("Saved {} users to {}", user.size(), loc.toString());
	}

	private static void loadJikaiUsers(Path loc) {
		TypeReference<Set<JikaiUser>> ref = new TypeReference<>() {};
		try {
			Set<JikaiUser> set = new ObjectMapper().readValue(Files.readString(loc), ref);
			JikaiUserManager jum = JikaiUserManager.getInstance();
			set.forEach(jum::loadUser);
			jum.setUpLinks();
		} catch (IOException e) {
			log.error("Failed reading users", e);
		}
	}
	/*
	 * private static void saveAnimeReleaseTracker() throws JsonProcessingException, IOException {
	 * Map<Integer, BoundedArrayList<Long>> map = AnimeReleaseTracker.getInstance().getMap();
	 * Path loc = Path.of(Core.DATA_LOC.toString(), "/art.json");
	 * log.debug("Saving Anime Release Tracker, size {}, to {}", map.size(), loc.toAbsolutePath());
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.enable(SerializationFeature.INDENT_OUTPUT);
	 * //
	 * mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class
	 * ).build(),
	 * // DefaultTyping.OBJECT_AND_NON_CONCRETE);
	 * Files.writeString(loc, mapper.writeValueAsString(map), StandardOpenOption.TRUNCATE_EXISTING,
	 * StandardOpenOption.CREATE);
	 * }
	 * private static void loadAnimeReleaseTracker(Path path) {
	 * ObjectMapper mapper = new ObjectMapper();
	 * //
	 * mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class
	 * ).build(),
	 * // DefaultTyping.OBJECT_AND_NON_CONCRETE);
	 * TypeFactory def = TypeFactory.defaultInstance();
	 * JavaType type = def.constructMapLikeType(TreeMap.class,
	 * def.constructFromCanonical("java.lang.Integer"),
	 * def.constructParametricType(BoundedArrayList.class, Long.class));
	 * try {
	 * AnimeReleaseTracker.getInstance().loadMap(mapper.readValue(Files.readString(path), type));
	 * } catch (IOException e) {
	 * Core.ERROR_LOG.error("Failed loading the anime release tracker!", e);
	 * }
	 * }
	 */
}
