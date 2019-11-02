package com.xerragnaroek.jikai.anime.db;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.core.Connector;
import com.github.Doomsdayrs.Jikan4java.enums.Season;
import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.github.Doomsdayrs.Jikan4java.types.Main.Season.SeasonSearch;
import com.github.Doomsdayrs.Jikan4java.types.Main.Season.SeasonSearchAnime;
import com.github.Doomsdayrs.Jikan4java.types.Support.Prop.From;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.BotData;
import com.xerragnaroek.jikai.data.Jikai;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Initilizable;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

/**
 * Does the animes.
 * 
 * @author XerRagnaroek
 *
 */
class AnimeDBImpl implements Initilizable {
	private ZoneId jst = ZoneId.of("Japan");
	private Map<ZoneId, ZoneAnimeBase> animes = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(AnimeDBImpl.class);
	private AtomicBoolean loading = new AtomicBoolean(true);
	private AtomicBoolean initialized = new AtomicBoolean(false);
	private BotData bd;
	private Map<Integer, String> mapToNum;

	AnimeDBImpl() {}

	public void init() {
		log.info("Initializing AnimeBase");
		animes.put(jst, new ZoneAnimeBase(jst));
		bd = Core.JM.getJDM().getBotData();
		loadSeason();
		Jikai.timeZoneMapProperty().onPut((z, v) -> addTimeZone(z, false));
		Jikai.timeZoneMapProperty().onRemove((z, v) -> removeTimeZone(z));
		initialized.set(true);
	}

	/**
	 * Queries jikan for the current seasonal animes, loads their pages, and stores them with their
	 * broadcast day/time in the list.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void loadSeason() {
		ZonedDateTime zdt = ZonedDateTime.now(jst);
		loading.set(true);
		Core.JDA.getPresence().setActivity(Activity.watching("the AnimeDataBase load"));
		try {
			SeasonSearch ss = new Connector().seasonSearch(zdt.getYear(), getSeason(zdt)).get();
			log.info("Retrieving season search for {} {}", ss.season_name, ss.season_year);
			//compare hashs, so only new data will be used
			String hash = Objects.requireNonNullElse(bd.getCurrentSeasonHash(), "");
			log.debug("Current search hash = " + hash);
			if (!ss.request_hash.equals(hash) || !animes.get(jst).hasEntries()) {
				loadSeasonImpl(ss, zdt);
				if (!ss.request_hash.equals(hash)) {
					BotUtils.sendToAllInfoChannels("AnimeDB has updated to version " + AnimeDB.incrementAndGetDBVersion());
					bd.setCurrentSeasonHash(ss.request_hash);
					log.info("AnimeDB has updated to version {}", AnimeDB.getAnimeDBVersion());
				}
			} else {
				log.info("Current schedule is up to date.");
			}
		} catch (InterruptedException | ExecutionException e1) {
			BotUtils.sendThrowableToDev("Error while loading season", e1);
		}
		Core.JDA.getPresence().setActivity(null);
	}

	private void loadSeasonImpl(SeasonSearch ss, ZonedDateTime zdt) throws InterruptedException, ExecutionException {
		Instant start = Instant.now();
		List<AnimeDayTime> tmp = ss.animes.stream().map(SeasonSearchAnime::getAnime).map(t -> {
			try {
				return t.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Exception while getting anime", e);
			}
			return null;
		}).filter(Objects::nonNull).filter(a -> a.airing).map(this::makeAnimeDayTime).filter(AnimeDayTime::isKnown).collect(Collectors.toList());
		Collections.sort(tmp);
		animes.get(jst).setAnimeDayTimes(tmp);
		mapToNum(tmp);
		loading.set(false);
		zoneAnimes(true);
		log.info("Loaded {} animes in {}ms", tmp.size(), Duration.between(start, Instant.now()).toMillis());
	}

	private void mapToNum(List<AnimeDayTime> adts) {
		Map<Integer, String> tmp = new ConcurrentHashMap<>();
		for (int n = 0; n < adts.size(); n++) {
			tmp.put(n, adts.get(n).getAnime().title);
		}
		mapToNum = tmp;
	}

	/**
	 * Parse the broadcast time and day from the available data;
	 * 
	 */
	private AnimeDayTime makeAnimeDayTime(Anime a) {
		log.debug("Parsing broadcast day and time for {}", a.title);

		ZonedDateTime jzdt = makeZDT(a);
		if (jzdt == null) {
			return AnimeDayTime.UNKNOWN;
		}
		AnimeDayTime adt;
		adt = new AnimeDayTime(a, jzdt);
		log.info("Loaded {}", adt);
		return adt;
	}

	/*private AnimeDayTime GetAdjustedADT(AnimeDayTime adt, ZoneId z) {
		if (adt.getZonedDateTime().getZone().equals(z)) {
			log.debug("Supplied ADT already has requested zone");
			return adt;
		}
		if (animes.containsKey(z)) {
			AnimeDayTime a = animes.get(z).get(adt.getAnime().title);
			if (a != null) {
				log.debug("Adt for anime {} has been adjusted into zone {} before", adt.getAnime().title, z.toString());
				return a;
			}
		}
		AnimeDayTime adjusted = adjust(adt, z);
		log.debug("Adjusted adt for anime {} into zone {}", adjusted.getAnime().title, z);
		return adjusted;
	}*/

