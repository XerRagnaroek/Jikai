package com.github.xerragnaroek.jikai.user.link;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

	private LinkRequest(JikaiUser initiator, JikaiUser target, int direction) {
		this.initiator = initiator;
		this.target = target;
		dir = direction;
	}

	private void doLink() {
		switch (dir) {
			case UNIDIRECTIONAL -> unidi();
			case BIDIRECTIONAL -> bidi();
		}
	}

	private void unidi() {
		target.linkUser(initiator);
		JikaiLocale loc = initiator.getLocale();
		initiator.sendPM(BotUtils.embedBuilder().setTitle(loc.getString("ju_link_req_eb_suc_title")).setDescription(loc.getStringFormatted("ju_link_req_unidi_suc", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl()).build());
	}

	private void bidi() {
		JikaiLocale loc = target.getLocale();
		String initName = initiator.getUser().getName();
		MessageEmbed me = BotUtils.embedBuilder().setTitle(loc.getStringFormatted("ju_link_req_tgt_eb_title", Arrays.asList("name"), initName)).setDescription(loc.getStringFormatted("ju_link_req_tgt_msg", Arrays.asList("name"), initName)).setThumbnail(initiator.getUser().getEffectiveAvatarUrl()).build();
		BotUtils.sendPM(target.getUser(), me).thenAccept(m -> {
			msg = m;
			m.addReaction(checkMark).and(m.addReaction(crossMark)).submit().thenAccept(v -> {
				initiator.sendPM(BotUtils.embedBuilder().setDescription(initiator.getLocale().getStringFormatted("ju_link_req_init_msg", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl()).build());
				Core.JDA.addEventListener(this);
				future = Core.EXEC.schedule(() -> reqExpired(m), 1, TimeUnit.DAYS);
			});
		});
	}

	private void reqExpired(Message m) {
		Core.JDA.removeEventListener(this);
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(target.getLocale().getString("ju_link_req_tgt_exp_eb_title")).setDescription(target.getLocale().getStringFormatted("ju_link_req_tgt_exp", Arrays.asList("name", "date"), initiator.getUser().getName(), BotUtils.formatTime(LocalDateTime.now(), "eeee, dd.MM.yyyy", target.getLocale().getLocale()))).setThumbnail(initiator.getUser().getEffectiveAvatarUrl());
		m.editMessage(eb.build()).submit();
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
		msg.editMessage(eb.build()).submit();
	}

	private void reqDeclined() {
		Core.JDA.removeEventListener(this);
		future.cancel(true);
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(initiator.getLocale().getStringFormatted("ju_link_req_bidi_init_dec", Arrays.asList("name"), target.getUser().getName())).setThumbnail(target.getUser().getEffectiveAvatarUrl());
		initiator.sendPM(eb.build());
		eb = BotUtils.embedBuilder();
		eb.setTitle(target.getLocale().getStringFormatted("ju_link_req_bidi_tgt_dec", Arrays.asList("name"), initiator.getUser().getName())).setThumbnail(initiator.getUser().getEffectiveAvatarUrl());
		msg.editMessage(eb.build()).submit();
	}

	static void handleLinkRequest(JikaiUser src, JikaiUser target, int direction) {
		new LinkRequest(src, target, direction).doLink();
	}
}
