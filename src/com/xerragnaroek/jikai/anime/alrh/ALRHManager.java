package com.xerragnaroek.jikai.anime.alrh;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.util.Initilizable;
import com.xerragnaroek.jikai.util.Manager;
import com.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
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
	private Map<String, Set<ALRHData>> initMap = new TreeMap<>();

	@Override
	public void init() {
		if (!isInitialized()) {
			update();
			init.set(true);
			initImpls();
			AnimeDB.dbVersionProperty().onChange((ov, nv) -> {
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
		ForkJoinPool.commonPool().submit(() -> impls.values().forEach(ALRHandler::update));
	}

	private void initImpls() {
		log.debug("Loading ALRHs");
		initMap.forEach((str, data) -> {
			ALRHandler impl = new ALRHandler(str);
			if (data != null) {
				impl.setData(data);
			}
			impl.init();
			impls.put(str, impl);
		});
		//not needed anymore
		initMap.clear();
		initMap = null;
	}

	Set<DTO> getListMessages() {
		if (!listMsgs.hasNonNullValue()) {
			makeListMessages();
		}
		return listMsgs.get();
	}

	private void makeListMessages() {
		Set<DTO> tmp = new TreeSet<>((d1, d2) -> d1.getMessage().getContentRaw().compareTo(d2.getMessage().getContentRaw()));
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
		MessageBuilder mb = new MessageBuilder();
		mb.append("**" + letter + "**\n");
		//:regional_indicator_a:
		int cp = 0x1F1E6;
		String uni = "";
		for (String t : titles) {
			uni = new String(Character.toChars(cp));
			data.add(new ALRHData(EncodingUtil.encodeCodepoints(uni), t));
			mb.append(uni + " : **" + t + "**\n");
			cp++;
		}
		return new DTO(mb.build(), data);
	}

	Property<Set<DTO>> listMessagesProperty() {
		return listMsgs;
	}

	/**
	 * groups all animes by the first letter in their title
	 */
	private void mapAnimesToStartingLetter() {
		log.debug("Mapping animes to starting letter");
		Set<AnimeDayTime> data = AnimeDB.getSeasonalAnimes();
		aniAlph = new TreeMap<>(data.stream().map(a -> a.getAnime().title).collect(Collectors.groupingBy(a -> "" + a.charAt(0))));
		aniAlph.values().forEach(Collections::sort);
		log.info("Mapped {} animes to {} letters", data.size(), aniAlph.size());
	}

	public void addToInitMap(String id, Set<ALRHData> data) {
		initMap.put(id, data);
	}

	@Override
	protected ALRHandler makeNew(String gId) {
		return new ALRHandler(gId);
	}

}

/**
 * Utility class for passing a message and a map of titles and unicodes
 */
class DTO {
	Set<ALRHData> data;
	Message me;

	DTO(Message message, Set<ALRHData> data) {
		me = message;
		this.data = data;
	}

	Message getMessage() {
		return me;
	}

	Set<ALRHData> getALRHData() {
		return data;
	}
}
