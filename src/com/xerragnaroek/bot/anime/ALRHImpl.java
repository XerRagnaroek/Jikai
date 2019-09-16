package com.xerragnaroek.bot.anime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.data.GuildData;
import com.xerragnaroek.bot.data.GuildDataKey;
import com.xerragnaroek.bot.data.GuildDataManager;
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
	private final GuildData gData;
	private final String gId;
	private final Logger log;
	private Map<String, Map<String, String>> msgUcTitMap = Collections.synchronizedMap(new HashMap<>()); /*Message Unicode Title Map*/
	private Map<String, String> roleMap = Collections.synchronizedSortedMap(new TreeMap<>()); /*Title, RoleId*/
	private String tcId;

	//TODO store the starting letter of the msg too, for finding the correct unicode title list!
	ALRHImpl(String gId) {
		this.gId = gId;
		log = LoggerFactory.getLogger(ALRHImpl.class.getName() + "#" + gId);
		gData = GuildDataManager.getDataForGuild(gId);
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
		gData.set(GuildDataKey.LIST_MESSAGES, String.join(";", msgUcTitMap.keySet()));
		gData.set(GuildDataKey.LIST_MESSAGES_TC, tc.getId());
		gData.set(GuildDataKey.ANIME_ROLES, roleMapString());
		gData.set(GuildDataKey.LIST_MESSAGES_AB_VERSION, gData.get(GuildDataKey.ANIME_BASE_VERSION));
	}

	private String roleMapString() {
		StringBuilder bob = new StringBuilder();
		roleMap.forEach((k, v) -> bob.append(String.format("[%s:%s]", k, v)));
		return bob.toString();
	}

	void init() {
		log.debug("Initializing...");
		setTextChannelId(gData.get(GuildDataKey.ROLE_CHANNEL));
		loadRoles();
		loadMessages();
		gData.registerDataChangedConsumer(GuildDataKey.ROLE_CHANNEL, this::setTextChannelId);
	}

	private void addReactions(TextChannel tc, String msgId, Collection<String> uniCodes) {
		log.debug("Adding {} Reactions to Message#{} in TextChannel#{}", uniCodes.size(), msgId, tc.getName());
		uniCodes.forEach(uni -> tc.addReactionById(msgId, uni).queue(v -> log.info("Added Reaction {} to Message#{}", uni, msgId), e -> log.error("Failed adding Reaction to Message#{}", msgId, e)));
	}

	private void handleDTO(Guild g, TextChannel tc, DTO dto) {
		Message me = dto.getMessage();
		Map<String, String> map = dto.getUnicodeTitleMap();
		log.debug("Sending list message...");
		try {
			tc.sendMessage(me).submit().whenComplete((m, e) -> {
				msgUcTitMap.put(m.getId(), map);
				map.forEach((uni, title) -> {
					if (!roleExists(g, title)) {
						log.info("Creating role for {}", title);
						g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r -> roleMap.put(title, r.getId()));
					}
					log.debug("Adding reaction {} to message", uni);
					m.addReaction(uni).queue();
				});
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private void moveList(String oldTc, String newTc) {
		log.info("Moving list...");
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
			log.info("Set TextChannelId to {}", tcId);
		} else {
			if (!tcId.equals(id)) {
				moveList(tcId, id);
			}
		}
	}

	private void loadMessages() {
		log.debug("Loading list messages...");
		String id = gData.get(GuildDataKey.LIST_MESSAGES_TC);
		if (id != null) {
			String raw = gData.get(GuildDataKey.LIST_MESSAGES);
			if (!raw.isEmpty()) {
				List<String> ids = new LinkedList<>(Arrays.asList(raw.split(";")));
				if (haveMessagesChanged(id, ids)) {
					log.debug("Old messages are invalid, deleting them and posting new ones");
					ids.forEach(mId -> msgUcTitMap.put(mId, null));
					moveList(id, tcId);
				} else {
					ALRHManager.getListMessages().forEach(dto -> {
						msgUcTitMap.put(ids.remove(0), dto.getUnicodeTitleMap());
					});
					log.info("Succesfully loaded all {} list messages", msgUcTitMap.size());
				}
				return;
			}
		}
		log.info("No stored list messages found");
	}

	private boolean haveMessagesChanged(String oldTc, List<String> ids) {
		Set<DTO> amsgs = ALRHManager.getListMessages();
		TextChannel tc = Core.getJDA().getTextChannelById(oldTc);
		return ids.size() != amsgs.size() || tc == null || !gData.get(GuildDataKey.ANIME_BASE_VERSION).equals(gData.get(GuildDataKey.LIST_MESSAGES_AB_VERSION)) || !oldTc.equals(tcId);
	}

	private void loadRoles() {
		log.debug("Loading roles");
		String raw = gData.get(GuildDataKey.ANIME_ROLES);
		if (raw != null) {
			Guild g = Core.getJDA().getGuildById(gId);
			Matcher m = Pattern.compile("(?<=\\[).+?(?=\\])").matcher(raw);
			while (m.find()) {
				String tmp[] = m.group().split(":");
				String title = tmp[0];
				String rId = tmp[1];
				log.debug("Found role {}:{}", title, rId);
				roleMap.put(title, rId);
			}
			checkForRoleChanges(g);
		} else {
			log.debug("No roles found");
		}
	}

	private void checkForRoleChanges(Guild g) {
		log.debug("Checking for any changes to the roles while the bot was offline");
		roleMap.forEach((title, rId) -> {
			Role r = g.getRoleById(rId);
			if (r == null || !r.getName().equals(title)) {
				log.debug("Role for {} is invalid", title);
				createRole(g, title);
			}
		});
	}

	private void createRole(Guild g, String title) {
		log.info("Creating role for {}", title);
		g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r -> roleMap.put(title, r.getId()));
	}

	private void storeData() {
		StringBuilder bob = new StringBuilder();
		msgUcTitMap.forEach((msgId, ucTitMap) -> {
			bob.append("{" + msgId + ";");
		});
	}
}
