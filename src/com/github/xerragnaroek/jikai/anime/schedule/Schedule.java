package com.github.xerragnaroek.jikai.anime.schedule;

import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
 */
public class Schedule {

	private Map<DayOfWeek, Map<LocalTime, List<Anime>>> week = new ConcurrentHashMap<>();
	private LocalDate monDate;
	private ZoneId zone;
	private SelfResettingFlagProperty changed = new SelfResettingFlagProperty();
	private final Logger log;
	private BufferedImage schedImg;

	public Schedule(ZoneId zone) {
		this.zone = zone;
		log = LoggerFactory.getLogger(Schedule.class + "#" + zone.getId());
		AnimeDB.runOnDBUpdate(this::update);
		changed.onSet(() -> log.debug("Schedule has changed!"));
	}

	void init() {
		init(null);
	}

	void init(Set<Anime> anime) {
		log.debug("Initializing schedule...");
		setMonDate();
		populateSchedule(anime);
		updateImage();
		log.debug("Schedule initialized!");
	}

	public ZoneId getZoneId() {
		return zone;
	}

	public BufferedImage getScheduleImage() {
		return schedImg;
	}

	public void runOnUpdate(Consumer<Schedule> con) {
		changed.onSet(() -> con.accept(this));
	}

	public AnimeTable makeUserTable(JikaiUser ju) {
		log.debug("Making table for user {}", ju.getId());
		AnimeTable at = new AnimeTable(zone);
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).filter(this::airsLaterThisWeek).forEach(at::addAnime);
		return at;
	}

	private void setMonDate() {
		monDate = adjustedToPastOrSameMonday(LocalDate.now(zone));
		log.debug("Set monDate to {}", monDate);
	}

	void populateSchedule(Collection<Anime> oldA) {
		List<Anime> anime = new ArrayList<>();
		anime.addAll(AnimeDB.getAiringOrUpcomingAnime());
		if (oldA != null) {
			anime.addAll(oldA);
		}
		log.debug("Populating schedule, clearing map");
		week.clear();
		mapAnimeToDayOfWeek(zone, anime).forEach((day, set) -> set.stream().filter(this::airsLaterThisWeek).peek(a -> log.debug("{} airs this week", a.getTitle(TitleLanguage.ROMAJI))).forEach(a -> addAnimeToWeek(day, a)));
	}

	private void addAnimeToWeek(DayOfWeek day, Anime a) {
		LocalTime lt = a.getNextEpisodeDateTime(zone).get().toLocalTime();
		week.putIfAbsent(day, Collections.synchronizedMap(new TreeMap<>()));
		week.compute(day, (d, map) -> {
			map.compute(lt, (t, s) -> {
				if (s == null) {
					s = Collections.synchronizedList(new ArrayList<>());
				}
				s.add(a);
				log.debug("Added new entry: {},{},{}", day, lt, a.getTitleRomaji());
				return s;
			});
			return map;
		});
	}

	private void update(AnimeUpdate au) {
		log.debug("Updating schedule");
		LocalDateTime now = LocalDateTime.now(zone);
		DayOfWeek today = now.getDayOfWeek();
		if (today == DayOfWeek.MONDAY && !now.toLocalDate().equals(monDate)) {
			setMonDate();
			populateSchedule(null);
			updateImage();
		} else {
			BooleanProperty hasChanged = new BooleanProperty();
			if (au.hasAnimeNextEp()) {
				au.getAnimeNextEp().forEach(a -> hasChanged.setIfTrue(nextEpisode(today, a)));
			}
			if (au.hasChangedReleaseAnime()) {
				au.getChangedReleaseAnime().stream().map(Pair::getLeft).forEach(a -> hasChanged.setIfTrue(changedReleaseAnime(today, a)));
			}
			if (au.hasRemovedAnime()) {
				au.getRemovedAnime().forEach(a -> hasChanged.setIfTrue(handleRemoved(now, today, a)));
			}
			if (hasChanged.get()) {
				log.debug("Schedule has changed!");
				updateImage();
			}
		}
	}

	private void updateImage() {
		log.debug("Updating image");
		if (!week.isEmpty()) {
			AnimeTable at = new AnimeTable(zone);
			sortWeek();
			at.setTable(week);
			schedImg = at.toImage();
			changed.setFlag();
		} else {
			log.debug("Schedule is empty, no need to make an image!");
		}
	}

	private boolean changedReleaseAnime(DayOfWeek today, Anime a) {
		log.debug("Handling anime '{}' that's been postponed", a.getTitleRomaji());
		removeAnime(a);
		if (airsLaterThisWeek(a)) {
			addAnimeToWeek(today, a);
			return true;
		}
		log.debug("It's next episode doesn't air later this week");
		return false;
	}

	private boolean nextEpisode(DayOfWeek today, Anime a) {
		// only add the anime to the schedule if it airs later this week
		if (airsLaterThisWeek(a)) {
			addAnimeToWeek(today, a);
			return true;
		}
		return false;
	}

	private boolean handleRemoved(LocalDateTime now, DayOfWeek today, Anime a) {
		log.debug("Handling removed anime {}", a.getTitleRomaji());
		if (airsLaterThisWeek(a)) {
			return removeAnime(a) != null;
		}
		return false;
	}

	private boolean airsThisWeek(Anime a) {
		if (!a.hasDataForNextEpisode()) {
			return false;
		}
		return isThisWeek(LocalDate.now(zone), a.getNextEpisodeDateTime(zone).get().toLocalDate());
	}

	private boolean airsLaterThisWeek(Anime a) {
		if (!a.hasDataForNextEpisode()) {
			return false;
		}
		LocalDateTime now = LocalDateTime.now(zone);
		LocalDateTime aLDT = a.getNextEpisodeDateTime(zone).get();
		log.debug("{} airs on {}", a.getTitleRomaji(), aLDT);
		return isThisWeek(now.toLocalDate(), aLDT.toLocalDate()) && aLDT.isAfter(now);
	}

	private LocalDate adjustedToPastOrSameMonday(LocalDate ld) {
		/*
		 * DayOfWeek day = ld.getDayOfWeek();
		 * if (day != DayOfWeek.MONDAY) {
		 * // get day difference from monday
		 * int dayFromMon = day.getValue() - 1;
		 * ld = ld.minusDays(dayFromMon);
		 * }
		 * return ld;
		 */
		return ld.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private boolean isThisWeek(LocalDate today, LocalDate date) {
		if (today.isEqual(date)) {
			return true;
		}
		return adjustedToPastOrSameMonday(today).isEqual(adjustedToPastOrSameMonday(date));
	}

	private Anime removeAnime(DayOfWeek day, Anime a) {
		Map<LocalTime, List<Anime>> map = week.get(day);
		for (LocalTime lt : map.keySet()) {
			List<Anime> l = map.get(lt);
			int oldIndex = l.indexOf(a);
			if (oldIndex > -1) {
				Anime rem = l.get(oldIndex);
				l.remove(oldIndex);
				log.debug("Removed old entry {},{},{}", day, lt, a.getTitle(TitleLanguage.ROMAJI));
				if (l.isEmpty()) {
					log.debug("Anime list for {},{} is empty, removing", day, lt);
					map.remove(lt);
				}
				return rem;
			}
		}
		return null;
	}

	private Anime removeAnime(Anime a) {
		Anime old = null;
		// delete the furthest entry of the anime
		for (int d = 7; d >= 1; d--) {
			if ((old = removeAnime(DayOfWeek.of(d), a)) != null) {
				return old;
			}
		}
		return null;
	}

	private void sortWeek() {
		log.debug("Sorting map");
		week.forEach((d, m) -> {
			m.forEach((lt, list) -> {
				Collections.sort(list);
			});
		});
	}

	private Map<DayOfWeek, Set<Anime>> mapAnimeToDayOfWeek(ZoneId zone, Collection<Anime> anime) {
		Map<DayOfWeek, Set<Anime>> map = new TreeMap<>();
		for (DayOfWeek d : DayOfWeek.values()) {
			map.put(d, new TreeSet<>());
		}
		anime.forEach(a -> {
			Optional<LocalDateTime> ldt = a.getNextEpisodeDateTime(zone);
			if (ldt.isPresent()) {
				map.compute(ldt.get().getDayOfWeek(), (d, s) -> {
					s.add(a);
					return s;
				});
			}
		});
		return map;
	}

	Set<Anime> getAnimeInSchedule() {
		return week.values().stream().flatMap(v -> v.values().stream()).flatMap(l -> l.stream()).collect(Collectors.toSet());
	}
}
