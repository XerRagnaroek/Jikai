package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

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
		if (arguments.length < 1) {
			ju.sendPM(getUsage(ju.getLocale()));
		} else {
			Anime a;
			try {
				int id = Integer.parseInt(arguments[0]);
				a = AnimeDB.getAnime(id);
				if (a == null) {
					ju.sendPM(ju.getLocale().getStringFormatted("com_ju_hide_anime_invalid", Arrays.asList("input"), id));
					return;
				}
			} catch (NumberFormatException e) {
				String title = String.join(" ", arguments);
				a = AnimeDB.getAnime(title);
				if (a == null) {
					ju.sendPM(ju.getLocale().getStringFormatted("com_ju_hide_anime_invalid", Arrays.asList("input"), title));
					return;
				}
			}
			if (a != null) {
				EmbedBuilder eb = BotUtils.embedBuilder();
				eb.setTitle((ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())), a.getAniUrl());
				eb.setThumbnail(a.getBiggestAvailableCoverImage());
				if (ju.isHiddenAnime(a.getId())) {
					ju.unhideAnimeFromLists(a.getId());
					eb.setDescription(ju.getLocale().getStringFormatted("com_ju_hide_anime_eb_desc_unhidden", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))));
				} else {
					ju.hideAnimeFromLists(a.getId());
					eb.setDescription(ju.getLocale().getStringFormatted("com_ju_hide_anime_eb_desc_hidden", Arrays.asList("title"), (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage()))));
				}
				ju.sendPM(eb.build());
			}
		}
	}
}