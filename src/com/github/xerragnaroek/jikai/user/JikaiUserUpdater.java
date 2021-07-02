
package com.github.xerragnaroek.jikai.user;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.anime.schedule.AnimeTable;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

public class JikaiUserUpdater implements ButtonInteractor {
	private Map<Integer, Map<Integer, Set<JikaiUser>>> stepMap = new ConcurrentHashMap<>();
	private Map<Integer, Map<Integer, ScheduledFuture<?>>> futureMap = new ConcurrentHashMap<>();
	private Map<ZoneId, ScheduledFuture<?>> dailyFutures = new ConcurrentHashMap<>();
	private JikaiUserManager jum = JikaiUserManager.getInstance();
	private final Logger log;
	private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("eeee, dd.MM.yyyy");

	public JikaiUserUpdater() {
		log = LoggerFactory.getLogger(JikaiUserUpdater.class);
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
		Core.getEventListener().registerButtonInteractor(this);
	}

	public void registerUser(JikaiUser ju) {
		subAdd(ju);
		subRem(ju);
		stepAdd(ju);
		stepRem(ju);
	}

	private void subAdd(JikaiUser ju) {
		ju.getSubscribedAnime().onAdd(sa -> {
			MDC.put("id", String.valueOf(ju.getId()));
			log.debug("subscribed to {}", sa.id());
			Anime a = AnimeDB.getAnime(sa.id());
			Message me;
			if (a.hasDataForNextEpisode()) {
				animeAddImpl(a, sa.id(), ju);
			}
			if (!sa.silent()) {
				if (a.hasDataForNextEpisode()) {
					me = subAddMsg(a, ju, sa.cause(), sa.linked());
				} else {
					me = subAddNoDataMsg(a, ju, sa.cause(), sa.linked());
				}
				if (!Core.INITIAL_LOAD.get()) {
					ju.sendPM(me);
				}
			}
			MDC.remove("id");
		});
	}

	private Message subAddMsg(Anime a, JikaiUser ju, String cause, boolean linked) {
		JikaiLocale loc = ju.getLocale();
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setThumbnail(a.getBiggestAvailableCoverImage());
		eb.setTitle(loc.getString("ju_eb_sub_add_title"));
		long seconds = Duration.between(LocalDateTime.now(ju.getTimeZone()), a.getNextEpisodeDateTime(ju.getTimeZone()).get()).toSeconds();
		eb.setDescription(loc.getStringFormatted("ju_eb_sub_add_desc", Arrays.asList("title", "time", "cause"), "[" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + "](" + a.getAniUrl() + ")", BotUtils.formatSeconds(seconds, loc), cause));
		MessageBuilder mb = new MessageBuilder(eb);
		if (linked) {
			mb.setActionRows(ActionRow.of(Button.danger("juu:" + a.getId(), loc.getString("ju_unsub_btn"))));
		}
		return mb.build();
	}

	private Message subAddNoDataMsg(Anime a, JikaiUser ju, String cause, boolean linked) {
		JikaiLocale loc = ju.getLocale();
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setTitle(loc.getString("ju_eb_sub_add_title"));
		eb.setThumbnail(a.getBiggestAvailableCoverImage());
		eb.setDescription(loc.getStringFormatted("ju_eb_sub_add_no_data_desc", Arrays.asList("title", "cause"), "[" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + "](" + a.getAniUrl() + ")", cause));
		MessageBuilder mb = new MessageBuilder(eb);
		if (linked) {
			mb.setActionRows(ActionRow.of(Button.danger("juu:" + a.getId(), loc.getString("ju_unsub_btn"))));
		}
		return mb.build();
	}

	private MessageEmbed subRemMsg(Anime a, JikaiUser ju, String cause) {
		JikaiLocale loc = ju.getLocale();
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setThumbnail(a.getBiggestAvailableCoverImage());
		eb.setTitle(loc.getString("ju_eb_sub_rem_title"));
		eb.setDescription(loc.getStringFormatted("ju_eb_sub_rem_desc", Arrays.asList("title", "cause"), "[" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + "](" + a.getAniUrl() + ")", cause));
		return eb.build();
	}

	private void animeAddImpl(Anime a, int id, JikaiUser ju) {
		log.debug("Scheduling Anime {} for JUser {}", a.getTitleRomaji(), ju.getId());
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
			MDC.put("id", String.valueOf(ju.getId()));
			log.debug("{} added PreReleaseNotificationStep {}", ju.getId(), step);
			ju.getSubscribedAnime().forEach(id -> {
				Anime a = AnimeDB.getAnime(id);
				if (a.hasDataForNextEpisode()) {
					addToAnimeStepMap(id, step, a, ju);
				}
			});
			MDC.remove("id");
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
		log.debug("JUser {} has been added to map for {}, step {}", ju.getId(), a.getTitleRomaji(), step);
		if (!hasFuture(id, step)) {
			log.debug("Scheduling new future for {}, step {}", a.getTitleRomaji(), step);
			scheduleStepUpdate(a, step);
			log.debug("Scheduled new update future {} minutes before release of {}", step / 60, a.getId());
		}
		return map;
	}

