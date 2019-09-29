package com.xerragnaroek.bot.anime.alrh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

public class ARHandler {

	private ALRHandler alrh;
	final Logger log;

	ARHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ARHandler.class + "#" + alrh.gId);
	}

	void handleReactionAdded(MessageReactionAddEvent event) {
		log.trace("Handling MessageReactionAdded event");
		String msgId = event.getMessageId();
		if (alrh.alrhDB.hasDataForMessage(msgId)) {
			log.trace("Message is part of the anime list");
			ReactionEmote re = event.getReactionEmote();
			Member m = event.getMember();
			log.debug("Member#{} added reaction {} to the anime list", m.getId(), re.getName());
			log.debug("Is Emoji? {}", re.isEmoji());
			if (re.isEmoji()) {
				ALRHData data = alrh.alrhDB.getDataForUnicodeCodePoint(msgId, re.getAsCodepoints());
				String rId = data.getRoleId();
				if (rId != null) {
					log.trace("Reaction has internally associated role {}", rId);
					Guild g = event.getGuild();
					Role r = g.getRoleById(rId);
					if (r != null) {
						log.debug("Guild has associated role '{}'", r.getName());
						data.setReacted(true);
						if (!m.getRoles().contains(r)) {
							log.debug("Adding role to member#{}", m.getId());
							g.addRoleToMember(m, r)
									.queue(	v -> log.info(	"Succesfully added role {} to member#{}", r.getName(),
															m.getId()),
											e -> log.error("Failed adding role to member", e));
						} else {
							log.debug("Member#{} already has that role", m.getId());
						}
					}
				}
				alrh.storeData();
			}
		}
	}

	void handleReactionRemoved(MessageReactionRemoveEvent event) {
		String msgId = event.getMessageId();
		if (alrh.alrhDB.hasDataForMessage(msgId)) {
			ReactionEmote re = event.getReactionEmote();
			Guild g = event.getGuild();
			if (re.isEmoji()) {
				ALRHData data = alrh.alrhDB.getDataForUnicodeCodePoint(msgId, re.getAsCodepoints());
				String title = data.getTitle();
				if (!event.getUser().isBot()) {
					Member m = event.getMember();
					log.debug("Member#{} removed reaction {} from the anime list", m.getId(), re.getName());
					log.debug("Is Emoji? {}", re.isEmoji());
					String rId = data.getRoleId();
					if (rId != null) {
						log.trace("Reaction has internally associated role {}", rId);
						Role r = g.getRoleById(rId);
						if (r != null) {
							log.debug("Found associated role '{}'", r.getName());
							if (m.getRoles().contains(r)) {
								removeRoleFromMember(g, m, r);
							} else {
								log.debug("Member#{} doesn't have that role", m.getId());
							}
						}
					}
				} else {
					log.debug("Reaction was removed by a bot");
				}
				validateReaction(event.getTextChannel(), event.getMessageId(), re.getAsCodepoints(), title);
			}

		}
	}

	void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		String msgId = event.getMessageId();
		if (alrh.alrhDB.hasDataForMessage(msgId)) {
			log.trace("Message is part of the anime list");
			Guild g = event.getGuild();
			alrh.alrhDB.getDataForMessage(msgId).forEach(data -> {
				Role r = g.getRoleById(data.getRoleId());
				log.debug("Guild has role '{}'? {}", data.getTitle(), r != null);
				if (r != null) {
					g.getMembersWithRoles(r).forEach(m -> removeRoleFromMember(g, m, r));
				}
				data.setReacted(false);
				addReaction(event.getTextChannel(), event.getMessageId(), data.getUnicodeCodePoint());
			});
			alrh.storeData();
		}
	}

	private void addReaction(TextChannel tc, String msgId, String uniCode) {
		log.debug("Adding {} to Message#{} in TextChannel#{}", uniCode, msgId, tc.getName());
		tc.addReactionById(msgId, uniCode).queue(	v -> log.info("Added Reaction {} to Message#{}", uniCode, msgId),
													e -> log.error("Failed adding Reaction to Message#{}", msgId, e));
	}

	private void removeRoleFromMember(Guild g, Member m, Role r) {
		log.debug("Removing role from member#{}", m.getId());
		g.removeRoleFromMember(m, r)
				.queue(	v -> log.info("Succesfully removed role {} from member#{}", r.getName(), m.getId()),
						e -> log.error("Failed removing role", e));
	}

	private void validateReaction(TextChannel tc, String msgId, String uni, String title) {
		tc.retrieveMessageById(msgId).queue(m -> {
			boolean found = false;
			for (MessageReaction mr : m.getReactions()) {
				ReactionEmote re = mr.getReactionEmote();
				if (re.isEmoji()) {
					if (re.getAsCodepoints().equals(uni)) {
						if (mr.getCount() == 1) {
							log.debug("No user reaction left for {}", title);
							alrh.alrhDB.getDataForTitle(title).setReacted(false);
							alrh.storeData();
						}
						found = true;
						break;
					}
				}
			}
			if (!found) {
				m.addReaction(uni).queue(	v -> log.info("Readded reaction {}", uni),
											e -> log.error("Failed adding reacion {} to msg#{}", uni, m.getId(), e));
			}

		});
	}
}
