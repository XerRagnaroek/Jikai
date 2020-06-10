package com.github.xerragnaroek.jikai.commands.user;

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
	public String getDescription() {
		return "Unregisters you from this bot, deleting you from the userbase.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiUserManager.getInstance().removeUser(ju.getId());
		ju.sendPM("You have been unregistered from Jikai!");
	}

}
