package com.github.xerragnaroek.jikai.anime.schedule;

import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.prop.BooleanProperty;
import com.github.xerragnaroek.jikai.util.prop.SelfResettingFlagProperty;

/**
 * @author XerRagnaroek
 *
 */
public class Schedule {

	private Map<DayOfWeek, Map<LocalTime, Set<Anime>>> week = new ConcurrentHashMap<>();
	private LocalDate monDate;
	private ZoneId zone;
	private SelfResettingFlagProperty changed = new SelfResettingFlagProperty();
	private final Logger log;
	private BufferedImage schedImg;

	public Schedule(ZoneId zone) {
		this.zone = zone;
		log = LoggerFactory.getLogger(Schedule.class + "#" + zone.getId());
		AnimeDB.runOnDBUpdate(this::update);
	}

	void init() {
		setMonDate();
		populateSchedule();
		updateImage();
	}

	public ZoneId getZoneId() {
		return zone;
	}

	public BufferedImage getScheduleImage() {
		return schedImg;
	}

	public void runOnUpdate(Consumer<Schedule> con) {
		changed.onChangeToTrue(() -> con.accept(this));
	}

	public AnimeTable makeUserTable(JikaiUser ju) {
		AnimeTable at = new AnimeTable(zone);
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).filter(this::airsThisWeek).forEach(at::addAnime);
		return at;
	}

	private void setMonDate() {
		monDate = adjustedToPastMonday(LocalDate.now(zone));
	}

	private void populateSchedule() {
		AnimeDB.getAnimesMappedToDayOfAiring(zone).forEach((day, set) -> set.stream().filter(this::airsThisWeek).peek(a -> log.debug("{} airs this week", a.getTitle(TitleLanguage.ROMAJI))).forEach(a -> addAnimeToWeek(day, a)));
	}

	private void addAnimeToWeek(DayOfWeek day, Anime a) {
		LocalTime lt = a.getNextEpisodeDateTime(zone).get().toLocalTime();
		week.putIfAbsent(day, Collections.synchronizedMap(new TreeMap<>()));
		week.compute(day, (d, map) -> {
			map.compute(lt, (t, s) -> {
				if (s == null) {
					s = Collections.synchronizedSet(new TreeSet<>());
				}
				s.add(a);
				log.debug("Added new entry: {},{},{}", day, lt, a.getTitleRomaji());
				return s;
			});
			return map;
		});
	}

	private void update(AnimeUpdate au) {
		LocalDateTime now = LocalDateTime.now(zone);
		DayOfWeek today = now.getDayOfWeek();
		if (today == DayOfWeek.MONDAY && !now.toLocalDate().equals(monDate)) {
			populateSchedule();
			monDate = now.toLocalDate();
			updateImage();
		} else {
			BooleanProperty hasChanged = new BooleanProperty();
			if (au.hasAnimeNextEp()) {
				au.getAnimeNextEp().forEach(a -> hasChanged.setIfTrue(laterAnime(now, today, a)));
			}
			if (au.hasChangedReleaseAnime()) {
				au.getChangedReleaseAnime().stream().map(Pair::getLeft).forEach(a -> hasChanged.setIfTrue(laterAnime(now, today, a)));
			}
			if (au.hasRemovedAnime()) {
				au.getRemovedAnime().forEach(a -> hasChanged.setIfTrue(handleRemoved(now, today, a)));
			}
			if (hasChanged.get()) {
				updateImage();
			}
		}
	}

	private void updateImage() {
		AnimeTable at = new AnimeTable(zone);
		at.setTable(week);
		schedImg = at.toImage();
		changed.setFlag();
	}

	private boolean laterAnime(LocalDateTime now, DayOfWeek today, Anime a) {
		log.debug("Handling anime '{}' that airs later, either postponed or a new episode", a.getTitleRomaji());
		return computeLater(now, today, a, () -> {
			removeAnime(today, a);
			addAnimeToWeek(today, a);
		});
	}

	private boolean handleRemoved(LocalDateTime now, DayOfWeek today, Anime a) {
		log.debug("Handling removed anime {}", a.getTitleRomaji());
		return computeLater(now, today, a, () -> removeAnime(a));
	}

	private boolean computeLater(LocalDateTime now, DayOfWeek today, Anime a, Runnable run) {
		LocalDateTime ldt = a.getNextEpisodeDateTime(zone).get();
		int dayDifference = today.getValue() - ldt.getDayOfWeek().getValue();
		if (dayDifference == 0) {
			if (ldt.isAfter(now)) {
				log.debug("{} airs later today", a.getTitle(TitleLanguage.ROMAJI));
				//remove old entry
				run.run();
				return true;
			}
			log.debug("{} has already aired today, no point in updating the schedule!", a.getTitle(TitleLanguage.ROMAJI));
		} else if (dayDifference > 0) {
			if (isThisWeek(now.toLocalDate(), ldt.toLocalDate())) {
				log.debug("{} airs later this week, in {} days", a.getTitle(TitleLanguage.ROMAJI), dayDifference);
				run.run();
				return true;
			}
			log.debug("{} doesn't air this week", a.getTitle(TitleLanguage.ROMAJI));
		}
		return false;
	}

	private LocalDate adjustedToPastMonday(LocalDate ld) {
		DayOfWeek day = ld.getDayOfWeek();
		if (!(day == DayOfWeek.MONDAY)) {
			//get day difference from monday
			int dayFromMon = day.getValue() - 1;
			ld = ld.minusDays(dayFromMon);
		}
		return ld;
	}

	private boolean airsThisWeek(Anime a) {
		LocalDate today = LocalDate.now();
		Optional<LocalDateTime> opt = a.getNextEpisodeDateTime(zone);
		if (opt.isPresent()) {
			return isThisWeek(today, opt.get().toLocalDate());
		}
		return false;
	}

	private boolean isThisWeek(LocalDate today, LocalDate date) {
		if (today.isEqual(date)) {
			return true;
		}
		return adjustedToPastMonday(today).isEqual(adjustedToPastMonday(date));
	}

	private boolean removeAnime(DayOfWeek day, Anime a) {
		BooleanProperty rem = new BooleanProperty();
		week.compute(day, (d, map) -> {
			Iterator<LocalTime> keys = map.keySet().iterator();
			keys.forEachRemaining(key -> {
				Set<Anime> set = map.get(key);
				if (set.remove(a)) {
					log.debug("Removed old entry {},{},{}", day, key, a.getTitle(TitleLanguage.ROMAJI));
					rem.set(true);
					if (set.isEmpty()) {
						keys.remove();
					}
				}
			});
			return map;
		});
		return rem.get();
	}

	private void removeAnime(Anime a) {
		//delete the furthest entry of the anime
		for (int d = 7; d >= 1; d--) {
			if (removeAnime(DayOfWeek.of(d), a)) {
				break;
			}
		}
	}
}
