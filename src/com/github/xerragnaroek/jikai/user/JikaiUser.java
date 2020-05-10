
package com.github.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.Property;
import com.github.xerragnaroek.jikai.util.prop.SetProperty;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

public class JikaiUser {

	private long id;
	private TitleLanguage tt;
	private SetProperty<Integer> subscribedAnime = new SetProperty<>();
	private Property<Boolean> sendDailyUpdate = new Property<>();
	private SetProperty<Integer> notifBeforeRelease = new SetProperty<>();
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
		loading = false;
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
		return notifBeforeRelease.contains(0);
	}

	public void setNotifyToRelease(boolean notify) {
		if (notify) {
			addPreReleaseNotificaionStep(0);
		} else {
			notifBeforeRelease.remove(0);
		}
	}

	public boolean isUpdatedDaily() {
		return sendDailyUpdate.get();
	}

	public void setUpdateDaily(boolean upd) {
		sendDailyUpdate.set(upd);
	}

	public void subscribeAnime(int id) {
		Anime a = AnimeDB.getAnime(id);
		if (subscribedAnime.add(id) && !loading) {
			sendPM("You have subscribed to '**" + a.getTitle(tt) + "**'");
		}
	}

	public Set<Integer> getSubscribedAnime() {
		return subscribedAnime.get();
	}

	public void unsubscribeAnime(int id) {
		Anime a = AnimeDB.getAnime(id);
		if (subscribedAnime.remove(id) && !loading) {
			sendPM("You have unsubscribed from '**" + a.getTitle(tt) + "**'");
		}
	}

	public void unsubscribeAnime(Collection<Integer> ids) {
		subscribedAnime.removeAll(ids);
	}

	public Set<Integer> getPreReleaseNotifcationSteps() {
		return notifBeforeRelease.get();
	}

	public SetProperty<Integer> preReleaseNotificationStepsProperty() {
		return notifBeforeRelease;
	}

	public SetProperty<Integer> subscribedAnimesProperty() {
		return subscribedAnime;
	}

	public void addPreReleaseNotificaionStep(int seconds) {
		log.debug("User added new release notification step: {} minutes", seconds / 60);
		notifBeforeRelease.add(seconds);
		if (!loading) {
			sendPM("You will be notified at " + BotUtils.formatSeconds(seconds) + " to a release!");
		}
	}

	public void removePreReleasNotificationStep(int seconds) {
		log.debug("User removed release notification step: {} minutes", seconds / 60);
		notifBeforeRelease.remove(seconds);
		if (!loading) {
			sendPM("You won't be notified at" + BotUtils.formatSeconds(seconds) + " to a release!");
		}
	}

	public Property<Boolean> isUpdatedDailyProperty() {
		return sendDailyUpdate;
	}

	public CompletableFuture<PrivateChannel> sendPM(String message) {
		return BotUtils.sendPM(getUser(), message);
	}

	public CompletableFuture<PrivateChannel> sendPMFormat(String message, Object... args) {
		return BotUtils.sendPM(getUser(), String.format(message, args));
	}

	public CompletableFuture<PrivateChannel> sendPM(Message message) {
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

	public boolean isSubscribedTo(Anime a) {
		return subscribedAnime.contains(a.getId());
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
						right = l <= 10080;
				}
				if (right) {
					if (add) {
						addPreReleaseNotificaionStep((int) l * 60);
					} else {
						removePreReleasNotificationStep((int) l * 60);
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
		return String.format("\"%d\",\"%d\",\"%s\",\"%d\",\"%s\",\"%s\"", id, tt.ordinal(), zone.get().getId(), sendDailyUpdate.get() ? 1 : 0, notifBeforeRelease.toString(), subscribedAnime.get());
	}
}
