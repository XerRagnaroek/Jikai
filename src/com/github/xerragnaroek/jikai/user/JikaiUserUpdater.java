
package com.github.xerragnaroek.jikai.user;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.anime.schedule.AnimeTable;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class JikaiUserUpdater {
	private Map<Integer, Map<Integer, Set<JikaiUser>>> stepMap = new ConcurrentHashMap<>();
	private Map<Integer, Map<Integer, ScheduledFuture<?>>> futureMap = new ConcurrentHashMap<>();
	private Map<ZoneId, ScheduledFuture<?>> dailyFutures = new ConcurrentHashMap<>();
	private JikaiUserManager jum = JikaiUserManager.getInstance();
	private final Logger log = LoggerFactory.getLogger(JikaiUserUpdater.class);
	private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("eeee, dd.MM.yyyy");

	public JikaiUserUpdater() {
		jum.timeZoneMapProperty().onPut((z, s) -> {
			if (!dailyFutures.containsKey(z)) {
				startDailyUpdateThread(z);
			}
		});
		jum.timeZoneMapProperty().onRemove((z, s) -> {
			if (dailyFutures.containsKey(z)) {
				dailyFutures.remove(z).cancel(false);
				log.debug("Cancelled daily update future for time zone '{}'", z.getId());
			}
		});
		AnimeDB.runOnDBUpdate(this::update);
	}

	public void registerUser(JikaiUser ju) {
		subAdd(ju);
		subRem(ju);
		stepAdd(ju);
		stepRem(ju);
	}

	private void subAdd(JikaiUser ju) {
		ju.subscribedAnimesProperty().onAdd(id -> {
			log.debug("{} subscribed to {}", ju.getId(), id);
			Anime a = AnimeDB.getAnime(id);
			if (a.hasDataForNextEpisode()) {
				animeAddImpl(a, id, ju);
			} else {
				if (!Core.INITIAL_LOAD.get()) {
					ju.sendPM("**" + a.getTitle(ju.getTitleLanguage()) + "** doesn't have any data concerning the release of the next episode, so you won't recieve any updates!\nIf any data becomes available, then notifications will resume.");
				}
			}
		});
	}

	private void animeAddImpl(Anime a, int id, JikaiUser ju) {
		stepMap.putIfAbsent(id, new ConcurrentHashMap<>());
		stepMap.compute(id, (idA, map) -> {
			ju.getPreReleaseNotifcationSteps().forEach(step -> {
				addToStepUserMap(map, step, a, id, ju);
			});
			return map;
		});
	}

	private void stepAdd(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onAdd(step -> {
			log.debug("{} added PreReleaseNotificationStep {}", ju.getId(), step);
			ju.getSubscribedAnime().forEach(id -> {
				Anime a = AnimeDB.getAnime(id);
				if (a.hasDataForNextEpisode()) {
					addToAnimeStepMap(id, step, a, ju);
				}
			});
		});
	}

	private void addToAnimeStepMap(int id, int step, Anime a, JikaiUser ju) {
		stepMap.putIfAbsent(id, new ConcurrentHashMap<>());
		stepMap.compute(id, (idA, map) -> {
			return addToStepUserMap(map, step, a, id, ju);
		});
	}

	private Map<Integer, Set<JikaiUser>> addToStepUserMap(Map<Integer, Set<JikaiUser>> map, int step, Anime a, int id, JikaiUser ju) {
		map.putIfAbsent(step, Collections.synchronizedSet(new HashSet<>()));
		map.get(step).add(ju);
		if (!hasFuture(id, step)) {
			scheduleStepUpdate(a, step);
			log.debug("Scheduled new update future {} minutes before release of {}", step / 60, a.getId());
		}
		return map;
	}

	private void subRem(JikaiUser ju) {
		ju.subscribedAnimesProperty().onRemove(id -> {
			if (stepMap.containsKey(id)) {
				stepMap.compute(id, (idA, map) -> {
					ju.getPreReleaseNotifcationSteps().forEach(step -> {
						removeFromStepJUMap(id, step, ju, map);
					});
					return handleMapEmpty(map, id);
				});
			}
		});

	}

	private void stepRem(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onRemove(step -> {
			ju.getSubscribedAnime().forEach(id -> {
				stepMap.compute(id, (idA, map) -> {
					removeFromStepJUMap(id, step, ju, map);
					return handleMapEmpty(map, id);
				});
			});
		});
	}

	private void removeFromStepJUMap(int id, int step, JikaiUser ju, Map<Integer, Set<JikaiUser>> map) {
		map.compute(step, (s, set) -> {
			set.remove(ju);
			if (set.isEmpty()) {
				log.debug("User set for step {} for anime {} is empty", step, id);
				cancelFuture(id, step);
				return null;
			}
			return set;
		});
	}

	private Map<Integer, Set<JikaiUser>> handleMapEmpty(Map<Integer, Set<JikaiUser>> map, int id) {
		if (map.isEmpty()) {
			log.debug("Step->User map for anime {} is empty", id);
			return null;
		}
		return map;
	}

	private void cancelFuture(int id, int step) {
		ScheduledFuture<?> sf = removeFuture(id, step);
		if (sf != null) {
			sf.cancel(false);
			log.debug("Cancelled future for id {} and step {}", id, step);
		}
	}

	private ScheduledFuture<?> removeFuture(int id, int step) {
		ScheduledFuture<?>[] sf = new ScheduledFuture<?>[1];
		if (hasFuture(id, step)) {
			futureMap.compute(id, (idA, map) -> {
				sf[0] = map.remove(step);
				if (map.isEmpty()) {
					log.debug("Future map for step {} for anime {} is empty", step, id);
					return null;
				}
				return map;
			});
		}
		return sf[0];
	}

	// TODO handle negative initial delays!
	private void scheduleStepUpdate(Anime a, int step) {
		// min -> seconds
		int secsToNotif = a.getNextEpisodesAirsIn() - step;
		if (secsToNotif < 0) {
			secsToNotif += TimeUnit.DAYS.toSeconds(7);
		}
		ScheduledFuture<?> sf = Core.EXEC.schedule(() -> {
			updateUser(a, step);
			removeFuture(a.getId(), step);
		}, secsToNotif, TimeUnit.SECONDS);
		putInFutureMap(a.getId(), step, sf);
		log.debug("Scheduled future for step {} of {} first running in {} mins", step, a.getTitleRomaji(), secsToNotif / 60);
	}

	private void putInFutureMap(int id, int step, ScheduledFuture<?> sf) {
		futureMap.putIfAbsent(id, new ConcurrentHashMap<>());
		futureMap.get(id).put(step, sf);
	}

	public void updateUser(Anime a, int step) {
		stepMap.get(a.getId()).get(step).forEach(ju -> {
			MessageEmbed me = step == 0 ? makeNotifyRelease(a, ju) : makeNotifyEmbed(a, step, ju);
			ju.sendPM(me);
		});
	}

	private void startDailyUpdateThread(ZoneId z) {
		ZonedDateTime now = ZonedDateTime.now(z);
		// + 1 cause end is excluded and +10 so it's at 00:00:10, which guarantees than any LocalDate.now()
		// calls is actually the next day and not still the last one
		long untilMidnight = now.until(now.plusDays(1).withHour(0).truncatedTo(ChronoUnit.HOURS), ChronoUnit.SECONDS) + 11;
		ScheduledFuture<?> cf = Core.EXEC.scheduleAtFixedRate(() -> jum.getJikaiUsersWithTimeZone(z).forEach(this::sendDailyUpdate), untilMidnight, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
		dailyFutures.put(z, cf);
		long mins = TimeUnit.SECONDS.toMinutes(untilMidnight);
		untilMidnight -= TimeUnit.MINUTES.toSeconds(mins);
		log.info("Started daily update thread for time zone {}, midnight in {}m, {}s", z.getId(), mins, untilMidnight);
	}

	private void update(AnimeUpdate au) {
		if (au.hasChange()) {
			updateRem(au);
			updateReleaseChanged(au);
			updateNextEp(au);
			updatePeriodChanged(au);
		}
	}

	private void updateRem(AnimeUpdate au) {
		if (au.hasRemovedAnime()) {
			List<Anime> removed = au.getRemovedAnime();
			Set<JikaiUser> jus = jum.users();
			jus.forEach(ju -> {
				Set<Integer> rem = removed.stream().map(Anime::getId).collect(Collectors.toSet());
				rem.retainAll(ju.getSubscribedAnime());
				if (!rem.isEmpty()) {
					ju.sendPMFormat("%d anime %s finished airing!", rem.size(), rem.size() > 1 ? "have" : "has");
					rem.stream().map(AnimeDB::getAnime).forEach(a -> {
						ju.unsubscribeAnime(a.getId());
					});
				}
			});
		}
	}

	private void updateReleaseChanged(AnimeUpdate au) {
		if (au.hasChangedReleaseAnime()) {
			List<Pair<Anime, Long>> pp = au.getChangedReleaseAnime();
			pp.forEach(p -> {
				Anime a = p.getLeft();
				long delay = p.getRight();
				cancelAnime(a);
				jum.users().stream().filter(ju -> ju.isSubscribedTo(a)).forEach(ju -> {
					ju.sendPM(makeReleaseChangedEmbed(ju, a, delay));
					ju.getPreReleaseNotifcationSteps().forEach(step -> {
						addToAnimeStepMap(a.getId(), step, a, ju);
					});
				});
			});
		}
	}

	private void updatePeriodChanged(AnimeUpdate au) {
		if (au.hasAnimeChangedPeriod()) {
			List<Pair<Anime, String>> changedP = au.getAnimeChangedPeriod();
			changedP.forEach(pair -> {
				Anime a = pair.getLeft();
				String dayDif = pair.getRight();
				jum.getJUSubscribedToAnime(a).forEach(ju -> {
					EmbedBuilder eb = new EmbedBuilder();
					eb.setTitle(a.getTitle(ju.getTitleLanguage()), a.getAniUrl()).setThumbnail(a.getCoverImageMedium()).setDescription(String.format("airs later than usual!%nThe next episode will be in **%s** on%n**%s**.", dayDif, formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get())) + "**.");
					ju.sendPM(eb.build());
				});
			});
		}
	}

	private static MessageEmbed makeReleaseChangedEmbed(JikaiUser ju, Anime a, long delay) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(a.getTitle(ju.getTitleLanguage()), a.getAniUrl());
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		List<String> externalLinks = a.getExternalLinks().stream().filter(es -> es.getSite().equals("Twitter") || es.getSite().equals("Official Site")).map(es -> String.format("[**%s**](%s)", es.getSite(), es.getUrl())).collect(Collectors.toList());
		StringBuilder bob = new StringBuilder();
		if (delay > 0) {
			bob.append("has been postponed by " + BotUtils.formatSeconds(delay) + " to **");
		} else {
			bob.append("airs " + BotUtils.formatSeconds(delay) + " earlier! That's **");
		}
		bob.append(formatter.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(a.getNextEpisodesAirsAt()), ju.getTimeZone())) + "**");
		if (!externalLinks.isEmpty()) {
			bob.append("\nCheck these links for more information: " + StringUtils.joinWith(", ", externalLinks.toArray()));
		}
		eb.setDescription(bob).setTimestamp(Instant.now());
		return eb.build();
	}

	private void updateNextEp(AnimeUpdate au) {
		if (au.hasAnimeNextEp()) {
			au.getAnimeNextEp().forEach(a -> {
				jum.getJUSubscribedToAnime(a).forEach(ju -> animeAddImpl(a, a.getId(), ju));
			});
		}
	}

	private void cancelAnime(Anime a) {
		stepMap.remove(a.getId());
		futureMap.compute(a.getId(), (id, map) -> {
			map.values().forEach(sf -> sf.cancel(true));
			return null;
		});
	}

	private boolean hasFuture(int id, int step) {
		if (futureMap.containsKey(id)) {
			Map<Integer, ScheduledFuture<?>> map = futureMap.get(id);
			return map.containsKey(step);
		}
		return false;
	}

	private static MessageEmbed makeNotifyEmbed(Anime a, int step, JikaiUser ju) {
		EmbedBuilder eb = new EmbedBuilder();
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		eb.setTitle("**" + a.getTitleRomaji() + "**", a.getAniUrl()).setDescription(String.format("**%s%nEpisode %2d/%2d airs in %s.**", formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get()), a.getNextEpisodeNumber(), a.getEpisodes(), BotUtils.formatSeconds(step)));
		eb.setTimestamp(ZonedDateTime.now(ju.getTimeZone()));
		return eb.build();
	}

	private static String formatAirDateTime(LocalDateTime ldt) {
		return ldt.format(DateTimeFormatter.ofPattern("eeee, dd.MM.yyyy")) + " at " + ldt.format(DateTimeFormatter.ofPattern("HH:mm"));
	}

	private static MessageEmbed makeNotifyRelease(Anime a, JikaiUser ju) {
		EmbedBuilder eb = new EmbedBuilder();
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		eb.setTitle("**" + a.getTitleRomaji() + "**", a.getAniUrl()).setDescription(String.format("Episode %2d/%2d just released!%nIt will probably be up on your favourite streaming sites in a few minutes.", a.getNextEpisodeNumber(), a.getEpisodes()));
		eb.setTimestamp(ZonedDateTime.now(ju.getTimeZone()));
		return eb.build();
	}

	public static MessageEmbed testNotify(Anime a, int step, JikaiUser ju) {
		return step == 0 ? makeNotifyRelease(a, ju) : makeNotifyEmbed(a, step, ju);
	}

	public static MessageEmbed testPostpone(Anime a, long delay, JikaiUser ju) {
		return makeReleaseChangedEmbed(ju, a, delay);
	}

	public void testDailyUpdate(JikaiUser ju) {
		sendDailyUpdate(ju);
	}

	private Message createDailyMessage(JikaiUser ju, AnimeTable at, DayOfWeek day) {
		String content = at.toFormatedWeekString(ju.getTitleLanguage(), false).get(day);
		StringBuilder bob = new StringBuilder();
		if (content.isBlank()) {
			bob.append("Jikai tried her best but couldn't find any anime airing on " + StringUtils.capitalize(day.toString().toLowerCase()) + "s that you are subscribed to :(");
		} else {
			bob.append(content);
		}
		MessageBuilder mb = new MessageBuilder();
		String str = "Your daily anime are:";
		mb.appendCodeBlock(str + "\n" + "=".repeat(str.length()) + "\n" + bob.toString(), "asciidoc");
		return mb.build();
	}

	private void sendDailyUpdate(JikaiUser ju) {
		ZoneId zone = ju.getTimeZone();
		LocalDate ld = ZonedDateTime.now(zone).toLocalDate();
		DayOfWeek today = ld.getDayOfWeek();
		AnimeTable at = ScheduleManager.getSchedule(zone).makeUserTable(ju);
		if (today == DayOfWeek.MONDAY && ju.isSentWeeklySchedule()) {
			StringBuilder bob = new StringBuilder();
			String str = "Here's your anime schedule for this week:";
			bob.append(str + "\n" + "=".repeat(str.length()));
			at.toFormatedWeekString(ju.getTitleLanguage(), true).values().forEach(s -> bob.append("\n" + s));
			MessageBuilder mb = new MessageBuilder();
			mb.appendCodeBlock(bob, "asciidoc");
			mb.buildAll(SplitPolicy.NEWLINE).forEach(ju::sendPM);
		} else if (ju.isUpdatedDaily()) {
			ju.sendPM(createDailyMessage(ju, at, today));
		}
	}
}
