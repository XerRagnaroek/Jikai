
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class ForceDBUpdateCommand implements JUCommand {

	@Override
	public String getName() {
		return "update";
	}

	@Override
	public String getDescription() {
		return "Forces an update of the anime db";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		AnimeDB.update();
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
