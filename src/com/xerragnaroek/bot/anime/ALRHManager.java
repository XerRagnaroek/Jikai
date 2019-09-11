package com.xerragnaroek.bot.anime;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * ALRH stands for AnimeListReactionHandler
 * 
 * @author XerRagnaroek
 *
 */
public class ALRHManager extends ListenerAdapter {

	private static final Map<String, List<String>> aniAlph = new TreeMap<>();
	private static final Set<DTO> listMsgs = new TreeSet<>((d1, d2) -> d1.getMessageEmbed().getTitle().compareTo(d2.getMessageEmbed().getTitle()));
	private static final Logger log = LoggerFactory.getLogger(ALRHManager.class);
	private static final Map<String, ALRHImpl> impls = Collections.synchronizedMap(new HashMap<>());

	public static void init() {
		mapAnimesToStartingLetter();
	}

	static Map<String, List<String>> getMappedAnimes() {
		return aniAlph;
	}

	private static void mapAnimesToStartingLetter() {
		log.debug("Mapping animes to starting letter");
		List<Anime> animes = AnimeBase.getSeasonalAnimes();
		animes.stream().map(a -> a.title).forEach(title -> aniAlph.compute(("" + title.charAt(0)).toUpperCase(), (k, v) -> {
			v = (v == null) ? new LinkedList<>() : v;
			v.add(title);
			return v;
		}));
	}

	static Set<DTO> getListMessages() {
		if (listMsgs.isEmpty()) {
			aniAlph.forEach((l, list) -> {
				listMsgs.add(getLetterListMessage(l, list));
			});
		}
		return listMsgs;
	}

	private static DTO getLetterListMessage(String letter, List<String> titles) {
		log.debug("Creating list for letter {} with {} titles", letter, titles.size());
		Map<String, String> map = new TreeMap<>();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(letter);
		int cp = 0x1F1E6;
		String uni = "";
		for (String t : titles) {
			uni = new String(Character.toChars(cp));
			map.put(uni, t);
			eb.addField(uni + " " + t, "", false);
			cp++;
		}
		return new DTO(eb.build(), map);
	}

	public static ALRHImpl getAnimeListReactionHandlerForGuild(Guild g) {
		return getAnimeListReactionHandlerForGuild(g.getId());
	}

	public static ALRHImpl getAnimeListReactionHandlerForGuild(String g) {
		return impls.compute(g, (gid, alrh) -> (alrh = (alrh == null) ? new ALRHImpl(gid) : alrh));
	}
}

class DTO {
	MessageEmbed me;
	Map<String, String> map;

	DTO(MessageEmbed message, Map<String, String> emojis) {
		me = message;
		map = emojis;
	}

	MessageEmbed getMessageEmbed() {
		return me;
	}

	Map<String, String> getUnicodeTitleMap() {
		return map;
	}
}
