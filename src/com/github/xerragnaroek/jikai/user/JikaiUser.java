
package com.github.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.util.Arrays;
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
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.BooleanProperty;
import com.github.xerragnaroek.jikai.util.prop.Property;
import com.github.xerragnaroek.jikai.util.prop.SetProperty;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class JikaiUser {

	private long id;
	private TitleLanguage tt;
	private SetProperty<Integer> subscribedAnime = new SetProperty<>();
	private BooleanProperty sendDailyUpdate = new BooleanProperty();
	private BooleanProperty sendWeeklySchedule = new BooleanProperty();
	private SetProperty<Integer> notifBeforeRelease = new SetProperty<>();
	private Property<ZoneId> zone = new Property<>();
	private Property<JikaiLocale> locale = new Property<>(JikaiLocaleManager.getInstance().getLocale("en"));
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
		sendWeeklySchedule.onChange((o, n) -> log.debug("Send weekly schedule: " + n));
		locale.onChange((o, n) -> log.debug("Locale: " + n));
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

	public boolean isSentWeeklySchedule() {
		return sendWeeklySchedule.get();
	}

	public void setSentWeeklySchedule(boolean upd) {
		sendWeeklySchedule.set(upd);
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
			sendPM(locale.get().getStringFormatted("ju_step_add", Arrays.asList("%time%"), BotUtils.formatSeconds(seconds, locale.get())));
		}
	}

	public void removePreReleasNotificationStep(int seconds) {
		log.debug("User removed release notification step: {} minutes", seconds / 60);
		notifBeforeRelease.remove(seconds);
		if (!loading) {
			sendPM(locale.get().getStringFormatted("ju_step_add", Arrays.asList("%time%"), BotUtils.formatSeconds(seconds, locale.get())));
		}
	}

	public BooleanProperty isUpdatedDailyProperty() {
		return sendDailyUpdate;
	}

	public BooleanProperty isSentWeeklyScheduleProperty() {
		return sendWeeklySchedule;
	}

	public CompletableFuture<Boolean> sendPM(String message) {
		return BotUtils.sendPM(getUser(), message);
	}

	public CompletableFuture<Boolean> sendPMFormat(String message, Object... args) {
		return BotUtils.sendPM(getUser(), String.format(message, args));
	}

	public CompletableFuture<Boolean> sendPM(Message message) {
		return BotUtils.sendPM(getUser(), message);
	}

	public CompletableFuture<Boolean> sendPM(MessageEmbed message) {
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

	public JikaiLocale getLocale() {
		return locale.get();
	}

	public void setLocale(JikaiLocale loc) {
		locale.set(loc);
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
		notifBeforeRelease.clear();
		sendDailyUpdate.set(false);
		sendWeeklySchedule.set(false);
		zone.set(null);
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
		// id,titletype,zone,localeIdentifier,sendDailyUpdate,notifBeforeRelease,anime
		return String.format("\"%d\",\"%d\",\"%s\",\"%s\",\"%d\",\"%d\",\"%s\",\"%s\"", id, tt.ordinal(), zone.get().getId(), locale.get().getIdentifier(), sendDailyUpdate.get() ? 1 : 0, sendWeeklySchedule.get() ? 1 : 0, notifBeforeRelease.toString(), subscribedAnime.get());
	}
}
