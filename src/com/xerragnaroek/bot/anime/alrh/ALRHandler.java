package com.xerragnaroek.bot.anime.alrh;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.base.AnimeBase;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.data.GuildData;
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
	ALRHDataBase alrhDB = new ALRHDataBase();
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
		setTextChannelId(gData.getListChannelId());
		alrhDB.addData(gData.getALRHData());
		alrhDB.forEachMessage(this::checkIfListChanged);
		gData.registerOnListChannelIdChange((gId, tcId) -> this.setTextChannelId(tcId));
	}

	/*private void moveList(String oldTc, String newTc) {
		log.info("Moving list...");
		TextChannel tc = Core.getJDA().getGuildById(gId).getTextChannelById(oldTc);
		new TreeMap<String, Map<String, String>>(msgUcTitMap).keySet().forEach(msg -> {
			tc.retrieveMessageById(msg).queue(m -> {
				log.debug("Found list message, deleting...");
				m.delete().queue(	v -> log.info("Deleted an anime list message"),
									e -> log.error("Failed deleting message {}", msg, e));
			}, e -> log.error("Failed looking up list message {}", msg, e));
			;
		});
		msgUcTitMap.clear();
		tcId = newTc;
		sendList();
	}*/

	private void checkIfListChanged(String msgId, Set<ALRHData> data) {
		Set<String> unis = data.stream().map(ALRHData::getUnicodeCodePoint).collect(Collectors.toSet());
		Guild g = Core.getJDA().getGuildById(gId);
		TextChannel tc = g.getTextChannelById(alrhDB.getSentTextChannelId());
		if (tc != null) {
			reAddReactionsIfGone(tc, msgId, unis);
		}
	}

	private void reAddReactionsIfGone(TextChannel tc, String msgId, Collection<String> unis) {
		log.debug("Readding missing reactions on msg {} in TextChannel {}", msgId, tc.getName());
		tc.retrieveMessageById(msgId).queue(m -> {
			List<String> cps = m.getReactions().stream().map(mr -> mr.getReactionEmote().getAsCodepoints())
					.collect(Collectors.toList());
			for (String uni : unis) {
				if (!cps.contains(uni)) {
					log.info("Reaction {} is missing on message {}", uni, msgId);
					m.addReaction(uni).queue(	v -> log.info("Successfully readded reaction"),
												e -> log.error("Failed adding reaction", e));
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
		return noMsg.get() || ids.size() != amsgs.size() || oldTc == null
				|| !(AnimeBase.getAnimeBaseVersion() == alrhDB.getSentABVersion()) || !oldTc.getId().equals(tcId);
	}

	private void checkForRoleChanges(Guild g) {
		log.debug("Checking for any changes to the roles while the bot was offline");
		alrhDB.getData().forEach(d -> {
			Role r = g.getRoleById(d.getRoleId());
			String title = d.getTitle();
			if (r == null || !r.getName().equals(title)) {
				log.debug("Role for {} is invalid", title);
				createRole(g, title);
			}
		});
	}

	void storeData() {
		gData.setALRHData(alrhDB.getData());
	}

	private void createRole(Guild g, String title) {
		log.info("Creating role for {}", title);
		g.createRole().setName(title).setMentionable(true).setPermissions(0l)
				.queue(r -> alrhDB.getDataForTitle(title).setRoleId(r.getId()));
	}

	public String getRoleId(String title) {
		return alrhDB.getDataForTitle(title).getRoleId();
	}

	public Set<String> getReactedAnimes() {
		return alrhDB.getReactedAnimes().stream().map(ALRHData::getTitle).collect(Collectors.toSet());
	}
}
