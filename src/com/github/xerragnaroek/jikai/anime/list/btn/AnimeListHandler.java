package com.github.xerragnaroek.jikai.anime.list.btn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.UnicodeUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

/**
 * 
 */
public class AnimeListHandler {

	private static Map<Long, Map<TitleLanguage, Map<Long, List<Integer>>>> initMap = new ConcurrentHashMap<>();

	private Map<Long, List<Integer>> msgAniMap = new ConcurrentHashMap<>();
	private Map<Integer, Long> aniMsgMap = new ConcurrentHashMap<>();
	private Collector<Anime, ?, Map<String, List<Anime>>> groupingBy;
	private Predicate<Anime> filter = a -> true;
	private Function<Anime, String> animeTitleFunction;
	private Comparator<Anime> sortingBy;
	private TextChannel tc;
	private long tcId;
	private final Logger log;
	private long gId;
	private Runnable afterGrouping;
	private AtomicBoolean isSending = new AtomicBoolean(false);

	public AnimeListHandler(long gId, TextChannel tc) {
		setTextChannel(tc);
		this.gId = gId;
		log = LoggerFactory.getLogger(AnimeListHandler.class + "#" + tc.getName());
		AnimeDB.runOnDBUpdate(au -> {
			if (au.hasCancelledAnime() || au.hasFinishedAnime() || au.hasNewAnime() || au.hasRemovedAnime()) {
				log.debug("Updating list!");
				Set<Anime> anime = AnimeDB.getLoadedAnime();
				if (au.hasRemovedButStillValid()) {
					anime.addAll(au.getRemovedButStillValid());
				}
				sendList(anime);
			}
		});
	}

	public void groupingBy(Function<Anime, String> function) {
		groupingBy = Collectors.groupingBy(function, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Collectors.toList());
	}

	public void doAfterGroupingBy(Runnable run) {
		afterGrouping = run;
	}

	public void setFilter(Predicate<Anime> filter) {
		this.filter = filter;
	}

	public void setAnimeTitleFunction(Function<Anime, String> func) {
		animeTitleFunction = func;
	}

	public void sortingBy(Comparator<Anime> comp) {
		sortingBy = comp;
	}

	public void setTextChannel(TextChannel tc) {
		this.tc = tc;
		tcId = tc.getIdLong();
	}

	public void loadInitData(TitleLanguage lang) {
		initMap.computeIfPresent(gId, (id, m) -> {
			m.computeIfPresent(lang, (l, map) -> {
				setMessageIdAnimeIdMap(map);
				return map;
			});
			return m;
		});
	}

	public CompletableFuture<Void> sendList(Collection<Anime> animeList) {
		isSending.set(true);
		if (!msgAniMap.isEmpty()) {
			msgAniMap.clear();
			aniMsgMap.clear();
			return BotUtils.clearChannel(tc).thenCompose(o -> sendImpl(animeList));
		} else {
			return sendImpl(animeList);
		}

	}

	private CompletableFuture<Void> sendImpl(Collection<Anime> animeList) {
		log.debug("Sending list");
		Map<String, List<Anime>> anime = animeList.stream().filter(filter).collect(groupingBy);
		if (afterGrouping != null) {
			afterGrouping.run();
		}
		List<CompletableFuture<?>> cfs = new LinkedList<>();
		anime.forEach((str, l) -> {
			makeMessage(str, l).forEach(p -> {
				cfs.add(tc.sendMessage(p.getLeft()).submit().whenComplete((msg, t) -> {
					if (t != null) {
						log.error("Failed sending message", t);
					} else {
						long id = msg.getIdLong();
						log.debug("Sent message {}", id);
						msgAniMap.put(id, p.getRight().stream().map(Anime::getId).peek(aniId -> aniMsgMap.put(aniId, id)).collect(Collectors.toList()));
						log.debug("Added {} anime to map for {}", p.getRight().size(), id);
					}
				}));
			});
		});
		return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).thenAccept(v -> isSending.set(false));
	}

	private List<Pair<Message, List<Anime>>> makeMessage(String title, List<Anime> anime) {
		anime.sort(sortingBy);
		List<List<Anime>> partitioned = BotUtils.partitionCollection(anime, 25);
		List<Pair<Message, List<Anime>>> messages = new LinkedList<>();
		for (int p = 0; p < partitioned.size(); p++) {
			EmbedBuilder eb = BotUtils.embedBuilder();
			String msgTitle = title + (partitioned.size() == 1 ? "" : (p + 1) + "/" + p);
			eb.setTitle(msgTitle);
			List<Anime> an = partitioned.get(p);
			List<Button> btns = new ArrayList<>(an.size());
			List<String> strings = new ArrayList<>(an.size());
			for (int i = 0; i < an.size(); i++) {
				Anime a = an.get(i);
				strings.add(String.format("%s: %s", BotUtils.processUnicode(UnicodeUtils.getNumberCodePoints(i + 1)), animeTitleFunction.apply(a)));
				btns.add(Button.secondary("gsh:" + a.getId(), "" + (i + 1)));
			}
			eb.setDescription(String.join("\n", strings));
			MessageBuilder mb = new MessageBuilder(eb.build());
			List<ActionRow> rows = BotUtils.partitionCollection(btns, 5).stream().map(ActionRow::of).collect(Collectors.toList());
			mb.setActionRows(rows);
			messages.add(Pair.of(mb.build(), an));
		}
		return messages;
	}

	public void validateList() {
		Core.EXEC.execute(() -> {
			log.debug("Validating list...");
			Set<Long> msgIds = new TreeSet<>();
			msgIds.addAll(msgAniMap.keySet());
			boolean resend = false;
			for (long id : msgIds) {
				if (tc.retrieveMessageById(id).mapToResult().complete().isFailure()) {
					resend = true;
					break;
				}
			}
			if (resend || AnimeDB.getLoadedAnime().stream().filter(filter).count() != aniMsgMap.size()) {
				log.debug("Data doesn't match messages in channel, resending the list");
				sendList(AnimeDB.getLoadedAnime());
			}
			log.debug("List is valid!");
		});
	}

	public Map<Long, List<Integer>> getMessageIdAnimeIdMap() {
		return msgAniMap;
	}

	public void setMessageIdAnimeIdMap(Map<Long, List<Integer>> map) {
		msgAniMap.putAll(map);
		msgAniMap.forEach((id, list) -> {
			list.forEach(aniId -> aniMsgMap.put(aniId, id));
		});
	}

	public static void addToInitMap(long gId, Map<TitleLanguage, Map<Long, List<Integer>>> map) {
		initMap.put(gId, map);
	}

	public static AnimeListHandler makeDefault(long gId, TextChannel tc, TitleLanguage lang) {
		AnimeListHandler alh = new AnimeListHandler(gId, tc);
		alh.setAnimeTitleFunction(a -> String.format("**[%s](%s)**", a.getTitle(lang), a.getAniUrl()));
		alh.setFilter(a -> !a.isAdult() && (a.isReleasing() || a.isNotYetReleased() || a.isOnHiatus() || a.hasDataForNextEpisode()));
		alh.sortingBy(Anime.IGNORE_TITLE_CASE);
		alh.groupingBy(a -> (a.getTitle(lang).charAt(0) + "").toUpperCase());
		alh.loadInitData(lang);
		return alh;
	}

	public boolean isSendingList() {
		return isSending.get();
	}
}
