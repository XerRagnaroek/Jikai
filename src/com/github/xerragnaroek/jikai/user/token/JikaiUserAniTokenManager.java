package com.github.xerragnaroek.jikai.user.token;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jikai.anime.ani.AniListSyncer;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.core.Secrets;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 
 */
public class JikaiUserAniTokenManager {

	private static Map<Integer, JikaiUserAniToken> token = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(JikaiUserAniTokenManager.class);

	public static boolean hasToken(int aniId) {
		return token.containsKey(aniId);
	}

	public static boolean hasToken(JikaiUser ju) {
		validateUserIsLinked(ju);
		return token.containsKey(ju.getAniId());
	}

	public static JikaiUserAniToken getAniToken(int aniId) {
		return token.get(aniId);
	}

	public static JikaiUserAniToken getAniToken(JikaiUser ju) {
		validateUserIsLinked(ju);
		return token.get(ju.getAniId());
	}

	private static void validateUserIsLinked(JikaiUser ju) {
		if (!ju.hasAniId()) {
			throw new IllegalArgumentException("JikaiUser has no linked AniList account!");
		}
	}

	public static String getOAuthUrl() {
		return String.format("https://anilist.co/api/v2/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code", Secrets.ANI_CLIENT_ID, Secrets.ANI_REDIRECT_URL);
	}

	public static int getTokenFromCode(String code) {
		String codeExchangeJson = String.format("{\"grant_type\": \"authorization_code\",\"client_id\": %s, \"client_secret\" : \"%s\", \"redirect_uri\": \"%s\",\"code\":\"%s\"}", Secrets.ANI_CLIENT_ID, Secrets.ANI_SECRET, Secrets.ANI_REDIRECT_URL, code);
		try {
			JikaiUserAniToken juat = postToOAuthApi(codeExchangeJson);
			if (juat != null) {
				int aniId = AnimeDB.getJASA().getAniUserIdFromToken(juat.getAccessToken());
				token.put(aniId, juat);
				return aniId;
			}
		} catch (IOException | AniException e) {
			BotUtils.logAndSendToDev(log, "Failed getting token!", e);
		}
		return -1;
	}

	private static void refreshToken(String refresh, int aniId) {
		String refreshJson = String.format("{\"grant_type\": \"refresh_token\",\"client_id\": %s, \"client_secret\" : \"%s\", \"redirect_uri\": \"%s\",\"refresh_token\":\"%s\"}", Secrets.ANI_CLIENT_ID, Secrets.ANI_SECRET, Secrets.ANI_REDIRECT_URL, token);
		try {
			JikaiUserAniToken juat = postToOAuthApi(refreshJson);
			token.put(aniId, juat);
		} catch (IOException e) {
			BotUtils.logAndSendToDev(log, "Failed refreshing token!", e);
		}
	}

	private static JikaiUserAniToken postToOAuthApi(String json) throws IOException {
		RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
		Request request = new Request.Builder().url("https://anilist.co/api/v2/oauth/token").post(body).build();
		Response response = AnimeDB.getJASA().getAniClient().newCall(request).execute();
		ObjectMapper map = new ObjectMapper();
		JsonNode node = map.readTree(response.body().string());
		if (node.hasNonNull("error")) {
			log.error("Error repsone: {}", node.toString());
			return null;
		} else {
			return map.treeToValue(node, JikaiUserAniToken.class);
		}
	}

	public static Map<Integer, JikaiUserAniToken> getMap() {
		return token;
	}

	public static void setMap(Map<Integer, JikaiUserAniToken> map) {
		token = new ConcurrentHashMap<>(map);
	}

	public static void startCodeWatchThread() {
		Core.EXEC.execute(() -> {
			try {
				watchForCodeChanges();
			} catch (IOException | InterruptedException e) {
				log.error("Excpetion while watching codes! Restarting watch service", e);
				startCodeWatchThread();
			}
		});
	}

	private static void watchForCodeChanges() throws IOException, InterruptedException {
		Path path = Core.DATA_LOC;
		try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
			path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
			log.debug("Watching for code file changes...");
			while (true) {
				WatchKey wk = watchService.take();
				for (WatchEvent<?> event : wk.pollEvents()) {
					// we only register "ENTRY_MODIFY" so the context is always a Path.
					Path changed = (Path) event.context();
					if (changed.endsWith("codes.txt")) {
						log.debug("codes.txt changed!");
						readCodes();
					}
				}
				// reset the key
				boolean valid = wk.reset();
				if (!valid) {
					System.out.println("Key has been unregistered");
				}
			}
		}
	}

	public static void readCodes() {
		Path loc = Path.of(Core.DATA_LOC.toString(), "codes.txt");
		if (Files.exists(loc)) {
			String str = null;
			try (FileChannel fc = FileChannel.open(loc, StandardOpenOption.READ, StandardOpenOption.WRITE); FileLock lock = fc.lock()) {
				ByteBuffer buf = ByteBuffer.allocate((int) fc.size());
				fc.read(buf);
				str = new String(buf.array());
				fc.truncate(0);
			} catch (IOException e) {
				log.error("Error reading codes!", e);
			}
			if (str != null && !str.isEmpty()) {
				String[] split = str.split("\\r?\\n");
				log.debug("Processing {} codes", split.length);
				List<Integer> aniIds = Stream.of(split).parallel().distinct().map(JikaiUserAniTokenManager::getTokenFromCode).filter(i -> i > -1).collect(Collectors.toList());
				log.info("Processed {} valid codes into tokens!", aniIds.size());
				if (!aniIds.isEmpty()) {
					syncLists(aniIds);
				}
			}
		}
	}

	private static void syncLists(List<Integer> aniIds) {
		Map<Integer, JikaiUser> mapped = JikaiUserManager.getInstance().users().stream().filter(ju -> ju.getAniId() > 0).collect(Collectors.toMap(JikaiUser::getAniId, ju -> ju));
		aniIds.stream().map(id -> mapped.get(id)).filter(Objects::nonNull).forEach(AniListSyncer.getInstance()::syncAniListsWithSubs);
	}

	public static void removeToken(JikaiUser ju) {
		validateUserIsLinked(ju);
		token.remove(ju.getAniId());
	}

	public static void removeToken(int aniId) {
		token.remove(aniId);
	}

	public static void init() {
		readCodes();
		startCodeWatchThread();
	}
}
