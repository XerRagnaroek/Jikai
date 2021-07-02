package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * 
 */
public class ShowAdultCommand implements JUCommand {

	@Override
	public String getName() {
		return "show_adult";
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_show_adult";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		boolean show;
		if (arguments.length == 0) {
			show = ju.showAdultProperty().flip();
		} else {
			switch (arguments[0].toLowerCase()) {
				case "true":
					show = true;
					break;
				case "false":
					show = false;
					break;
				default:
					throw new IllegalArgumentException();
			}
		}
		EmbedBuilder eb = BotUtils.embedBuilder();
		if (show) {
			eb.setDescription(ju.getLocale().getString(getLocaleKey() + "_true"));
		} else {
			eb.setDescription(ju.getLocale().getString(getLocaleKey() + "_false"));
		}
		ju.sendPM(eb.build());
	}

}
