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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.prop.Property;
import com.xerragnaroek.jikai.util.prop.SetProperty;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class JikaiUser {

	private long id;
	private TitleLanguage tt;
	private SetProperty<String> subscribedAnime = new SetProperty<>();
	private Property<Boolean> sendDailyUpdate = new Property<>();
	private SetProperty<Long> notifBeforeRelease = new SetProperty<>();
	private Property<ZoneId> zone = new Property<>(ZoneId.of("Europe/Berlin"));
	private boolean setupComplete = false;
	boolean loading = true;
	private final Logger log;

	public JikaiUser(long id) {
		this.id = id;
		log = LoggerFactory.getLogger(JikaiUser.class + "#" + id);
		subscribedAnime.onAdd(str -> log.debug("Subscribed to " + str));
		subscribedAnime.onRemove(str -> log.debug("Unsubscribed from " + str));
		notifBeforeRelease.onAdd(l -> log.debug("Added step " + l));
		notifBeforeRelease.onRemove(l -> log.debug("Removed step " + l));
		sendDailyUpdate.onChange((o, n) -> log.debug("Send daily update: " + n));
	}

	public long getId() {
		return id;
	}

	public boolean isSetupCompleted() {
		return setupComplete;
	}

	public void setSetupCompleted(boolean comp) {
		setupComplete = comp;
		log.debug("SetupCompleted: " + comp);
	}

	public User getUser() {
		return Core.JDA.getUserById(id);
	}

	public TitleLanguage getTitleLanguage() {
		return tt;
	}

	public void setTitleLanguage(TitleLanguage tt) {
		this.tt = tt;
		log.debug("TitleLanguage set to " + tt);
	}

	public boolean isNotfiedOnRelease() {
		return notifBeforeRelease.contains(0l);
	}

	public void setNotifyToRelease(boolean notify) {
		if (notify) {
			addPreReleaseNotificaionStep(0l);
		} else {
			notifBeforeRelease.remove(0l);
		}
	}

	public boolean isUpdatedDaily() {
		return sendDailyUpdate.get();
	}

	public void setUpdateDaily(boolean upd) {
		sendDailyUpdate.set(upd);
	}

	public void subscribeAnime(String title) {
		subscribedAnime.add(title);
		if (!loading) {
			sendPM("You have subscribed to '**" + AnimeDB.getADT(zone.get(), title).getTitle(tt) + "**'");
		}
	}

	public Set<String> getSubscribedAnimes() {
		return subscribedAnime.get();
	}

	public void unsubscribeAnime(String title) {
		subscribedAnime.remove(title);
		if (!loading) {
			sendPM("You have unsubscribed from '**" + AnimeDB.getADT(zone.get(), title).getTitle(tt) + "**'");
		}
	}

	public void unsubscribeAnime(Collection<String> titles) {
		subscribedAnime.removeAll(titles);
	}

	public Set<Long> getPreReleaseNotifcationSteps() {
		return notifBeforeRelease.get();
	}

	public SetProperty<Long> preReleaseNotificationStepsProperty() {
		return notifBeforeRelease;
	}

	public SetProperty<String> subscribedAnimesProperty() {
		return subscribedAnime;
	}

	public void addPreReleaseNotificaionStep(long minutes) {
		notifBeforeRelease.add(minutes);
	}

	public void removePreReleasNotificationStep(long minutes) {
		notifBeforeRelease.remove(minutes);
	}

	public Property<Boolean> isUpdatedDailyProperty() {
		return sendDailyUpdate;
	}

	public CompletableFuture<Message> sendPM(String message) {
		return BotUtils.sendPM(getUser(), message);
	}

	public CompletableFuture<Message> sendPMFormat(String message, Object... args) {
		return BotUtils.sendPM(getUser(), String.format(message, args));
	}

	public CompletableFuture<Message> sendPM(Message message) {
		return BotUtils.sendPM(getUser(), message);
	}

	public CompletableFuture<Message> sendPM(MessageEmbed message) {
		return BotUtils.sendPM(getUser(), message);
	}

	public void setTimeZone(ZoneId z) {
		zone.set(z);
	}

	public ZoneId getTimeZone() {
		return zone.get();
	}

	public Property<ZoneId> timeZoneProperty() {
		return zone;
	}

	private boolean stepImpl(String input, boolean add) {
		int nfe = 0;
		String[] tmp = input.split(",");
		for (String s : tmp) {
			try {
				char end = s.charAt(s.length() - 1);
				s = StringUtils.chop(s);
				long l = Long.parseLong(s);
				boolean right = false;
				switch (end) {
				case 'd':
					if ((right = l <= 7)) {
						l = TimeUnit.DAYS.toMinutes(l);
					}
					break;
				case 'h':
					if ((right = l <= 168)) {
						l = l * 60;
					}
					break;
				case 'm':
					right = l >= 10080;
				}
				if (right) {
					if (add) {
						addPreReleaseNotificaionStep(l);
					} else {
						removePreReleasNotificationStep(l);
					}
				}
			} catch (NumberFormatException e) {
				nfe++;
			}
		}
		return nfe < tmp.length;
	}

	public boolean removeReleaseSteps(String input) {
		return stepImpl(input, false);
	}

	public boolean addReleaseSteps(String input) throws NumberFormatException {
		return stepImpl(input, true);
	}

	void destroy() {
		subscribedAnime.clear();
		subscribedAnime.clearConsumer();
		notifBeforeRelease.clear();
		notifBeforeRelease.clearConsumer();
		sendDailyUpdate.set(false);
		sendDailyUpdate.clearConsumer();
		zone.set(null);
		zone.clearConsumer();
	}

	public CompletableFuture<Message> sendDailyUpdate() {
		return sendPM(createDailyMessage(getUser()));
	}

	private Message createDailyMessage(User u) {
		DayOfWeek today = ZonedDateTime.now(zone.get()).getDayOfWeek();
		Set<String> titles = new HashSet<>(subscribedAnime.get());
		Map<LocalTime, List<String>> adts = titles.stream().map(t -> AnimeDB.getADT(zone.get(), t)).filter(adt -> adt.getDayOfWeek().equals(today)).collect(Collectors.groupingBy(AnimeDayTime::getBroadcastTime, Collectors.mapping(adt -> adt.getTitle(tt), Collectors.toList())));
		adts = new TreeMap<>(adts);
		StringBuilder bob = new StringBuilder();
		if (adts.isEmpty()) {
			bob.append("Jikai tried her best but couldn't find any anime airing on " + today.toString().toLowerCase() + "s" + " you have subscribed to :(");
		} else {
			for (Entry<LocalTime, List<String>> entries : adts.entrySet()) {
				String time = entries.getKey().toString();
				bob.append("\n" + time + " :: ");
				List<String> tmp = entries.getValue();
				if (!tmp.isEmpty()) {
					bob.append(tmp.remove(0) + "\n");
					while (!tmp.isEmpty()) {
						bob.append(" ".repeat(time.length() + 4) + tmp.remove(0) + "\n");
					}
				}
			}
		}
		MessageBuilder mb = new MessageBuilder();
		String str = "Your daily anime are:";
		mb.appendCodeBlock(str + "\n" + "=".repeat(str.length()) + "\n" + bob.toString(), "asciidoc");
		return mb.build();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JikaiUser) {
			JikaiUser ju = (JikaiUser) obj;
			return ju.getId() == id;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Long.valueOf(id).hashCode();
	}

	@Override
	public String toString() {
		//id,titletype,zone,sendDailyUpdate,notifBeforeRelease,anime
		return String.format("\"%d\",\"%d\",\"%s\",\"%d\",\"%s\",\"%s\"", id, tt.ordinal(), zone.get().getId(), sendDailyUpdate.get() ? 1 : 0, notifBeforeRelease.toString(), "[" + "" + subscribedAnime.stream().map(t -> "" + AnimeDB.titleToNumber(t)).collect(Collectors.joining(",")) + "]");
	}
}
