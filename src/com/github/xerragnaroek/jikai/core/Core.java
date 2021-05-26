
package com.github.xerragnaroek.jikai.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.ani.AniListSyncer;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.JikaiManager;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.PrivateList;
import com.github.xerragnaroek.jikai.user.link.LinkRequest;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.BooleanProperty;
import com.github.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Core {

	private final static Logger log = LoggerFactory.getLogger(Core.class);
	public static JDA JDA;
	private static String token;
	public static List<Long> DEV_IDS = new LinkedList<>();
	private static long saveDelay;
	private static long aniSyncMinutes;
	private static long linkRequestDuration;
	private static long privateListDuration;
	public final static Logger ERROR_LOG = LoggerFactory.getLogger("ERROR");
	public static final Path DATA_LOC = Paths.get("./data/");
	public static final ScheduledExecutorService EXEC = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	public static final Property<String> CUR_SEASON = new Property<>();
	public static BooleanProperty INITIAL_LOAD = new BooleanProperty(true);
	public static final JikaiManager JM = new JikaiManager();

	public static void main(String[] args) throws LoginException, InterruptedException, ExecutionException, ClassNotFoundException, IOException {
		Instant start = Instant.now();
		log.info("Initializing");
		long mem = Runtime.getRuntime().freeMemory();
		handleArgs(args);
		JDABuilder builder = JDABuilder.create(token, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS);
		builder.disableCache(Arrays.asList(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE));
		builder.setEventPool(EXEC, true);
		builder.addEventListeners(new JikaiEventListener());
		JDA = builder.build();
		JDA.awaitReady();
		init(args);
		sendOnlineMsg(start);
		log.info("Initialized, using a total of " + ((mem - Runtime.getRuntime().freeMemory()) / 1024) + " kb of memory");
	}

	private static void init(String[] args) {
		RestAction.setDefaultFailure(Core::logThrowable);
		CUR_SEASON.dontRunConsumerIf(() -> INITIAL_LOAD.get());
		JM.init();
		JM.startSaveThread(saveDelay);
		JM.forEach(j -> j.validateMemberRoles());
		INITIAL_LOAD.set(false);
		AniListSyncer.init();
		AniListSyncer.startSyncThread(aniSyncMinutes);
		LinkRequest.setBidiRequestDuration(linkRequestDuration);
		PrivateList.setListDuration(privateListDuration);
	}

	private static void sendOnlineMsg(Instant start) {
		long millis = Duration.between(start, Instant.now()).toMillis();
		JM.forEach(j -> {
			JikaiLocale loc = j.getLocale();
			try {
				if (j.hasInfoChannelSet()) {
					j.getInfoChannel().sendMessage(loc.getStringFormatted("g_online_msg", Arrays.asList("time"), BotUtils.formatMillis(millis, loc))).submit();
				}
			} catch (Exception e) {
				ERROR_LOG.error("", e);
			}
		});
	}

	private static void handleArgs(String args[]) {
		Iterator<String> it = IteratorUtils.arrayIterator(args);
		while (it.hasNext()) {
			handleArg(it, it.next());
		}
	}

	private static void handleArg(Iterator<String> it, String arg) {
		long tmp;
		try {
			switch (arg) {
				case "-token":
					token = it.next();
					log.info("Set token to '{}'", token);
					break;
				case "-dev_id":
					String[] split = it.next().split(",");
					Stream.of(split).map(Long::parseLong).forEach(l -> DEV_IDS.add(l));
					log.info("Set devId to '{}'", DEV_IDS);
					break;
				case "-save_delay":
					saveDelay = Long.parseLong(it.next());
					log.info("Set save_delay to " + saveDelay);
					break;
				case "-anime_base_update_rate":
					tmp = Long.parseLong(it.next());
					AnimeDB.setUpdateRate(tmp);
					log.info("Set anime_base_update_rate to " + tmp);
					break;
				case "-ani_sync_rate":
					aniSyncMinutes = Long.parseLong(it.next());
					log.info("Set AniList sync rate to " + aniSyncMinutes);
					break;
				case "-private_list_duration":
					privateListDuration = Long.parseLong(it.next());
					log.info("Set import_list_duration to {}", privateListDuration);
					break;
				case "-link_request_duration":
					linkRequestDuration = Long.parseLong(it.next());
					log.info("Set link_request_duration to {}", linkRequestDuration);
					break;
				default:
					ERROR_LOG.error("Unrecognized option '" + arg + "'");
					throw new IllegalArgumentException("Unrecognized option '" + arg + "'");
			}
		} catch (Exception e) {
			ERROR_LOG.error("Malformed argument: " + e.getMessage(), e);
			throw new IllegalArgumentException("Unrecognized option '" + arg + "'");
		}
	}

	public static void logThrowable(Throwable e) {
		ERROR_LOG.error("", e);
	}

	public static void executeLogException(Runnable r) {
		EXEC.execute(() -> {
			try {
				r.run();
			} catch (Exception e) {
				logThrowable(e);
			}
		});
	}
}
