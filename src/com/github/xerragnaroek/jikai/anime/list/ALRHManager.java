package com.github.xerragnaroek.jikai.anime.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.DetailedAnimeMessageBuilder;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * ALRH stands for AnimeListReactionHandler. Manages ALRH instances. Makes the anime list prefab
 * messages.
 * 
 * @author XerRagnaroek
 */
public class ALRHManager {

	public ALRHManager() {}

	private Map<TitleLanguage, Map<String, List<String>>> aniAlph = new TreeMap<>();
	private Map<TitleLanguage, Set<DTO>> listMsgs = new TreeMap<>();
	private final Logger log = LoggerFactory.getLogger(ALRHManager.class);
	private Map<Long, List<InitData>> initMap = new TreeMap<>();
	private Map<Long, Map<TitleLanguage, ALRHandler>> impls = Collections.synchronizedMap(new HashMap<>());

	public void init() {
		mapAnimesToStartingLetter();
		makeListMessages();
		initImpls();
		AnimeDB.runOnDBUpdate(au -> {
			log.debug("Updating ALRHs");
			update(au);
		});

	}

	public ALRHandler makeNew(Jikai j, TitleLanguage lang) {
		long id = j.getJikaiData().getGuildId();
		ALRHandler impl = new ALRHandler(id, lang);
		putInMap(impl, id, lang);
		return impl;
	}

	private void putInMap(ALRHandler impl, long id, TitleLanguage lang) {
		impls.compute(id, (i, m) -> {
			m = m == null ? new HashMap<>() : m;
			m.put(lang, impl);
			return m;
		});
	}

	private void update(AnimeUpdate au) {
		au.withFilter(a -> !a.isAdult());
		if (au.hasChange()) {
			mapAnimesToStartingLetter();
			makeListMessages();
			if (au.hasNewAnime()) {
				sendNewAnime(au);
			}
			if (au.hasRemovedAnime()) {
				sendRemovedAnime(au);
			}
			if (au.hasCancelledAnime()) {
				sendCancelledAnime(au);
			}
			if (au.hasFinishedAnime()) {
				sendFinishedAnime(au);
			}
			if (au.hasHiatusAnime()) {
				sendHiatusAnime(au);
			}
			if (au.hasChangedReleaseAnime()) {
				sendChangedReleaseAnime(au);
			}
			Core.executeLogException(() -> impls.values().forEach(impl -> impl.values().forEach(imp -> imp.update(au))));
		}
	}

	private void sendNewAnime(AnimeUpdate au) {
		List<Anime> newA = au.getNewAnime();
		log.debug("Sending {} removed anime embeds to anime channel", newA.size());
		JikaiLocale loc = JikaiLocaleManager.getEN();
		sendToAnimeChannel(newA, loc.getString("g_eb_new_anime_desc"), loc);
	}

	private void sendRemovedAnime(AnimeUpdate au) {
		List<Anime> removedA = au.getRemovedAnime();
		log.debug("Sending {} removed anime embeds to anime channel", removedA.size());
		JikaiLocale loc = JikaiLocaleManager.getEN();
		sendToAnimeChannel(removedA, loc.getString("g_eb_rem_anime_desc_unknown"), loc);
	}

	private void sendFinishedAnime(AnimeUpdate au) {
		List<Anime> finished = au.getFinishedAnime();
		log.debug("Sending {} finished anime embeds to anime channels", finished.size());
		JikaiLocale loc = JikaiLocaleManager.getEN();
		sendToAnimeChannel(finished, loc.getString("g_eb_rem_anime_desc_finished"), loc);
	}

	private void sendCancelledAnime(AnimeUpdate au) {
		List<Anime> cancelled = au.getFinishedAnime();
		log.debug("Sending {} cancelled anime embeds to anime channels", cancelled.size());
		JikaiLocale loc = JikaiLocaleManager.getEN();
		sendToAnimeChannel(cancelled, loc.getString("g_eb_cancelled_anime_desc"), loc);
	}

