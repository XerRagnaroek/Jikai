/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.prop.MapProperty;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class JikaiUserUpdater {
	//TODO react to update of anime db
	//TODO release notify
	//dayTimeStepUserMap
	private Map<Integer, Map<Integer, Set<JikaiUser>>> stepMap = new ConcurrentHashMap<>();
	private Map<Integer, Map<Integer, ScheduledFuture<?>>> futureMap = new ConcurrentHashMap<>();
	private MapProperty<ZoneId, Set<JikaiUser>> updatedDaily = new MapProperty<>();
	private Map<ZoneId, ScheduledFuture<?>> dailyFutures = new ConcurrentHashMap<>();
	private final Logger log = LoggerFactory.getLogger(JikaiUserUpdater.class);
	private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("eeee, dd.MM.yyyy");

	public JikaiUserUpdater() {
		updatedDaily.onPut((z, s) -> {
			if (!dailyFutures.containsKey(z)) {
				startDailyUpdateThread(z);
			}
		});
		updatedDaily.onRemove((z, s) -> {
			if (dailyFutures.containsKey(z)) {
				dailyFutures.remove(z).cancel(false);
			}
		});
		AnimeDB.runOnDBUpdate(this::update);
	}

	public void registerUser(JikaiUser ju) {
		subAdd(ju);
		subRem(ju);
		stepAdd(ju);
		stepRem(ju);
		notifDaily(ju);
	}

	private void subAdd(JikaiUser ju) {
		ju.subscribedAnimesProperty().onAdd(id -> {
			log.debug("{} subscribed to {}", ju.getId(), id);
			Anime a = AnimeDB.getAnime(id);
			stepMap.putIfAbsent(id, new ConcurrentHashMap<>());
			stepMap.compute(id, (idA, map) -> {
				ju.getPreReleaseNotifcationSteps().forEach(step -> {
					addToStepUserMap(map, step, a, id, ju);
				});
				return map;
			});
		});
	}

	private void stepAdd(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onAdd(step -> {
			log.debug("{} added PreReleaseNotificationStep {}", ju.getId(), step);
			ju.getSubscribedAnimes().forEach(id -> {
				Anime a = AnimeDB.getAnime(id);
				addToAnimeStepMap(id, step, a, ju);
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
		map.putIfAbsent(step, Collections.synchronizedSet(new TreeSet<>()));
		map.get(step).add(ju);
		if (!hasFuture(id, step)) {
			scheduleStepUpdate(a, step);
			log.debug("Scheduled new update future {} minutes before release of {}", step / 60, a.getId());
		}
		return map;
	}

	private void subRem(JikaiUser ju) {
		ju.subscribedAnimesProperty().onRemove(id -> {
			stepMap.compute(id, (idA, map) -> {
				ju.getPreReleaseNotifcationSteps().forEach(step -> {
					removeFromStepJUMap(id, step, ju, map);
				});
				return handleMapEmpty(map, id);
			});
		});
	}

	private void stepRem(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onRemove(step -> {
			ju.getSubscribedAnimes().forEach(id -> {
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

	private void notifDaily(JikaiUser ju) {
		ju.isUpdatedDailyProperty().onChange((ov, nv) -> {
			if (nv) {
				updatedDaily.compute(ju.getTimeZone(), (z, s) -> {
					if (s == null) {
						s = Collections.synchronizedSet(new HashSet<>());
					}
					s.add(ju);
					log.debug("Added user to daily update thread for timezone {}", ju.getTimeZone().getId());
					return s;
				});
			} else {
				updatedDaily.computeIfPresent(ju.getTimeZone(), (z, s) -> {
					s.remove(ju);
					log.debug("Removed user from daily update thread for timezone {}", ju.getTimeZone().getId());
					return (s.isEmpty() ? null : s);
				});
			}
		});
	}

	private void cancelFuture(int id, int step) {
		if (hasFuture(id, step)) {
			futureMap.compute(id, (idA, map) -> {
				map.get(step).cancel(false);
				if (map.isEmpty()) {
					log.debug("Future map for step {} for anime {}", step, id);
					return null;
				}
				return map;
			});
		}
	}

	//TODO handle negative initial delays!
	private void scheduleStepUpdate(Anime a, int step) {
		//min -> seconds
		int secsToNotif = a.getNextEpisodesAirsIn() - step;
		ScheduledFuture<?> sf = Core.EXEC.scheduleAtFixedRate(() -> updateUser(a, step), secsToNotif, TimeUnit.DAYS.toSeconds(7), TimeUnit.SECONDS);
		putInFutureMap(a.getId(), step, sf);
		log.debug("Scheduled future for step {} of {} first running in {} mins", step, a.getTitleRomaji(), secsToNotif / 60);
	}

	private void putInFutureMap(int id, int step, ScheduledFuture<?> sf) {
		futureMap.putIfAbsent(id, new ConcurrentHashMap<>());
		futureMap.get(id).put(step, sf);
	}

	private void updateUser(Anime a, int step) {
		stepMap.get(a.getId()).get(step).forEach(ju -> {
			MessageEmbed me = step == 0 ? makeNotifyRelease(a) : makeNotifyEmbed(a, step, ju);
			ju.sendPM(me);
		});
	}

	private void startDailyUpdateThread(ZoneId z) {
		ZonedDateTime now = ZonedDateTime.now(z);
		long untilMidnight = now.until(now.plusDays(1).withHour(0).truncatedTo(ChronoUnit.HOURS), ChronoUnit.SECONDS) + 1;
		ScheduledFuture<?> cf = Core.EXEC.scheduleAtFixedRate(() -> updatedDaily.get(z).forEach(JikaiUser::sendDailyUpdate), untilMidnight, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
		dailyFutures.put(z, cf);
		long mins = TimeUnit.SECONDS.toMinutes(untilMidnight);
		untilMidnight -= TimeUnit.MINUTES.toSeconds(mins);
		log.info("Started daily update thread for time zone {}, midnight in {}m, {}s", z.getId(), mins, untilMidnight);
	}

	private void update(AnimeUpdate au) {
		updateRem(au);
		updatePostponed(au);
	}

	private void updateRem(AnimeUpdate au) {
		if (au.hasRemovedAnime()) {
			List<Anime> removed = au.getRemovedAnime();
			Set<JikaiUser> jus = Jikai.getUserManager().users();
			jus.forEach(ju -> {
				StringBuilder bob = new StringBuilder();
				bob.append("These anime have finished airing and you have been automatically unsubscribed from them:");
				Set<Integer> rem = removed.stream().map(Anime::getId).collect(Collectors.toSet());
				rem.retainAll(ju.getSubscribedAnimes());
				rem.stream().map(AnimeDB::getAnime).forEach(a -> {
					bob.append("\\n**" + a.getTitle(ju.getTitleLanguage()) + "**");
					ju.unsubscribeAnime(a.getId());
				});
				ju.sendPM(bob.toString());
			});
		}
	}

	private void updatePostponed(AnimeUpdate au) {
		if (au.hasPostponedAnime()) {
			List<Pair<Anime, Long>> pp = au.getPostponedAnime();
			pp.forEach(p -> {
				Anime a = p.getLeft();
				long delay = p.getRight();
				cancelAnime(a);
				Jikai.getUserManager().users().stream().filter(ju -> ju.isSubscribedTo(a)).forEach(ju -> {
					ju.sendPM(a.getTitle(ju.getTitleLanguage()) + " has been postponed by " + secondsToTime(delay) + "!");
					ju.getPreReleaseNotifcationSteps().forEach(step -> {
						addToAnimeStepMap(a.getId(), step, a, ju);
					});
				});
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
		if (a.hasCoverImage()) {
			eb.setThumbnail(a.getCoverImageURL());
		}
		eb.setTitle("**" + a.getTitleRomaji() + "**", a.getAniUrl()).setDescription(String.format("**%s\nAirs in %s**", a.getNextEpisodeDateTime(ju.getTimeZone()).get().toLocalTime().format(formatter), secondsToTime(step)));
		return eb.build();
	}

	private static MessageEmbed makeNotifyRelease(Anime a) {
		EmbedBuilder eb = new EmbedBuilder();
		if (a.hasCoverImage()) {
			eb.setThumbnail(a.getCoverImageURL());
		}
		eb.setTitle("**" + a.getTitleRomaji() + "**", a.getAniUrl()).setDescription("just released!\nIt will probably be up on your favourite streaming sites in a few minutes.");
		return eb.build();
	}

	private static String secondsToTime(long seconds) {
		long days = TimeUnit.SECONDS.toDays(seconds);
		seconds -= TimeUnit.DAYS.toSeconds(days);
		long hours = TimeUnit.SECONDS.toHours(seconds);
		seconds -= TimeUnit.HOURS.toSeconds(hours);
		long minutes = TimeUnit.SECONDS.toMinutes(seconds);
		BiFunction<String, Long, String> f = (s, l) -> (l == 0 ? "" : l + " " + s + (l == 1 ? "" : "s"));
		List<String> time = new LinkedList<>();
		if (days > 0) {
			time.add(days + " " + (days > 1 ? "days" : "day"));
		}
		if (hours > 0) {
			time.add(hours + " " + (hours > 1 ? "hours" : "hour"));
		}
		if (minutes > 0) {
			time.add(minutes + " " + (minutes > 1 ? "minutes" : "minute"));
		}
		return String.join(", ", time);
	}

}