	void zoneAnimes(boolean overwrite) {
		Jikai.getUsedTimeZones().forEach(z -> addTimeZone(z, overwrite));
	}

	void addTimeZone(ZoneId z, boolean overwrite) {
		if (!loading.get() && (!animes.containsKey(z) || overwrite)) {
			ZoneAnimeBase zab = new ZoneAnimeBase(z);
			zab.setAnimeDayTimes(animes.get(jst).getAnimes());
			animes.put(z, zab);
			log.info("Adjusted animes for zone {}", z.toString());
		}
	}

	AnimeDayTime getADT(ZoneId zone, String title) {
		if (!animes.containsKey(zone)) {
			addTimeZone(zone, false);
		}
		return animes.get(zone).getADT(title);

	}

	private ZonedDateTime makeZDT(Anime a) {
		ZonedDateTime zdt = null;
		if (a.broadcast == null || a.broadcast.equals("Not scheduled once per week")) {
			return null;
		} else if (a.broadcast.equals("Unknown")) {
			log.debug("Broadcast is unknown, defaulting to airing day = day it first aired");
			zdt = makeZDTFromAired(a);
		} else {
			zdt = makeZDTFromBroadcast(a);
		}
		//adjusted into the next time they air
		ZonedDateTime now = ZonedDateTime.now(jst);
		ZonedDateTime tmp = ZonedDateTime.of(LocalDate.now(jst), zdt.toLocalTime(), jst);
		if (now.isBefore(tmp)) {
			return tmp.with(TemporalAdjusters.nextOrSame(zdt.getDayOfWeek()));
		} else {
			return tmp.with(TemporalAdjusters.next(zdt.getDayOfWeek()));
		}
	}

	private ZonedDateTime makeZDTFromAired(Anime a) {
		From from = a.aired.prop.from;
		//only date, no time :(
		return ZonedDateTime.of(LocalDate.of(from.year, from.month, from.day), AnimeDayTime.UNKNOWN_TIME, jst);
	}

	private ZonedDateTime makeZDTFromBroadcast(Anime a) {
		String broadcast[] = a.broadcast.split("s at ");
		if (!broadcast[1].equals("Unknown")) {
			return ZonedDateTime.of(dummyWithDayOfWeek(DayOfWeek.valueOf(broadcast[0].toUpperCase())), LocalTime.parse(broadcast[1].substring(0, broadcast[1].length() - 6)), jst);
		} else {
			return ZonedDateTime.of(dummyWithDayOfWeek(DayOfWeek.valueOf(broadcast[0].toUpperCase())), AnimeDayTime.UNKNOWN_TIME, jst);
		}
	}

	/**
	 * Makes a LocalDate that is the given DayOfWeek. Arbitrary dates, it's the week I worked on
	 * this file.
	 */
	private LocalDate dummyWithDayOfWeek(DayOfWeek day) {
		switch (day) {
		case MONDAY:
			return LocalDate.of(2019, 8, 12);
		case TUESDAY:
			return LocalDate.of(2019, 8, 13);
		case WEDNESDAY:
			return LocalDate.of(2019, 8, 14);
		case THURSDAY:
			return LocalDate.of(2019, 8, 15);
		case FRIDAY:
			return LocalDate.of(2019, 8, 16);
		case SATURDAY:
			return LocalDate.of(2019, 8, 17);
		case SUNDAY:
			return LocalDate.of(2019, 8, 18);
		default:
			return null;
		}
	}

	private Season getSeason(ZonedDateTime zdt) {
		switch (zdt.getMonth()) {
		case JANUARY:
		case FEBRUARY:
		case MARCH:
			return Season.WINTER;
		case APRIL:
		case MAY:
		case JUNE:
			return Season.SPRING;
		case JULY:
		case AUGUST:
		case SEPTEMBER:
			return Season.SUMMER;
		case OCTOBER:
		case NOVEMBER:
		case DECEMBER:
			return Season.FALL;
		default:
			return Season.SPRING;
		}
	}

	Anime getAnime(String title) {
		return animes.get(jst).getAnime(title);
	}

	Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		return getAnimesAiringOnWeekday(day, Core.JM.get(g).getJikaiData().getTimeZone());
	}

	Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId zone) {
		if (!animes.containsKey(zone)) {
			addTimeZone(zone, false);
		}
		return animes.get(zone).getAnimesOnDayOfWeek(day);
	}

	Set<AnimeDayTime> getSeasonalAnimes() {
		return animes.get(jst).getAnimes().stream().sorted((a1, a2) -> a1.getAnime().title.compareTo(a2.getAnime().title)).collect(Collectors.toSet());
	}

	boolean isLoading() {
		return loading.get();
	}

	Set<AnimeDayTime> getSeasonalAnimesAdjusted(ZoneId tz) {
		return new TreeSet<>(animes.get(tz).getAnimes());
	}

	int size() {
		return animes.get(jst).size();
	}

	Anime getAnimeByNum(int num) {
		return getAnime(mapToNum.get(num));
	}

	AnimeDayTime getADTByNum(ZoneId zone, int num) {
		return getADT(zone, mapToNum.get(num));
	}

	int titleToNumber(String title) {
		for (Entry<Integer, String> pair : mapToNum.entrySet()) {
			if (pair.getValue().equals(title)) {
				return pair.getKey();
			}
		}
		return -1;
	}

	private void removeTimeZone(ZoneId z) {
		animes.remove(z).clear();
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}
}
