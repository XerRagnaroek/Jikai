package com.github.xerragnaroek.jikai.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;

/**
 * 
 */
public class JikaiUserSetupRewritten extends ListenerAdapter {

	private Jikai j;
	private JikaiUser ju;
	private AtomicInteger stage = new AtomicInteger(-1);
	private AtomicInteger reactStage = new AtomicInteger(-1);
	private final Logger log;
	private long curMsgId;
	private static final String yesUni = "U+2705";
	private static final String noUni = "U+274c";
	private AtomicBoolean ignoreReacts = new AtomicBoolean(true);

	public JikaiUserSetupRewritten(JikaiUser ju, Jikai j) {
		log = LoggerFactory.getLogger(JikaiUserSetupRewritten.class + "#" + ju.getId());
		this.ju = ju;
		this.j = j;
	}

	public void startSetup() {
		BotUtils.sendPM(ju.getUser(), BotUtils.localedEmbed(ju.getLocale(), "setup_greetings_eb", Pair.of(Arrays.asList("name"), new Object[] { ju.getUser().getName() }))).thenAccept((m) -> {
			Core.JDA.addEventListener(this);
			nextStage();
		});
	}

	private void askLanguage() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_lang_eb_title"));
		List<JikaiLocale> locs = new LinkedList<>(JikaiLocaleManager.getInstance().getLocales());
		Collections.sort(locs);
		List<String> flags = new ArrayList<>(locs.size());
		locs.forEach(jl -> {
			String flag = BotUtils.processUnicode(ju.getLocale().getString("u_flag_uni"));
			flags.add(flag);
			eb.addField(flag, jl.getLanguageName(), true);
		});
		sendReactionMessage(eb.build(), null, flags.toArray(new String[flags.size()]));
	}

	private void langCheck(PrivateMessageReactionAddEvent event) {
		JikaiLocale loc;
		if ((loc = JikaiLocaleManager.getInstance().getLocaleViaFlagUnicode(event.getReactionEmote().getAsCodepoints())) != null) {
			ju.setLocale(loc);
			ignoreReacts.set(true);
			nextStage();
		}
	}

	private void askConsent() {
		sendYesNoReactionMessage(BotUtils.localedEmbed(ju.getLocale(), "setup_consent_eb", null), null);
	}

	private void consentCheck(PrivateMessageReactionAddEvent event) {
		switch (isYesOrNoReacted(event)) {
			case 0 -> noConsent();
			case 1 -> consent();
		}
	}

	private void noConsent() {
		log.debug("no consent");
		ju.sendPM(ju.getLocale().getString("setup_consent_no"));
		cancelSetup();
	}

	private void consent() {
		log.debug("consent");
		nextStage();
	}

	private void askSkipSetup() {
		// sendReactionMessage(BotUtils.localedEmbed(ju.getLocale(), noUni, null))
	}

	private void nextStage() {
		ignoreReacts.set(true);
		switch (stage.incrementAndGet()) {
			case 0 -> askLanguage();
			case 1 -> askConsent();
		}
	}

	private void cancelSetup() {
		Core.JDA.removeEventListener(this);
		JikaiUserManager.getInstance().removeUser(ju.getId());
	}

	private void sendReactionMessage(MessageEmbed embed, Runnable thenAccept, String... reactions) {
		BotUtils.sendPM(ju.getUser(), embed).thenAccept(m -> {
			curMsgId = m.getIdLong();
			RestAction<Void> action = m.addReaction(reactions[0]);
			for (int i = 1; i < reactions.length; i++) {
				action = action.and(m.addReaction(reactions[i]));
			}
			action.submit().thenAccept(v -> {
				if (thenAccept != null) {
					thenAccept.run();
				}
				ignoreReacts.set(false);
				reactStage.incrementAndGet();
			});
		});
	}

	private void sendYesNoReactionMessage(MessageEmbed embed, Runnable thenAccept) {
		sendReactionMessage(embed, thenAccept, yesUni, noUni);
	}

	private int isYesOrNoReacted(PrivateMessageReactionAddEvent event) {
		int i = 2;
		String cps = event.getReactionEmote().getAsCodepoints();
		switch (cps) {
			case yesUni -> i = 1;
			case noUni -> i = 0;
		}
		return i;
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		if (!ignoreReacts.get() && event.getUserIdLong() == ju.getId() && event.getMessageIdLong() == curMsgId) {
			switch (reactStage.get()) {
				case 0 -> langCheck(event);
				case 1 -> consentCheck(event);
			}
		}
	}
}
