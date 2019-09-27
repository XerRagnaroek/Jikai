package com.xerragnaroek.bot.anime.alrh;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

public class ARHandler {

	private ALRHandler alrh;
	final Logger log;
	private Set<String> reactedAnimes = Collections.synchronizedSet(new TreeSet<>());

	ARHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ARHandler.class + "#" + alrh.gId);
	}

	void handleReactionAdded(MessageReactionAddEvent event) {
		log.trace("Handling MessageReactionAdded event");
		Map<String, String> ucTitleMap;
		if ((ucTitleMap = alrh.msgUcTitMap.get(event.getMessageId())) != null) {
			log.trace("Message is part of the anime list");
			ReactionEmote re = event.getReactionEmote();
			Member m = event.getMember();
			log.debug("Member#{} added reaction {} to the anime list", m.getId(), re.getName());
			log.debug("Is Emoji? {}", re.isEmoji());
			if (re.isEmoji()) {
				String title = ucTitleMap.get(re.getAsCodepoints());
				String rId = alrh.roleMap.get(title);
				if (rId != null) {
					log.trace("Reaction has internally associated role {}", rId);
					Guild g = event.getGuild();
					Role r = g.getRoleById(rId);
					if (r != null) {
						log.debug("Guild has associated role '{}'", r.getName());
						reactedAnimes.add(title);
						if (!m.getRoles().contains(r)) {
							log.debug("Adding role to member#{}", m.getId());
							g.addRoleToMember(m, r).queue(v -> log.info("Succesfully added role {} to member#{}", r.getName(), m.getId()), e -> log.error("Failed adding role to member", e));
						} else {
							log.debug("Member#{} already has that role", m.getId());
						}
					}
				}
			}
		}
	}

	void handleReactionRemoved(MessageReactionRemoveEvent event) {
		Map<String, String> ucTitleMap = alrh.msgUcTitMap.get(event.getMessageId());
		if (ucTitleMap != null) {
			ReactionEmote re = event.getReactionEmote();
			Guild g = event.getGuild();
			if (re.isEmoji()) {
				String title = ucTitleMap.get(re.getAsCodepoints());
				if (!event.getUser().isBot()) {
					Member m = event.getMember();
					log.debug("Member#{} removed reaction {} from the anime list", m.getId(), re.getName());
					log.debug("Is Emoji? {}", re.isEmoji());
					String rId = alrh.roleMap.get(title);
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
				reAddReactionIfGone(event.getTextChannel(), event.getMessageId(), re.getAsCodepoints(), title);
			}

		}
	}

	void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		Map<String, String> ucTitleMap;
		if ((ucTitleMap = alrh.msgUcTitMap.get(event.getMessageId())) != null) {
			log.trace("Message is part of the anime list");
			Guild g = event.getGuild();
			ucTitleMap.forEach((uni, title) -> {
				Role r = g.getRoleById(alrh.roleMap.get(title));
				log.debug("Guild has role '{}'? {}", title, r != null);
				if (r != null) {
					g.getMembersWithRoles(r).forEach(m -> removeRoleFromMember(g, m, r));
				}
			});
			addReactions(event.getTextChannel(), event.getMessageId(), ucTitleMap.values());
		}
	}

	private void addReactions(TextChannel tc, String msgId, Collection<String> uniCodes) {
		log.debug("Adding {} Reactions to Message#{} in TextChannel#{}", uniCodes.size(), msgId, tc.getName());
		uniCodes.forEach(uni -> tc.addReactionById(msgId, uni).queue(v -> log.info("Added Reaction {} to Message#{}", uni, msgId), e -> log.error("Failed adding Reaction to Message#{}", msgId, e)));
	}

	private void removeRoleFromMember(Guild g, Member m, Role r) {
		log.debug("Removing role from member#{}", m.getId());
		g.removeRoleFromMember(m, r).queue(v -> log.info("Succesfully removed role {} from member#{}", r.getName(), m.getId()), e -> log.error("Failed removing role", e));
	}

	private void reAddReactionIfGone(TextChannel tc, String msgId, String uni, String title) {
		tc.retrieveMessageById(msgId).queue(m -> {
			List<String> cps = m.getReactions().stream().map(mr -> mr.getReactionEmote().getAsCodepoints()).collect(Collectors.toList());
			if (!cps.contains(uni)) {
				log.debug("Reaction {} was removed, readding...", uni);
				reactedAnimes.remove(title);
				m.addReaction(uni).queue(v -> log.info("Readded reaction {}", uni), e -> log.error("Failed adding reacion {} to msg#{}", uni, m.getId(), e));
			}
		});
	}

	Set<String> getReactedAnimes() {
		log.debug("Getting reacted to animes");
		return new TreeSet<String>(reactedAnimes);
	}
}
