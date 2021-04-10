package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.user.PrivateList;
import com.github.xerragnaroek.jikai.user.ExportKeyHandler;

import net.dv8tion.jda.api.entities.User;

/**
 * 
 */
public class ImportSubscriptionsCommand implements JUCommand {

	@Override
	public String getName() {
		return "import";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		if (arguments.length < 1) {
			ju.sendPM(loc.getString(getLocaleKey() + "_no_key"));
			return;
		}
		String key = arguments[0];
		ExportKeyHandler seh = ExportKeyHandler.getInstance();
		if (seh.hasIdForKey(key)) {
			long id = seh.getJikaiUserIdFromKey(key);
			JikaiUser user = JikaiUserManager.getInstance().getUser(id);
			User dUser = user.getUser();
			if (!user.getSubscribedAnime().isEmpty()) {
				String name = "This user";
				String thumb = null;
				if (dUser != null) {
					name = dUser.getName();
					thumb = dUser.getAvatarUrl();
				}
				PrivateList pl = new PrivateList(ju, loc.getStringFormatted(getLocaleKey() + "_eb_title", Arrays.asList("user"), name), thumb);
				pl.sendList(user.getSubscribedAnime());
			} else {
				ju.sendPM(loc.getStringFormatted(getLocaleKey() + "_no_subs", Arrays.asList("user"), dUser == null ? "This user" : dUser.getAsTag()));
			}
		} else {
			ju.sendPM(loc.getString(getLocaleKey()) + "_inv_key");
		}
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_import";
	}

}
