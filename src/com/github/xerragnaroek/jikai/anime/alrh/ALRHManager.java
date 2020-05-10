package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.util.Initilizable;
import com.github.xerragnaroek.jikai.util.Manager;
import com.github.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * ALRH stands for AnimeListReactionHandler. Manages ALRH instances. Makes the anime list prefab
 * messages.
 * 
 * @author XerRagnaroek
 *
 */
public class ALRHManager extends Manager<ALRHandler> implements Initilizable {

	public ALRHManager() {
		super(ALRHandler.class);
	}

	private Map<String, List<String>> aniAlph = new TreeMap<>();
	private Property<Set<DTO>> listMsgs = new Property<>();
	private final Logger log = LoggerFactory.getLogger(ALRHManager.class);
	private Map<Long, Set<ALRHData>> initMap = new TreeMap<>();

	@Override
	public void init() {
		if (!isInitialized()) {
			mapAnimesToStartingLetter();
			makeListMessages();
			initImpls();
			init.set(true);
			AnimeDB.runOnDBUpdate(au -> {
				if (isInitialized()) {
					log.debug("Updating ALRHs");
					update(au);
				}
			});
		} else {
			throw new IllegalStateException("Already initialized!");
		}
	}

	private void update(AnimeUpdate au) {
		if (au.hasChange()) {
			mapAnimesToStartingLetter();
			makeListMessages();
			Core.EXEC.execute(() -> impls.values().forEach(impl -> impl.update(au)));
		}
	}

	private void initImpls() {
		log.debug("Loading ALRHs");
		initMap.forEach((l, data) -> {
			ALRHandler impl = new ALRHandler(l);
			if (data != null) {
				removeOldEntries(data);
				impl.setData(data);
			}
			Jikai j = Core.JM.get(impl.gId);
			j.setALRH(impl);
			impl.init();
			impls.put(l, impl);
		});
		//not needed anymore
		initMap.clear();
		initMap = null;
	}

	private void removeOldEntries(Set<ALRHData> data) {
		Set<String> anime = AnimeDB.getSeasonalAnime().stream().map(Anime::getTitleRomaji).collect(Collectors.toSet());
		new TreeSet<>(data).stream().filter(t -> !anime.contains(t.getTitle())).forEach(t -> {
			data.remove(t);
			log.debug("Removed old ALRHData for '{}'", t);
		});
	}

	Set<DTO> getListMessages() {
		if (!listMsgs.hasNonNullValue()) {
			makeListMessages();
		}
		return listMsgs.get();
	}

	private void makeListMessages() {
		Set<DTO> tmp = new TreeSet<>((d1, d2) -> d1.getMessage().getDescription().compareTo(d2.getMessage().getDescription()));
		aniAlph.forEach((l, list) -> {
			tmp.add(getLetterListMessage(l, list));
		});
		listMsgs.set(tmp);
		log.info("Made {} list massages", aniAlph.size());
	}

	/**
	 * Gets the animes' titles mapped to their starting letter
	 * 
	 * @return - a map where a letter is mapped to a list of titles.
	 */
	Map<String, List<String>> getMappedAnimes() {
		return aniAlph;
	}

	/**
	 * Creates the list message
	 * 
	 * @return - a DataTransferObject (DTO) containing the message and the title mapped to its
	 *         respective unicode
	 */
	private DTO getLetterListMessage(String letter, List<String> titles) {
		log.debug("Creating list for letter {} with {} titles", letter, titles.size());
		Set<ALRHData> data = new TreeSet<>();
		StringBuilder mb = new StringBuilder();
		mb.append("**" + letter + "**\n");
		//:regional_indicator_a:
		int cp = 0x1F1E6;
		String uni = "";
		for (String t : titles) {
			Anime a = AnimeDB.getAnime(t);
			uni = new String(Character.toChars(cp));
			data.add(new ALRHData(EncodingUtil.encodeCodepoints(uni), t));
			mb.append(uni + " : [**" + t + "**](" + a.getAniUrl() + ")\n");
			cp++;
		}
		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(mb);
		return new DTO(eb.build(), data);
	}

	Property<Set<DTO>> listMessagesProperty() {
		return listMsgs;
	}

	/**
	 * groups all animes by the first letter in their title
	 */
	private void mapAnimesToStartingLetter() {
		log.debug("Mapping animes to starting letter");
		Set<Anime> data = AnimeDB.getSeasonalAnime();
		aniAlph = checkReactionLimit(data.stream().map(Anime::getTitleRomaji).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.groupingBy(a -> "" + a.toUpperCase().charAt(0))));
		log.info("Mapped {} anime to {} letters", data.size(), aniAlph.size());
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

	public void addToInitMap(long id, Set<ALRHData> data) {
		initMap.put(id, data);
	}

	@Override
	protected ALRHandler makeNew(long gId) {
		return new ALRHandler(gId);
	}

}

/**
 * Utility class for passing a message and a map of titles and unicodes
 */
class DTO {
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
}
