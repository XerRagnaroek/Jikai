package com.xerragnaroek.bot.anime.alrh;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class ALRHandler {
	final GuildData gData;
	final String gId;
	private final Logger log;
	Map<String, Map<String, String>> msgUcTitMap = Collections.synchronizedMap(new HashMap<>()); /*Message Unicode Title Map*/
	Map<String, String> roleMap = Collections.synchronizedSortedMap(new TreeMap<>()); /*Title, RoleId*/
	String tcId; //Textchannel Id
	private ALHandler alh;
	private ARHandler arh;

	/**
	 * A new AnimeListReactionHandler
	 * 
	 * @param gId
	 */
	ALRHandler(String gId) {
		this.gId = gId;
		log = LoggerFactory.getLogger(ALRHandler.class.getName() + "#" + gId);
		gData = GuildDataManager.getDataForGuild(gId);
		alh = new ALHandler(this);
		arh = new ARHandler(this);
		init();
		log.info("AnimeListReactionHandler initialized", gId);
	}

	public void handleReactionAdded(MessageReactionAddEvent event) {
		arh.handleReactionAdded(event);
	}

	public void handleReactionRemoved(MessageReactionRemoveEvent event) {
		arh.handleReactionRemoved(event);
	}

	public void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		arh.handleReactionRemovedAll(event);
	}

	public void sendList() {
		alh.sendList();
	}

	void init() {
		log.debug("Initializing...");
		setTextChannelId(gData.get(GuildDataKey.LIST_CHANNEL));
		loadData();
		gData.registerDataChangedConsumer(GuildDataKey.LIST_CHANNEL, (gId, tcId) -> this.setTextChannelId(tcId));
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

	private void reAddReactionsIfGone(TextChannel tc, String msgId, Collection<String> unis) {
		log.debug("Readding missing reactions on msg {} in TextChannel {}", msgId, tc.getName());
		tc.retrieveMessageById(msgId).queue(m -> {
			List<String> cps = m.getReactions().stream().map(mr -> mr.getReactionEmote().getAsCodepoints()).collect(Collectors.toList());
			for (String uni : unis) {
				if (!cps.contains(uni)) {
					log.info("Reaction {} is missing on message {}", uni, msgId);
					m.addReaction(uni).queue(v -> log.info("Successfully readded reaction"), e -> log.error("Failed adding reaction", e));
				}
			}
		});
	}

	boolean roleExists(Guild g, String name) {
		return !g.getRolesByName(name, false).isEmpty();
	}

	private void setTextChannelId(String id) {
		if (tcId == null) {
			tcId = BotUtils.getChannelOrDefault(id, gId).getId();
			log.info("Set TextChannelId to {}", tcId);
		} else {
			if (!tcId.equals(id)) {
				//moveList(tcId, id);
			}
		}
	}

	private boolean haveMessagesChanged(TextChannel oldTc, List<String> ids) {
		Set<DTO> amsgs = ALRHManager.getListMessages();
		AtomicBoolean noMsg = new AtomicBoolean(false);
		if (oldTc == null) {
			return true;
		}
		for (String id : ids) {
			try {
				oldTc.retrieveMessageById(id).submit().whenComplete((m, e) -> noMsg.set(e != null)).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		return noMsg.get() || ids.size() != amsgs.size() || oldTc == null || !gData.get(GuildDataKey.ANIME_BASE_VERSION).equals(gData.get(GuildDataKey.LIST_MESSAGES_AB_VERSION)) || !oldTc.getId().equals(tcId);
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

	void storeData() {
		log.debug("Storing data");
		//{msgId;[title,emoji unicode,roleId]...}
		StringBuilder bob = new StringBuilder();
		msgUcTitMap.forEach((msgId, ucTitMap) -> {
			bob.append("{" + msgId + ";");
			ucTitMap.forEach((uc, title) -> {
				bob.append(String.format("%s;%s;%s;", title, uc, roleMap.get(title)));
			});
			bob.append("}");
		});
		gData.set(GuildDataKey.LIST_MESSAGES, bob.toString());
		gData.set(GuildDataKey.LIST_MESSAGES_TC, tcId);
		gData.set(GuildDataKey.LIST_MESSAGES_AB_VERSION, gData.get(GuildDataKey.ANIME_BASE_VERSION));
	}

	//TODO do something when the text channel was deleted
	//TODO save if anime was reacted to
	private void loadData() {
		log.info("Loading data...");
		Guild g = Core.getJDA().getGuildById(gId);
		TextChannel tc = g.getTextChannelById(gData.get(GuildDataKey.LIST_MESSAGES_TC));
		if (tc != null) {
			String raw = gData.get(GuildDataKey.LIST_MESSAGES);
			if (raw != null) {
				List<String> ids = new LinkedList<>();
				Matcher m = Pattern.compile("(?<=\\{).+?(?=\\})").matcher(raw);
				while (m.find()) {
					String msg = m.group();
					String msgId = msg.substring(0, msg.indexOf(";"));
					msg = msg.substring(msgId.length() + 1);
					log.debug("Found message {}", msgId);
					Map<String, String> ucTitMap = new TreeMap<>();
					String cont[] = msg.split(";");
					//title,unicode,role
					for (int i = 0; i < cont.length; i += 3) {
						String title = cont[i];
						String uc = cont[i + 1];
						String roleId = cont[i + 2];
						ucTitMap.put(uc, title);
						roleMap.put(title, roleId);
					}
					ids.add(msgId);
					msgUcTitMap.put(msgId, ucTitMap);
					reAddReactionsIfGone(tc, msgId, ucTitMap.keySet());
				}
				if (haveMessagesChanged(tc, ids)) {
					//moveList(listMsgTc, tcId);
				} else {
					checkForRoleChanges(Core.getJDA().getGuildById(gId));
				}
			} else {
				log.info("No data to load");
			}
		} else {
			log.info("The old TextChannel was deleted, no point in loading data.");
		}
	}

	public String getRoleId(String title) {
		return roleMap.get(title);
	}

	public Set<String> getReactedAnimes() {
		return arh.getReactedAnimes();
	}
}
