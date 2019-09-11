package com.xerragnaroek.bot.anime;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
import com.xerragnaroek.bot.config.ConfigManager;
import com.xerragnaroek.bot.config.ConfigOption;

import net.dv8tion.jda.api.entities.Guild;

/**
 * Does the animes.
 * 
 * @author XerRagnaroek
 *
 */
class AnimeBaseImpl {
	private ZoneId jst = ZoneId.of("Japan");
	private Map<ZoneId, ZoneAnimeBase> animes = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(AnimeBaseImpl.class);
	private AtomicBoolean loading = new AtomicBoolean(true);
	private Executor exec = Executors.newCachedThreadPool();

	AnimeBaseImpl() {}

	void init() {
		log.info("Initializing AnimeBase");
		animes.put(jst, new ZoneAnimeBase(jst));
		loadSeason();
	}

	/**
	 * Queries jikan for the current seasonal animes, loads their pages, and stores them with their
	 * broadcast day/time in the list.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void loadSeason() {
		exec.execute(() -> {
			ZonedDateTime zdt = ZonedDateTime.now(jst);
			loading.set(true);
			try {
				SeasonSearch ss = new Connector().seasonSearch(zdt.getYear(), getSeason(zdt)).get();
				log.info("Retrieving season search for {} {}", ss.season_name, ss.season_year);
				//compare hashs, so only new data will be used
				String hash = Objects.requireNonNullElse(ConfigManager.getBotConfig().getOption(ConfigOption.LATEST_SEASON_HASH), "");
				if (!ss.request_hash.equals(hash) || !animes.get(jst).hasEntries()) {
					Instant start = Instant.now();
					List<AnimeDayTime> tmp = ss.animes.stream().map(SeasonSearchAnime::getAnime).map(t -> {
						try {
							return t.get();
						} catch (InterruptedException | ExecutionException e) {
							log.error("Exception while getting anime", e);
						}
						return null;
					}).filter(Objects::nonNull).filter(a -> a.airing).map(this::makeAnimeDayTime).filter(AnimeDayTime::isKnown).collect(Collectors.toList());
					//TODO remove limit above for final version!!!
					animes.get(jst).setAnimeDayTimes(tmp);
					ConfigManager.getBotConfig().setOption(ConfigOption.LATEST_SEASON_HASH, ss.request_hash);
					loading.set(false);
					zoneAnimes(true);
					log.info("Loaded {} animes in {}ms", tmp.size(), Duration.between(start, Instant.now()).toMillis());
				} else {
					log.info("Current schedule is up to date.");
				}
			} catch (InterruptedException | ExecutionException e1) {
				log.error("Error while loading season", e1);
			}
		});
	}

	private void putInMap(Map<ZoneId, Map<String, AnimeDayTime>> m, AnimeDayTime adt, ZoneId z) {
		m.compute(z, (zone, map) -> {
			if (map == null) {
				map = new ConcurrentHashMap<>();
			}
			map.put(adt.getAnime().title, adt);
			return map;
		});
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
		log.debug("Loaded {}", adt);
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
		ConfigManager.getUsedTimeZones().forEach(z -> {
			addTimeZone(z, overwrite);
		});
	}

	void addTimeZone(ZoneId z, boolean overwrite) {
		if (!loading.get() && (!animes.containsKey(z) || overwrite)) {
			ZoneAnimeBase zab = new ZoneAnimeBase(z);
			zab.setAnimeDayTimes(animes.get(jst).getAnimes());
			animes.put(z, zab);
			log.info("Adjusted animes for zone {}", z.toString());
		}
	}

	private ZonedDateTime makeZDT(Anime a) {
		if (a.broadcast == null || a.broadcast.equals("Not scheduled once per week")) {
			return null;
		} else if (a.broadcast.equals("Unknown")) {
			log.debug("Broadcast is unknown, defaulting to airing day = day it first aired");
			return makeZDTFromAired(a);
		} else {
			return makeZDTFromBroadcast(a);
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

	List<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		ZoneId zone = ZoneId.of(ConfigManager.getConfigForGuild(g.getId()).getOption(ConfigOption.TIMEZONE));
		if (!animes.containsKey(zone)) {
			addTimeZone(zone, false);
		}
		return animes.get(zone).getAnimesOnDayOfWeek(day);
	}

	private List<AnimeDayTime> filterForWeekDay(List<AnimeDayTime> list, DayOfWeek day) {
		return list.stream().filter(adt -> adt.releasesOnDay(day)).collect(Collectors.toList());
	}

	List<Anime> getSeasonalAnimes() {
		return animes.get(jst).getAnimes().stream().map(AnimeDayTime::getAnime).sorted((a1, a2) -> a1.title.compareTo(a2.title)).collect(Collectors.toCollection(LinkedList::new));
	}

	boolean isLoading() {
		return loading.get();
	}
}
