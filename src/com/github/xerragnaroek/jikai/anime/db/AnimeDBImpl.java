package com.github.xerragnaroek.jikai.anime.db;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Initilizable;

import net.dv8tion.jda.api.entities.Activity;

/**
 * Does the animes.
 * 
 * @author XerRagnaroek
 */
class AnimeDBImpl implements Initilizable {
	private Map<Integer, Anime> anime = new ConcurrentHashMap<>();
	private Map<Integer, BufferedImage> coverImages = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(AnimeDBImpl.class);
	private AtomicBoolean loading = new AtomicBoolean(true);
	private AtomicBoolean initialized = new AtomicBoolean(false);
	private JASA jasa = new JASA();
	private Set<Consumer<AnimeUpdate>> updateCon = Collections.synchronizedSet(new HashSet<>());
	private ScheduledFuture<?> updateFuture;

	AnimeDBImpl() {}

	public void init() {
		log.info("Initializing AnimeBase");
		loadAiringAnime();
		initialized.set(true);
	}

	void loadAiringAnime() {
		loading.set(true);
		Core.JDA.getPresence().setPresence(Activity.watching("the anime load!"), false);
		try {
			Set<Anime> distinctAnime = new TreeSet<>();
			distinctAnime.addAll(jasa.fetchAllAiringAnime(0, LocalDate.now().getYear() + 1));
			distinctAnime.addAll(jasa.fetchSeasonalAnime(1));
			List<Anime> newAnime = distinctAnime.stream().filter(a -> !a.getStatus().equals("FINISHED")).collect(Collectors.toList());
			loadImages(newAnime);
			List<Anime> old = new ArrayList<>();
			CollectionUtils.addAll(old, anime.values());
			if (old.isEmpty()) {
				anime = newAnime.stream().collect(Collectors.toConcurrentMap(a -> a.getId(), a -> a));
			} else {
				handleUpdate(old, newAnime);
			}
			Core.CUR_SEASON.set(jasa.getCurrentSeason());
		} catch (AniException | IOException e) {
			BotUtils.logAndSendToDev(log, "Exception while updating AnimeDB", e);
		}
		Core.JDA.getPresence().setActivity(null);
		loading.set(false);
	}

	/*
	 * Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
	 * return getAnimesAiringOnWeekday(day, Core.JM.get(g).getJikaiData().getTimeZone());
	 * }
	 * Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId zone) {
	 * return getAnimeMappedToDayOfWeek(zone).get(day);
	 * }
	 * Map<DayOfWeek, Set<Anime>> getAnimeMappedToDayOfWeek(ZoneId zone) {
	 * Map<DayOfWeek, Set<Anime>> map = new TreeMap<>();
	 * for (DayOfWeek d : DayOfWeek.values()) {
	 * map.put(d, new TreeSet<>());
	 * }
	 * Collection<Anime> vals = anime.values();
	 * synchronized (vals) {
	 * vals.forEach(a -> {
	 * Optional<LocalDateTime> ldt = a.getNextEpisodeDateTime(zone);
	 * if (ldt.isPresent()) {
	 * map.compute(ldt.get().getDayOfWeek(), (d, s) -> {
	 * s.add(a);
	 * return s;
	 * });
	 * }
	 * });
	 * }
	 * return map;
	 * }
	 */

	Set<Anime> getLoadedAnime() {
		return anime.values().stream().sorted().collect(Collectors.toSet());
	}

	boolean isLoading() {
		return loading.get();
	}

	int size() {
		return anime.size();
	}

	Anime getAnime(int id) {
		return anime.get(id);
	}

	Anime getAnime(String title) {
		return anime.values().stream().filter(a -> a.getTitleRomaji().equals(title)).findFirst().get();
	}

	BufferedImage getCoverImage(Anime a) {
		return coverImages.get(a.getId());
	}

	void addUpdateCon(Consumer<AnimeUpdate> con) {
		updateCon.add(con);
	}

	void dbUpdated(AnimeUpdate au) {
		Core.EXEC.execute(() -> updateCon.forEach(con -> con.accept(au)));
	}

	void startUpdateThread(long updateRateSeconds, boolean errorCheck) {
		startUpdateThreadImpl(updateRateSeconds);
		if (errorCheck) {
			startUpdateErrorChecking();
		}
	}

	void startUpdateThreadImpl(long updateRateSeconds) {
		LocalDateTime now = LocalDateTime.now();
		long untilNextFullHour = now.until(now.truncatedTo(ChronoUnit.HOURS).plusHours(1), ChronoUnit.SECONDS);
		updateFuture = Core.EXEC.scheduleAtFixedRate(this::loadAiringAnime, untilNextFullHour, updateRateSeconds, TimeUnit.SECONDS);
		log.debug("Update thread started, first running in {} and updating every {} hours", BotUtils.formatSeconds(untilNextFullHour, JikaiLocaleManager.getEN()), BotUtils.formatSeconds(updateRateSeconds, JikaiLocaleManager.getEN()));
	}

	void startUpdateErrorChecking() {
		log.debug("Starting update thread error checking");
		Core.EXEC.scheduleAtFixedRate((() -> {
			log.debug("Checking if update thread is still running...");
			if (updateFuture.isDone()) {
				log.debug("Update thread is done, checking cause...");
				try {
					updateFuture.get();
					log.debug("It finished without exception, cancelled? " + updateFuture.isCancelled());
					// catch Exceptions and Errors, explicitly catching Interrupted and ExecutionExceptions still causes
					// the executor to halt!
				} catch (Throwable e) {
					BotUtils.logAndSendToDev(log, "Update thread threw an exception!", e);
				}
				log.info("Restarting update thread...");
				AnimeDB.startUpdateThread(false);
			} else {
				log.debug("Update thread is still running!");
			}
		}), 0, 15, TimeUnit.MINUTES);
	}

	private void loadImages(List<Anime> list) {
		list.stream().parallel().filter(a -> !coverImages.containsKey(a.getId())).forEach(a -> {
			for (int tries = 0; tries < 5; tries++) {
				try {
					coverImages.put(a.getId(), ImageIO.read(new URL(a.getCoverImageMedium())));
					log.debug("Loaded medium cover image for {}", a.getTitleRomaji());
					break;
				} catch (IOException e) {
					log.error("Failed loading image for '{}', retrying after 250ms (current try: {})", a.getTitleRomaji(), tries, e);
					try {
						Thread.sleep(250);
					} catch (InterruptedException e1) {
						log.error("", e1);
					}
				}
			}
		});
	}

	boolean isUpdateThreadRunning() {
		return !updateFuture.isDone();
	}

	boolean cancelUpdateFuture() {
		return updateFuture.cancel(true);
	}

	private void handleUpdate(List<Anime> old, List<Anime> newA) {
		AnimeUpdate au = AnimeUpdate.categoriseUpdates(old, newA);
		if (au.hasChange()) {
			// newA.removeIf(a -> !a.hasDataForNextEpisode());
			anime = newA.stream().collect(Collectors.toConcurrentMap(a -> a.getId(), a -> a));
		}
		AnimeDB.dBUpdated(au);
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

}
