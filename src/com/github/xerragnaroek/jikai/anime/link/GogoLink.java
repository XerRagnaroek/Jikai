package com.github.xerragnaroek.jikai.anime.link;

import com.github.xerragnaroek.jasa.Anime;

import okhttp3.Request;

/**
 * 
 */
public class GogoLink {
	private static final String urlBase = "https://www.gogoanime.ai/%s-episode-%d";

	public static String makeLink(Anime a) {
		String title = a.getTitleRomaji().replaceAll("[^\\w ]", "").trim().replace(" ", "-");
		return String.format(urlBase, title, a.getNextEpisodeNumber());
	}

	private static boolean testLink(String url) {
		Request req = new Request.Builder().url(url).build();
		return false;
	}
}
