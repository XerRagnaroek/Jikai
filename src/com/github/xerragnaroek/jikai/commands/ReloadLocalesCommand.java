package com.github.xerragnaroek.jikai.commands;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * 
 */
public class ReloadLocalesCommand implements JUCommand, GuildCommand {

	@Override
	public String getName() {
		return "reload_locales";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("rl");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Reloads all locales.";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		JikaiLocaleManager.loadLocales();
		BotUtils.sendPM(event.getAuthor(), "Reloaded locales!");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocaleManager.loadLocales();
		ju.sendPM("Reloaded locales!");
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
