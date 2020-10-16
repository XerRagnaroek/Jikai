package com.github.xerragnaroek.jikai.commands.guild.set;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetLanguageCommand implements GuildCommand {

	@Override
	public String getName() {
		return "language";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("lang", "locale");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_g_set_lang_desc");
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		JikaiLocale loc = JikaiLocaleManager.getInstance().getLocale(arguments[0]);
		Jikai j = Core.JM.get(event.getGuild());
		if (loc != null) {
			JikaiData jd = j.getJikaiData();
			if (!jd.getLocale().equals(loc)) {
				jd.setLocale(loc);
				try {
					j.getInfoChannel().sendMessage(loc.getStringFormatted("com_g_set_lang_success", Arrays.asList("lang"), loc.getLanguageName())).queue();
				} catch (Exception e) {}
			}
		} else {
			event.getChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_lang_fail", Arrays.asList("langs"), JikaiLocaleManager.getInstance().getAvailableLocales())).queue();
		}
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_g_set_lang_use", Arrays.asList("com"), getName());
	}
}
