package com.github.xerragnaroek.jikai.anime.db;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;

public class AnimeDB {

	private static AnimeDBImpl aDB = new AnimeDBImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeDB.class);
	private static long updateRate = 6 * 3600;

	public static void init() {
		if (!initialized) {
			aDB.init();
			initialized = true;
		} else {
			log.error("Already initialized!");
			throw new IllegalStateException("Already initialized!");
		}
	}

	/*
	 * public static Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
	 * return aDB.getAnimesAiringOnWeekday(day, g);
	 * }
	 * public static Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId z) {
	 * return aDB.getAnimesAiringOnWeekday(day, z);
	 * }
	 */

	public static Set<Anime> getLoadedAnime() {
		return aDB.getLoadedAnime();
	}

	public static void waitUntilLoaded() {
		while (aDB.isLoading()) {}
	}

	public static Anime getAnime(int id) {
		return aDB.getAnime(id);
	}

	public static Anime getAnime(String title, TitleLanguage tt) {
		return aDB.getAnime(title, tt);
	}

	public static void startUpdateThread(boolean errorCheck) {
		aDB.startUpdateThread(updateRate, errorCheck);
	}

	public static void setUpdateRate(long rate) {
		updateRate = rate;
	}

	public static int size() {
		return aDB.size();
	}

	public static int countAnimeWithNextEpData() {
		return (int) aDB.getLoadedAnime().stream().filter(Anime::hasDataForNextEpisode).count();
	}

	public static void runOnDBUpdate(Consumer<AnimeUpdate> con) {
		aDB.addUpdateCon(con);
	}

	public static BufferedImage getCoverImage(Anime a) {
		return aDB.getCoverImage(a);
	}

	static void dBUpdated(AnimeUpdate au) {
		aDB.dbUpdated(au);
	}

	public static void update() {
		aDB.loadAiringAnime();
	}

	public static boolean isUpdateThreadRunning() {
		return aDB.isUpdateThreadRunning();
	}

	public static boolean cancelUpdateFuture() {
		return aDB.cancelUpdateFuture();
	}

	public static boolean hasAnime(int id) {
		return aDB.getAnime(id) != null;
	}
}
