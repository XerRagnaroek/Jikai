package com.github.xerragnaroek.jikai.commands.user;

import java.awt.GraphicsEnvironment;

import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class FontsCommand implements JUCommand {

	@Override
	public String getName() {
		return "fonts";
	}

	@Override
	public String getDescription() {
		return "Lists all available fonts";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		ju.sendPM("Available fonts are:\n" + String.join("\n", fonts));

	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
