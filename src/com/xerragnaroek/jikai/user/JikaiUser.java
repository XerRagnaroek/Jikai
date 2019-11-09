package com.xerragnaroek.jikai.user;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
	private TitleType tt;
	private SetProperty<String> subscribedAnime = new SetProperty<>();
	private Property<Boolean> sendDailyUpdate = new Property<>();
	private SetProperty<Long> notifBeforeRelease = new SetProperty<>();
	private Property<ZoneId> zone = new Property<>(ZoneId.of("Europe/Berlin"));
	private boolean setupComplete = false;
	boolean loading = true;

	public JikaiUser(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public boolean isSetupCompleted() {
		return setupComplete;
	}

	public void setSetupCompleted(boolean comp) {
		setupComplete = comp;
	}

	public User getUser() {
		return Core.JDA.getUserById(id);
	}

	public TitleType getTitleType() {
		return tt;
	}

	public void setTitleType(TitleType tt) {
		this.tt = tt;
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
