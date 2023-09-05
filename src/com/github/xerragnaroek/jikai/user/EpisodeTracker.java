package com.github.xerragnaroek.jikai.user;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.internal.utils.EncodingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@JsonAutoDetect(fieldVisibility = Visibility.NONE)
public class EpisodeTracker {

    public static final String WATCHED_EMOJI_UNICODE = "U+1f440";
    public static final String WATCHED_EMOJI_CP = EncodingUtil.encodeCodepoints(WATCHED_EMOJI_UNICODE);
    private JikaiUser ju;
    private Map<Integer, Long> lastWatched = new TreeMap<>();
    Map<Integer, Map<Long, Integer>> episodes = Collections.synchronizedMap(new TreeMap<>());
    private final Map<Long, Integer> idAnime = new TreeMap<>();

    private final Logger log;

    EpisodeTracker(JikaiUser ju) {
        this.ju = ju;
        log = LoggerFactory.getLogger(EpisodeTracker.class.getCanonicalName() + "#" + ju.getId());
    }

    private EpisodeTracker(long id) {
        log = LoggerFactory.getLogger(EpisodeTracker.class.getCanonicalName() + "#" + id);
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
                boolean firstWatch = getLastWatchedTimeStamp(anime) == 0;
                lastWatched.put(a, Instant.now().getEpochSecond());
                log.debug("User watched {} episode {} of {}", lastEp ? "final" : "", epNum, a);
                updateAniList(anime, epNum, firstWatch, lastEp);
                if (lastEp) {
                    ju.unsubscribeAnime(anime, ju.getLocale().getString("ju_sub_rem_cause_finished"));
                }
                return null;
            });
            return m.isEmpty() ? null : m;
        });
    }

    private void updateAniList(int anime, int epNum, boolean firstWatch, boolean lastEp) {
        if (ju.getAniId() > 0 && JikaiUserAniTokenManager.hasToken(ju)) {
            try {
                String token = JikaiUserAniTokenManager.getAniToken(ju.getAniId()).getAccessToken();
                JASA jasa = AnimeDB.getJASA();
                int mediaListEntryId = 0;
                boolean exceptionAdd = false;
                try {
                    mediaListEntryId = jasa.getMediaListEntryIdForUserFromAniId(ju.getAniId(), anime);
                } catch (AniException e) {
                    if (e.getStatusCode() == 404) {
                        if (lastEp) {
                            mediaListEntryId = jasa.addToUserCompletedList(token, anime);
                            log.debug("Moved {} to completed list!", anime);
                            return;
                        }
                        mediaListEntryId = jasa.addToUserCurrentList(token, anime);
                        exceptionAdd = true;
                    }
                }
                if (lastEp) {
                    jasa.updateMediaListEntryToCompletedList(token, mediaListEntryId);
                } else {
                    if (firstWatch && !exceptionAdd) {
                        jasa.updateMediaListEntryToCurrentList(token, mediaListEntryId);
                    }
                    updateAniListProgress(token, mediaListEntryId, anime, epNum);
                }

            } catch (AniException | IOException e) {
                BotUtils.logAndSendToDev(log, "", e);
            }
        }
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

    public List<MessageCreateData> makeEpisodeList() {
        long pcId = ju.getUser().openPrivateChannel().complete().getIdLong();
        List<Pair<Anime, List<String>>> aniStrings = episodes.keySet().stream().map(id -> formatAnimeEpisodes(pcId, id)).sorted((p1, p2) -> Anime.IGNORE_TITLE_CASE.compare(p1.getLeft(), p2.getLeft())).collect(Collectors.toList());
        List<MessageCreateData> msgs = new ArrayList<>(aniStrings.size());
        aniStrings.forEach(p -> {
            List<String> eps = p.getRight();
            List<List<String>> chunked = new ArrayList<>(BotUtils.partitionCollection(eps, 25));
            for (int i = 0; i < chunked.size(); i++) {
                EmbedBuilder eb = BotUtils.embedBuilder();
                Anime a = p.getLeft();
                eb.setTitle("**" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + (chunked.size() == 1 ? "" : " [" + (i + 1) + "/" + chunked.size() + "]") + "**", p.getLeft().getAniUrl());
                eb.setThumbnail(p.getLeft().getBiggestAvailableCoverImage());
                chunked.get(i).forEach(s -> eb.addField("", s, true));
                MessageCreateBuilder mb = new MessageCreateBuilder();
                mb.addEmbeds(eb.build());
                mb.setComponents(ActionRow.of(Button.secondary("ept:del:" + a.getId(), Emoji.fromUnicode("U+274C"))));
                msgs.add(mb.build());
            }
        });
        return msgs;
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

    public void handleButtonClick(String[] data, ButtonInteractionEvent event) {
        MDC.put("id", event.getMessageId());
        log.debug("Handling button click");
        if (data[0].equals("del")) {
            int aid = Integer.parseInt(data[1]);
            episodes.compute(aid, (id, map) -> {
                map.keySet().forEach(idAnime::remove);
                log.debug("Removed mapping for anime {}", id);
                return null;
            });
            event.deferEdit().queue();
            event.getChannel().deleteMessageById(event.getMessageId()).queue();
            dropAnime(aid);
        } else {
            JikaiLocale loc = ju.getLocale();
            MessageEmbed me = event.getMessage().getEmbeds().get(0);
            EmbedBuilder bob = new EmbedBuilder(me);
            bob.setDescription(me.getDescription() + "\n" + loc.getStringFormatted("ju_eb_notify_release_watched", List.of("date"), BotUtils.getTodayDateForJUserFormatted(ju)));
            event.editMessage(MessageEditData.fromEmbeds(bob.build())).queue(v -> {
                log.debug("Message edited!");
                episodeWatched(event.getMessageIdLong());
            });
        }
        MDC.remove("id");
    }

    private void dropAnime(int id) {
        if (ju.getAniId() > 0 && JikaiUserAniTokenManager.hasToken(ju)) {
            log.debug("Dropping {}", id);
            ju.unsubscribeAnime(id, ju.getLocale().getString("ju_ep_tracker_drop"));
            JASA jasa = AnimeDB.getJASA();
            try {
                jasa.updateMediaListEntryToDroppedList(JikaiUserAniTokenManager.getAniToken(ju).getAccessToken(), jasa.getMediaListEntryIdForUserFromAniId(ju.getAniId(), id));
            } catch (AniException | IOException e) {
                BotUtils.logAndSendToDev(log, "Failed dropping anime", e);
            }
        }
    }

    public static MessageCreateData addButton(Anime a, MessageEmbed m, boolean test) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addEmbeds(m);
        mb.addComponents(ActionRow.of(Button.secondary("ept:" + a.getId() + ":" + a.getNextEpisodeNumber() + (test ? ":t" : ""), Emoji.fromUnicode(WATCHED_EMOJI_UNICODE))));
        return mb.build();
    }

    @JsonProperty("juId")
    public long getJikaiUserId() {
        return ju.getId();
    }

    @JsonProperty("episodeMessageData")
    public Map<Integer, Map<Long, Integer>> getEpisodeMessageData() {
        return episodes;
    }

    @JsonProperty("lastWatched")
    public Map<Integer, Long> getLastWatchedMap() {
        return lastWatched;
    }

    public long getLastWatchedTimeStamp(int animeId) {
        return lastWatched.getOrDefault(animeId, 0L);
    }

    @JsonCreator
    static EpisodeTracker of(@JsonProperty("juId") long id, @JsonProperty("episodeMessageData") Map<Integer, Map<Long, Integer>> data, @JsonProperty("lastWatched") Map<Integer, Long> lastWatched) {
        EpisodeTracker et = new EpisodeTracker(id);
        JikaiUserManager jum = JikaiUserManager.getInstance();
        if (!jum.isKnownJikaiUser(id)) {
            LoggerFactory.getLogger(EpisodeTracker.class).error("invalid user id '{}', return null", id);
            return null;
        } else {
            et.ju = jum.getUser(id);
            data.forEach((aid, map) -> {
                map.forEach((mid, ep) -> et.registerEpisodeDetailed(aid, mid, ep));
            });
            et.lastWatched = lastWatched;
            return et;
        }
    }

}