	private void sendHiatusAnime(AnimeUpdate au) {
		List<Anime> cancelled = au.getFinishedAnime();
		log.debug("Sending {} hiatus anime embeds to anime channels", cancelled.size());
		JikaiLocale loc = JikaiLocaleManager.getEN();
		sendToAnimeChannel(cancelled, loc.getString("g_eb_hiatus_anime_desc"), loc);
	}

	private void sendChangedReleaseAnime(AnimeUpdate au) {
		List<Pair<Anime, Long>> changed = au.getChangedReleaseAnime().stream().filter(p -> !p.getLeft().isAdult()).collect(Collectors.toList());
		log.debug("Sending {} changed releaseanime embeds to anime channels", changed.size());
		JikaiLocale loc = JikaiLocaleManager.getEN();
		changed.forEach(p -> {
			DetailedAnimeMessageBuilder damb = new DetailedAnimeMessageBuilder(p.getLeft(), Core.EUROPE_BERLIN, loc);
			damb.ignoreEmptyFields().withAll(false);
			long time = p.getRight();
			String days = BotUtils.formatSeconds(Math.abs(time), loc);
			if (time > 0) {
				damb.setDescription(loc.getString("g_eb_changed_rel_anime_desc") + loc.getStringFormatted("g_eb_changed_rel_later_anime_desc", Arrays.asList("days"), days));
			} else if (time < 0) {
				damb.setDescription(loc.getString("g_eb_changed_rel_anime_desc") + loc.getStringFormatted("g_eb_changed_rel_earlier_anime_desc", Arrays.asList("days"), days));
			} else {
				damb.setDescription(loc.getString("g_eb_changed_rel_anime_desc") + loc.getString("g_eb_changed_rel_unk_anime_desc"));
			}
			try {
				BotUtils.sendToAllAnimeChannels(damb.build());
			} catch (Exception e) {
				log.error("", e);
			}
		});
	}

	private void sendToAnimeChannel(List<Anime> anime, String desc, JikaiLocale loc) {

		anime.forEach(a -> {
			try {
				DetailedAnimeMessageBuilder damb = new DetailedAnimeMessageBuilder(a, Core.EUROPE_BERLIN, loc);
				damb.ignoreEmptyFields().withAll(false);
				damb.setDescription(desc);
				BotUtils.sendToAllAnimeChannels(damb.build());
			} catch (Exception e) {
				log.error("", e);
			}
		});
	}

	private void initImpls() {
		log.debug("Loading ALRHs");
		initMap.forEach((l, list) -> {
			Jikai j = Core.JM.get(l);
			// j would be null if the bot's loading data for a guild that it isn't connected to
			if (j != null) {
				list.forEach(id -> {
					Set<ALRHData> data = id.data();
					Map<Long, String> msgIdTitleMap = id.msgIdEmbedTitles();
					Pair<String, Long> seasonMsg = id.seasonMsg();
					TitleLanguage lang = id.titleLang();
					ALRHandler impl = new ALRHandler(l, lang);
					if (data != null && !data.isEmpty()) {
						CollectionUtils.filter(data, d -> AnimeDB.hasAnime(d.getAnimeId()));
						impl.setData(data);
					}
					if (msgIdTitleMap != null && !msgIdTitleMap.isEmpty()) {
						impl.setMsgIdTitleMap(msgIdTitleMap);
					}
					impl.setSeasonMsg(seasonMsg);
					j.setALRHandler(impl, lang);
					impl.init();
					putInMap(impl, j.getJikaiData().getGuildId(), lang);
				});
			}
		});
		// not needed anymore
		initMap.clear();
		initMap = null;
	}

	Set<DTO> getListMessages(TitleLanguage lang) {
		if (listMsgs.isEmpty() || !listMsgs.containsKey(lang)) {
			makeListMessages();
		}
		return listMsgs.get(lang);
	}

