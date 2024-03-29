package com.github.xerragnaroek.jikai.anime.db;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jasa.TitleLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AnimeDB {

    private static final AnimeDBImpl aDB = new AnimeDBImpl();
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

    public static Set<Anime> getAiringOrUpcomingAnime() {
        return aDB.getLoadedAnime().stream().filter(a -> a.isReleasing() || a.isNotYetReleased()).sorted().collect(Collectors.toSet());
    }

    public static void waitUntilLoaded() {
        while (aDB.isLoading()) {
        }
    }

    public static Anime getAnime(int id) {
        return aDB.getAnime(id);
    }

    public static Anime getAnime(String title, TitleLanguage tt) {
        return aDB.getAnime(title, tt);
    }

    public static Anime getAnime(String title) {
        for (TitleLanguage tl : TitleLanguage.values()) {
            Anime a = getAnime(title, tl);
            if (a != null) {
                return a;
            }
        }
        return null;
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
        aDB.loadAnime();
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

    public static JASA getJASA() {
        return aDB.getJASA();
    }

    public static List<Anime> loadAnimeViaId(int... ids) throws AniException, IOException {
        return aDB.loadAnime(ids);
    }
}
