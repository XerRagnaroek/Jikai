
package com.github.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.BooleanProperty;
import com.github.xerragnaroek.jikai.util.prop.IntegerProperty;
import com.github.xerragnaroek.jikai.util.prop.Property;
import com.github.xerragnaroek.jikai.util.prop.SetProperty;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class JikaiUser {

	private long id;
	private IntegerProperty aniId = new IntegerProperty(0);
	private TitleLanguage tt;
	private SubscriptionSet subscribedAnime = new SubscriptionSet();
	private BooleanProperty sendDailyUpdate = new BooleanProperty();
	private BooleanProperty sendWeeklySchedule = new BooleanProperty();
	private SetProperty<Integer> notifBeforeRelease = new SetProperty<>();
	private Property<ZoneId> zone = new Property<>();
	private Property<String> locale = new Property<>("en");
	private boolean setupComplete = false;
	boolean loading = true;
	private final Logger log;

	public JikaiUser(long id) {
		this.id = id;
		log = LoggerFactory.getLogger(JikaiUser.class);
		subscribedAnime.onAdd((aid, str) -> log("subscribed to {}, cause: {}", aid, str));
		subscribedAnime.onRemove((aid, str) -> log("unsubscribed from {}, cause: {}", aid, str));
		notifBeforeRelease.onAdd(l -> log("added step {}", l));
		notifBeforeRelease.onRemove(l -> log("removed step {}", l));
		sendDailyUpdate.onChange((o, n) -> log("change send daily update {}", n));
		sendWeeklySchedule.onChange((o, n) -> log("change send weekly schedule {}", n));
		locale.onChange((o, n) -> log("change locale: {}", n));
		aniId.onChange((o, n) -> log("change aniId: {}", n));
	}

	private void log(String msg, Object... vals) {
		MDC.put("id", String.valueOf(id));
		log.debug(msg, vals);
		MDC.remove("id");
	}

	public long getId() {
		return id;
	}

	public int getAniId() {
		return aniId.get();
	}

	public boolean hasAniId() {
		return aniId.get() > 0;
	}

	public void setAniId(int aniId) {
		this.aniId.set(aniId);
	}

	public boolean isSetupCompleted() {
		return setupComplete;
	}

	public void setSetupCompleted(boolean comp) {
		setupComplete = comp;
		loading = false;
		log("Setup completed: {}", comp);
	}

	public User getUser() {
		return Core.JDA.retrieveUserById(id).complete();
	}

	public TitleLanguage getTitleLanguage() {
		return tt;
	}

	public void setTitleLanguage(TitleLanguage tt) {
		this.tt = tt;
		log("TitleLanguage set to {}", tt);
	}

	public boolean isNotfiedOnRelease() {
		return notifBeforeRelease.contains(0);
	}

	public void setNotifyToRelease(boolean notify) {
		if (notify) {
			addPreReleaseNotificaionStep(0);
		} else {
			removePreReleaseNotificationStep(0);
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

	public void subscribeAnime(int id, String cause) {
		subscribedAnime.add(id, cause);
	}

	public SubscriptionSet getSubscribedAnime() {
		return subscribedAnime;
	}

	public void unsubscribeAnime(int id, String cause) {
		subscribedAnime.remove(id, cause);
	}

	public void unsubscribeAnime(Anime a, String cause) {
		subscribedAnime.remove(a.getId(), cause);
	}

	public Set<Integer> getPreReleaseNotifcationSteps() {
		return notifBeforeRelease.get();
	}

	public SetProperty<Integer> preReleaseNotificationStepsProperty() {
		return notifBeforeRelease;
	}

	public void addPreReleaseNotificaionStep(int seconds) {
		if (notifBeforeRelease.add(seconds) && !loading) {
			JikaiLocale loc = getLocale();
			String msg = seconds == 0 ? loc.getString("ju_notify_release_true") : loc.getStringFormatted("ju_step_add", Arrays.asList("time"), BotUtils.formatSeconds(seconds, loc));
			sendPM(msg);
		}
	}

	public void removePreReleaseNotificationStep(int seconds) {
		if (notifBeforeRelease.remove(seconds) && !loading) {
			JikaiLocale loc = getLocale();
			String msg = seconds == 0 ? loc.getString("ju_notify_release_false") : loc.getStringFormatted("ju_step_rem", Arrays.asList("time"), BotUtils.formatSeconds(seconds, loc));
			sendPM(msg);
		}
	}

	public BooleanProperty isUpdatedDailyProperty() {
		return sendDailyUpdate;
	}

	public BooleanProperty isSentWeeklyScheduleProperty() {
		return sendWeeklySchedule;
	}

	public CompletableFuture<Boolean> sendPM(String message) {
		return BotUtils.sendPMChecked(getUser(), message);
	}

	public CompletableFuture<Boolean> sendPMFormat(String message, Object... args) {
		return BotUtils.sendPMChecked(getUser(), String.format(message, args));
	}

	public CompletableFuture<Boolean> sendPM(Message message) {
		return BotUtils.sendPMChecked(getUser(), message);
	}

	public CompletableFuture<Boolean> sendPM(MessageEmbed message) {
		return BotUtils.sendPMChecked(getUser(), message);
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

	public boolean isSubscribedTo(int animeId) {
		return subscribedAnime.contains(animeId);
	}

	public JikaiLocale getLocale() {
		return JikaiLocaleManager.getInstance().getLocale(locale.get());
	}

	public void setLocale(JikaiLocale loc) {
		locale.set(loc.getIdentifier());
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
						removePreReleaseNotificationStep((int) l * 60);
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

	public String getConfigFormatted() {
		JikaiLocale loc = getLocale();
		String yes = loc.getString("u_yes");
		String no = loc.getString("u_no");
		String zone = this.zone.get().getId();
		String daily = sendDailyUpdate.get() ? yes : no;
		String release = isNotfiedOnRelease() ? yes : no;
		String schedule = sendWeeklySchedule.get() ? yes : no;
		String title = tt.toString();
		String steps = getPreReleaseNotifcationSteps().stream().map(i -> i / 60).sorted().map(String::valueOf).collect(Collectors.joining(", "));
		return loc.getStringFormatted("ju_config", Arrays.asList("lang", "zone", "daily", "release", "schedule", "title", "steps", "aniId"), loc.getString("u_lang_name"), zone, daily, release, schedule, title, steps, aniId.get());
	}

	public IntegerProperty aniIdProperty() {
		return aniId;
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
		return String.format("\"%d\",\"%d\",\"%d\",\"%s\",\"%s\",\"%d\",\"%d\",\"%s\",\"%s\"", id, aniId.get(), tt.ordinal(), zone.get().getId(), locale.get(), sendDailyUpdate.get() ? 1 : 0, sendWeeklySchedule.get() ? 1 : 0, notifBeforeRelease.toString(), subscribedAnime);
	}
}
