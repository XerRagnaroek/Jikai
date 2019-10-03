package com.xerragnaroek.bot.anime.alrh;

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
import com.xerragnaroek.bot.util.Initilizable;
import com.xerragnaroek.bot.util.Manager;

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

	private final Map<String, List<String>> aniAlph = new TreeMap<>();
	private final Set<DTO> listMsgs =
			new TreeSet<>((d1, d2) -> d1.getMessage().getContentRaw().compareTo(d2.getMessage().getContentRaw()));
	private final Logger log = LoggerFactory.getLogger(ALRHManager.class);
	private Map<String, Set<ALRHData>> initMap = new TreeMap<>();
	private boolean initialized = false;

	@Override
	public void init() {
		if (!initialized) {
			mapAnimesToStartingLetter();
			initialized = true;
			initImpls();
		} else {
			throw new IllegalStateException("Already initialized!");
		}
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

	/**
	 * groups all animes by the first letter in their title
	 */
	private void mapAnimesToStartingLetter() {
		log.debug("Mapping animes to starting letter");
		List<AnimeDayTime> animes = AnimeBase.getSeasonalAnimes();
		animes.stream().map(a -> a.getAnime().title)
				.forEach(title -> aniAlph.compute(("" + title.charAt(0)).toUpperCase(), (k, v) -> {
					v = (v == null) ? new LinkedList<>() : v;
					v.add(title);
					return v;
				}));
	}

	@Override
	protected void assertInitialisation() {
		if (!initialized) {
			log.error("ALRHManager hasn't been initialized yet!");
			throw new IllegalStateException("ALRHManager hasn't been initialized yet!");
		}
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
