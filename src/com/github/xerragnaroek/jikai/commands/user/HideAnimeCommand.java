package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.pagi.PrivateAnimePagination;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * 
 */
public class HideAnimeCommand implements JUCommand {

	@Override
	public String getName() {
		return "hide_anime";
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_hide_anime";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		if (arguments.length < 1) {
			if (ju.hiddenAnimeProperty().isEmpty()) {
				ju.sendPM(loc.getString("com_ju_hide_anime_none"));
			} else {
				PrivateAnimePagination page = new PrivateAnimePagination(ju);
				page.setTitle(loc.getString("com_ju_hide_anime_eb_title"));
				page.setYesPredicate(a -> ju.isHiddenAnime(a.getId()));
				page.setOnReactionAdded(a -> flipHiddenState(a, ju));
				page.setOnReactionRemoved(a -> flipHiddenState(a, ju));
				page.setRefreshOnReaction(true);
				page.populate(ju.hiddenAnimeProperty().stream().map(AnimeDB::getAnime).collect(Collectors.toList()));
				page.send(5);
			}
		} else {
			Anime a;
			try {
				int id = Integer.parseInt(arguments[0]);
				a = AnimeDB.getAnime(id);
				if (a == null) {
					ju.sendPM(loc.getStringFormatted("com_ju_hide_anime_invalid", Arrays.asList("input"), id));
					return;
				}
			} catch (NumberFormatException e) {
				String title = String.join(" ", arguments);
				a = AnimeDB.getAnime(title);
				if (a == null) {
					ju.sendPM(loc.getStringFormatted("com_ju_hide_anime_invalid", Arrays.asList("input"), title));
					return;
				}
			}
			if (a != null) {
				EmbedBuilder eb = BotUtils.embedBuilder();
				eb.setTitle((ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())), a.getAniUrl());
				eb.setThumbnail(a.getBiggestAvailableCoverImage());
				if (ju.isHiddenAnime(a.getId())) {
					ju.unhideAnimeFromLists(a.getId());
					eb.setDescription(loc.getStringFormatted("com_ju_hide_anime_eb_desc_unhidden", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))));
				} else {
					ju.hideAnimeFromLists(a.getId());
					eb.setDescription(loc.getStringFormatted("com_ju_hide_anime_eb_desc_hidden", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))));
				}
				ju.sendPM(eb.build());
			}
		}
	}

	private void flipHiddenState(Anime a, JikaiUser ju) {
		if (ju.isHiddenAnime(a.getId())) {
			ju.unhideAnimeFromLists(a.getId());
		} else {
			ju.hideAnimeFromLists(a.getId());
		}
	}
}