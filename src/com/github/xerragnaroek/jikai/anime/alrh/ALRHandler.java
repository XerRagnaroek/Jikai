package com.github.xerragnaroek.jikai.anime.alrh;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Initilizable;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

/**
 * The actual implementation of a per guild AnimeListReactionHandler. <br>
 * Sends the list messages and gives users the appropriate roles when they react to those messages.
 * 
 * @author XerRagnaroek
 */
public class ALRHandler implements Initilizable {
	JikaiData jData;
	final long gId;
	private final Logger log;
	ALRHDataBase alrhDB;
	Property<Long> tcId = new Property<>(); // Textchannel Id
	private ALHandler alh;
	private ARHandler arh;
	private AtomicBoolean changed;
	private AtomicBoolean initialized;
	Jikai j;
	TitleLanguage lang;

	/**
	 * A new AnimeListReactionHandler
	 * 
	 * @param gId
	 */
	ALRHandler(long gId, TitleLanguage lang) {
		this.gId = gId;
		this.lang = lang;
		j = Core.JM.get(gId);
		log = LoggerFactory.getLogger(ALRHandler.class.getName() + "#" + gId);
		alrhDB = new ALRHDataBase();
		changed = new AtomicBoolean(false);
		alh = new ALHandler(this, lang);
		arh = new ARHandler(this);
		jData = j.getJikaiData();
		jData.listChannelIdProperty().bindAndSet(tcId);
		initialized = new AtomicBoolean(false);
		j.setALRHandler(this, lang);
	}

	/**
	 * To be called by the EventListener. <br>
	 * Handle a MessageReactionAddEvent
	 * 
	 * @param event
	 *            - the event to handle
	 */
	public void handleReactionAdded(GuildMessageReactionAddEvent event) {
		if (!alh.isSending()) {
			arh.handleReactionAdded(event);
		}
	}

	/**
	 * To be called by the EventListener. <br>
	 * Handle a MessageReactionRemoveEvent
	 * 
	 * @param event
	 *            - the event to handle
	 */
	public void handleReactionRemoved(GuildMessageReactionRemoveEvent event) {
		if (!alh.isSending()) {
			arh.handleReactionRemoved(event);
		}
	}

	/**
	 * To be called by the EventListener. <br>
	 * Handle a MessageReactionRemoveAllEvent
	 * 
	 * @param event
	 *            - the event to handle
	 */
	public void handleReactionRemovedAll(GuildMessageReactionRemoveAllEvent event) {
		if (!alh.isSending()) {
			arh.handleReactionRemovedAll(event);
		}
	}

	/**
	 * Send the anime list.
	 */
	public CompletableFuture<Void> sendList() {
		if (j.hasListChannelSet(lang)) {
			try {
				CompletableFuture<Void> send = BotUtils.retryFuture(2, () -> {
					try {
						return alh.sendListNew();
					} catch (Exception e1) {
						return CompletableFuture.failedFuture(e1);
					}
				});
				try {
					TextChannel info = j.getInfoChannel();
					JikaiLocale loc = j.getLocale();
					info.sendMessage(loc.getString("g_list_send")).submit().thenAccept(m -> {
						Instant start = Instant.now();
						send.whenComplete((v, e) -> {
							if (e == null) {
								m.editMessage(loc.getStringFormatted("g_list_done", Arrays.asList("time"), BotUtils.formatMillis(Duration.between(start, Instant.now()).toMillis(), loc))).queue();
							} else {
								alh.setSending(false);
								m.editMessage(loc.getString("g_list_fail")).queue();
								BotUtils.sendToDev("Failed sending the list after two retries, check the logs please!");
							}
						});
					});
				} catch (Exception e) {
					// no info channel
				}
				return send;
			} catch (Exception e) {
				return CompletableFuture.failedFuture(e);
			}
		}
		log.debug("List channel hasn't been set, not sending list");
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void init() {
		log.debug("Initializing...");
		// jData.animeChannelIdProperty().bindAndSet(tcId);
		jData.listChannelIdProperty().bindAndSet(tcId);
		if (alrhDB.getData().size() != AnimeDB.size()) {
			log.debug("Saved ALRHData doesn't fit loaded anime");
			sendList();
		} else {
			if (!checkIfListValid()) {
				log.debug("List is invalid, resending it!");
				BotUtils.clearChannel(Core.JDA.getGuildById(gId).getTextChannelById(tcId.get()));
				alrhDB.clearData();
				sendList();
			}
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

	private boolean checkIfListValid() {
		Guild g = Core.JDA.getGuildById(gId);
		long tcId = alrhDB.getSentTextChannelId();
		if (tcId != 0 && g != null) {
			TextChannel tc = g.getTextChannelById(alrhDB.getSentTextChannelId());
			if (tc != null) {
				Pair<String, Long> seasonMsg = alrhDB.getSeasonMsg();
				if (seasonMsg != null) {
					try {
						Message msg = tc.retrieveMessageById(seasonMsg.getRight()).submit().exceptionally(e -> {
							return null;
						}).get();
						if (msg != null) {
							for (Entry<Long, Set<ALRHData>> entry : alrhDB.getMsgIdDataMap().entrySet()) {
								try {
									Message m = tc.retrieveMessageById(entry.getKey()).submit().get();
									return arh.validateReactions(m, entry.getValue());
								} catch (InterruptedException | ExecutionException e) {
									return false;
								}
							}
						}
					} catch (InterruptedException | ExecutionException e1) {
						log.error("", e1);
					}
				}
			}
		}
		return false;
	}

	boolean roleExists(Guild g, String name) {
		return !g.getRolesByName(name, false).isEmpty();
	}

	private boolean haveMessagesChanged(TextChannel oldTc, List<String> ids) {
		Set<DTO> amsgs = Core.JM.getALHRM().getListMessages(lang);
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
		return noMsg.get() || ids.size() != amsgs.size() || oldTc == null || !oldTc.getId().equals(tcId);
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

	void setMsgIdTitleMap(Map<Long, String> map) {
		alrhDB.setMsgIdEmbedTitleMap(map);
	}

	public Set<ALRHData> getData() {
		return alrhDB.getData();
	}

	public Map<Long, String> getMessageIdTitleMap() {
		return alrhDB.getMsgIdTitleMap();
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	void update(AnimeUpdate au) {
		arh.update(au);
		alh.update(au);
	}

	void setSeasonMsg(Pair<String, Long> msg) {
		alrhDB.setSeasonMsg(msg);
	}

	public Pair<String, Long> getSeasonMsg() {
		return alrhDB.getSeasonMsg();
	}

}
