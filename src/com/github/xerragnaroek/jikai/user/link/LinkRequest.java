package com.github.xerragnaroek.jikai.user.link;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class LinkRequest extends ListenerAdapter {
	private static final String checkMark = "U+2714";
	private static final String crossMark = "U+274c";
	/**
	 * Will link the source user to the target user
	 */
	public static final int UNIDIRECTIONAL = -1;

	/**
	 * Will attempt to link both users. Requires confirmation of target user
	 */
	public static final int BIDIRECTIONAL = -2;
	private JikaiUser initiator;
	private JikaiUser target;
	private int dir;
	private Message msg;
	private ScheduledFuture<?> future;

	private final Logger log;

	private LinkRequest(JikaiUser initiator, JikaiUser target, int direction) {
		this.initiator = initiator;
		this.target = target;
		dir = direction;
		log = LoggerFactory.getLogger(LinkRequest.class.getCanonicalName() + "#" + initiator.getId() + "->" + target.getId());
		log.debug("new request: {}", dir == -1 ? "UNIDIRECTIONAL" : (dir == -2 ? "BIDIRECTIONAL" : "INVALID"));
	}

	private void doLink() {
		switch (dir) {
			case UNIDIRECTIONAL -> unidi();
			case BIDIRECTIONAL -> bidi();
		}
	}

	private void unidi() {
		log.debug("performing unidirectional link");
		target.linkUser(initiator);
		JikaiLocale loc = initiator.getLocale();
		initiator.sendPM(BotUtils.embedBuilder().setTitle(loc.getString("ju_link_req_eb_suc_title")).setDescription(loc.getStringFormatted("ju_link_req_unidi_suc", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl()).build());
		log.debug("linked");
	}

	private void bidi() {
		log.debug("performing bidirectional link");
		JikaiLocale loc = target.getLocale();
		String initName = initiator.getUser().getName();
		MessageEmbed me = BotUtils.embedBuilder().setTitle(loc.getStringFormatted("ju_link_req_tgt_eb_title", Arrays.asList("name"), initName)).setDescription(loc.getStringFormatted("ju_link_req_tgt_msg", Arrays.asList("name"), initName)).setThumbnail(initiator.getUser().getEffectiveAvatarUrl()).build();
		BotUtils.sendPM(target.getUser(), me).thenAccept(m -> {
			msg = m;
			m.addReaction(checkMark).and(m.addReaction(crossMark)).submit().thenAccept(v -> {
				log.debug("reactions added");
				m.pin().submit().thenAccept(vo -> log.debug("request pinned"));
				initiator.sendPM(BotUtils.embedBuilder().setDescription(initiator.getLocale().getStringFormatted("ju_link_req_init_msg", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl()).build());
				Core.JDA.addEventListener(this);
				log.debug("registered eventListener");
				future = Core.EXEC.schedule(() -> reqExpired(m), 1, TimeUnit.DAYS);
			});
		});
	}

	private void reqExpired(Message m) {
		Core.JDA.removeEventListener(this);
		log.debug("request expired, removed eventListener");
		JikaiLocale loc = target.getLocale();
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(loc.getString("ju_link_req_tgt_exp_eb_title")).setDescription(loc.getStringFormatted("ju_link_req_tgt_exp", Arrays.asList("name", "date"), initiator.getUser().getName(), BotUtils.formatTime(LocalDateTime.now(), "eeee, dd.MM.yyyy", loc.getLocale()))).setThumbnail(initiator.getUser().getEffectiveAvatarUrl());
		m.editMessage(eb.build()).submit().thenAccept(msg -> log.debug("message edited"));
		m.unpin().submit().thenAccept(v -> log.debug("message unpinned"));
		loc = initiator.getLocale();
		eb = BotUtils.embedBuilder();
		eb.setTitle(loc.getString("ju_link_req_init_exp_eb_title")).setDescription(loc.getStringFormatted("ju_link_req_init_exp", Arrays.asList("name", "date"), target.getUser().getName(), BotUtils.formatTime(LocalDateTime.now(), "eeee, dd.MM.yyyy", loc.getLocale()))).setThumbnail(target.getUser().getEffectiveAvatarUrl());
		initiator.sendPM(eb.build());
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		if (event.getMessageIdLong() == msg.getIdLong() && event.getUserIdLong() == target.getId()) {
			switch (event.getReactionEmote().getAsCodepoints()) {
				case checkMark -> reqAccepted();
				case crossMark -> reqDeclined();
			}
		}
	}

	private void reqAccepted() {
		log.debug("request accepted");
		Core.JDA.removeEventListener(this);
		future.cancel(true);
		target.linkUser(initiator);
		initiator.linkUser(target);
		EmbedBuilder eb = BotUtils.embedBuilder();
		JikaiLocale loc = initiator.getLocale();
		eb.setTitle(loc.getString("ju_link_req_eb_suc_title")).setDescription(loc.getStringFormatted("ju_link_req_bidi_suc", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl());
		initiator.sendPM(eb.build());
		eb = BotUtils.embedBuilder();
		loc = target.getLocale();
		eb.setTitle(loc.getString("ju_link_req_eb_suc_title")).setDescription(loc.getStringFormatted("ju_link_req_bidi_suc", Arrays.asList("name"), initiator.getUser().getName())).setThumbnail(initiator.getUser().getEffectiveAvatarUrl());
		msg.editMessage(eb.build()).submit().thenAccept(m -> log.debug("messsage edited"));
		log.debug("users linked bidrectionally");
	}

	private void reqDeclined() {
		log.debug("request declined");
		Core.JDA.removeEventListener(this);
		future.cancel(true);
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(initiator.getLocale().getStringFormatted("ju_link_req_bidi_init_dec", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl());
		initiator.sendPM(eb.build());
		eb = BotUtils.embedBuilder();
		eb.setTitle(target.getLocale().getStringFormatted("ju_link_req_bidi_tgt_dec", Arrays.asList("name"), initiator.getUser().getName())).setThumbnail(initiator.getUser().getEffectiveAvatarUrl());
		msg.editMessage(eb.build()).submit().thenAccept(m -> log.debug("message edited"));
	}

	static void handleLinkRequest(JikaiUser src, JikaiUser target, int direction) {
		new LinkRequest(src, target, direction).doLink();
	}
}
