package com.github.xerragnaroek.jikai.commands.user.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserSetup;

/**
 * 
 */
public class TestCommand implements JUCommand {

	private final Logger log = LoggerFactory.getLogger(TestCommand.class);

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
		// new JikaiUserSetupRewritten(ju, Core.JM.getAny()).startSetup();
		/*
		 * Pagination page = new PrivatePagination();
		 * for (int c = 0; c < 5; c++) {
		 * page.addStage(BotUtils.makeSimpleEmbed("Stage " + (c + 1)), Arrays.asList("U+1f1f9", "U+1f1ea",
		 * "U+1f1f8", "U+1f1e9", "U+3" + c + "U+fe0fU+20e3"), i -> log.debug("added {}", i), i ->
		 * log.debug("removed {}", i));
		 * }
		 * page.send(ju.getUser().openPrivateChannel().complete());
		 */
		JikaiUserSetup.runSetup(ju, Core.JM.getAny());

	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