	private void subRem(JikaiUser ju) {
		ju.getSubscribedAnime().onRemove(sr -> {
			if (stepMap.containsKey(sr.id())) {
				stepMap.compute(sr.id(), (idA, map) -> {
					ju.getPreReleaseNotifcationSteps().forEach(step -> {
						removeFromStepJUMap(sr.id(), step, ju, map);
					});
					return handleMapEmpty(map, sr.id());
				});
			}
			Anime a;
			if ((a = AnimeDB.getAnime(sr.id())) != null && !sr.silent()) {
				ju.sendPM(subRemMsg(a, ju, sr.cause()));
			}
		});

	}

	private void stepRem(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onRemove(step -> {
			ju.getSubscribedAnime().forEach(id -> {
				if (stepMap.containsKey(id)) {
					stepMap.compute(id, (idA, map) -> {
						removeFromStepJUMap(id, step, ju, map);
						return handleMapEmpty(map, id);
					});
				}
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
			log.debug("Sending update: JUser={},Anime={},Step={}", ju.getId(), a.getTitleRomaji(), step);
			if (step == 0) {
				Message m = EpisodeTracker.addButton(a, makeNotifyRelease(a, ju), false);
				BotUtils.sendPM(ju.getUser(), m).get(0).thenAccept(msg -> {
					log.debug("Sent release msg {}", msg.getId());
					EpisodeTrackerManager.getTracker(ju).registerEpisode(a, msg.getIdLong());
				});
			} else {
				ju.sendPM(makeNotifyEmbed(a, step, ju)).thenAccept(b -> log.debug("Send notify msg success: {}", b));
			}
			/*
			 * MessageEmbed me = step == 0 ? makeNotifyRelease(a, ju) : makeNotifyEmbed(a, step, ju);
			 * BotUtils.retryFuture(2, () -> BotUtils.sendPM(ju.getUser(), me).thenAccept(m -> {
			 * if (step == 0) {
			 * m.addReaction(EpisodeTracker.WATCHED_EMOJI_UNICODE).and(m.pin()).submit().thenAccept(v -> {
			 * log.debug("Pinned and added watched emoji to message {}" + m.getIdLong());
			 * EpisodeTracker.getTracker(ju).registerEpisode(a, m.getIdLong());
			 * });
			 * }
			 * }));
			 */
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
			Map<Integer, Anime> remA = removed.stream().collect(Collectors.toMap(Anime::getId, a -> a));
			Set<JikaiUser> jus = jum.users();
			jus.forEach(ju -> {
				Set<Integer> rem = removed.stream().map(Anime::getId).collect(Collectors.toSet());
				rem.retainAll(ju.getSubscribedAnime());
				if (!rem.isEmpty()) {
					rem.forEach(id -> {
						Anime a = remA.get(id);
						JikaiLocale loc = ju.getLocale();
						String cause = null;
						if (a.isFinished()) {
							cause = loc.getString("ju_sub_rem_cause_finished");
						} else {
							cause = loc.getStringFormatted("ju_sub_rem_cause_unknown", Arrays.asList("links"), BotUtils.formatExternalSites(a));
						}
						ju.unsubscribeAnime(a, cause);
						ju.sendPM(subRemMsg(a, ju, cause));
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
			List<Pair<Anime, Long>> changedP = au.getAnimeChangedPeriod();
			changedP.forEach(pair -> {
				Anime a = pair.getLeft();
				long dif = pair.getRight();
				jum.getJUSubscribedToAnime(a).forEach(ju -> {
					ju.sendPM(makePeriodChangedEmbed(ju, a, dif));
				});
			});
		}
	}

	private MessageEmbed makePeriodChangedEmbed(JikaiUser ju, Anime a, long dif) {
		JikaiLocale loc = ju.getLocale();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(loc.getStringFormatted("ju_eb_period_change_title", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))), a.getAniUrl()).setThumbnail(a.getBiggestAvailableCoverImage());
		// later
		if (dif > 0) {
			eb.setDescription(loc.getStringFormatted("ju_eb_period_change_later_desc", Arrays.asList("dif", "time", "date"), BotUtils.formatSeconds(dif, loc), BotUtils.formatSeconds(a.getNextEpisodesAirsIn(), loc), formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get(), loc.getLocale())));
		} else if (dif < 0) {
			// earlier
			eb.setDescription(loc.getStringFormatted("ju_eb_period_change_earlier_desc", Arrays.asList("dif", "time", "date"), BotUtils.formatSeconds(dif, loc), BotUtils.formatSeconds(a.getNextEpisodesAirsIn(), loc), formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get(), loc.getLocale())));
		} else {
			// dif = 0
			eb.setDescription(loc.getStringFormatted("ju_eb_period_change_unknown", Arrays.asList("links"), BotUtils.formatExternalSites(a)));
		}
		return eb.build();
	}

	private MessageEmbed makeReleaseChangedEmbed(JikaiUser ju, Anime a, long delay) {
		EmbedBuilder eb = BotUtils.embedBuilder();
		JikaiLocale loc = ju.getLocale();
		eb.setTitle(loc.getStringFormatted("ju_eb_release_change_title", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))), a.getAniUrl());
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		List<String> externalLinks = a.getExternalLinks().stream().filter(es -> es.getSite().equals("Twitter") || es.getSite().equals("Official Site")).map(es -> String.format("[**%s**](%s)", es.getSite(), es.getUrl())).collect(Collectors.toList());
		StringBuilder bob = new StringBuilder();
		if (delay == 0) {
			bob.append(loc.getString("ju_eb_release_change_unknown"));
		} else {
			// the optional here will always have a value since this method can only get called when its value
			// changes
			String date = formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get(), loc.getLocale());
			if (delay > 0) {
				bob.append(loc.getStringFormatted("ju_eb_release_change_pp", Arrays.asList("time", "date"), BotUtils.formatSeconds(delay, loc), date));
			} else {
				bob.append(loc.getStringFormatted("ju_eb_release_change_earlier", Arrays.asList("time", "date"), BotUtils.formatSeconds(delay, loc), date));
			}
		}
		if (!externalLinks.isEmpty()) {
			bob.append(loc.getStringFormatted("ju_eb_release_change_links", Arrays.asList("links"), StringUtils.joinWith(", ", externalLinks)));
		}
		eb.setDescription(bob);
		return eb.build();
	}

	private void updateNextEp(AnimeUpdate au) {
		if (au.hasAnimeNextEp()) {
			log.debug("Handling next eps");
			au.getAnimeNextEp().forEach(a -> {
				log.debug("Handling next ep for {}", a.getTitleRomaji());
				jum.getJUSubscribedToAnime(a).forEach(ju -> {
					animeAddImpl(a, a.getId(), ju);
					log.debug("Checking if user is sent next ep msg");
					if (ju.isSentNextEpMessage()) {
						sendNextEpMsg(ju, a);
					}
				});
			});
		}
	}

	private void sendNextEpMsg(JikaiUser ju, Anime a) {
		MDC.put("id", ju.getId() + "");
		log.debug("Sending next episode message for {}, {}", a.getTitleRomaji(), a.getId());
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		JikaiLocale loc = ju.getLocale();
		eb.setTitle(loc.getStringFormatted("ju_eb_next_ep_title", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))), a.getAniUrl());
		long minsTillAir = Instant.now().until(Instant.ofEpochSecond(a.getNextEpisodesAirsAt()), ChronoUnit.MINUTES) + 1;
		eb.setDescription(loc.getStringFormatted("ju_eb_next_ep_desc", Arrays.asList("time", "date"), BotUtils.formatMinutes(minsTillAir, loc), formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get(), loc.getLocale())));
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		ju.sendPM(eb.build());
		log.debug("Message sent");
		MDC.remove("id");
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
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		JikaiLocale loc = ju.getLocale();
		eb.setTitle(loc.getStringFormatted("ju_eb_notify_title", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))), a.getAniUrl());
		int eps = a.getEpisodes();
		String episodes = eps == 0 ? "" : String.format("/%2d", eps);
		eb.setDescription(loc.getStringFormatted("ju_eb_notify_desc", Arrays.asList("date", "episodes", "time"), formatAirDateTime(a.getNextEpisodeDateTime(ju.getTimeZone()).get(), loc.getLocale()), String.format("%2d%s", a.getNextEpisodeNumber(), episodes), BotUtils.formatSeconds(step, loc)));
		return eb.build();
	}

	private static String formatAirDateTime(LocalDateTime ldt, Locale loc) {
		return ldt.format(DateTimeFormatter.ofPattern("eeee, dd.MM.yyyy, HH:mm").localizedBy(loc));
	}

	private static MessageEmbed makeNotifyRelease(Anime a, JikaiUser ju) {
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		if (a.hasCoverImageMedium()) {
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
		}
		JikaiLocale loc = ju.getLocale();
		eb.setTitle(loc.getStringFormatted("ju_eb_notify_release_title", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))), a.getAniUrl());
		int eps = a.getEpisodes();
		String episodes = eps == 0 ? "" : String.format("/%2d", eps);
		eb.setDescription(loc.getStringFormatted("ju_eb_notify_release_desc", Arrays.asList("episodes"), String.format("%2d%s", a.getNextEpisodeNumber(), episodes)));
		eb.appendDescription("\n" + a.getEpisodeLinks().stream().sorted().map(el -> String.format("**[[%s]](%s)**", el.siteName(), el.url())).collect(Collectors.joining(", ")));
		return eb.build();
	}

	public Message testNotify(Anime a, int step, JikaiUser ju) {
		if (a.hasDataForNextEpisode()) {
			MessageEmbed me = step == 0 ? makeNotifyRelease(a, ju) : makeNotifyEmbed(a, step, ju);
			Message m;
			if (step == 0) {
				m = EpisodeTracker.addButton(a, me, true);
			} else {
				m = new MessageBuilder(me).build();
			}
			return m;
		} else {
			MessageEmbed me = BotUtils.addJikaiMark(new EmbedBuilder()).setDescription("**" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + "** has no data available! step: " + step).build();
			return new MessageBuilder(me).build();
		}
	}

	public MessageEmbed testPostpone(Anime a, long delay, JikaiUser ju) {
		return makeReleaseChangedEmbed(ju, a, delay);
	}

	public void testDailyUpdate(JikaiUser ju) {
		sendDailyUpdate(ju);
	}

	public MessageEmbed testPeriodChanged(Anime a, long dif, JikaiUser ju) {
		return makePeriodChangedEmbed(ju, a, dif);
	}

	public void testNextEpMessage(JikaiUser ju, Anime a) {
		sendNextEpMsg(ju, a);
	}

	private Queue<MessageEmbed> createDailyMessage(JikaiUser ju, AnimeTable at, DayOfWeek day) {
		JikaiLocale loc = ju.getLocale();
		String content = at.toFormatedWeekString(ju.getTitleLanguage(), false, loc.getLocale(), ju).get(day);
		StringBuilder bob = new StringBuilder();
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setTitle(loc.getString("ju_eb_daily_update_msg"));
		if (content.isBlank()) {
			eb.setDescription(loc.getStringFormatted("ju_eb_daily_update_none", Arrays.asList("day"), day.getDisplayName(TextStyle.FULL, loc.getLocale())));
			Queue<MessageEmbed> single = new LinkedList<>();
			single.add(eb.build());
			return single;
		} else {
			bob.append(content);
		}
		MessageBuilder mb = new MessageBuilder();
		mb.setContent(bob.toString());
		Queue<MessageEmbed> q = new LinkedList<>();
		Queue<Message> msgs = mb.buildAll(SplitPolicy.NEWLINE);
		Consumer<Message> setAndAdd = m -> {
			eb.setDescription("```asciidoc\n" + m.getContentRaw() + "\n```");
			q.add(eb.build());
		};
		setAndAdd.accept(msgs.poll());
		eb.setTitle(null);
		msgs.forEach(setAndAdd);
		return q;
	}

	private void sendDailyUpdate(JikaiUser ju) {
		log.debug("Sending daily update to JUser '{}'", ju.getId());
		ZoneId zone = ju.getTimeZone();
		LocalDate ld = ZonedDateTime.now(zone).toLocalDate();
		DayOfWeek today = ld.getDayOfWeek();
		AnimeTable at = ScheduleManager.getSchedule(zone).makeUserTable(ju);
		if (today == DayOfWeek.MONDAY) {
			if (ju.isSentWeeklySchedule()) {
				BotUtils.sendPMsEmbed(ju.getUser(), BotUtils.createWeeklySchedule(ju, at));
			} else if (ju.isUpdatedDaily()) {
				BotUtils.sendPMsEmbed(ju.getUser(), createDailyMessage(ju, at, today));
			}
		} else if (ju.isUpdatedDaily()) {
			BotUtils.sendPMsEmbed(ju.getUser(), createDailyMessage(ju, at, today));
		}
	}

	@Override
	public String getIdentifier() {
		return "juu";
	}

	@Override
	public void handleButtonClick(String[] data, ButtonClickEvent event) {
		if (jum.isKnownJikaiUser(event.getUser().getIdLong())) {
			JikaiUser ju = jum.getUser(event.getUser().getIdLong());
			int id = Integer.parseInt(data[0]);
			if (AnimeDB.hasAnime(id)) {
				ju.unsubscribeAnime(id, "Clicked unsub button", true);
			}
			event.editButton(event.getButton().withLabel(ju.getLocale().getString("ju_unsub_btn_click")).asDisabled()).queue();
		}
	}

}
