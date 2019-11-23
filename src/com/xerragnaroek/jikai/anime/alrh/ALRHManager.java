/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.anime.alrh;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.doomsdayrs.jikan4java.types.main.anime.Anime;
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.Initilizable;
import com.xerragnaroek.jikai.util.Manager;
import com.xerragnaroek.jikai.util.prop.Property;

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
			update();
			init.set(true);
			initImpls();
			AnimeDB.runOnDBUpdate(oa -> {
				if (isInitialized()) {
					update();
				}
			});
		} else {
			throw new IllegalStateException("Already initialized!");
		}
	}

	private void update() {
		mapAnimesToStartingLetter();
		makeListMessages();
		Core.EXEC.execute(() -> impls.values().forEach(ALRHandler::update));
	}

	private void initImpls() {
		log.debug("Loading ALRHs");
		initMap.forEach((l, data) -> {
			ALRHandler impl = new ALRHandler(l);
			if (data != null) {
				removeOldEntries(data);
				impl.setData(data);
			}
			impl.init();
			impls.put(l, impl);
		});
		//not needed anymore
		initMap.clear();
		initMap = null;
	}

	private void removeOldEntries(Set<ALRHData> data) {
		Set<String> anime = AnimeDB.getSeasonalAnime().stream().map(AnimeDayTime::getTitle).collect(Collectors.toSet());
		new TreeSet<>(data).stream().filter(t -> !anime.contains(t.getTitle())).forEach(t -> data.remove(t));
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
			mb.append(uni + " : [**" + t + "**](" + a.url + ")\n");
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
		Set<AnimeDayTime> data = AnimeDB.getSeasonalAnime();
		aniAlph = new TreeMap<>(data.stream().map(a -> a.getAnime().title).collect(Collectors.groupingBy(a -> "" + a.charAt(0))));
		aniAlph.values().forEach(Collections::sort);
		log.info("Mapped {} animes to {} letters", data.size(), aniAlph.size());
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
