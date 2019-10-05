package com.xerragnaroek.jikai.anime.alrh;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
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
		String msgId = event.getMessageId();
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
					Guild g = event.getGuild();
					if (data.hasRoleId()) {
						roleExistsAdd(g, data, m);
					} else {
						roleNotExistAdd(g, data, m);
					}
				}
			}
		}
	}

	private void roleExistsAdd(Guild g, ALRHData data, Member m) {
		String rId = data.getRoleId();
		log.trace("Reaction has internally associated role {}", rId);
		Role r = g.getRoleById(rId);
		if (r != null) {
			log.debug("Guild has associated role '{}'", r.getName());
			if (!m.getRoles().contains(r)) {
				log.debug("Adding role to member#{}", m.getId());
				g.addRoleToMember(m, r).queue(v -> {
					log.info("Succesfully added role {} to member#{}", r.getName(), m.getId());
					alrh.dataChanged();
				}, e -> BotUtils.logAndSendToDev(log, "Failed adding role to member", e));
			} else {
				log.debug("Member#{} already has that role", m.getId());
			}
		}
	}

	private void roleNotExistAdd(Guild g, ALRHData data, Member m) {
		g.createRole().setMentionable(true).setName(data.getTitle()).setPermissions(0l).queue(r -> {
			log.debug("Created role for {}", data.getTitle());
			data.setRoleId(r.getId());
			alrh.dataChanged();
			g.addRoleToMember(m, r).queue(v -> {
				log.info("Added role {} to member", r.getName());
			}, e -> BotUtils.logAndSendToDev(log, "Failed adding role to member", e));
		}, e -> {
			BotUtils.sendThrowableToDev("Failed creating role {}" + data.getTitle(), e);
		});
	}

	void handleReactionRemoved(MessageReactionRemoveEvent event) {
		String msgId = event.getMessageId();
		if (alrhDB.hasDataForMessage(msgId)) {
			ReactionEmote re = event.getReactionEmote();
			Guild g = event.getGuild();
			if (re.isEmoji()) {
				ALRHData data = alrhDB.getDataForUnicodeCodePoint(msgId, re.getAsCodepoints());
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
				validateReaction(g, event.getTextChannel(), event.getMessageId(), re.getAsCodepoints(), title);
			}

		}
	}

	void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		String msgId = event.getMessageId();
		if (alrhDB.hasDataForMessage(msgId)) {
			log.trace("Message is part of the anime list");
			Guild g = event.getGuild();
			alrhDB.getDataForMessage(msgId).forEach(data -> {
				Role r = g.getRoleById(data.getRoleId());
				log.debug("Guild has role '{}'? {}", data.getTitle(), r != null);
				if (r != null) {
					g.getMembersWithRoles(r).forEach(m -> removeRoleFromMember(g, m, r));
				}
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

	private void removeRoleFromMember(Guild g, Member m, Role r) {
		log.debug("Removing role from member#{}", m.getId());
		g.removeRoleFromMember(m, r).queue(v -> log.info("Succesfully removed role {} from member#{}", r.getName(), m.getId()), e -> BotUtils.logAndSendToDev(log, "Failed removing role", e));
	}

	private void validateReaction(Guild g, TextChannel tc, String msgId, String uni, String title) {
		tc.retrieveMessageById(msgId).queue(m -> {
			validateReaction(g, m, uni, title);
		});
	}

	private void validateReaction(Guild g, Message m, String uni, String title) {
		boolean found = false;
		for (MessageReaction mr : m.getReactions()) {
			ReactionEmote re = mr.getReactionEmote();
			if (re.isEmoji()) {
				if (re.getAsCodepoints().equals(uni)) {
					if (mr.getCount() == 1) {
						log.debug("No user reaction left for {}", title);
						ALRHData data = alrhDB.getDataForTitle(title);
						data.setReacted(false);
						deleteRole(g, data);
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

	void deleteRole(Guild g, ALRHData data) {
		if (data.hasRoleId()) {
			deleteRole(g.getRoleById(data.getRoleId()), data);
		}
	}

	void deleteRole(Role r, ALRHData data) {
		r.delete().queue(v -> {
			log.info("Deleted role {}", data.getTitle());
			data.setRoleId(null);
			alrh.dataChanged();
		});
	}

	void validateReactions(Guild g, TextChannel tc, String msgId, Set<ALRHData> data) {
		log.debug("Readding missing reactions on msg {} in TextChannel {}", msgId, tc.getName());
		tc.retrieveMessageById(msgId).queue(m -> {
			data.forEach(d -> validateReaction(g, m, d.getUnicodeCodePoint(), d.getTitle()));
		});
	}

	void update() {
		Set<ALRHData> reacted = alrhDB.getReactedAnimes();
		Set<String> titles = AnimeDB.getSeasonalAnimes().stream().map(adt -> adt.getAnime().title).collect(Collectors.toSet());
		reacted.forEach(data -> {
			String title = data.getTitle();
			if (!titles.contains(title)) {
				Core.JDA.getGuildById(alrh.gId).getRoleById(data.getRoleId()).delete().queue();
				alrhDB.deleteEntry(data);
				log.info("Deleted obsolete Role and data for {}", title);
			}
		});
	}
}
