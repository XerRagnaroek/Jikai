package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

/**
 * 
 */
public class TestReactionCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_reaction";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Test a unicode reaction";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		if (arguments.length >= 1) {
			BotUtils.sendPM(ju.getUser(), "Reaction Test").get(0).thenCompose(m -> m.addReaction(arguments[0]).submit()).exceptionally(t -> {
				ju.sendPM(t.getMessage());
				return null;
			});
		}
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
