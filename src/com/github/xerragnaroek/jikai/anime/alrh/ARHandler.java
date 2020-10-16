package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

class ARHandler {

	private ALRHandler alrh;
	private ALRHDataBase alrhDB;
	final Logger log;

	ARHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ARHandler.class + "#" + alrh.gId);
		alrhDB = alrh.alrhDB;
	}

	void handleReactionAdded(GuildMessageReactionAddEvent event) {
		log.trace("Handling MessageReactionAdded event");
		long msgId = event.getMessageIdLong();
		if (alrhDB.hasDataForMessage(msgId)) {
			log.trace("Message is part of the anime list");
			ReactionEmote re = event.getReactionEmote();
			Member m = event.getMember();
			log.debug("Member#{} added reaction {} to the anime list", m.getId(), re.getName());
			log.debug("Is Emoji? {}", re.isEmoji());
			if (re.isEmoji()) {
				ALRHData data = alrhDB.getDataForUnicodeCodePoint(msgId, re.getAsCodepoints());
				if (data != null) {
					data.setReacted(true);
					JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getUser().getIdLong());
					ju.subscribeAnime(AnimeDB.getAnime(data.getTitle()).getId(), ju.getLocale().getString("ju_sub_add_cause_user"));
				}
			}
		}
	}

	void handleReactionRemoved(GuildMessageReactionRemoveEvent event) {
		long msgId = event.getMessageIdLong();
		if (alrhDB.hasDataForMessage(msgId)) {
			ReactionEmote re = event.getReactionEmote();
			Guild g = event.getGuild();
			if (re.isEmoji()) {
				ALRHData data = alrhDB.getDataForUnicodeCodePoint(msgId, re.getAsCodepoints());
				String title = data.getTitle();
				if (!event.getUser().isBot()) {
					JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getUser().getIdLong());
					ju.unsubscribeAnime(AnimeDB.getAnime(data.getTitle()).getId(), ju.getLocale().getString("ju_sub_rem_cause_user"));
				} else {
					log.debug("Reaction was removed by a bot");
				}
				validateReaction(g, event.getChannel(), event.getMessageId(), re.getAsCodepoints(), title);
			}

		}
	}

	void handleReactionRemovedAll(GuildMessageReactionRemoveAllEvent event) {
		long msgId = event.getMessageIdLong();
		if (alrhDB.hasDataForMessage(msgId)) {
			log.trace("Message is part of the anime list");
			alrhDB.getDataForMessage(msgId).forEach(data -> {
				data.setReacted(false);
				addReaction(event.getChannel(), event.getMessageId(), data.getUnicodeCodePoint());
			});
			alrh.dataChanged();
		}
	}

	private void addReaction(TextChannel tc, String msgId, String uniCode) {
		log.debug("Adding {} to Message#{} in TextChannel#{}", uniCode, msgId, tc.getName());
		tc.addReactionById(msgId, uniCode).queue(v -> log.info("Added Reaction {} to Message#{}", uniCode, msgId), e -> BotUtils.logAndSendToDev(log, "Failed adding Reaction to Message#{}" + msgId, e));
	}

	private void validateReaction(Guild g, TextChannel tc, String msgId, String uni, String title) {
		tc.retrieveMessageById(msgId).queue(m -> {
			validateReaction(m, uni, title);
		});
	}

	private void validateReaction(Message m, String uni, String title) {
		boolean found = false;
		for (MessageReaction mr : m.getReactions()) {
			ReactionEmote re = mr.getReactionEmote();
			if (re.isEmoji()) {
				if (re.getAsCodepoints().equals(uni)) {
					if (mr.getCount() == 1) {
						ALRHData data = alrhDB.getDataForTitle(title);
						if (data.isReacted()) {
							log.debug("No user reaction left for {}", title);
							data.setReacted(false);
						}
					}
					found = true;
					break;
				}
			}
		}
		if (!found) {
			m.addReaction(uni).queue(v -> log.info("Readded reaction {}", uni), e -> BotUtils.sendThrowableToDev(String.format("Failed adding reacion %s to msg#%s", uni, m.getId()), e));
		}
	}

	boolean validateReactions(Message m, Set<ALRHData> data) {
		if (m != null) {
			data.forEach(d -> validateReaction(m, d.getUnicodeCodePoint(), d.getTitle()));
			return true;
		} else {
			log.error("Saved message id is invalid!");
			return false;
		}
	}

	void update(AnimeUpdate au) {
		if (au.hasRemovedAnime()) {
			Set<ALRHData> reacted = alrhDB.getReactedAnimes();
			Set<String> titles = au.getRemovedAnime().stream().map(Anime::getTitleRomaji).collect(Collectors.toSet());
			reacted.forEach(data -> {
				String title = data.getTitle();
				if (!titles.contains(title)) {
					alrhDB.deleteEntry(data);
					log.info("Deleted obsolete data for {}", title);
				}
			});
		}
	}

}
