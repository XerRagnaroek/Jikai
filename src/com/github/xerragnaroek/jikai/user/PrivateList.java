package com.github.xerragnaroek.jikai.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 
 */
public class PrivateList extends ListenerAdapter {
	private Map<Long, Map<String, Integer>> msgReactionMap;
	private JikaiUser ju;
	private String title;
	private String listThumb;
	private final Logger log = LoggerFactory.getLogger(PrivateList.class);

	public PrivateList(JikaiUser ju, String title, String listThumb) {
		this.ju = ju;
		this.title = title;
		this.listThumb = listThumb;
		msgReactionMap = new TreeMap<>();
		MDC.put("id", "" + ju.getId());
	}

	public void sendList(Set<Integer> anime) {
		Set<Anime> a = anime.stream().map(AnimeDB::getAnime).collect(Collectors.toCollection(() -> new TreeSet<>()));
		List<Map<String, Anime>> cpToAnime = new ArrayList<>();
		BotUtils.partitionCollection(a, 10).forEach(l -> {
			Map<String, Anime> map = new TreeMap<>();
			for (int i = 0; i < l.size(); i++) {
				map.put("U+3" + (i) + "U+fe0fU+20e3", l.get(i));
			}
			cpToAnime.add(map);
		});
		ju.getUser().openPrivateChannel().submit().thenAccept(pc -> {
			for (int i = 0; i < cpToAnime.size(); i++) {
				Map<String, Anime> map = cpToAnime.get(i);
				EmbedBuilder eb = buildEmbed(map);
				eb.setTitle(title + (cpToAnime.size() > 1 ? " " + (i + 1) + "/" + cpToAnime.size() : ""));
				if (i + 1 == cpToAnime.size()) {
					eb.appendDescription(ju.getLocale().getString("ju_eb_private_list_timed"));
				}
				pc.sendMessage(eb.build()).submit().thenAccept(m -> {
					map.keySet().forEach(s -> m.addReaction(s).queue());
					Map<String, Integer> cpAni = new TreeMap<>();
					map.forEach((s, an) -> {
						cpAni.put(s, an.getId());
					});
					msgReactionMap.put(m.getIdLong(), cpAni);
				});
			}
		});
		registerListener();
	}

	private EmbedBuilder buildEmbed(Map<String, Anime> m) {
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		StringBuilder bob = new StringBuilder();
		m.forEach((s, a) -> bob.append(String.format("%s: [**%s**](%s)\n", BotUtils.processUnicode(s), a.getTitle(ju.getTitleLanguage()), a.getAniUrl())));
		eb.setDescription(bob).setThumbnail(listThumb);
		return eb;
	}

	private void registerListener() {
		log.debug("Registering listener");
		Core.JDA.addEventListener(this);
		Core.EXEC.schedule(() -> {
			Core.JDA.removeEventListener(this);
			log.debug("Removed listener after an hour");
			MDC.remove("id");
		}, 1, TimeUnit.HOURS);
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		if (event.getUserIdLong() == ju.getId()) {
			msgReactionMap.computeIfPresent(event.getMessageIdLong(), (l, m) -> {
				m.computeIfPresent(event.getReactionEmote().getAsCodepoints(), (s, i) -> {
					ju.subscribeAnime(i, ju.getLocale().getString("ju_private_list_sub_cause"));
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
					ju.unsubscribeAnime(i, ju.getLocale().getString("ju_private_list_unsub_cause"));
					return i;
				});
				return m;
			});
		}
	}
}
