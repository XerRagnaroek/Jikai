package com.github.xerragnaroek.jikai.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * 
 */
public class EpisodeTracker {

	public static final String WATCHED_EMOJI_UNICODE = "U+1f440";
	public static final String WATCHED_EMOJI_CP = EncodingUtil.encodeCodepoints(WATCHED_EMOJI_UNICODE);
	private JikaiUser ju;
	Map<Integer, Map<Long, Integer>> episodes = Collections.synchronizedMap(new TreeMap<>());
	private Map<Long, Integer> idAnime = new TreeMap<>();

	private final Logger log;

	EpisodeTracker(JikaiUser ju) {
		this.ju = ju;
		log = LoggerFactory.getLogger(EpisodeTracker.class.getCanonicalName() + "#" + ju.getId());
	}

	public void registerEpisode(Anime a, long id) {
		registerEpisodeDetailed(a.getId(), id, a.getNextEpisodeNumber());
	}

	public void registerEpisodeDetailed(int animeId, long msgId, int epNum) {
		log.debug("Registering new episode: anime={},msgId={},epNum={}", animeId, msgId, epNum);
		episodes.compute(animeId, (l, m) -> {
			if (m == null) {
				m = new TreeMap<>();
			}
			m.put(msgId, epNum);
			return m;
		});
		idAnime.put(msgId, animeId);
	}

	public void episodeWatched(long id) {
		int anime = idAnime.get(id);
		idAnime.remove(id);
		episodes.compute(anime, (a, m) -> {
			m.computeIfPresent(id, (l, epNum) -> {
				boolean lastEp = wasLastEpisode(anime, epNum);
				log.debug("User watched {} episode {} of {}", lastEp ? "final" : "", epNum, a);
				if (ju.getAniId() > 0 && JikaiUserAniTokenManager.hasToken(ju)) {
					try {
						String token = JikaiUserAniTokenManager.getAniToken(ju.getAniId()).getAccessToken();
						JASA jasa = AnimeDB.getJASA();
						int mediaListEntryId = 0;
						try {
							mediaListEntryId = jasa.getMediaListEntryIdForUserFromAniId(ju.getAniId(), anime);
						} catch (AniException e) {
							if (e.getStatusCode() == 404) {
								mediaListEntryId = jasa.addToUserCurrentList(token, anime);
							}
						}
						updateAniListProgress(token, mediaListEntryId, anime, epNum);
						if (lastEp) {
							jasa.updateMediaListEntryToCompletedList(token, mediaListEntryId);
							log.debug("Moved {} to completed list!", anime);
						}
					} catch (AniException | IOException e) {
						BotUtils.logAndSendToDev(log, "", e);
					}
				}
				return null;
			});
			return m.isEmpty() ? null : m;
		});
	}

	private boolean wasLastEpisode(int animeId, int ep) {
		try {
			Anime a = AnimeDB.loadAnimeViaId(animeId).get(0);
			return a.getEpisodes() == ep;
		} catch (AniException | IOException e) {
			BotUtils.logAndSendToDev(log, "", e);
		}
		return false;
	}

	private void updateAniListProgress(String token, int mediaListEntryId, int animeId, int ep) throws IOException, AniException {
		if (ju.getAniId() > 0) {
			JASA jasa = AnimeDB.getJASA();
			jasa.updateMediaListEntryProgress(token, mediaListEntryId, ep);
			log.debug("Updated progress on ani list to ep {} for anime {}", ep, animeId);
		}
	}

	public void handleEmojiReacted(PrivateMessageReactionAddEvent event) {
		User u = event.getUser();
		if (!u.isBot()) {
			long msgId = event.getMessageIdLong();
			log.debug("Handling emoji reacted for msg {}", msgId);
			if (idAnime.containsKey(msgId)) {
				JikaiUser ju = JikaiUserManager.getInstance().getUser(u.getIdLong());
				JikaiLocale jLoc = ju.getLocale();
				event.getChannel().retrieveMessageById(msgId).flatMap(m -> {
					log.debug("Editing release notify message to show that user watched it!");
					MessageEmbed me = m.getEmbeds().get(0);
					EmbedBuilder bob = new EmbedBuilder(me);
					bob.setDescription(me.getDescription() + "\n" + jLoc.getStringFormatted("ju_eb_notify_release_watched", Arrays.asList("date"), BotUtils.getTodayDateForJUserFormatted(ju)));
					return m.editMessage(bob.build());
				}).submit().thenAccept(m -> {
					log.debug("Successfully edited msg: {}", m.getId());
					episodeWatched(msgId);
				});
			} else {
				log.debug("Msg isn't a registered release notify message");
			}
		}
	}

