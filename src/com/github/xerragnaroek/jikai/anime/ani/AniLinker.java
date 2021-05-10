package com.github.xerragnaroek.jikai.anime.ani;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jasa.User;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 
 */

public class AniLinker {
	private final static Logger log = LoggerFactory.getLogger(AniLinker.class);
	private final static Map<Long, AniLinker> linkerMap = Collections.synchronizedMap(new HashMap<>());
	private UserChecker checker;
	private CompletableFuture<Boolean> future;

	private AniLinker() {}

	// TODO linkAniAccount should return an AniLinker that's cancellable to actually stop the process!!
	public static AniLinker linkAniAccount(JikaiUser ju, String nameOrId) {
		AniLinker linker = new AniLinker();
		if (!linkerMap.containsKey(ju.getId())) {
			JikaiLocale loc = ju.getLocale();
			JASA jasa = new JASA();
			List<User> user = new ArrayList<>();
			try {
				int id = Integer.parseInt(nameOrId);
				try {
					user.add(jasa.fetchUserWithId(id));
				} catch (IOException | AniException e) {
					BotUtils.logAndSendToDev(Core.ERROR_LOG, "Error ", e);
					ju.sendPM(loc.getString("ju_link_ani_error"));
				}
			} catch (NumberFormatException e) {
				nameOrId = nameOrId.endsWith("/") ? nameOrId.substring(0, nameOrId.length() - 1) : nameOrId;
				String name = nameOrId.startsWith("http") ? nameOrId.substring(nameOrId.lastIndexOf('/') + 1) : nameOrId;
				try {
					user.addAll(jasa.fetchUsersWithName(name));
				} catch (IOException | AniException ex) {
					BotUtils.logAndSendToDev(Core.ERROR_LOG, "Error ", ex);
					ju.sendPM(loc.getString("ju_link_ani_error"));
				}
			}
			log.debug("Found {} user matching input", user.size());
			log.debug("Starting UserChecker");
			linkerMap.put(ju.getId(), linker);
			if (user.size() > 0) {
				linker.future = CompletableFuture.runAsync(() -> linker.startChecker(user, ju), Core.EXEC).thenApply(v -> true);
			} else {
				ju.sendPM(loc.getString("ju_link_ani_no_user"));
				linker.future = CompletableFuture.completedFuture(false);
			}
		} else {
			linker.future = CompletableFuture.completedFuture(false);
		}
		return linker;
	}

	private void startChecker(List<User> user, JikaiUser ju) {
		long id = ju.getId();
		checker = new UserChecker(user, ju);
		checker.start();
		try {
			while (!checker.isDone()) {
				synchronized (checker) {
					checker.wait();
				}
			}
		} catch (InterruptedException e) {
			log.error("Interrupted!", e);
		}
	}

	static void removeLinker(long id) {
		linkerMap.remove(id);
	}

	public CompletableFuture<Boolean> getFuture() {
		return future;
	}

	public void stop(boolean userStopped) {
		checker.stop(userStopped);
	}
}

class UserChecker extends ListenerAdapter {
	// check mark uni codepoint
	private static final String YES_UCP = "U+2705";
	private static final String NO_UCP = "U+274c";
	private static final String STOP_UCP = "U+1f6d1";
	private List<User> user;
	private JikaiUser ju;
	private Map<Long, User> msgIdUser = new HashMap<>();
	private AtomicLong sureMsgId = new AtomicLong(0);
	private Queue<Long> sentMsgs = new LinkedList<>();
	private final Logger log;
	private boolean done = false;

	UserChecker(List<User> user, JikaiUser ju) {
		this.user = user;
		this.ju = ju;
		Core.JDA.addEventListener(this);
		log = LoggerFactory.getLogger(UserChecker.class + "#" + ju.getId());
	}

	void start() {
		log.debug("Starting check");
		BotUtils.sendPM(ju.getUser(), ju.getLocale().getStringFormatted("ju_link_ani_list", Arrays.asList("amount"), user.size())).get(0).thenAccept(m -> sentMsgs.add(m.getIdLong()));
		user.forEach(u -> sendUserEmbed(u, YES_UCP, STOP_UCP));
	}

	private MessageEmbed makeUserEmbed(User u) {
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setThumbnail(u.getBiggestAvailableAvatar()).setTitle(u.getName(), u.getSiteUrl()).setDescription(u.getAbout());
		return eb.build();
	}

	private CompletableFuture<Message> sendUserEmbed(User u, String... reactions) {
		log.debug("Sending user pm");
		return BotUtils.sendPM(ju.getUser(), makeUserEmbed(u)).thenApply(m -> {
			for (String react : reactions) {
				m.addReaction(react).submit();
			}
			msgIdUser.put(m.getIdLong(), u);
			sentMsgs.add(m.getIdLong());
			log.debug("Successfully sent and added reactions");
			return m;
		});
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		if (event.getUserIdLong() != Core.JDA.getSelfUser().getIdLong()) {
			long msgId = event.getMessageIdLong();
			if (msgIdUser.containsKey(msgId)) {
				String ucp = event.getReactionEmote().getAsCodepoints();
				if (msgId == sureMsgId.get()) {
					switch (ucp) {
						case YES_UCP -> correctUser(msgIdUser.get(msgId));
						case NO_UCP -> wrongUser();
						case STOP_UCP -> stop(true);
					}
				} else {
					switch (ucp) {
						case YES_UCP -> askIfCorrectUser(msgIdUser.get(msgId));
						case STOP_UCP -> stop(true);
					}

				}
			}
		}
	}

	private void correctUser(User u) {
		log.debug("Correct user!");
		ju.setAniId(u.getId());
		ju.sendPM(ju.getLocale().getString("ju_link_ani_done"));
		ju.sendPM(makeUserEmbed(u));
		stop(false);
	}

	private void wrongUser() {
		log.debug("Wrong user!");
		deleteMsgs();
		start();
	}

	private void deleteMsgs() {
		log.debug("Deleting msgs");
		List<String> msgIds = sentMsgs.stream().map(String::valueOf).collect(Collectors.toList());
		sentMsgs.clear();
		ju.getUser().openPrivateChannel().submit().thenAccept(pc -> pc.purgeMessagesById(msgIds));
		msgIdUser.clear();
		sureMsgId.set(0);
	}

	private void askIfCorrectUser(User u) {
		log.debug("Asking if correct user");
		BotUtils.sendPM(ju.getUser(), ju.getLocale().getString("ju_link_ani_make_sure")).get(0).thenAccept(m -> sentMsgs.add(m.getIdLong()));
		sendUserEmbed(u, YES_UCP, NO_UCP, STOP_UCP).thenAccept(m -> sureMsgId.set(m.getIdLong()));
	}

	void stop(boolean user) {
		log.debug("Stopping check, user stopped: {}", user);
		if (user) {
			ju.sendPM(BotUtils.makeSimpleEmbed(ju.getLocale().getString("ju_link_ani_stopped")));
		}
		Core.JDA.removeEventListener(this);
		deleteMsgs();
		AniLinker.removeLinker(ju.getId());
		done = true;
		synchronized (this) {
			this.notify();
		}
	}

	boolean isDone() {
		return done;
	}
}
