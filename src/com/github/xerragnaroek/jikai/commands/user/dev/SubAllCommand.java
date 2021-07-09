package com.github.xerragnaroek.jikai.commands.user.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class SubAllCommand implements JUCommand {

	@Override
	public String getName() {
		return "sub_all";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		AnimeDB.getAiringOrUpcomingAnime().stream().forEach(a -> ju.subscribeAnime(a.getId(), "Sub all command"));
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
