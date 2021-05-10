package com.github.xerragnaroek.jikai.anime.link;

import com.github.xerragnaroek.jasa.Anime;

import okhttp3.OkHttpClient;

/**
 * 
 */
public class EpisodeLinker {
	static final OkHttpClient client = new OkHttpClient();

	public static String getStreamLinksFormatted(Anime a) {
		return "**[[Gogoanime]](" + GogoLink.makeLink(a) + ")**";
	}
}