	private void makeListMessages() {
		aniAlph.forEach((tl, m) -> {
			Set<DTO> tmp = new TreeSet<>((d1, d2) -> d1.getMessage().getDescription().compareTo(d2.getMessage().getDescription()));
			m.forEach((l, list) -> tmp.add(getLetterListMessage(l, list, tl)));
			listMsgs.put(tl, tmp);
		});
		log.info("Made {} list massages", aniAlph.size());
	}

	/**
	 * Gets the animes' titles mapped to their starting letter
	 * 
	 * @return - a map where a letter is mapped to a list of titles.
	 */
	Map<String, List<String>> getMappedAnimes(TitleLanguage lang) {
		return aniAlph.get(lang);
	}

	/**
	 * Creates the list message
	 * 
	 * @return - a DataTransferObject (DTO) containing the message and the title mapped to its
	 *         respective unicode
	 */
	private DTO getLetterListMessage(String letter, List<String> titles, TitleLanguage lang) {
		log.debug("Creating list for letter {} with {} titles", letter, titles.size());
		Set<ALRHData> data = new TreeSet<>();
		StringBuilder mb = new StringBuilder();
		// :regional_indicator_a:
		int cp = 0x1F1E6;
		String uni = "";
		for (String t : titles) {
			Anime a = AnimeDB.getAnime(t, lang);
			uni = new String(Character.toChars(cp));
			data.add(new ALRHData(EncodingUtil.encodeCodepoints(uni), a.getId()));
			mb.append(uni + " : [**" + t + "**](" + a.getAniUrl() + ")\n");
			cp++;
		}
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(letter);
		eb.setDescription(mb);
		BotUtils.addJikaiMark(eb);
		return new DTO(eb.build(), data);
	}

	/**
	 * groups all animes by the first letter in their title
	 */
	private void mapAnimesToStartingLetter() {
		log.debug("Mapping animes to starting letter");
		Set<Anime> data = AnimeDB.getAiringOrUpcomingAnime().stream().filter(a -> !a.isAdult()).collect(Collectors.toSet());
		for (TitleLanguage lang : TitleLanguage.values()) {
			aniAlph.put(lang, checkReactionLimit(data.stream().map(a -> a.getTitle(lang)).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.groupingBy(a -> "" + a.toUpperCase().charAt(0)))));
		}
		log.info("Mapped {} non adult animes to {} letters", data.size(), aniAlph.size());
	}

	/**
	 * Discord has a limit of 20 reactions per message, so any grouping that has more than 20
	 * elements has to be split
	 */
	private Map<String, List<String>> checkReactionLimit(Map<String, List<String>> map) {
		Map<String, List<String>> checked = new TreeMap<>();
		for (String l : map.keySet()) {
			List<String> titles = map.get(l);
			int c = 0;
			for (int size = titles.size(); size > 20; size -= 20) {
				c++;
				List<String> tmp = new ArrayList<>(20);
				for (int n = 0; n < 20; n++) {
					tmp.add(titles.remove(0));
				}
				checked.put(l + c, tmp);
			}
			checked.put((c == 0 ? l : l + (++c)), titles);
		}
		return checked;
	}

	public void addToInitMap(long id, Set<ALRHData> data, Map<Long, String> map, Pair<String, Long> seasonMsg, TitleLanguage lang) {
		initMap.compute(id, (i, l) -> {
			l = l == null ? l = new ArrayList<>(3) : l;
			l.add(new InitData(data, map, seasonMsg, lang));
			return l;
		});
	}

	public void remove(long id) {
		impls.remove(id).clear();
	}
}

/**
 * Utility class for passing a message and a map of titles and unicodes
 */
class DTO implements Comparable<DTO> {
	Set<ALRHData> data;
	MessageEmbed me;

	DTO(MessageEmbed message, Set<ALRHData> data) {
		me = message;
		this.data = data;
	}

	MessageEmbed getMessage() {
		return me;
	}

	Set<ALRHData> getALRHData() {
		return data;
	}

	@Override
	public int compareTo(DTO o) {
		return me.getTitle().compareTo(o.me.getTitle());
	}

	@Override
	public String toString() {
		return "DTO:[" + me.getTitle() + "," + data.size() + "]";
	}
}
