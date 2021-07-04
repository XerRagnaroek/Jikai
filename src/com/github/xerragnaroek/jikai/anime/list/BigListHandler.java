package com.github.xerragnaroek.jikai.anime.list;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUserUpdater;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

/**
 * 
 */
public class BigListHandler {

	private Jikai j;
	private Map<Integer, Long> messages = Collections.synchronizedMap(new TreeMap<>());
	private static DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMMM, yyyy");

	public BigListHandler(Jikai j) {
		this.j = j;
	}

	public static Message makeMessage(Anime a, ZoneId zone, JikaiLocale loc) {
		List<String> titles = new LinkedList<>();
		List<String> titlesIgnoreCase = new LinkedList<>();
		String title = "";
		if (a.hasTitleRomaji()) {
			titles.add(a.getTitleRomaji());
			titlesIgnoreCase.add(a.getTitleRomaji().toLowerCase());
		}
		if (a.hasTitleEnglish() && !titlesIgnoreCase.contains((title = a.getTitleEnglish()).toLowerCase())) {
			titles.add(title);
			titlesIgnoreCase.add(title.toLowerCase());
		}
		if (a.hasTitleNative() && !titlesIgnoreCase.contains((title = a.getTitleNative()).toLowerCase())) {
			titles.add(title);
		}
		title = String.join("\n", titles);
		String synonyms = String.join("\n", a.getSynonyms());
		String date = makeDateString(a, zone);
		int eps = a.getEpisodes();
		String totalEps = eps == 0 ? "TBA" : "" + a.getEpisodes();
		String nextEp = makeNextEpString(a, zone, loc.getLocale());
		String external = a.getExternalLinks().stream().map(es -> String.format("[%s](%s)", es.getSite(), es.getUrl())).collect(Collectors.joining("\n"));
		MessageEmbed meb = BotUtils.localedEmbed(loc, "g_list_big_msg_eb", makePair("title", title), makePair("url", a.getAniUrl()), makePair("thumb", a.getBiggestAvailableCoverImage()), makePair("synonyms", synonyms), makePair("date", date), makePair("eps", totalEps), makePair("nextEp", nextEp), makePair("links", external));
		MessageBuilder bob = new MessageBuilder(meb);
		bob.setActionRows(ActionRow.of(Button.secondary("blh:" + a.getId(), Emoji.fromMarkdown("❤"))));
		return bob.build();
	}

	private static Pair<List<String>, Object[]> makePair(String name, String content) {
		return Pair.of(Arrays.asList(name), new Object[] { content });
	}

	private static String makeDateString(Anime a, ZoneId zone) {
		if (a.hasDataForNextEpisode()) {
			return a.getNextEpisodeDateTime(zone).get().format(format);
		} else {
			JASA jasa = AnimeDB.getJASA();
			return jasa.getCurrentSeason() + " " + jasa.getCurrentSeasonYear();
		}
	}

	private static String makeNextEpString(Anime a, ZoneId zone, Locale loc) {
		if (a.hasDataForNextEpisode()) {
			return JikaiUserUpdater.formatAirDateTime(a.getNextEpisodeDateTime(zone).get(), loc);
		} else {
			return "TBA";
		}
	}
}
