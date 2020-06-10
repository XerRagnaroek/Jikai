package com.github.xerragnaroek.jikai.anime.db;

import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.IntegerProperty;

import net.dv8tion.jda.api.entities.Guild;

public class AnimeDB {

	private static AnimeDBImpl aDB = new AnimeDBImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeDB.class);
	private static long updateRate = 6;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private static IntegerProperty version = new IntegerProperty(0);
	private static Set<Consumer<AnimeUpdate>> updateCon = Collections.synchronizedSet(new HashSet<>());

	public static void init() {
		if (!initialized) {
			aDB.init();
			initialized = true;
		} else {
			log.error("Already initialized!");
			throw new IllegalStateException("Already initialized!");
		}
		"test".isBlank();
	}

	public static Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		return aDB.getAnimesAiringOnWeekday(day, g);
	}

	public static Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId z) {
		return aDB.getAnimesAiringOnWeekday(day, z);
	}

	public static Map<DayOfWeek, Set<Anime>> getAnimesMappedToDayOfAiring(ZoneId zone) {
		return aDB.getAnimeMappedToDayOfWeek(zone);
	}

	public static Set<Anime> getSeasonalAnime() {
		return aDB.getSeasonalAnime();
	}

	public static void waitUntilLoaded() {
		while (aDB.isLoading()) {}
	}

	public static Anime getAnime(int id) {
		return aDB.getAnime(id);
	}

	public static Anime getAnime(String title) {
		return aDB.getAnime(title);
	}

	public static int getAnimeDBVersion() {
		return version.get();
	}

	public static IntegerProperty dbVersionProperty() {
		return version;
	}

	public static int incrementAndGetDBVersion() {
		return version.incrementAndGet();
	}

	public static int getAndIncrementDBVersion() {
		return version.getAndIncrement();
	}

	public static void startUpdateThread() {
		LocalDateTime now = LocalDateTime.now();
		long untilNextFullHour = now.until(now.truncatedTo(ChronoUnit.HOURS).plusHours(1), ChronoUnit.SECONDS);
		exec.scheduleAtFixedRate(aDB::loadAiringAnime, untilNextFullHour, updateRate * 3600, TimeUnit.SECONDS);
		log.debug("Update thread started, first running in {} and updating every {} hours", BotUtils.formatSeconds(untilNextFullHour), updateRate);
	}

	public static void setUpdateRate(long rate) {
		updateRate = rate;
	}

	public static int size() {
		return aDB.size();
	}

	public static void setDBVersionProperty(IntegerProperty prop) {
		version = prop;
	}

	public static void runOnDBUpdate(Consumer<AnimeUpdate> con) {
		updateCon.add(con);
	}

	public static BufferedImage getCoverImage(Anime a) {
		return aDB.getCoverImage(a);
	}

	static void dBUpdated(AnimeUpdate au) {
		Core.EXEC.execute(() -> updateCon.forEach(con -> con.accept(au)));
	}

	public static void update() {
		aDB.loadAiringAnime();
	}
}
