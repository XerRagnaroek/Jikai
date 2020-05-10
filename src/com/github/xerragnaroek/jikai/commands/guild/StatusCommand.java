
package com.github.xerragnaroek.jikai.commands.guild;

import static com.github.xerragnaroek.jikai.core.Core.JDA;

import java.time.Instant;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class StatusCommand implements GuildCommand {
	//TODO StatusCommand
	@Override
	public String getName() {
		return "status";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		Jikai j = Core.JM.get(g);
		JikaiData jd = j.getJikaiData();
		try {
			TextChannel tc = j.getInfoChannel();
			Instant now = Instant.now();
			Message m = tc.sendMessage("If you can see this, something broke along the lines.").complete();
			long ping = Instant.now().toEpochMilli() - now.toEpochMilli();
			MessageBuilder mb = new MessageBuilder();
			mb.appendCodeBlock(String.format("= Status =%nPing :: %d ms%nGateway-Ping :: %d ms%nExecuted Commands :: %d%nAnimes in DB :: %02d%nDB Version :: %d%nUpdate-Rate :: %d min%nConnected Servers :: %d%nUsers :: %d", ping, JDA.getGatewayPing(), jd.getExecutedCommandCount(), AnimeDB.size(), AnimeDB.getAnimeDBVersion(), JDA.getGuildCache().size(), Jikai.getUserManager().userAmount()), "asciidoc");
			m.editMessage(mb.build()).queue();
		} catch (Exception e) {}
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
		return CommandHandler.MOD_PERMS;
	}
}
