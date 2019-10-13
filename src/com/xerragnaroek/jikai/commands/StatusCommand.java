package com.xerragnaroek.jikai.commands;

import static com.xerragnaroek.jikai.core.Core.JDA;
import static com.xerragnaroek.jikai.core.Core.RTKM;

import java.time.Instant;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.GuildData;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class StatusCommand implements Command {
	//TODO StatusCommand
	@Override
	public String getName() {
		return "status";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		GuildData gd = Core.GDM.get(g);
		TextChannel tc = g.getTextChannelById(gd.getInfoChannelId());
		Instant now = Instant.now();
		Message m = tc.sendMessage("If you can see this, something broke along the lines.").complete();
		long ping = Instant.now().toEpochMilli() - now.toEpochMilli();
		MessageBuilder mb = new MessageBuilder();
		mb.appendCodeBlock(String.format("= Status =%nPing :: %d ms%nGateway-Ping :: %d ms%nExecuted Commands :: %d%nAnimes in DB :: %02d%nDB Version :: %d%nDay-Threshold :: %d%nHour-Threshold :: %d%nUpdate-Rate :: %d min%nConnected Servers :: %d", ping, JDA.getGatewayPing(), gd.getExecutedCommandCount(), AnimeDB.loadedAnimes(), AnimeDB.getAnimeBaseVersion(), RTKM.getDayThreshold(), RTKM.getHourThreshold(), RTKM.getUpdateRate(), JDA.getGuildCache().size()), "asciidoc");
		m.editMessage(mb.build()).queue();
	}

	@Override
	public String getIdentifier() {
		return "stc";
	}

	@Override
	public String getDescription() {
		return "Shows general information concerning the bot's status (Ping, executed commands, etc.)";
	}

	@Override
	public boolean isAlwaysEnabled() {
		return true;
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandlerManager.MOD_PERMS;
	}
}
