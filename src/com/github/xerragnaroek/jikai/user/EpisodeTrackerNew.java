package com.github.xerragnaroek.jikai.user;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * 
 */
public class EpisodeTrackerNew implements ButtonInteractor {

	public static final String WATCHED_EMOJI_UNICODE = "U+1f440";
	public static final String WATCHED_EMOJI_CP = EncodingUtil.encodeCodepoints(WATCHED_EMOJI_UNICODE);
	private static EpisodeTrackerNew et;
	private final Logger log = LoggerFactory.getLogger(EpisodeTrackerNew.class);

	private EpisodeTrackerNew() {}

	@Override
	public String getIdentifier() {
		return "ept";
	}

	@Override
	public void handleButtonClick(String[] data, ButtonClickEvent event) {
		MDC.put("id", event.getMessageId());
		log.debug("Handling button click");
		JikaiUserManager jum = JikaiUserManager.getInstance();
		if (!jum.isKnownJikaiUser(event.getUser().getIdLong())) {
			log.debug("User is no JikaiUser!");
			return;
		}
		JikaiUser ju = jum.getUser(event.getUser().getIdLong());
		// so far nothing happens besides the msg being edited
		JikaiLocale loc = ju.getLocale();
		MessageEmbed me = event.getMessage().getEmbeds().get(0);
		EmbedBuilder bob = new EmbedBuilder(me);
		bob.setDescription(me.getDescription() + "\n" + loc.getStringFormatted("ju_eb_notify_release_watched", Arrays.asList("date"), BotUtils.getTodayDateForJUserFormatted(ju)));
		event.editMessage(new MessageBuilder(bob.build()).build()).flatMap(InteractionHook::retrieveOriginal).map(Message::unpin).queue(v -> log.debug("Unpinned message!"));
		MDC.remove("id");
	}

	public static Message addButton(Anime a, MessageEmbed m, boolean test) {
		MessageBuilder mb = new MessageBuilder(m);
		mb.setActionRows(ActionRow.of(Button.secondary("ept:" + a.getId() + ":" + a.getNextEpisodeNumber() + (test ? ":t" : ""), Emoji.fromUnicode(WATCHED_EMOJI_UNICODE))));
		return mb.build();
	}

	public static void init() {
		et = new EpisodeTrackerNew();
		Core.getEventListener().registerButtonInteractor(et);
	}
}
