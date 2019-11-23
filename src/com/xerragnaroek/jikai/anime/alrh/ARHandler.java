/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.anime.alrh;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.user.JikaiUser;
import com.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

class ARHandler {

	private ALRHandler alrh;
	private ALRHDataBase alrhDB;
	final Logger log;

	ARHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ARHandler.class + "#" + alrh.gId);
		alrhDB = alrh.alrhDB;
	}

	void handleReactionAdded(MessageReactionAddEvent event) {
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
					JikaiUser ju = Jikai.getUserManager().getUser(event.getUser().getIdLong());
					ju.subscribeAnime(data.getTitle());
				}
			}
		}
	}

	void handleReactionRemoved(MessageReactionRemoveEvent event) {
		long msgId = event.getMessageIdLong();
		if (alrhDB.hasDataForMessage(msgId)) {
			ReactionEmote re = event.getReactionEmote();
			Guild g = event.getGuild();
			if (re.isEmoji()) {
				ALRHData data = alrhDB.getDataForUnicodeCodePoint(msgId, re.getAsCodepoints());
				String title = data.getTitle();
				if (!event.getUser().isBot()) {
					Jikai.getUserManager().getUser(event.getUser().getIdLong()).unsubscribeAnime(title);
				} else {
					log.debug("Reaction was removed by a bot");
				}
				validateReaction(g, event.getTextChannel(), event.getMessageId(), re.getAsCodepoints(), title);
			}

		}
	}

	void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		long msgId = event.getMessageIdLong();
		if (alrhDB.hasDataForMessage(msgId)) {
			log.trace("Message is part of the anime list");
			Guild g = event.getGuild();
			alrhDB.getDataForMessage(msgId).forEach(data -> {
				data.setReacted(false);
				addReaction(event.getTextChannel(), event.getMessageId(), data.getUnicodeCodePoint());
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
						log.debug("No user reaction left for {}", title);
						ALRHData data = alrhDB.getDataForTitle(title);
						data.setReacted(false);
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

	void validateReactions(Guild g, TextChannel tc, long msgId, Set<ALRHData> data) {
		log.debug("Readding missing reactions on msg {} in TextChannel {}", msgId, tc.getName());
		tc.retrieveMessageById(msgId).submit().whenComplete((m, t) -> {
			long tmp = msgId;
			if (t == null) {
				data.forEach(d -> validateReaction(m, d.getUnicodeCodePoint(), d.getTitle()));
			} else {
				log.error("Message not found", t);
			}
		});
	}

	void update() {
		Set<ALRHData> reacted = alrhDB.getReactedAnimes();
		Set<String> titles = AnimeDB.getSeasonalAnime().stream().map(adt -> adt.getAnime().title).collect(Collectors.toSet());
		reacted.forEach(data -> {
			String title = data.getTitle();
			if (!titles.contains(title)) {
				alrhDB.deleteEntry(data);
				log.info("Deleted obsolete Role and data for {}", title);
			}
		});
	}

}
