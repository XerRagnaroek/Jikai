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
package com.xerragnaroek.jikai.user;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
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

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.util.prop.MapProperty;
import com.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class JikaiUserUpdater {
	//TODO react to update of anime db
	//TODO release notify
	//dayTimeStepUserMap
	private Map<DayOfWeek, Map<LocalTime, Map<Long, Map<String, Set<JikaiUser>>>>> dTSTUMap = new ConcurrentHashMap<>();
	private Map<DayOfWeek, Map<LocalTime, Map<Long, Map<String, ScheduledFuture<?>>>>> futureMap = new ConcurrentHashMap<>();
	private MapProperty<ZoneId, Set<JikaiUser>> updatedDaily = new MapProperty<>();
	private Map<ZoneId, ScheduledFuture<?>> dailyFutures = new ConcurrentHashMap<>();
	private final Logger log = LoggerFactory.getLogger(JikaiUserUpdater.class);

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
		AnimeDB.dbVersionProperty().onChange((n1, n2) -> update());
	}

	public void registerUser(JikaiUser ju) {
		subAdd(ju);
		subRem(ju);
		stepAdd(ju);
		stepRem(ju);
		notifDaily(ju);
	}

	private boolean hasFuture(DayOfWeek d, LocalTime time, long step, String title) {
		if (futureMap.containsKey(d)) {
			Map<LocalTime, Map<Long, Map<String, ScheduledFuture<?>>>> tstsm = futureMap.get(d);
			if (tstsm.containsKey(time)) {
				Map<Long, Map<String, ScheduledFuture<?>>> stsm = tstsm.get(time);
				if (stsm.containsKey(step)) {
					Map<String, ScheduledFuture<?>> tsm = stsm.get(step);
					return tsm.containsKey(title);
				}
			}
		}
		return false;
	}

	private void subAdd(JikaiUser ju) {
		ju.subscribedAnimesProperty().onAdd(title -> {
			log.debug("{} subscribed to {}", ju.getId(), title);
			userTitleToMap(ju, title);
		});
	}

	private void userTitleToMap(JikaiUser ju, String title) {
		AnimeDayTime adt = AnimeDB.getADT(ju.getTimeZone(), title);
		dTSTUMap.putIfAbsent(adt.getDayOfWeek(), new ConcurrentHashMap<>());
		dTSTUMap.compute(adt.getDayOfWeek(), (d, tstum) -> {
			tstum.putIfAbsent(adt.getBroadcastTime(), new ConcurrentHashMap<>());
			tstum.compute(adt.getBroadcastTime(), (time, stum) -> {
				Property<Map<Long, Map<String, Set<JikaiUser>>>> wrapper = new Property<>(stum);
				ju.getPreReleaseNotifcationSteps().forEach(step -> {
					wrapper.get().putIfAbsent(step, new ConcurrentHashMap<>());
					wrapper.get().compute(step, (st, tum) -> {
						tum.putIfAbsent(title, Collections.synchronizedSet(new HashSet<>()));
						tum.compute(title, (t, u) -> {
							u.add(ju);
							if (!hasFuture(d, time, step, title)) {
								scheduleStepUpdate(d, adt.getBroadcastTime(), step, title, adt);
							}
							return u;
						});
						return tum;
					});
				});
				return stum;
			});
			return tstum;
		});

	}

	private void subRem(JikaiUser ju) {
		ju.subscribedAnimesProperty().onRemove(title -> {
			log.debug("{} unsubscribed from {}", ju.getId(), title);
			AnimeDayTime adt = AnimeDB.getADT(ju.getTimeZone(), title);
			dTSTUMap.compute(adt.getDayOfWeek(), (d, tstum) -> {
				tstum.compute(adt.getBroadcastTime(), (time, stum) -> {
					Property<Map<Long, Map<String, Set<JikaiUser>>>> wrapper = new Property<>(stum);
					ju.getPreReleaseNotifcationSteps().forEach(step -> {
						wrapper.get().compute(step, (st, tum) -> {
							tum.compute(title, (t, u) -> {
								u.remove(ju);
								if (u.isEmpty()) {
									cancelFuture(d, time, step, title);
									log.debug("User is empty for {}->{}->{}->{}", d, time, st, t);
									return null;
								}
								return u;
							});
							if (tum.isEmpty()) {
								log.debug("Title->User map is empty for {}->{}->{}", d, time, st);
								return null;
							}
							return tum;
						});
					});
					if (stum.isEmpty()) {
						log.debug("Step->Title->User map is empty for {}->{}", d, time);
						return null;
					}
					return stum;
				});
				if (tstum.isEmpty()) {
					log.debug("Time->Step->Title->User map is empty for {}", d);
					return null;
				}
				return tstum;
			});
		});
	}

	private void stepAdd(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onAdd(step -> {
			log.debug("{} added PreReleaseNotificationStep {}", ju.getId(), step);
			ju.getSubscribedAnimes().forEach(title -> {
				AnimeDayTime adt = AnimeDB.getADT(ju.getTimeZone(), title);
				dTSTUMap.putIfAbsent(adt.getDayOfWeek(), new ConcurrentHashMap<>());
				dTSTUMap.compute(adt.getDayOfWeek(), (d, tstum) -> {
					tstum.putIfAbsent(adt.getBroadcastTime(), new ConcurrentHashMap<>());
					tstum.compute(adt.getBroadcastTime(), (t, stum) -> {
						stum.putIfAbsent(step, new ConcurrentHashMap<>());
						stum.compute(step, (ti, tum) -> {
							tum.putIfAbsent(title, Collections.synchronizedSet(new HashSet<>()));
							tum.compute(title, (str, u) -> {
								u.add(ju);
								if (!hasFuture(d, adt.getBroadcastTime(), step, title)) {
									scheduleStepUpdate(d, adt.getBroadcastTime(), step, title, adt);
								}
								return u;
							});
							return tum;
						});
						return stum;
					});
					return tstum;
				});
			});
		});
	}

	private void stepRem(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onRemove(step -> {
			ju.getSubscribedAnimes().forEach(title -> {
				AnimeDayTime adt = AnimeDB.getADT(ju.getTimeZone(), title);
				dTSTUMap.compute(adt.getDayOfWeek(), (d, tstum) -> {
					tstum.compute(adt.getBroadcastTime(), (time, stum) -> {
						stum.compute(step, (st, tum) -> {
							tum.compute(title, (t, u) -> {
								u.remove(ju);
								if (u.isEmpty()) {
									cancelFuture(d, time, step, title);
									log.debug("User is empty for {}->{}->{}->{}", d, time, st, t);
									return null;
								}
								return u;
							});
							if (tum.isEmpty()) {
								log.debug("Title->User map is empty for {}->{}->{}", d, time, st);
								return null;
							}
							return tum;
						});
						if (stum.isEmpty()) {
							log.debug("Step->Title->User map is empty for {}->{}", d, time);
							return null;
						}
						return stum;
					});
					if (tstum.isEmpty()) {
						log.debug("Time->Step->Title->User map is empty for {}", d);
						return null;
					}
					return tstum;
				});
			});
		});
	}

	private void notifDaily(JikaiUser ju) {
		ju.isUpdatedDailyProperty().onChange((ov, nv) -> {
			if (nv) {
				updatedDaily.compute(ju.getTimeZone(), (z, s) -> {
					if (s == null) {
						s = Collections.synchronizedSet(new HashSet<>());
					}
					s.add(ju);
					return s;
				});
			} else {
				updatedDaily.computeIfPresent(ju.getTimeZone(), (z, s) -> {
					s.remove(ju);
					return (s.isEmpty() ? null : s);
				});
			}
		});
	}

	private void cancelFuture(DayOfWeek d, LocalTime time, long step, String title) {
		if (hasFuture(d, time, step, title)) {
			futureMap.compute(d, (day, tsts) -> {
				tsts.compute(time, (t, sts) -> {
					sts.compute(step, (st, ts) -> {
						ts.remove(title).cancel(false);
						if (ts.isEmpty()) {
							log.debug("Title->Future map is empty for {}->{}->{}", d, time, step);
							return null;
						}
						return ts;
					});
					if (sts.isEmpty()) {
						log.debug("Step->Title->Future map is empty for {}->{}", d, time);
						return null;
					}
					return sts;
				});
				if (tsts.isEmpty()) {
					log.debug("Time->Step->Title->Future map is emptry for {}", d);
					return null;
				}
				return tsts.isEmpty() ? null : tsts;
			});
			log.debug("Canceled future with step {} for {}", step, title);
		}
	}

	//TODO handle negative initial delays!
	private void scheduleStepUpdate(DayOfWeek d, LocalTime time, long step, String title, AnimeDayTime adt) {
		ZonedDateTime now = ZonedDateTime.now(adt.getZonedDateTime().getZone());
		//until +1 - step cause it's end exclusive
		long tmp = step;
		long until = now.until(adt.getZonedDateTime(), ChronoUnit.MINUTES) - --step;
		if (until < 0) {
			until = now.until(adt.getZonedDateTime().plusDays(7), ChronoUnit.MINUTES) - step;
		}
		ScheduledFuture<?> cf = Core.EXEC.scheduleAtFixedRate(() -> updateUser(adt, tmp), until, TimeUnit.DAYS.toMinutes(7), TimeUnit.MINUTES);
		putInFutureMap(d, time, tmp, title, cf);
		log.debug("Scheduled future for step {} of {} first running in {} mins", tmp, title, until);
	}

	private void putInFutureMap(DayOfWeek d, LocalTime time, long step, String title, ScheduledFuture<?> sf) {
		futureMap.putIfAbsent(d, new ConcurrentHashMap<>());
		futureMap.compute(d, (day, tstsm) -> {
			tstsm.putIfAbsent(time, new ConcurrentHashMap<>());
			tstsm.compute(time, (t, stsm) -> {
				stsm.putIfAbsent(step, new ConcurrentHashMap<>());
				stsm.compute(step, (l, tsm) -> {
					tsm.put(title, sf);
					return tsm;
				});
				return stsm;
			});
			return tstsm;
		});
	}

	private void updateUser(AnimeDayTime adt, long step) {
		MessageEmbed me = step == 0 ? makeNotifyRelease(adt) : makeNotifyEmbed(adt, step);
		dTSTUMap.get(adt.getDayOfWeek()).get(adt.getBroadcastTime()).get(step).get(adt.getAnime().title).forEach(ju -> ju.sendPM(me));
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

	private void update() {
		Set<JikaiUser> jus = Jikai.getUserManager().users();
		Set<String> titles = AnimeDB.getSeasonalAnimes().stream().map(adt -> adt.getAnime().title).collect(Collectors.toSet());
		jus.forEach(ju -> {
			Set<String> old = new TreeSet<>();
			StringBuilder bob = new StringBuilder();
			bob.append("These anime have finished airing and you have been automatically unsubscribed from them:");
			ju.getSubscribedAnimes().forEach(title -> {
				if (!titles.contains(title)) {
					old.add(title);
					bob.append("\\n**" + title + "**");
				}
			});
			if (!old.isEmpty()) {
				ju.unsubscribeAnime(old);
				ju.sendPM(bob.toString());
			}
		});
	}

	private static MessageEmbed makeNotifyEmbed(AnimeDayTime adt, long step) {
		EmbedBuilder eb = new EmbedBuilder();
		Anime a = adt.getAnime();
		eb.setThumbnail(a.imageURL).setTitle("**" + a.title + "**", a.url).setDescription(String.format("**%s\nAirs in %s**", adt.getReleaseDateTimeFormatted(), minsToTime(step))).setTimestamp(ZonedDateTime.now(adt.getZonedDateTime().getZone()));
		return eb.build();
	}

	private static MessageEmbed makeNotifyRelease(AnimeDayTime adt) {
		EmbedBuilder eb = new EmbedBuilder();
		Anime a = adt.getAnime();
		eb.setThumbnail(a.imageURL).setTitle("**" + a.title + "**", a.url).setDescription("just released!\nIt will probably be up on your favourite streaming sites in a few minutes.").setTimestamp(ZonedDateTime.now(adt.getZonedDateTime().getZone()));
		return eb.build();
	}

	private static String minsToTime(long minutes) {
		long days = TimeUnit.MINUTES.toDays(minutes);
		minutes -= TimeUnit.DAYS.toMinutes(days);
		long hours = TimeUnit.MINUTES.toHours(minutes);
		minutes -= TimeUnit.HOURS.toMinutes(hours);
		BiFunction<String, Long, String> f = (s, l) -> (l == 0 ? "" : l + " " + s + (l == 1 ? "" : "s"));
		StringBuilder bob = new StringBuilder();
		if (days > 0) {
			bob.append(f.apply("day", days));
			if (hours > 0 && days > 0) {
				bob.append(", ");
			}
		}
		bob.append(f.apply("hour", hours));
		if (hours > 0 && minutes > 0) {
			bob.append(", ");
		}
		if (days == 0) {
			bob.append(f.apply("minute", minutes));
		}
		return bob.toString();
	}

}
