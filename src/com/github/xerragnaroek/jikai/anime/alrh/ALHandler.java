package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Handles the sending and updating of the anime list. <br>
 * Only used by the ALRHandler.
 * 
 * @author XerRagnaroek
 */
class ALHandler {
	private ALRHandler alrh;
	private final Logger log;
	private ALRHDataBase alrhDB;
	private AtomicBoolean sending = new AtomicBoolean(false);

	ALHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ALHandler.class.getName());
		alrhDB = alrh.alrhDB;
	}

	/**
	 * Whether the list is currently being sent, or not.
	 * 
	 * @return true - if the list is currently being sent <br>
	 *         false - nothing is being sent
	 */
	boolean isSending() {
		return sending.get();
	}

	void setSending(boolean val) {
		sending.set(val);
	}

	/**
	 * Send the anime list.<br>
	 * Deletes the old list (if existing) and replaces it with the new one. <br>
	 * Once all RestActions have successfully completed, ALRH is set to updated.
	 * 
	 * @throws Exception
	 */
	CompletableFuture<Void> sendList() throws Exception {
		MDC.put("id", String.valueOf(alrh.gId));
		sending.set(true);
		Set<DTO> dtos = Core.JM.getALHRM().getListMessages();
		log.info("Deleting old messages and data");
		TextChannel tc = alrh.j.getGuild().getTextChannelById(alrhDB.getSentTextChannelId());
		if (tc != null) {
			BotUtils.clearChannel(tc);
		}
		alrhDB.clearUcMsgMap();
		TextChannel lc = alrh.j.getListChannel();
		log.info("Sending list messages to channel {}", lc.getName() + "#" + alrh.tcId);
		CompletableFuture<?> seasonMsg = lc.sendMessage(alrh.j.getLocale().getStringFormatted("g_list_season", Arrays.asList("season"), Core.CUR_SEASON.get())).submit();
		List<CompletableFuture<?>> cfs = dtos.stream().map(dto -> handleDTO(lc, dto)).collect(Collectors.toList());
		cfs.add(seasonMsg);
		return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).thenAccept(v -> {
			alrh.dataChanged();
			// successes.set(0);
			alrh.jData.save(true);
			sending.set(false);
		});
	}

	CompletableFuture<Void> sendListNew() throws Exception {
		MDC.put("id", String.valueOf(alrh.gId));
		log.debug("Sending list...");
		sending.set(true);
		List<DTO> dtos = new ArrayList<>();
		dtos.addAll(Core.JM.getALHRM().getListMessages());
		Collections.sort(dtos);
		TextChannel tc = handleListChannelChanged();
		List<CompletableFuture<?>> cfs = new ArrayList<>();
		Pair<String, Long> seasonMsg = alrhDB.getSeasonMsg();
		boolean resendAll = true;
		log.debug("Checking if season msg exists...");
		if (seasonMsg != null && seasonMsg.getRight() != null) {
			Message msg = tc.retrieveMessageById(seasonMsg.getRight()).submit().exceptionally(e -> {
				return null;
			}).get();
			resendAll = msg == null;
		}
		log.debug("Season msg exists: {}", !resendAll);
		if (resendAll) {
			log.debug("No seasons msg present, resending the whole list!");
			BotUtils.clearChannel(tc);
			alrhDB.clearData();
			cfs.add(tc.sendMessage(alrh.j.getLocale().getStringFormatted("g_list_season", Arrays.asList("season"), Core.CUR_SEASON.get())).submit().thenAccept(m -> alrhDB.setSeasonMsg(Pair.of(Core.CUR_SEASON.get(), m.getIdLong()))));
			cfs.addAll(dtos.stream().map(dto -> handleDTONew(tc, dto, false)).collect(Collectors.toList()));
		} else {
			if (seasonMsg != null && !seasonMsg.getLeft().equals(Core.CUR_SEASON.get())) {
				log.debug("Editing season msg!");
				cfs.add(tc.editMessageById(seasonMsg.getRight(), alrh.j.getLocale().getStringFormatted("g_list_season", Arrays.asList("season"), Core.CUR_SEASON.get())).submit().thenAccept(m -> alrhDB.setSeasonMsg(Pair.of(Core.CUR_SEASON.get(), m.getIdLong()))));
			}
			findObsoleteMsgs(dtos).forEach(p -> {
				log.debug("Deleting obsolete message '{}',{}", p.getLeft(), p.getRight());
				cfs.add(tc.deleteMessageById(p.getRight()).submit());
				alrhDB.removeDataForMessage(p.getRight(), p.getLeft());
			});
			List<String> newM = findNewMessages(dtos);
			if (!newM.isEmpty()) {
				log.debug("New messages: {}", newM.size());
				int latestIndex = -1;
				String newestTitle = newM.get(0);
				Map<String, DTO> dtoMapped = dtos.stream().collect(Collectors.toMap(dto -> dto.getMessage().getTitle(), dto -> dto));
				Set<String> oldTitles = alrhDB.getMsgIdTitleMap().values();
				// if newestTitle is lexographically after the first oldTitle go through all old ones to find the
				// point after which the list has to be resent
				if (newestTitle.compareTo(oldTitles.iterator().next()) > 1) {
					for (String str : alrhDB.getMsgIdTitleMap().values()) {
						if (str.compareTo(newestTitle) < 1) {
							int index = dtos.indexOf(dtoMapped.get(str));
							latestIndex = latestIndex <= index ? index : latestIndex;
						}
					}
				}
				// +1 cause lists
				if (latestIndex >= 0) {
					// split DTO on latestIndex: everything up to and including it can be edited, the rest has to be
					// sent.
					List<DTO> edit = dtos.subList(0, latestIndex + 1);
					log.debug("Editing {} messages prior to the first new message", edit.size());
					edit.forEach(dto -> cfs.add(handleDTONew(tc, dto, true)));
				}
				List<DTO> sendNew = new ArrayList<>(dtos.subList(latestIndex + 1, dtos.size()));
				Collections.sort(sendNew);
				List<CompletableFuture<?>> deleteFs = new ArrayList<>();
				sendNew.forEach(dto -> {
					String title = dto.getMessage().getTitle();
					if (alrhDB.hasMsgIdForEmbedTitle(title)) {
						deleteFs.add(tc.deleteMessageById(alrhDB.getMsgIdForEmbedTitle(title)).submit().thenAccept(v -> log.debug("Successfully deleted msg '{}'", title)));
					}
				});
				// wait for all old msgs to be deleted
				CompletableFuture.allOf(deleteFs.toArray(new CompletableFuture[deleteFs.size()])).join();
				sendNew.forEach(dto -> cfs.add(handleDTONew(tc, dto, false)));
			} else {
				log.debug("No new messages, editing all messages");
				dtos.forEach(dto -> cfs.add(handleDTONew(tc, dto, true)));
			}
		}
		MDC.remove("id");
		return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).orTimeout(5, TimeUnit.MINUTES).whenComplete((v, e) -> {
			if (e == null) {
				log.debug("List sent successfully!");
				alrh.dataChanged();
				alrh.jData.save(true);
			}
			sending.set(false);
		});
	}

	private CompletableFuture<Void> handleDTONew(TextChannel tc, DTO dto, boolean edit) {
		MessageEmbed me = dto.getMessage();
		Set<ALRHData> data = dto.getALRHData();
		CompletableFuture<Message> msg = null;
		if (edit) {
			log.debug("Editing list msg '{}'", me.getTitle());
			msg = tc.editMessageById(alrhDB.getMsgIdForEmbedTitle(me.getTitle()), me).submit().thenApply(m -> {
				MDC.put("id", String.valueOf(alrh.gId));
				log.debug("Edited msg, clearing reactions");
				m.clearReactions().submit().join();
				log.debug("Cleared reactions.");
				MDC.remove("id");
				return m;
			});
		} else {
			log.debug("Sending new msg '{}'", me.getTitle());
			msg = tc.sendMessage(me).submit();
		}

		return msg.<CompletableFuture<Void>>thenApply(m -> {
			alrhDB.mapEmbedTitleToId(m.getIdLong(), me.getTitle());
			List<CompletableFuture<Void>> cfs = new ArrayList<>(data.size());
			data.stream().forEachOrdered(alrhd -> {
				MDC.put("id", String.valueOf(alrh.gId));
				if (alrhDB.isReacted(alrhd)) {
					alrhd.setReacted(true);
				}
				alrhd.setTextChannelId(tc.getIdLong());

				String uniCP = alrhd.getUnicodeCodePoint();
				log.debug("Adding reaction {} to message", uniCP);
				cfs.add(m.addReaction(uniCP).submit().thenAccept(v -> log.debug("Added reaction {} to message", uniCP)));
				MDC.remove("id");
			});
			alrhDB.setDataForMessage(m.getIdLong(), data);
			return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
		}).thenAccept(CompletableFuture::join);
	}

	private List<Pair<String, Long>> findObsoleteMsgs(List<DTO> dtos) {
		List<String> titles = dtos.stream().map(dto -> dto.getMessage().getTitle()).collect(Collectors.toList());
		BidiMap<Long, String> msgIdTM = alrhDB.getMsgIdTitleMap();
		Set<String> obslt = new TreeSet<>(alrhDB.getMsgIdTitleMap().values());
		obslt.removeAll(titles);
		// return obslt.stream().peek(t -> log.debug("Obsolete message: '{}'", t)).map(t -> Pair.of(t,
		// msgIdTM.getKey(t))).collect(Collectors.toList());
		// return dtos.stream().filter(dto -> obslt.contains(dto.getMessage().getTitle())).peek(dto ->
		// log.debug("Obsolete message: '{}'", dto.getMessage().getTitle())).map(dto -> Pair.of(dto,
		// msgIdTM.getKey(dto.getMessage().getTitle()))).collect(Collectors.toList());
		return obslt.stream().map(t -> Pair.of(t, alrhDB.getMsgIdForEmbedTitle(t))).collect(Collectors.toList());
	}

	private List<String> findNewMessages(List<DTO> dtos) {
		List<String> titles = dtos.stream().map(dto -> dto.getMessage().getTitle()).collect(Collectors.toList());
		BidiMap<Long, String> msgIdTM = alrhDB.getMsgIdTitleMap();
		return titles.stream().filter(t -> !msgIdTM.containsValue(t)).sorted().peek(t -> log.debug("New message: '{}'", t)).collect(Collectors.toList());
	}

	/**
	 * Handles the actual sending of the list messages.
	 * 
	 * @param g
	 *            - The Guild to send to
	 * @param tc
	 *            - The TextChannel to send to
	 * @param dto
	 *            - The DTO containing the data to send
	 */
	private CompletableFuture<Void> handleDTO(TextChannel tc, DTO dto) {
		MessageEmbed me = dto.getMessage();
		Set<ALRHData> data = dto.getALRHData();
		log.debug("Sending list message...");
		return tc.sendMessage(me).submit().<CompletableFuture<Void>>thenApply(m -> {
			alrhDB.mapEmbedTitleToId(m.getIdLong(), me.getTitle());
			List<CompletableFuture<Void>> cfs = new ArrayList<>(data.size());
			data.stream().forEachOrdered(alrhd -> {
				if (alrhDB.isReacted(alrhd)) {
					alrhd.setReacted(true);
				}
				alrhd.setTextChannelId(tc.getIdLong());

				String uniCP = alrhd.getUnicodeCodePoint();
				log.debug("Adding reaction {} to message", uniCP);
				cfs.add(m.addReaction(uniCP).submit().thenAccept(v -> log.debug("Added reaction {} to message", uniCP)));
			});
			alrhDB.setDataForMessage(m.getIdLong(), data);
			return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
		}).thenAccept(CompletableFuture::join);
	}

	void update(AnimeUpdate au) {
		MDC.put("id", String.valueOf(alrh.gId));
		if (au.hasNewAnime() || au.hasRemovedAnime()) {
			log.debug("Updating list");
			try {
				alrh.sendList().thenAccept(v -> sendUpdate(au));
			} catch (Exception e) {
				log.error("", e);
			}
		}
		MDC.remove("id");
	}

	private void sendUpdate(AnimeUpdate au) {
		MDC.put("id", String.valueOf(alrh.gId));
		sendNewAnime(au);
		sendRemovedAnime(au);
		MDC.remove("id");
	}

	private void sendNewAnime(AnimeUpdate au) {
		List<Anime> newA = au.getNewAnime();
		if (!newA.isEmpty()) {
			log.debug("Sending {} new anime embeds to anime channel", newA.size());
			for (Anime a : newA) {
				EmbedBuilder eb = new EmbedBuilder();
				BotUtils.addJikaiMark(eb);
				eb.setThumbnail(a.getBiggestAvailableCoverImage());
				try {
					String title = a.getTitle(TitleLanguage.ROMAJI);
					JikaiLocale loc = alrh.j.getLocale();
					eb.setTitle(loc.getStringFormatted("g_eb_new_anime_title", Arrays.asList("title"), title), a.getAniUrl()).setDescription(loc.getStringFormatted("g_eb_new_anime_desc", Arrays.asList("listch", "links"), alrh.j.getListChannel().getAsMention(), BotUtils.formatExternalSites(a)));
					alrh.j.getAnimeChannel().sendMessage(eb.build()).submit().thenAccept(m -> log.debug("Sent embed for {}", title));
				} catch (Exception e) {
					log.error("", e);
				}
			}
		}

	}

	private void sendRemovedAnime(AnimeUpdate au) {
		List<Anime> removedA = au.getRemovedAnime();
		log.debug("Sending {} removed anime embeds to anime channel", removedA.size());
		for (Anime a : removedA) {
			log.debug("{} status: {}", a.getTitleRomaji(), a.getStatus());
			EmbedBuilder eb = new EmbedBuilder();
			BotUtils.addJikaiMark(eb);
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
			try {
				String title = a.getTitle(TitleLanguage.ROMAJI);
				JikaiLocale loc = alrh.j.getLocale();
				eb.setTitle(loc.getStringFormatted("g_eb_rem_anime_title", Arrays.asList("title"), title), a.getAniUrl());
				if (a.isFinished()) {
					eb.setDescription(loc.getString("g_eb_rem_anime_desc_finished"));
				} else {
					log.debug("{} has been removed but isn't finished. NextEpNum={},Episodes={}", a.getTitleRomaji(), a.getNextEpisodeNumber(), a.getEpisodes());
					eb.setDescription(loc.getString("g_eb_rem_anime_desc_unknown"));
				}
				alrh.j.getAnimeChannel().sendMessage(eb.build()).submit().thenAccept(m -> log.debug("Sent embed for {}", title));
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	private TextChannel handleListChannelChanged() throws Exception {
		log.debug("Checking if the list channel changed...");
		long sentChannelId = alrhDB.getSentTextChannelId();
		TextChannel listChannel = alrh.j.getListChannel();
		Guild g = alrh.j.getGuild();
		if (sentChannelId > 0 && sentChannelId != listChannel.getIdLong()) {
			log.debug("List channel has changed, deleting the old list...");
			TextChannel oldChannel = g.getTextChannelById(sentChannelId);
			CompletableFuture.allOf(oldChannel.purgeMessagesById(alrhDB.getAllMessageIdsAsList()).toArray(CompletableFuture<?>[]::new)).whenComplete((v, e) -> {
				if (e == null) {
					log.debug("Successfully deleted the old list!");
				} else {
					BotUtils.logAndSendToDev(log, "Failed deleting the old list", e);
				}
			});
			alrhDB.clearData();
		}
		return listChannel;
	}
}
