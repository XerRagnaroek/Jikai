package com.github.xerragnaroek.jikai.user;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
public class PrivateList extends ListenerAdapter {
    private static long listDuration = 60;
    private static final String subbed = ":white_check_mark:";
    private static final String notSubbed = ":x:";
    private final SortedMap<Long, Map<String, Integer>> msgReactionMap;
    private final JikaiUser ju;
    private final String title;
    private final String listThumb;
    private final Logger log;
    private boolean flip;
    private long firstMsgId;
    private final Set<Runnable> runOnExpire = Collections.synchronizedSet(new HashSet<>());

    public PrivateList(JikaiUser ju, String title, String listThumb) {
        this.ju = ju;
        this.title = title;
        this.listThumb = listThumb;
        msgReactionMap = new TreeMap<>();
        log = LoggerFactory.getLogger(PrivateList.class.getCanonicalName() + "#" + ju.getId());
    }

    public PrivateList(JikaiUser ju, String title, String listThumb, boolean flipped) {
        this(ju, title, listThumb);
        flip = flipped;
    }

    public void sendList(Set<Integer> anime) {
        Set<Anime> a = anime.stream().map(AnimeDB::getAnime).collect(Collectors.toCollection(() -> new TreeSet<>(Anime.IGNORE_TITLE_CASE)));
        List<Map<String, Anime>> cpToAnime = new ArrayList<>();
        BotUtils.partitionCollection(a, 10).forEach(l -> {
            Map<String, Anime> map = new TreeMap<>();
            for (int i = 0; i < l.size(); i++) {
                map.put("U+3" + (i) + "U+fe0fU+20e3", l.get(i));
            }
            cpToAnime.add(map);
        });
        ju.getUser().openPrivateChannel().submit().thenAccept(pc -> {
            List<CompletableFuture<?>> sync = new ArrayList<>(cpToAnime.size());
            for (int i = 0; i < cpToAnime.size(); i++) {
                boolean first = i == 0;
                Map<String, Anime> map = cpToAnime.get(i);
                EmbedBuilder eb = buildEmbed(map);
                eb.setTitle(title + (cpToAnime.size() > 1 ? " " + (i + 1) + "/" + cpToAnime.size() : ""));
                sync.add(pc.sendMessageEmbeds(eb.build()).submit().thenAccept(m -> {
                    if (first) {
                        firstMsgId = m.getIdLong();
                    }
                    List<CompletableFuture<?>> cfs = new ArrayList<>(map.size());
                    log.debug("adding {} reactions", map.size());
                    map.keySet().forEach(s -> cfs.add(m.addReaction(Emoji.fromUnicode(s)).submit()));
                    // wait for all reactions to be added
                    CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[0])).thenAccept(v -> {
                        log.debug("reactions added");
                        Map<String, Integer> cpAni = new TreeMap<>();
                        map.forEach((s, an) -> {
                            cpAni.put(s, an.getId());
                        });
                        msgReactionMap.put(m.getIdLong(), cpAni);
                    }).join();
                }));
            }
            CompletableFuture.allOf(sync.toArray(new CompletableFuture<?>[0])).thenAccept(v -> {
                registerListener();
                addTimeLimitMsg(pc);
            });
        });
    }

    public long getFirstMessageId() {
        return firstMsgId;
    }

    public void runOnExpire(Runnable run) {
        runOnExpire.add(run);
    }

    private EmbedBuilder buildEmbed(Map<String, Anime> m) {
        EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
        StringBuilder bob = new StringBuilder();
        m.forEach((s, a) -> bob.append(String.format("%s: %s: [**%s**](%s)\n", BotUtils.processUnicode(s), ju.isSubscribedTo(a) ? subbed : notSubbed, (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())), a.getAniUrl())));
        eb.setDescription(bob);
        if (listThumb != null && !listThumb.isBlank()) {
            eb.setThumbnail(listThumb);
        }
        return eb;
    }

    private void registerListener() {
        log.debug("Registering listener");
        Core.JDA.addEventListener(this);
        Core.EXEC.schedule(() -> expire(true), listDuration, TimeUnit.MINUTES);
    }

    private void addTimeLimitMsg(PrivateChannel pc) {
        pc.retrieveMessageById(msgReactionMap.lastKey()).flatMap(m -> {
            EmbedBuilder eb = new EmbedBuilder(m.getEmbeds().get(0));
            eb.appendDescription(ju.getLocale().getStringFormatted("ju_eb_private_list_timed", List.of("time"), BotUtils.formatMinutes(listDuration, ju.getLocale())));
            return m.editMessageEmbeds(eb.build());
        }).submit();
    }

    public void expire(boolean runOnExpireRunnables) {
        Core.JDA.removeEventListener(this);
        log.debug("PrivateList expired, removed listener after {} minutes", listDuration);
        ju.getUser().openPrivateChannel().flatMap(pc -> pc.retrieveMessageById(msgReactionMap.lastKey())).flatMap(m -> {
            MessageEmbed me = m.getEmbeds().get(0);
            String[] content = me.getDescription().split("\n");
            content[content.length - 1] = ju.getLocale().getString("ju_eb_private_list_exp");
            return m.editMessageEmbeds(new EmbedBuilder(me).setDescription(String.join("\n", content)).build());
        }).submit().thenAccept(m -> log.debug("last list message edited"));
        log.debug("Running {} runOnExpire runnables", runOnExpire.size());
        if (runOnExpireRunnables) {
            runOnExpire.forEach(Runnable::run);
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        handleReactionEvent(event, true);
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        handleReactionEvent(event, false);
    }

    private void handleReactionEvent(GenericMessageReactionEvent event, boolean add) {
        if (event.getChannel().getType() == ChannelType.PRIVATE && event.getUserIdLong() == ju.getId()) {
            msgReactionMap.computeIfPresent(event.getMessageIdLong(), (l, m) -> {
                m.computeIfPresent(event.getEmoji().asUnicode().getAsCodepoints(), (s, i) -> {
                    if (!flip) {
                        if (add) {
                            sub(i, event);
                        } else {
                            unsub(i, event);
                        }
                    } else {
                        if (add) {
                            unsub(i, event);
                        } else {
                            sub(i, event);
                        }
                    }
                    return i;
                });
                return m;
            });
        }
    }

    private void sub(int id, GenericMessageReactionEvent event) {
        if (ju.subscribeAnime(id, ju.getLocale().getString("ju_private_list_sub_cause"))) {
            event.getChannel().retrieveMessageById(event.getMessageIdLong()).submit().thenAccept(msg -> editListMsg(id, msg, true));
        }
    }

    private void unsub(int id, GenericMessageReactionEvent event) {
        if (ju.unsubscribeAnime(id, ju.getLocale().getString("ju_private_list_unsub_cause"))) {
            event.getChannel().retrieveMessageById(event.getMessageIdLong()).submit().thenAccept(msg -> editListMsg(id, msg, false));
        }
    }

    private void editListMsg(int aniId, Message m, boolean subscribed) {
        Anime a = AnimeDB.getAnime(aniId);
        if (a != null) {
            MessageEmbed me = m.getEmbeds().get(0);
            EmbedBuilder eb = new EmbedBuilder(me);
            eb.setDescription(me.getDescription().replace(String.format("%s: [**%s**]", subscribed ? notSubbed : subbed, (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))), String.format("%s: [**%s**]", subscribed ? subbed : notSubbed, (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())))));
            m.editMessageEmbeds(eb.build()).submit();
        }

    }

    public static void setListDuration(long mins) {
        listDuration = mins;
    }
}
