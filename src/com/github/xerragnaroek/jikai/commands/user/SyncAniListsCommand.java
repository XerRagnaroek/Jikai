package com.github.xerragnaroek.jikai.commands.user;

import java.io.IOException;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jikai.anime.ani.AniListSyncer;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

/**
 * 
 */
public class SyncAniListsCommand implements JUCommand {

	@Override
	public String getName() {
		return "sync";
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_sync";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		AniListSyncer als = AniListSyncer.getInstance();
		String msg = "";
		JikaiLocale loc = ju.getLocale();
		if (ju.getAniId() > 0) {
			try {
				als.syncSubsWithAniList(ju);
				msg = loc.getString(getLocaleKey() + "_suc");
				if (JikaiUserAniTokenManager.hasToken(ju)) {
					als.syncAniListsWithSubs(ju);
					msg += "\n" + loc.getString(getLocaleKey() + "_suc_auth");
				}
			} catch (AniException | IOException e) {
				BotUtils.logAndSendToDev(Core.ERROR_LOG, "Failed syncing list!", e);
				msg = loc.getString(getLocaleKey() + "_fail");
			}
		} else {
			msg = loc.getString(getLocaleKey() + "_no_ani");
		}
		ju.sendPM(BotUtils.makeSimpleEmbed(msg));
	}

}
