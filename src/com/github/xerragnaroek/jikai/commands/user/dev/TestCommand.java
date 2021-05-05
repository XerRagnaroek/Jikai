package com.github.xerragnaroek.jikai.commands.user.dev;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserSetupRewritten;

/**
 * 
 */
public class TestCommand implements JUCommand {

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		new JikaiUserSetupRewritten(ju, Core.JM.getAny()).startSetup();
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
