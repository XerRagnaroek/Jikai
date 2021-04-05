package com.github.xerragnaroek.jikai.commands.user.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class CancelUpdateThreadCommand implements JUCommand {

	@Override
	public String getName() {
		return "cancel_update_thread";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Cancels the update thread.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ju.sendPM("Cancelling update thread...");
		ju.sendPM("Thread cancelled: " + AnimeDB.cancelUpdateFuture());
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

}
