package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

/**
 * 
 */
public class UnregisterCommand implements JUCommand {

	@Override
	public String getName() {
		return "unregister";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_unregister_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ju.sendPM(ju.getLocale().getString("ju_unregister"));
		JikaiUserManager.getInstance().removeUser(ju.getId());
	}

}
