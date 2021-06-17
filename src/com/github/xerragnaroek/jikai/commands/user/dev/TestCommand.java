package com.github.xerragnaroek.jikai.commands.user.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

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
		MessageBuilder mb = new MessageBuilder();
		mb.append("Boop");
		mb.setActionRows(ActionRow.of(Button.link("https://www.pornhub.com/", "Epic webzone")));
		ju.sendPM(mb.build());
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
