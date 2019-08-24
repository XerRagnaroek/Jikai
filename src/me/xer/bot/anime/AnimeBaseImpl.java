package me.xer.bot.anime;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.github.Doomsdayrs.Jikan4java.core.Connector;
import com.github.Doomsdayrs.Jikan4java.enums.Season;
import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.github.Doomsdayrs.Jikan4java.types.Main.Season.SeasonSearch;
import com.github.Doomsdayrs.Jikan4java.types.Main.Season.SeasonSearchAnime;
import com.github.Doomsdayrs.Jikan4java.types.Support.Prop.From;
import com.github.xerragnaroek.xlog.XLogger;

import me.xer.bot.config.Config;
import me.xer.bot.config.ConfigOption;

/**
 * Does the animes.
 * 
 * @author XerRagnaroek
 *
 */
class AnimeBaseImpl {
	private List<AnimeDayTime> animes;
	private ZoneId zone;
	private ZoneId jst = ZoneId.of("Japan");
	private static final XLogger log = XLogger.getInstance();

	AnimeBaseImpl() {
		log.log("Initializing AnimeBase");
		setTimeZone(Config.getOption(ConfigOption.TIMEZONE));
		Config.registerOnOptionChange(ConfigOption.TIMEZONE, this::setTimeZone);
		try {
			loadSeason();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Queries jikan for the current seasonal animes, loads their pages, and stores them with their
	 * broadcast day/time in the list.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void loadSeason() throws InterruptedException, ExecutionException {
		ZonedDateTime zdt = ZonedDateTime.now(jst);
		SeasonSearch ss = new Connector().seasonSearch(zdt.getYear(), getSeason(zdt)).get();
		log.logf("Retrieving season search for %s %s", ss.season_name, ss.season_year);
		//compare hashs, so only new data will be used
		String hash = Objects.requireNonNullElse(Config.getOption(ConfigOption.LATEST_SEASON_HASH), "");
		if (!ss.request_hash.equals(hash) || animes == null) {
			Instant start = Instant.now();
			animes = ss.animes.stream().map(SeasonSearchAnime::getAnime).map(t -> {
				try {
					return t.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
				return null;
			}).filter(Objects::nonNull).filter(a -> a.airing).map(this::localizeAnime).filter(AnimeDayTime::isKnown).limit(10).collect(Collectors.toList());
			//TODO remove limit above for final version!!!
			Config.setOption(ConfigOption.LATEST_SEASON_HASH, ss.request_hash);
			log.logf("Loaded %02d animes in %sms", animes.size(), Duration.between(start, Instant.now()).toMillis());
		} else {
			log.log("Current schedule is up to date.");
		}
	}

	/**
	 * Parse the broadcast time and day from the available data and adjust the time to the set
	 * timezone.
	 * 
	 */
	private AnimeDayTime localizeAnime(Anime a) {
		log.logf("Parsing broadcast day and time for %s", a.title);

		ZonedDateTime jzdt = makeZDT(a);
		if (jzdt == null) {
			return AnimeDayTime.UNKNOWN;
		}
		AnimeDayTime adt;
		if (jzdt.toLocalTime().equals(AnimeDayTime.UNKNOWN_TIME)) {
			adt = new AnimeDayTime(a, jzdt.getDayOfWeek(), AnimeDayTime.UNKNOWN_TIME);
		} else {
			ZonedDateTime adjustedZdt = jzdt.withZoneSameInstant(zone);
			adt = new AnimeDayTime(a, adjustedZdt.getDayOfWeek(), adjustedZdt.toLocalTime());
		}
		log.logf("Loaded %s", adt);
		return adt;
	}

	private ZonedDateTime makeZDT(Anime a) {
		if (a.broadcast == null || a.broadcast.equals("Not scheduled once per week")) {
			return null;
		} else if (a.broadcast.equals("Unknown")) {
			log.log("Broadcast is unknown, defaulting to airing day = day it first aired");
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
			return ZonedDateTime.of(dummyWithDayOfWeek(DayOfWeek.valueOf(broadcast[0].toUpperCase())),
									LocalTime.parse(broadcast[1].substring(0, broadcast[1].length() - 6)), jst);
		} else {
			return ZonedDateTime.of(dummyWithDayOfWeek(DayOfWeek.valueOf(broadcast[0].toUpperCase())),
									AnimeDayTime.UNKNOWN_TIME, jst);
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

	private void setTimeZone(String tz) {
		String zone = tz;
		zone = (zone == null) ? "Europe/Berlin" : zone;
		this.zone = ZoneId.of(zone);
		log.logf("Set TimeZone to %s", this.zone.getId());
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

	List<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day) {
		return animes.stream().filter(a -> a.releasesOnDay(day)).sorted().collect(Collectors.toCollection(LinkedList::new));
	}

	List<Anime> getSeasonalAnimes() {
		return animes.stream().map(AnimeDayTime::getAnime).collect(Collectors.toCollection(LinkedList::new));
	}
}
