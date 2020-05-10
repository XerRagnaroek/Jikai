package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.util.Initilizable;
import com.github.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.entities.Guild;
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
public class ALRHandler implements Initilizable {
	JikaiData jData;
	final long gId;
	private final Logger log;
	ALRHDataBase alrhDB;
	Property<Long> tcId = new Property<>(); //Textchannel Id
	private ALHandler alh;
	private ARHandler arh;
	private AtomicBoolean changed;
	private AtomicBoolean initialized;
	Jikai j;

	/**
	 * A new AnimeListReactionHandler
	 * 
	 * @param gId
	 */
	ALRHandler(long gId) {
		this.gId = gId;
		j = Core.JM.get(gId);
		log = LoggerFactory.getLogger(ALRHandler.class.getName() + "#" + gId);
		alrhDB = new ALRHDataBase();
		changed = new AtomicBoolean(false);
		alh = new ALHandler(this);
		arh = new ARHandler(this);
		jData = j.getJikaiData();
		jData.listChannelIdProperty().bindAndSet(tcId);
		initialized = new AtomicBoolean(false);
		j.setALRH(this);
	}

	/**
	 * To be called by the EventListener. <br>
	 * Handle a MessageReactionAddEvent
	 * 
	 * @param event
	 *            - the event to handle
	 */
	public void handleReactionAdded(MessageReactionAddEvent event) {
		arh.handleReactionAdded(event);
	}

	/**
	 * To be called by the EventListener. <br>
	 * Handle a MessageReactionRemoveEvent
	 * 
	 * @param event
	 *            - the event to handle
	 */
	public void handleReactionRemoved(MessageReactionRemoveEvent event) {
		arh.handleReactionRemoved(event);
	}

	/**
	 * To be called by the EventListener. <br>
	 * Handle a MessageReactionRemoveAllEvent
	 * 
	 * @param event
	 *            - the event to handle
	 */
	public void handleReactionRemovedAll(MessageReactionRemoveAllEvent event) {
		arh.handleReactionRemovedAll(event);
	}

	/**
	 * Send the anime list.
	 */
	public void sendList() {
		try {
			alh.sendList();
		} catch (Exception e) {
			log.error("", e);
		}
	}

	@Override
	public void init() {
		log.debug("Initializing...");
		jData.animeChannelIdProperty().bindAndSet(tcId);
		jData.listChannelIdProperty().bind(tcId);
		if (alrhDB.getData().size() != AnimeDB.size()) {
			try {
				alh.sendList();
			} catch (Exception e) {
				log.error("", e);
			}
		} else {
			alrhDB.forEachMessage(this::checkIfListChanged);
		}
		initialized.set(true);
		log.info("Initialized");
	}

	/*
	 * private void moveList(String oldTc, String newTc) { log.info("Moving list..."); TextChannel
	 * tc = Core.getJDA().getGuildById(gId).getTextChannelById(oldTc); new TreeMap<String,
	 * Map<String, String>>(msgUcTitMap).keySet().forEach(msg -> {
	 * tc.retrieveMessageById(msg).queue(m -> { log.debug("Found list message, deleting...");
	 * m.delete().queue( v -> log.info("Deleted an anime list message"), e ->
	 * log.error("Failed deleting message {}", msg, e)); }, e ->
	 * log.error("Failed looking up list message {}", msg, e)); ; }); msgUcTitMap.clear(); tcId =
	 * newTc; sendList(); }
	 */

	private void checkIfListChanged(long msgId, Set<ALRHData> data) {
		Guild g = Core.JDA.getGuildById(gId);
		long tcId = alrhDB.getSentTextChannelId();
		if (tcId != 0) {
			TextChannel tc = g.getTextChannelById(alrhDB.getSentTextChannelId());
			if (tc != null) {
				arh.validateReactions(g, tc, msgId, data);
			}
		}
	}

	boolean roleExists(Guild g, String name) {
		return !g.getRolesByName(name, false).isEmpty();
	}

	private boolean haveMessagesChanged(TextChannel oldTc, List<String> ids) {
		Set<DTO> amsgs = Core.JM.getALHRM().getListMessages();
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
		return noMsg.get() || ids.size() != amsgs.size() || oldTc == null || !(AnimeDB.getAnimeDBVersion() == alrhDB.getSentABVersion()) || !oldTc.getId().equals(tcId);
	}

	void dataChanged() {
		changed.set(true);
	}

	public boolean isSendingList() {
		return alh.isSending();
	}

	public boolean hasUpdateFlagAndReset() {
		return changed.getAndSet(false);
	}

	void setData(Set<ALRHData> data) {
		alrhDB.addData(data);
	}

	public Set<ALRHData> getData() {
		return alrhDB.getData();
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	void update(AnimeUpdate au) {
		arh.update(au);
		alh.update(au);
	}

}
