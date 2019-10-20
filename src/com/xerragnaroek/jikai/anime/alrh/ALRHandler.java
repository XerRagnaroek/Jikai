package com.xerragnaroek.jikai.anime.alrh;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.GuildData;
import com.xerragnaroek.jikai.data.UpdatableData;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Initilizable;
import com.xerragnaroek.jikai.util.JikaiManaged;
import com.xerragnaroek.jikai.util.Property;

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
public class ALRHandler extends JikaiManaged implements UpdatableData, Initilizable {
	final GuildData gData;
	final String gId;
	private final Logger log;
	ALRHDataBase alrhDB;
	Property<String> tcId = new Property<>(); //Textchannel Id
	private ALHandler alh;
	private ARHandler arh;
	private AtomicBoolean changed;
	private AtomicBoolean initialized;

	/**
	 * A new AnimeListReactionHandler
	 * 
	 * @param gId
	 */
	ALRHandler(String gId) {
		this.gId = gId;
		log = LoggerFactory.getLogger(ALRHandler.class.getName() + "#" + gId);
		gData = Core.GDM.get(gId);
		gData.listChannelIdProperty().bind(tcId);
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
		setTextChannelId(gData.getListChannelId());
		alrhDB.forEachMessage(this::checkIfListChanged);
		gData.listChannelIdProperty().bind(tcId);
		initialized.set(true);
		log.info("Initialized");
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
		Guild g = Core.JDA.getGuildById(gId);
		String tcId = alrhDB.getSentTextChannelId();
		if (tcId != null) {
			TextChannel tc = g.getTextChannelById(alrhDB.getSentTextChannelId());
			if (tc != null) {
				arh.validateReactions(g, tc, msgId, data);
			}
		}
	}

	boolean roleExists(Guild g, String name) {
		return !g.getRolesByName(name, false).isEmpty();
	}

	private void setTextChannelId(String id) {
		if (tcId == null) {
			tcId = Property.of(BotUtils.getChannelOrDefault(id, gId).getId());
			log.debug("Set TextChannelId to {}", tcId);
		} else {
			if (!tcId.equals(id)) {
				//moveList(tcId, id);
			}
		}
	}

	private boolean haveMessagesChanged(TextChannel oldTc, List<String> ids) {
		Set<DTO> amsgs = Core.ALRHM.getListMessages();
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
		return noMsg.get() || ids.size() != amsgs.size() || oldTc == null || !(AnimeDB.getAnimeBaseVersion() == alrhDB.getSentABVersion()) || !oldTc.getId().equals(tcId);
	}

	void dataChanged() {
		changed.set(true);
	}

	public String getRoleId(String title) {
		return alrhDB.getDataForTitle(title).getRoleId();
	}

	public Set<String> getReactedAnimes() {
		return alrhDB.getReactedAnimes().stream().map(ALRHData::getTitle).collect(Collectors.toSet());
	}

	public boolean isSendingList() {
		return alh.isSending();
	}

	public void setReacted(String title, boolean reacted) {
		if (alrhDB.hasDataForTitle(title)) {
			alrhDB.getDataForTitle(title).setReacted(reacted);
		}
	}

	@Override
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
		String iId = Core.GDM.get(gId).getInfoChannelId();
		if (iId != null) {
			TextChannel iTc = Core.JDA.getGuildById(gId).getTextChannelById(iId);
			if (iTc != null) {
				iTc.sendMessage("The anime database has updated, sending a new list and deleting invalid roles.").queue();
				arh.update();
				alh.update();
			}
		}
	}

	public void deleteRole(Role r) {
		arh.deleteRole(r, alrhDB.getDataForTitle(r.getName()));
	}
}