	public List<EmbedBuilder> makeEpisodeList() {
		long pcId = ju.getUser().openPrivateChannel().complete().getIdLong();
		List<Pair<Anime, List<String>>> aniStrings = episodes.keySet().stream().map(id -> formatAnimeEpisodes(pcId, id)).sorted((p1, p2) -> Anime.IGNORE_TITLE_CASE.compare(p1.getLeft(), p2.getLeft())).collect(Collectors.toList());
		List<EmbedBuilder> ebs = new ArrayList<>(aniStrings.size());
		aniStrings.forEach(p -> {
			List<String> eps = p.getRight();
			List<List<String>> chunked = new ArrayList<>(BotUtils.partitionCollection(eps, 25));
			for (int i = 0; i < chunked.size(); i++) {
				EmbedBuilder eb = BotUtils.embedBuilder();
				Anime a = p.getLeft();
				eb.setTitle("**" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + (chunked.size() == 1 ? "" : " [" + (i + 1) + "/" + chunked.size() + "]") + "**", p.getLeft().getAniUrl());
				eb.setThumbnail(p.getLeft().getBiggestAvailableCoverImage());
				chunked.get(i).forEach(s -> eb.addField("", s, true));
				ebs.add(eb);
			}
		});
		return ebs;
		/*
		 * List<List<String>> splitMsgs = new ArrayList<>();
		 * List<String> msg = new ArrayList<>();
		 * Iterator<String> it = aniStrings.iterator();
		 * while (it.hasNext()) {
		 * String cur = it.next();
		 * charC += cur.length();
		 * splitMsgs.add(msg);
		 * msg = new ArrayList<>();
		 * charC = 0;
		 * }
		 * msg.add(cur);
		 * }
		 * splitMsgs.add(msg);
		 * List<EmbedBuilder> ebs = new ArrayList<>();
		 * for (int c = 0; c < splitMsgs.size(); c++) {
		 * msg = splitMsgs.get(c);
		 * EmbedBuilder eb = BotUtils.embedBuilder();
		 * eb.setTitle(loc.getString("ju_ep_tracker_title") + (splitMsgs.size() > 1 ? " " + c + "/" +
		 * splitMsgs.size() : ""));
		 * String desc = String.join("\n", msg);
		 * log.debug("Desc length {}", desc.length());
		 * eb.setDescription(desc);
		 * ebs.add(eb);
		 * }
		 * EmbedBuilder eb = BotUtils.embedBuilder();
		 * if (!aniStrings.isEmpty()) {
		 * eb.appendDescription(aniStrings.get(0));
		 * for (int i = 1; i < aniStrings.size(); i++) {
		 * String s = aniStrings.get(i);
		 * if (eb.getDescriptionBuilder().length() + s.length() <= 2048) {
		 * eb.appendDescription("\n" + s);
		 * } else {
		 * ebs.add(eb);
		 * eb = BotUtils.embedBuilder();
		 * }
		 * }
		 * ebs.add(eb);
		 * for (int i = 0; i < ebs.size(); i++) {
		 * ebs.get(i).setTitle(loc.getString("ju_ep_tracker_title") + (ebs.size() > 1 ? " " + (i + 1) + "/"
		 * + ebs.size() : ""));
		 * }
		 * }
		 * return ebs;
		 */
	}

	private Pair<Anime, List<String>> formatAnimeEpisodes(long pcId, int anime) {
		Anime a = AnimeDB.getAnime(anime);
		if (a == null) {
			try {
				List<Anime> loaded = AnimeDB.loadAnimeViaId(anime);
				if (!loaded.isEmpty()) {
					a = loaded.get(0);
				}
			} catch (AniException | IOException e) {
				log.error("Couldn't separately load anime '{}'", anime, e);
			}
		}
		Map<Integer, Long> msgs = new TreeMap<>();
		episodes.get(anime).forEach((l, i) -> msgs.put(i, l));
		List<String> episodes = new ArrayList<>();
		msgs.keySet().forEach(i -> {
			episodes.add(String.format("**[%02d](%s)**", i, BotUtils.makePrivateMessageLink(pcId, msgs.get(i))));
		});
		return Pair.of(a, episodes);
	}

	public void handleButtonClick(String[] data, ButtonClickEvent event) {
		MDC.put("id", event.getMessageId());
		log.debug("Handling button click");
		// so far nothing happens besides the msg being edited
		JikaiLocale loc = ju.getLocale();
		MessageEmbed me = event.getMessage().getEmbeds().get(0);
		EmbedBuilder bob = new EmbedBuilder(me);
		bob.setDescription(me.getDescription() + "\n" + loc.getStringFormatted("ju_eb_notify_release_watched", Arrays.asList("date"), BotUtils.getTodayDateForJUserFormatted(ju)));
		event.editMessage(new MessageBuilder(bob.build()).build()).queue(v -> {
			log.debug("Message edited!");
			episodeWatched(event.getMessageIdLong());
		});
		MDC.remove("id");
	}

	public static Message addButton(Anime a, MessageEmbed m, boolean test) {
		MessageBuilder mb = new MessageBuilder(m);
		mb.setActionRows(ActionRow.of(Button.secondary("ept:" + a.getId() + ":" + a.getNextEpisodeNumber() + (test ? ":t" : ""), Emoji.fromUnicode(WATCHED_EMOJI_UNICODE))));
		return mb.build();
	}

}
