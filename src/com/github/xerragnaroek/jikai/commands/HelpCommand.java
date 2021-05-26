
package com.github.xerragnaroek.jikai.commands;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommandHandler;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class HelpCommand implements GuildCommand, JUCommand {
	private final Logger log = LoggerFactory.getLogger(HelpCommand.class);

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		help(event.getAuthor(), Core.JM.get(event.getGuild()));
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "If you can see this, please report it as a bug!";
	}

	@Override
	public boolean isAlwaysEnabled() {
		return true;
	}

	@Override
	public boolean isJikaiUserOnly() {
		return false;
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		help(ju.getUser(), null);
	}

	private void help(User u, Jikai j) {
		Member m = BotUtils.getHighestPermissionMember(u);
		AtomicBoolean userIsDev = new AtomicBoolean(false);
		JikaiUser ju;
		JikaiLocale ltmp = j == null ? null : j.getLocale();
		if ((ju = JikaiUserManager.getInstance().getUser(u.getIdLong())) != null) {
			userIsDev.set(Core.DEV_IDS.contains(ju.getId()));
			ltmp = ju.getLocale();
		}
		String prefix = j == null ? Core.JM.getJDM().getBotData().getDefaultPrefix() : j.getJikaiData().getPrefix();
		JikaiLocale loc = ltmp;
		String serComs = CommandHandler.getCommands().stream().filter(com -> !com.getName().equals("help") && com.isEnabled() && ComUtils.checkPermissions(com, m)).map(com -> "**__" + prefix + com.getName() + "__**" + (com.hasUsage() ? " *" + com.getUsage(loc) + "*" : "") + (com.hasAlternativeNames() ? com.getAlternativeNames() : "") + "\n" + com.getDescription(loc)).collect(Collectors.joining("\n"));
		String pmComs = JUCommandHandler.getCommands().stream().filter(com -> !com.getName().equals("help") && com.isEnabled() && (!com.isDevOnly() || userIsDev.get())).map(com -> "**__" + prefix + com.getName() + "__**" + (com.hasUsage() ? " *" + com.getUsage(loc) + "*" : "") + (com.hasAlternativeNames() ? com.getAlternativeNames() : "") + "\n" + com.getDescription(loc)).collect(Collectors.joining("\n"));
		EmbedBuilder eb = new EmbedBuilder();
		BotUtils.addJikaiMark(eb);
		eb.setTitle(loc.getString("com_help_eb_server_title"));
		MessageBuilder splitter = new MessageBuilder(loc.getStringFormatted("com_help_eb_server_desc", Arrays.asList("sercoms"), serComs));
		splitter.buildAll(SplitPolicy.NEWLINE).forEach(msg -> {
			eb.setDescription(msg.getContentRaw());
			BotUtils.sendPMChecked(u, eb.build());
		});
		eb.setTitle(loc.getString("com_help_eb_pm_title"));
		splitter.setContent(loc.getStringFormatted("com_help_eb_pm_desc", Arrays.asList("pmcoms"), pmComs));
		splitter.buildAll(SplitPolicy.NEWLINE).forEach(msg -> {
			eb.setDescription(msg.getContentRaw());
			BotUtils.sendPMChecked(u, eb.build());
		});
	}

	@Override
	public String getLocaleKey() {
		return "com_help_eb";
	}

}
