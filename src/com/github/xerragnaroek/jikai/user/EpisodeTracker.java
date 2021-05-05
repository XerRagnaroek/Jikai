package com.github.xerragnaroek.jikai.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * 
 */
public class EpisodeTracker {

	public static final String WATCHED_EMOJI_UNICODE = "U+1f440";
	public static final String WATCHED_EMOJI_CP = EncodingUtil.encodeCodepoints(WATCHED_EMOJI_UNICODE);
	private static Map<Long, EpisodeTracker> tracker = Collections.synchronizedMap(new TreeMap<>());
	private JikaiUser ju;
	private Map<Integer, Map<Long, Integer>> episodes = Collections.synchronizedMap(new TreeMap<>());
	private Map<Long, Integer> idAnime = new TreeMap<>();

	private final Logger log;

	private EpisodeTracker(JikaiUser ju) {
		this.ju = ju;
		log = LoggerFactory.getLogger(EpisodeTracker.class.getCanonicalName() + "#" + ju.getId());
		ju.getSubscribedAnime().onRemove((i, s) -> {
			episodes.remove(i);
			log.debug("Removed mapping for anime {}", i);
		});
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
				log.debug("User watched episode {} of {}", epNum, a);
				return null;
			});
			return m.isEmpty() ? null : m;
		});
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
					m.unpin().submit().thenAccept(v -> log.debug("msg unpinned"));
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
				eb.setTitle("**" + p.getLeft().getTitle(ju.getTitleLanguage()) + (chunked.size() == 1 ? "" : " [" + (i + 1) + "/" + chunked.size() + "]") + "**", p.getLeft().getAniUrl());
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
		Map<Integer, Long> msgs = new TreeMap<>();
		episodes.get(anime).forEach((l, i) -> msgs.put(i, l));
		List<String> episodes = new ArrayList<>();
		msgs.keySet().forEach(i -> {
			episodes.add(String.format("**[%02d](%s)**", i, BotUtils.makePrivateMessageLink(pcId, msgs.get(i))));
		});
		return Pair.of(a, episodes);
	}

	public static EpisodeTracker getTracker(JikaiUser ju) {
		return tracker.compute(ju.getId(), (key, t) -> t == null ? new EpisodeTracker(ju) : t);
	}

	public static void removeTracker(JikaiUser ju) {
		tracker.remove(ju.getId());
	}

	public static void loadOld(Set<Long> ids) {
		Logger log = LoggerFactory.getLogger(EpisodeTracker.class);
		log.debug("Loading old rmids");
		ids.forEach(l -> {
			Core.EXEC.execute(() -> {
				log.debug("Checking {}", l);
				JikaiUserManager.getInstance().users().forEach(ju -> {
					try {
						Message m = ju.getUser().openPrivateChannel().complete().retrieveMessageById(l).complete();
						// since we're now here that means the message was for this user
						String title = m.getEmbeds().get(0).getTitle();
						title = title.substring(2, title.length() - 2);
						Anime a = AnimeDB.getAnime(title, ju.getTitleLanguage());
						if (a == null) {
							for (TitleLanguage tt : TitleLanguage.values()) {
								if (tt != ju.getTitleLanguage()) {
									a = AnimeDB.getAnime(title, tt);
								}
							}
						}
						log.debug("Found anime: {}", a);
						if (a != null) {
							String[] split = ju.getLocale().getString("ju_eb_notify_release_desc").split("%episodes%");
							int episode = Integer.parseInt(m.getEmbeds().get(0).getDescription().replace(split[0], "").replace(split[1], "").split("/")[0].trim());
							getTracker(ju).registerEpisodeDetailed(a.getId(), l, episode);
						}
					} catch (ErrorResponseException e) {
						log.debug("Message wasn't for user {}", ju.getId());
					}
				});
			});
		});
	}

	public static Map<Long, Map<Integer, Map<Long, Integer>>> getSavableMap() {
		Map<Long, Map<Integer, Map<Long, Integer>>> map = new TreeMap<>();
		tracker.forEach((l, t) -> map.put(l, t.episodes));
		return map;
	}

	public static void load(Map<Long, Map<Integer, Map<Long, Integer>>> map) {
		map.forEach((l, m) -> {
			JikaiUser ju = JikaiUserManager.getInstance().getUser(l);
			EpisodeTracker et = getTracker(ju);
			m.forEach((aniId, idEpMap) -> {
				if (ju.isSubscribedTo(aniId)) {
					idEpMap.forEach((msgId, epNum) -> et.registerEpisodeDetailed(aniId, msgId, epNum));
				}
			});
		});
	}
}
