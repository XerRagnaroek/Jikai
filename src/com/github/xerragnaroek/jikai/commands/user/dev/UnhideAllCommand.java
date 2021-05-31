package com.github.xerragnaroek.jikai.commands.user.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class UnhideAllCommand implements JUCommand {

	@Override
	public String getName() {
		return "unhide_all";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		AnimeDB.getLoadedAnime().forEach(a -> ju.unhideAnimeFromLists(a.getId()));
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
