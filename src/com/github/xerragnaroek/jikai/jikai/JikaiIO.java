
package com.github.xerragnaroek.jikai.jikai;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.xerragnaroek.jikai.anime.db.AnimeReleaseTracker;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.user.ReleaseMessageReactionHandler;

public class JikaiIO {
	private final static Logger log = LoggerFactory.getLogger(JikaiIO.class);

	public static void save(boolean now) {
		JM.getJDM().save(now);
		try {
			JikaiUserManager.getInstance().save();
			ScheduleManager.saveSchedule();
			saveReleaseMessageIds();
			saveAnimeReleaseTracker();
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
						case "user.db" -> JikaiUserManager.getInstance().load(path);
						case "schedules.json" -> ScheduleManager.loadSchedules(path);
						case "guilds" -> loadGuilds(path);
						case "rmids.txt" -> loadReleaseMessages(path);
						case "art.json" -> loadAnimeReleaseTracker(path);
					}
				});

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

	private static void loadReleaseMessages(Path path) {
		try {
			ReleaseMessageReactionHandler.getRMRH().setReleaseMessageIds(Files.lines(path).map(Long::parseLong).collect(Collectors.toSet()));
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading the release message ids!", e);
		}
	}

	public static void startSaveThread(long delay, TimeUnit unit) {
		Core.EXEC.scheduleAtFixedRate(() -> JikaiIO.save(false), delay, delay, unit);
	}

	private static void saveReleaseMessageIds() throws IOException {
		Set<String> ids = ReleaseMessageReactionHandler.getRMRH().getReleaseMessageIds().stream().map(String::valueOf).collect(Collectors.toSet());
		Path loc = Paths.get(Core.DATA_LOC.toString(), "/rmids.txt");
		log.debug("Writing {} release message ids to '{}'", ids.size(), loc.toAbsolutePath());
		Files.write(loc, ids, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private static void saveAnimeReleaseTracker() throws JsonProcessingException, IOException {
		Map<Integer, Queue<Long>> map = AnimeReleaseTracker.getInstance().getMap();
		Path loc = Path.of(Core.DATA_LOC.toString(), "/art.json");
		log.debug("Saving Anime Release Tracker, size {}, to {}", map.size(), loc.toAbsolutePath());
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		// mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
		// DefaultTyping.OBJECT_AND_NON_CONCRETE);
		Files.writeString(loc, mapper.writeValueAsString(map), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}

	private static void loadAnimeReleaseTracker(Path path) {
		ObjectMapper mapper = new ObjectMapper();
		// mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
		// DefaultTyping.OBJECT_AND_NON_CONCRETE);
		TypeFactory def = TypeFactory.defaultInstance();
		JavaType type = def.constructMapLikeType(TreeMap.class, def.constructFromCanonical("java.lang.Integer"), def.constructParametricType(CircularFifoQueue.class, Long.class));
		try {
			AnimeReleaseTracker.getInstance().loadMap(mapper.readValue(Files.readString(path), type));
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading the anime release tracker!", e);
		}
	}
}
