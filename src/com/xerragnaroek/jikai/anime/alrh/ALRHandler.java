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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.jikai.JikaiData;
import com.xerragnaroek.jikai.util.Destroyable;
import com.xerragnaroek.jikai.util.Initilizable;
import com.xerragnaroek.jikai.util.prop.Property;

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
public class ALRHandler implements Initilizable, Destroyable {
	final JikaiData jData;
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
		j.setALRH(this);
		log = LoggerFactory.getLogger(ALRHandler.class.getName() + "#" + gId);
		jData = j.getJikaiData();
		jData.listChannelIdProperty().bind(tcId);
		alrhDB = new ALRHDataBase();
		changed = new AtomicBoolean(false);
		alh = new ALHandler(this);
		arh = new ARHandler(this);
		initialized = new AtomicBoolean(false);
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
		alh.sendList();
	}

	@Override
	public void init() {
		log.debug("Initializing...");
		jData.animeChannelIdProperty().bindAndSet(tcId);
		alrhDB.forEachMessage(this::checkIfListChanged);
		jData.listChannelIdProperty().bind(tcId);
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

	void update() {
		try {
			TextChannel iTc = j.getInfoChannel();
			iTc.sendMessage("The anime database has updated, sending a new list.").queue();
			arh.update();
			alh.update();
		} catch (Exception e) {
			//already handled
		}
	}

	@Override
	public void destroy() {
		tcId.destroy();
		alrhDB.clearData();
	}

}
