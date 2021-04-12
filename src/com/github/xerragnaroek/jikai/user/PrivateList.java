package com.github.xerragnaroek.jikai.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 
 */
public class PrivateList extends ListenerAdapter {
	private static long listDuration = 60;
	private static String subbed = ":white_check_mark:";
	private static String notSubbed = ":x:";
	private SortedMap<Long, Map<String, Integer>> msgReactionMap;
	private JikaiUser ju;
	private String title;
	private String listThumb;
	private final Logger log;

	public PrivateList(JikaiUser ju, String title, String listThumb) {
		this.ju = ju;
		this.title = title;
		this.listThumb = listThumb;
		msgReactionMap = new TreeMap<>();
		log = LoggerFactory.getLogger(PrivateList.class.getCanonicalName() + "#" + ju.getId());
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
				Map<String, Anime> map = cpToAnime.get(i);
				EmbedBuilder eb = buildEmbed(map);
				eb.setTitle(title + (cpToAnime.size() > 1 ? " " + (i + 1) + "/" + cpToAnime.size() : ""));
				sync.add(pc.sendMessage(eb.build()).submit().thenAccept(m -> {
					List<CompletableFuture<?>> cfs = new ArrayList<>(map.size());
					log.debug("adding {} reactions", map.size());
					map.keySet().forEach(s -> cfs.add(m.addReaction(s).submit()));
					// wait for all reactions to be added
					CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[cfs.size()])).thenAccept(v -> {
						log.debug("reactions added");
						Map<String, Integer> cpAni = new TreeMap<>();
						map.forEach((s, an) -> {
							cpAni.put(s, an.getId());
						});
						msgReactionMap.put(m.getIdLong(), cpAni);
					}).join();
				}));
			}
			CompletableFuture.allOf(sync.toArray(new CompletableFuture<?>[sync.size()])).thenAccept(v -> {
				registerListener();
				addTimeLimitMsg(pc);
			});
		});
	}

	private EmbedBuilder buildEmbed(Map<String, Anime> m) {
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		StringBuilder bob = new StringBuilder();
		m.forEach((s, a) -> bob.append(String.format("%s: %s: [**%s**](%s)\n", BotUtils.processUnicode(s), ju.isSubscribedTo(a) ? subbed : notSubbed, a.getTitle(ju.getTitleLanguage()), a.getAniUrl())));
		eb.setDescription(bob).setThumbnail(listThumb);
		return eb;
	}

	private void registerListener() {
		log.debug("Registering listener");
		Core.JDA.addEventListener(this);
		Core.EXEC.schedule(() -> expired(), listDuration, TimeUnit.MINUTES);
	}

	private void addTimeLimitMsg(PrivateChannel pc) {
		pc.retrieveMessageById(msgReactionMap.lastKey()).flatMap(m -> {
			EmbedBuilder eb = new EmbedBuilder(m.getEmbeds().get(0));
			eb.appendDescription(ju.getLocale().getStringFormatted("ju_eb_private_list_timed", Arrays.asList("time"), BotUtils.formatMinutes(listDuration, ju.getLocale())));
			return m.editMessage(eb.build());
		}).submit();
	}

	private void expired() {
		Core.JDA.removeEventListener(this);
		log.debug("PrivateList expired, removed listener after {} minutes", listDuration);
		ju.getUser().openPrivateChannel().flatMap(pc -> pc.retrieveMessageById(msgReactionMap.lastKey())).flatMap(m -> {
			MessageEmbed me = m.getEmbeds().get(0);
			String[] content = me.getDescription().split("\n");
			content[content.length - 1] = ju.getLocale().getString("ju_eb_private_list_exp");
			return m.editMessage(new EmbedBuilder(me).setDescription(String.join("\n", content)).build());
		}).submit().thenAccept(m -> log.debug("last list message edited"));
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		if (event.getUserIdLong() == ju.getId()) {
			msgReactionMap.computeIfPresent(event.getMessageIdLong(), (l, m) -> {
				m.computeIfPresent(event.getReactionEmote().getAsCodepoints(), (s, i) -> {
					if (ju.subscribeAnime(i, ju.getLocale().getString("ju_private_list_sub_cause"))) {
						event.getChannel().retrieveMessageById(event.getMessageIdLong()).submit().thenAccept(msg -> editListMsg(i, msg, true));
					}
					return i;
				});
				return m;
			});
		}
	}

	@Override
	public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event) {
		if (event.getUserIdLong() == ju.getId()) {
			msgReactionMap.computeIfPresent(event.getMessageIdLong(), (l, m) -> {
				m.computeIfPresent(event.getReactionEmote().getAsCodepoints(), (s, i) -> {
					if (ju.unsubscribeAnime(i, ju.getLocale().getString("ju_private_list_unsub_cause"))) {
						event.getChannel().retrieveMessageById(event.getMessageIdLong()).submit().thenAccept(msg -> editListMsg(i, msg, false));
					}
					return i;
				});
				return m;
			});
		}
	}

	private void editListMsg(int aniId, Message m, boolean subscribed) {
		Anime a = AnimeDB.getAnime(aniId);
		if (a != null) {
			MessageEmbed me = m.getEmbeds().get(0);
			EmbedBuilder eb = new EmbedBuilder(me);
			eb.setDescription(me.getDescription().replace(String.format("%s: [**%s**]", subscribed ? notSubbed : subbed, a.getTitle(ju.getTitleLanguage())), String.format("%s: [**%s**]", subscribed ? subbed : notSubbed, a.getTitle(ju.getTitleLanguage()))));
			m.editMessage(eb.build()).submit();
		}

	}

	public static void setListDuration(long mins) {
		listDuration = mins;
	}
}
