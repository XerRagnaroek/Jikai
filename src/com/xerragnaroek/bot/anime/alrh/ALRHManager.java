package com.xerragnaroek.bot.anime.alrh;

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

import com.xerragnaroek.bot.anime.base.AnimeBase;
import com.xerragnaroek.bot.anime.base.AnimeDayTime;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * ALRH stands for AnimeListReactionHandler. Manages ALRH instances. Makes the anime list prefab
 * messages.
 * 
 * @author XerRagnaroek
 *
 */
public class ALRHManager extends ListenerAdapter {

	private static final Map<String, List<String>> aniAlph = new TreeMap<>();
	private static final Map<String, ALRHandler> impls = Collections.synchronizedMap(new HashMap<>());
	private static final Set<DTO> listMsgs = new TreeSet<>((d1, d2) -> d1.getMessage().getContentRaw().compareTo(d2.getMessage().getContentRaw()));
	private static final Logger log = LoggerFactory.getLogger(ALRHManager.class);
	private static boolean initialized = false;

	public static ALRHandler getAnimeListReactionHandlerForGuild(Guild g) {
		return getAnimeListReactionHandlerForGuild(g.getId());
	}

	/**
	 * Gets the ALRH associated to the guild or null if none was registered yet.
	 * 
	 * @param g
	 *            - the guild's id
	 * @return - an ALRH
	 */
	public static ALRHandler getAnimeListReactionHandlerForGuild(String g) {
		assertInitialisation();
		log.debug("Getting ALRH for guild {}", g);
		return impls.compute(g, (gid, alrh) -> (alrh = (alrh == null) ? new ALRHandler(gid) : alrh));
	}

	/**
	 * Initialises the manager. Needs to be called prior to any other method calls.
	 */
	public static void init() {
		if (!initialized) {
			mapAnimesToStartingLetter();
			initialized = true;
			initImpls();
		} else {
			throw new IllegalStateException("Already initialized!");
		}
	}

	private static void initImpls() {
		log.debug("Loading ALRHs");
		GuildDataManager.getGuildIds().forEach(id -> {
			getAnimeListReactionHandlerForGuild(id);
		});
	}

	static Set<DTO> getListMessages() {
		if (listMsgs.isEmpty()) {
			aniAlph.forEach((l, list) -> {
				listMsgs.add(getLetterListMessage(l, list));
			});
		}
		return listMsgs;
	}

	/**
	 * Gets the animes' titles mapped to their starting letter
	 * 
	 * @return - a map where a letter is mapped to a list of titles.
	 */
	static Map<String, List<String>> getMappedAnimes() {
		return aniAlph;
	}

	/**
	 * Creates the list message
	 * 
	 * @return - a DataTransferObject (DTO) containing the message and the title mapped to its
	 *         respective unicode
	 */
	private static DTO getLetterListMessage(String letter, List<String> titles) {
		log.debug("Creating list for letter {} with {} titles", letter, titles.size());
		Map<String, String> map = new TreeMap<>();
		MessageBuilder mb = new MessageBuilder();
		mb.append("**" + letter + "**\n");
		//:regional_indicator_a:
		int cp = 0x1F1E6;
		String uni = "";
		for (String t : titles) {
			uni = new String(Character.toChars(cp));
			map.put(EncodingUtil.encodeCodepoints(uni), t);
			mb.append(uni + " : **" + t + "**\n");
			cp++;
		}
		return new DTO(mb.build(), map);
	}

	/**
	 * groups all animes by the first letter in their title
	 */
	private static void mapAnimesToStartingLetter() {
		log.debug("Mapping animes to starting letter");
		List<AnimeDayTime> animes = AnimeBase.getSeasonalAnimes();
		animes.stream().map(a -> a.getAnime().title).forEach(title -> aniAlph.compute(("" + title.charAt(0)).toUpperCase(), (k, v) -> {
			v = (v == null) ? new LinkedList<>() : v;
			v.add(title);
			return v;
		}));
	}

	private static void assertInitialisation() {
		if (!initialized) {
			log.error("ALRHManager hasn't been initialized yet!");
			throw new IllegalStateException("ALRHManager hasn't been initialized yet!");
		}
	}

}

/**
 * Utility class for passing a message and a map of titles and unicodes
 */
class DTO {
	//TODO add the starting letter of the msg as well
	Map<String, String> map;
	Message me;

	DTO(Message message, Map<String, String> emojis) {
		me = message;
		map = emojis;
	}

	Message getMessage() {
		return me;
	}

	Map<String, String> getUnicodeTitleMap() {
		return map;
	}
}
