package com.xerragnaroek.bot.anime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.config.Config;
import com.xerragnaroek.bot.config.ConfigManager;
import com.xerragnaroek.bot.config.ConfigOption;
import com.xerragnaroek.bot.main.Core;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

/**
 * The actual implementation of a per guild AnimeListReactionHandler. <br>
 * Sends the list messages and gives users the appropriate roles when they react to those messages.
 * 
 * @author XerRagnaroek
 *
 */
public class ALRHImpl {
	private final Config config;
	private final String gId;
	private final Logger log;
	private Map<String, Map<String, String>> msgUcTitMap = Collections.synchronizedMap(new HashMap<>()); /*Message Unicode Title Map*/
	private Map<String, String> roleMap = Collections.synchronizedSortedMap(new TreeMap<>()); /*Title, RoleId*/
	private String tcId;

	ALRHImpl(String gId) {
		this.gId = gId;
		log = LoggerFactory.getLogger(ALRHImpl.class.getName() + "#" + gId);
		config = ConfigManager.getConfigForGuild(gId);
		init();
		log.debug("AnimeListReactionHandler initialized", gId);
	}

	public void handleReactionAdded(MessageReactionAddEvent event) {
		log.trace("Handling MessageReactionAdded event");
		Map<String, String> ucTitleMap;
		if ((ucTitleMap = msgUcTitMap.get(event.getMessageId())) != null) {
			log.trace("Message is part of the anime list");
			ReactionEmote re = event.getReactionEmote();
			Member m = event.getMember();
			log.debug("Member#{} added reaction {} to the anime list", m.getId(), re.getName());
			log.debug("Is Emoji? {}", re.isEmoji());
			if (re.isEmoji()) {
				String rId = roleMap.get(ucTitleMap.get(re.getAsCodepoints()));
				if (rId != null) {
					log.trace("Reaction has internally associated role {}", rId);
					Guild g = event.getGuild();
					Role r = g.getRoleById(rId);
					if (r != null) {
						log.debug("Guild has associated role '{}'", r.getName());
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

	public void handleReactionRemoved(MessageReactionRemoveEvent event) {
		Map<String, String> ucTitleMap = msgUcTitMap.get(event.getMessageId());
		if (ucTitleMap != null) {
			ReactionEmote re = event.getReactionEmote();
			Guild g = event.getGuild();
			if (re.isEmoji()) {
				if (!event.getUser().isBot()) {
					Member m = event.getMember();
					log.debug("Member#{} removed reaction {} from the anime list", m.getId(), re.getName());
					log.debug("Is Emoji? {}", re.isEmoji());
					String rId = roleMap.get(ucTitleMap.get(re.getAsCodepoints()));
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
				reAddReactionIfGone(event.getTextChannel(), event.getMessageId(), re.getAsCodepoints());
			}

		}
	}

	public void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		Map<String, String> ucTitleMap;
		if ((ucTitleMap = msgUcTitMap.get(event.getMessageId())) != null) {
			log.trace("Message is part of the anime list");
			Guild g = event.getGuild();
			ucTitleMap.forEach((uni, title) -> {
				Role r = g.getRoleById(roleMap.get(title));
				log.debug("Guild has role '{}'? {}", title, r != null);
				if (r != null) {
					g.getMembersWithRoles(r).forEach(m -> removeRoleFromMember(g, m, r));
				}
			});
			addReactions(event.getTextChannel(), event.getMessageId(), ucTitleMap.values());
		}
	}

	public void sendList() {
		Guild g = Core.getJDA().getGuildById(gId);
		TextChannel tc = g.getTextChannelById(tcId);
		log.info("Sending list messages to channel {}", tc.getName() + "#" + tcId);
		ALRHManager.getListMessages().forEach(dto -> handleDTO(g, tc, dto));
		config.setOption(ConfigOption.LIST_MESSAGES, tcId + ";" + String.join(";", msgUcTitMap.keySet()));
	}

	void init() {
		setTextChannelId(config.getOption(ConfigOption.ROLE_CHANNEL));
		List<String> ids = loadIds();
		if (!ids.isEmpty()) {

		}
		config.registerOptionChangedConsumer(ConfigOption.ROLE_CHANNEL, this::setTextChannelId);
	}

	private void addReactions(TextChannel tc, String msgId, Collection<String> uniCodes) {
		log.debug("Adding {} Reactions to Message#{} in TextChannel#{}", uniCodes.size(), msgId, tc.getName());
		uniCodes.forEach(uni -> tc.addReactionById(msgId, uni).queue(v -> log.info("Added Reaction {} to Message#{}", uni, msgId), e -> log.error("Failed adding Reaction to Message#{}", msgId, e)));
	}

	private void handleDTO(Guild g, TextChannel tc, DTO dto) {
		Message me = dto.getMessage();
		Map<String, String> map = dto.getUnicodeTitleMap();
		tc.sendMessage(me).queue(m -> {
			msgUcTitMap.put(m.getId(), map);
			map.forEach((uni, title) -> {
				if (!roleExists(g, title)) {
					log.info("Creating role for {}", title);
					g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r -> roleMap.put(title, r.getId()));
				}
				log.debug("Adding reaction {} to message", uni);
				m.addReaction(uni).queue();
			});
		}, e -> log.error("Error occured while setting up the animes role lists", e));
	}

	private void moveList(String oldTc, String newTc) {
		TextChannel tc = Core.getJDA().getGuildById(gId).getTextChannelById(oldTc);
		new TreeMap<String, Map<String, String>>(msgUcTitMap).keySet().forEach(msg -> {
			tc.retrieveMessageById(msg).queue(m -> {
				log.debug("Found list message, deleting...");
				m.delete().queue(v -> log.info("Deleted an anime list message"), e -> log.error("Failed deleting message {}", msg, e));
			}, e -> log.error("Failed looking up list message {}", msg, e));
			;
		});
		msgUcTitMap.clear();
		tcId = newTc;
		sendList();
	}

	private void reAddReactionIfGone(TextChannel tc, String msgId, String uni) {
		tc.retrieveMessageById(msgId).queue(m -> {
			List<String> cps = m.getReactions().stream().map(mr -> mr.getReactionEmote().getAsCodepoints()).collect(Collectors.toList());
			if (!cps.contains(uni)) {
				log.debug("Reaction {} was removed, readding...", uni);
				m.addReaction(uni).queue(v -> log.info("Readded reaction {}", uni), e -> log.error("Failed adding reacion {} to msg#{}", uni, m.getId(), e));
			}
		});
	}

	private void removeRoleFromMember(Guild g, Member m, Role r) {
		log.debug("Removing role from member#{}", m.getId());
		g.removeRoleFromMember(m, r).queue(v -> log.info("Succesfully removed role {} from member#{}", r.getName(), m.getId()), e -> log.error("Failed removing role", e));
	}

	private boolean roleExists(Guild g, String name) {
		return !g.getRolesByName(name, false).isEmpty();
	}

	private void setTextChannelId(String id) {
		if (tcId == null) {
			tcId = BotUtils.getChannelOrDefault(id, gId).getId();
		} else {
			if (!tcId.equals(id)) {
				moveList(tcId, id);
			}
		}
	}

	private List<String> loadIds() {
		List<String> ids = new LinkedList<>();
		String raw = config.getOption(ConfigOption.LIST_MESSAGES);
		if (!raw.isEmpty()) {
			ids = Arrays.asList(raw.split(";"));
		}
		return ids;
	}
}
