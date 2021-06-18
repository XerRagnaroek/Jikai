
package com.github.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.prop.BooleanProperty;
import com.github.xerragnaroek.jikai.util.prop.IntegerProperty;
import com.github.xerragnaroek.jikai.util.prop.MapProperty;
import com.github.xerragnaroek.jikai.util.prop.Property;
import com.github.xerragnaroek.jikai.util.prop.SetProperty;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({ "id" })
@JsonInclude(Include.NON_EMPTY)
public class JikaiUser {

	private long id;
	private IntegerProperty aniId = new IntegerProperty(0);
	private Property<TitleLanguage> titleLanguage = new Property<>();
	private SubscriptionSet subscribedAnime = new SubscriptionSet();
	private BooleanProperty sendDailyUpdate = new BooleanProperty();
	private BooleanProperty sendWeeklySchedule = new BooleanProperty();
	private BooleanProperty sendNextEpMessage = new BooleanProperty();
	private SetProperty<Integer> notifBeforeRelease = new SetProperty<>();
	// set of users that are linked to this user
	private SetProperty<Long> linkedUsers = new SetProperty<>();
	private MapProperty<Integer, String> customTitles = new MapProperty<>();
	private SetProperty<Integer> hiddenAnime = new SetProperty<>();

	// set of users this user is linked to
	@JsonIgnore
	private Set<Long> linkedToUsers = new TreeSet<Long>();
	@JsonIgnore
	private Property<ZoneId> zone = new Property<>();
	private Property<String> locale = new Property<>("en");
	@JsonIgnore
	private boolean setupComplete = false;
	@JsonIgnore
	boolean loading = true;
	@JsonIgnore
	private Logger log;

	public JikaiUser(long id) {
		this.id = id;
		log = LoggerFactory.getLogger(JikaiUser.class + "#" + id);
	}

	@SuppressWarnings("unused")
	private JikaiUser() {}

	void init() {
		subscribedAnime.onAdd((sa) -> log("subscribed to {}, cause: {}, linked: {}, silent: {}", sa.id(), sa.cause(), sa.linked(), sa.silent()));
		subscribedAnime.onRemove((sr) -> log("unsubscribed from {}, cause: {}, silent: {}", sr.id(), sr.cause(), sr.silent()));
		notifBeforeRelease.onAdd(l -> log("added step {}", l));
		notifBeforeRelease.onRemove(l -> log("removed step {}", l));
		linkedUsers.onAdd(uid -> log("linked user {}", uid));
		sendDailyUpdate.onChange((o, n) -> log("change send daily update {}", n));
		sendWeeklySchedule.onChange((o, n) -> log("change send weekly schedule {}", n));
		sendNextEpMessage.onChange((o, n) -> log("change send next ep message {}", n));
		locale.onChange((o, n) -> log("change locale: {}", n));
		aniId.onChange((o, n) -> log("change aniId: {}", n));
		titleLanguage.onChange((o, n) -> log("change titleLang: {}", n));
		hiddenAnime.onAdd(id -> log("added hidden anime: {}", id));
		hiddenAnime.onRemove(id -> log("removed hidden anime: {}", id));
		customTitles.onPut((id, s) -> log("added custom title: {} = {}", id, s));
		customTitles.onRemove((id, s) -> log("removed custom title: {} = {}", id, s));
	}

