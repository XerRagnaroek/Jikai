package com.github.xerragnaroek.jikai.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;

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
		registerEpisodeImpl(a.getId(), id, a.getNextEpisodeNumber());
	}

	private void registerEpisodeImpl(int animeId, long msgId, int epNum) {
		log.debug("Registering new episode: anime={},msgId={},epNum={}", animeId, msgId, epNum);
		episodes.compute(animeId, (l, m) -> {
			if (m == null) {
				m = new TreeMap<>();
			}
			m.put(msgId, epNum);
			return m;
		});

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
		JikaiLocale loc = ju.getLocale();
		String pcId = ju.getUser().openPrivateChannel().complete().getId();
		Set<String> aniStrings = episodes.keySet().stream().map(id -> formatAnimeEpisodes(pcId, id)).collect(Collectors.toCollection(() -> new TreeSet<>()));
		List<List<String>> splitMsgs = new ArrayList<>();
		int charC = 0;
		List<String> msg = new ArrayList<>();
		Iterator<String> it = aniStrings.iterator();
		while (it.hasNext()) {
			String cur = it.next();
			charC += cur.length();
			if (charC > 2048) {
				splitMsgs.add(msg);
				msg = new ArrayList<>();
				charC = 0;
			}
			msg.add(cur);
		}
		splitMsgs.add(msg);
		List<EmbedBuilder> ebs = new ArrayList<>();
		for (int c = 0; c < splitMsgs.size(); c++) {
			msg = splitMsgs.get(c);
			EmbedBuilder eb = BotUtils.embedBuilder();
			eb.setTitle(loc.getString("ju_ep_tracker_title") + (splitMsgs.size() > 1 ? " " + c + "/" + splitMsgs.size() : ""));
			eb.setDescription(String.join("\n", msg));
			ebs.add(eb);
		}
		return ebs;
	}

	private String formatAnimeEpisodes(String pcId, int anime) {
		Anime a = AnimeDB.getAnime(anime);
		Map<Long, Integer> msgs = episodes.get(anime);
		Set<String> episodes = new TreeSet<>();
		msgs.keySet().forEach(l -> {
			episodes.add(String.format("**[%02d](https://discord.com/channels/@me/%s/%s)**", msgs.get(l), pcId, l));
		});
		return String.format("__**[%s](%s)**__\n%s", a.getTitle(ju.getTitleLanguage()), a.getAniUrl(), String.join(", ", episodes));
	}

	public static EpisodeTracker getTracker(JikaiUser ju) {
		return tracker.compute(ju.getId(), (key, t) -> t == null ? new EpisodeTracker(ju) : t);
	}

	public static void removeTracker(JikaiUser ju) {
		tracker.remove(ju.getId());
	}

	public static void loadOld(Set<Long> ids) {
		ids.forEach(l -> {
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
					if (a != null) {
						String[] split = ju.getLocale().getString("ju_eb_notify_release_desc").split("%episodes%");
						int episode = Integer.parseInt(m.getEmbeds().get(0).getDescription().replace(split[0], "").replace(split[1], "").split("/")[0].trim());
						getTracker(ju).registerEpisodeImpl(a.getId(), l, episode);
					}
				} catch (ErrorResponseException e) {
					// ignore
				}
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
			EpisodeTracker et = getTracker(JikaiUserManager.getInstance().getUser(l));
			m.forEach((aniId, idEpMap) -> {
				idEpMap.forEach((msgId, epNum) -> et.registerEpisodeImpl(aniId, msgId, epNum));
			});
		});
	}
}
