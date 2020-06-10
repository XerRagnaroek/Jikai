
package com.github.xerragnaroek.jikai.commands.guild;

import static com.github.xerragnaroek.jikai.core.Core.JDA;

import java.time.Instant;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class StatusCommand implements GuildCommand {
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
			StringBuilder bob = new StringBuilder();
			bob.append("= Status =\n");
			bob.append("Ping :: " + ping + " ms\n");
			bob.append("Gateway-Ping :: " + JDA.getGatewayPing() + " ms\n");
			bob.append("Currently loaded anime :: " + AnimeDB.size() + "\n");
			bob.append("Executed Commands :: " + jd.getExecutedCommandCount() + "\n");
			bob.append("Servers running Jikai :: " + JDA.getGuildCache().size() + "\n");
			bob.append("Registered Users :: " + JikaiUserManager.getInstance().userAmount() + "\n");
			bob.append("Server Commands Enabled :: " + jd.areCommandsEnabled());
			mb.appendCodeBlock(bob.toString(), "asciidoc");
			m.editMessage(mb.build()).queue();
		} catch (Exception e) {
			Core.logThrowable(e);
		}
	}

	@Override
	public String getDescription() {
		return "Sends the bots current stats.";
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