	@JsonIgnore
	private void log(String msg, Object... vals) {
		log.debug(msg, vals);
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

	@JsonIgnore
	public void setSetupCompleted(boolean comp) {
		setupComplete = comp;
		loading = false;
		log("Setup completed: {}", comp);
	}

	public User getUser() {
		try {
			return Core.JDA.retrieveUserById(id).submit().get();
		} catch (InterruptedException | ExecutionException e) {
			MDC.put("id", String.valueOf(id));
			Core.ERROR_LOG.error("Couldn't retrieve User");
			MDC.remove("id");
			return null;
		}
	}

	public TitleLanguage getTitleLanguage() {
		return titleLanguage.get();
	}

	public void setTitleLanguage(TitleLanguage tt) {
		this.titleLanguage.set(tt);
	}

	public Property<TitleLanguage> titleLanguageProperty() {
		return titleLanguage;
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

	public void setSendWeeklySchedule(boolean upd) {
		sendWeeklySchedule.set(upd);
	}

	public void setSendNextEpMessage(boolean send) {
		sendNextEpMessage.set(send);
	}

	public boolean isSentNextEpMessage() {
		return sendNextEpMessage.get();
	}

	public BooleanProperty isSentNextEpMessageProperty() {
		return sendNextEpMessage;
	}

	public boolean subscribeAnime(int id, String cause) {
		return subscribeAnime(id, cause, false, false);
	}

	public boolean subscribeAnime(int id, String cause, boolean linked, boolean silent) {
		boolean subbed = subscribedAnime.add(id, cause, linked, silent);
		for (long uid : linkedUsers) {
			JikaiUser ju = JikaiUserManager.getInstance().getUser(uid);
			if (ju == null) {
				JikaiUserManager.getInstance().removeUser(uid);
			} else {
				if (subbed && !Core.INITIAL_LOAD.get()) {
					ju.subscribeLinked(id, ju.getLocale().getStringFormatted("ju_sub_add_cause_linked", Arrays.asList("name"), getUser().getName()));
				}
			}
		}
		return subbed;
	}

	private void subscribeLinked(int id, String cause) {
		subscribedAnime.add(id, cause, true);
	}

	public SubscriptionSet getSubscribedAnime() {
		return subscribedAnime;
	}

	public boolean unsubscribeAnime(int id, String cause) {
		return subscribedAnime.remove(id, cause);
	}

	public boolean unsubscribeAnime(int id, String cause, boolean silent) {
		return subscribedAnime.remove(id, cause, silent);
	}

	public boolean unsubscribeAnime(Anime a, String cause) {
		unhideAnimeFromLists(a.getId());
		removeCustomTitle(a.getId());
		return subscribedAnime.remove(a.getId(), cause);
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

	public boolean addReleaseStepNoMsg(int seconds) {
		return notifBeforeRelease.add(seconds);
	}

	public boolean remReleaseStepNoMsg(int seconds) {
		return notifBeforeRelease.remove(seconds);
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

	/**
	 * User links are stored in two ways:
	 * 1. Set containing the ids of all users that are linked to a user. (user -> this)
	 * (un/linkUser)
	 * 2. Set containing the ids of all users that a user is linked to. (this -> user)
	 * (linkTo/unlinkFromUser)
	 * First is needed to speed up syncing of subs and such. If I'd only employ #2 I'd have to iterate
	 * through every user to filter out who is linked to this one.
	 * Second is mostly just there so a user can see who they're linked to.
	 * #1 is also how the links are saved, #2 gets populated at runtime.
	 */

	/**
	 * Link a user to this user. user -> this
	 */
	public void linkUser(JikaiUser ju) {
		linkedUsers.add(ju.getId());
		ju.linkToUser(id);
	}

	/**
	 * Link a user to this user. user -> this
	 * Preferred to use {@link #linkUser(JikaiUser)}
	 */
	public void linkUser(long id) {
		linkedUsers.add(id);
	}

	/**
	 * Basically subscribing to a user. Needed for internal tracking of what users this one is linked
	 * to.
	 */
	public boolean linkToUser(long id) {
		return linkedToUsers.add(id);
	}

	public boolean isLinkedToUser(long id) {
		return linkedToUsers.contains(id);
	}

	public boolean unlinkUser(JikaiUser ju) {
		ju.linkedToUsers.remove(id);
		return linkedUsers.remove(ju.getId());
	}

	public boolean unlinkUser(long id) {
		return linkedUsers.remove(id);
	}

	public boolean unlinkFromUser(long id) {
		return linkedToUsers.remove(id);
	}

	public Set<Long> getLinkedUsers() {
		return linkedUsers.get();
	}

	public Set<Long> getLinkedToUsers() {
		return linkedToUsers;
	}

	private boolean stepImpl(String input, boolean add) {
		/*
		 * int nfe = 0;
		 * String[] tmp = input.split(",");
		 * for (String s : tmp) {
		 * try {
		 * char end = s.charAt(s.length() - 1);
		 * s = StringUtils.chop(s);
		 * long l = Long.parseLong(s);
		 * boolean right = false;
		 * switch (end) {
		 * case 'd':
		 * if ((right = l <= 7)) {
		 * l = TimeUnit.DAYS.toMinutes(l);
		 * }
		 * break;
		 * case 'h':
		 * if ((right = l <= 168)) {
		 * l = l * 60;
		 * }
		 * break;
		 * case 'm':
		 * right = l <= 10080;
		 * }
		 * if (right) {
		 * if (add) {
		 * addPreReleaseNotificaionStep((int) l * 60);
		 * } else {
		 * removePreReleaseNotificationStep((int) l * 60);
		 * }
		 * }
		 * } catch (NumberFormatException e) {
		 * nfe++;
		 * }
		 * }
		 * return nfe < tmp.length;
		 */
		int mins = BotUtils.stepStringToMins(input);
		if (mins > TimeUnit.DAYS.toMinutes(7)) {
			return false;
		}
		if (add) {
			addPreReleaseNotificaionStep(mins * 60);
		} else {
			removePreReleaseNotificationStep(mins * 60);
		}
		return true;
	}

	public boolean removeReleaseSteps(String input) {
		return stepImpl(input, false);
	}

	public boolean addReleaseSteps(String input) throws IllegalArgumentException {
		return stepImpl(input, true);
	}

	public String addCustomTitle(int aniId, String title) {
		return customTitles.put(aniId, title);
	}

	public String removeCustomTitle(int aniId) {
		return customTitles.remove(aniId);
	}

	public String getCustomTitle(int aniId) {
		return customTitles.get(aniId);
	}

	public boolean hasCustomTitle(int aniId) {
		return customTitles.containsKey(aniId);
	}

	public MapProperty<Integer, String> customAnimeTitlesProperty() {
		return customTitles;
	}

	public boolean hideAnimeFromLists(int id) {
		return hiddenAnime.add(id);
	}

	public boolean unhideAnimeFromLists(int id) {
		return hiddenAnime.remove(id);
	}

	public boolean isHiddenAnime(int aniId) {
		return hiddenAnime.contains(aniId);
	}

	public SetProperty<Integer> hiddenAnimeProperty() {
		return hiddenAnime;
	}

	public String getConfigFormatted() {
		JikaiLocale loc = getLocale();
		String yes = loc.getString("u_yes");
		String no = loc.getString("u_no");
		String zone = this.zone.get().getId();
		String daily = sendDailyUpdate.get() ? yes : no;
		String nextEpMsg = isSentNextEpMessage() ? yes : no;
		String release = isNotfiedOnRelease() ? yes : no;
		String schedule = sendWeeklySchedule.get() ? yes : no;
		String title = titleLanguage.toString();
		String steps = getPreReleaseNotifcationSteps().stream().map(i -> i / 60).sorted().map(String::valueOf).collect(Collectors.joining(", "));
		String aniId = (aniId = this.aniId.toString()).equals("0") ? "/" : aniId;
		String users = linkedToUsers.stream().map(juId -> JikaiUserManager.getInstance().getUser(juId)).map(ju -> ju.getUser().getName()).sorted().collect(Collectors.joining(", "));
		users = users.isEmpty() ? "/" : users;
		return loc.getStringFormatted("ju_config", Arrays.asList("lang", "zone", "daily", "nextEpMsg", "release", "schedule", "title", "steps", "aniId", "users", "anime", "hiddenAnime", "customT"), loc.getString("u_lang_name"), zone, daily, nextEpMsg, release, schedule, title, steps, aniId, users, subscribedAnime.size(), hiddenAnime.size(), customTitles.size());
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
		linkedToUsers.stream().map(l -> JikaiUserManager.getInstance().getUser(l)).forEach(ju -> ju.unlinkUser(id));
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
		// id,titletype,zone,localeIdentifier,sendDailyUpdate,notifBeforeRelease,anime,linked user
		return String.format("\"%d\",\"%d\",\"%d\",\"%s\",\"%s\",\"%d\",\"%d\",\"%s\",\"%s\",\"%s\"", id, aniId.get(), titleLanguage.get().ordinal(), zone.get().getId(), locale.get(), sendDailyUpdate.get() ? 1 : 0, sendWeeklySchedule.get() ? 1 : 0, notifBeforeRelease.toString(), subscribedAnime, linkedUsers.toString());
	}

	@JsonGetter("zoneId")
	private String jsonZoneId() {
		return getTimeZone().getId();
	}

	/*
	 * @JsonSetter("id")
	 * private void setId(long id) {
	 * this.id = id;
	 * log = LoggerFactory.getLogger(JikaiUser.class + "#" + id);
	 * }
	 */

	@JsonSetter("zoneId")
	private void setZoneIdJson(String id) {
		setTimeZone(ZoneId.of(id));
	}

	public void copy(JikaiUser ju) {
		id = ju.id;
		setTimeZone(ju.getTimeZone());
		setAniId(ju.getAniId());
		setTitleLanguage(ju.getTitleLanguage());
		setLocale(ju.getLocale());
		setUpdateDaily(ju.isUpdatedDaily());
		setSendWeeklySchedule(ju.isSentWeeklySchedule());
		setSendNextEpMessage(ju.isSentNextEpMessage());
		ju.notifBeforeRelease.forEach(this::addPreReleaseNotificaionStep);
		linkedUsers.addAll(ju.linkedUsers);
		ju.subscribedAnime.stream().filter(AnimeDB::hasAnime).forEach(id -> subscribeAnime(id, "Copy"));
		customTitles.putAll(ju.customTitles);
		hiddenAnime.addAll(ju.hiddenAnime);
	}
}
