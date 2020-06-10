package com.github.xerragnaroek.jikai.anime.alrh;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
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
	private AtomicInteger successes = new AtomicInteger(0); // Successful RestActions

	ALHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ALHandler.class.getName() + "#" + alrh.gId);
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

	/**
	 * Send the anime list.<br>
	 * Deletes the old list (if existing) and replaces it with the new one. <br>
	 * Once all RestActions have successfully completed, ALRH is set to updated.
	 * 
	 * @throws Exception
	 */
	CompletableFuture<Void> sendList() throws Exception {
		sending.set(true);
		Set<DTO> dtos = Core.JM.getALHRM().getListMessages();
		successes.set(0);
		log.info("Deleting old messages and data");
		TextChannel tc = alrh.j.getGuild().getTextChannelById(alrhDB.getSentTextChannelId());
		if (tc != null) {
			BotUtils.clearChannel(tc);
		}
		alrhDB.clearUcMsgMap();
		TextChannel lc = alrh.j.getListChannel();
		log.info("Sending list messages to channel {}", lc.getName() + "#" + alrh.tcId);
		CompletableFuture<?> seasonMsg = lc.sendMessage("Current season: **" + Core.CUR_SEASON.get() + "**").submit();
		List<CompletableFuture<?>> cfs = dtos.stream().map(dto -> handleDTO(lc, dto)).collect(Collectors.toList());
		cfs.add(seasonMsg);
		return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).thenAccept(v -> {
			alrh.dataChanged();
			// successes.set(0);
			alrh.jData.save(true);
			sending.set(false);
		});
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
		if (au.hasNewAnime() || au.hasRemovedAnime()) {
			log.debug("Updating list");
			try {
				sendList().thenAccept(v -> sendUpdate(au));
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	private void sendUpdate(AnimeUpdate au) {
		sendNewAnime(au);
		sendRemovedAnime(au);
	}

	private void sendNewAnime(AnimeUpdate au) {
		List<Anime> newA = au.getNewAnime();
		log.debug("Sending {} new anime embeds to anime channel", newA.size());
		for (Anime a : newA) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
			try {
				eb.setTitle(a.getTitle(TitleLanguage.ROMAJI), a.getAniUrl()).setDescription("has been added to the list! \n Subscribe to it here:" + alrh.j.getListChannel().getAsMention()).setTimestamp(Instant.now());
				alrh.j.getAnimeChannel().sendMessage(eb.build()).submit().thenAccept(m -> log.debug("Sent embed for {}", a.getTitle(TitleLanguage.ROMAJI)));
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	private void sendRemovedAnime(AnimeUpdate au) {
		List<Anime> removedA = au.getRemovedAnime();
		log.debug("Sending {} removed anime embeds to anime channel", removedA.size());
		for (Anime a : removedA) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setThumbnail(a.getBiggestAvailableCoverImage());
			try {
				eb.setTitle(a.getTitle(TitleLanguage.ROMAJI), a.getAniUrl()).setDescription("has been removed, presumably because it finished airing!").setTimestamp(Instant.now());
				alrh.j.getAnimeChannel().sendMessage(eb.build()).submit().thenAccept(m -> log.debug("Sent embed for {}", a.getTitle(TitleLanguage.ROMAJI)));
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}
}
