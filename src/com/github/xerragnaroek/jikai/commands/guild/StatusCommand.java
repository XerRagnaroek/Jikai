/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
			mb.appendCodeBlock(String.format("= Status =%nPing :: %d ms%nGateway-Ping :: %d ms%nExecuted Commands :: %d%nAnimes in DB :: %02d%nDB Version :: %d%nUpdate-Rate :: %d min%nConnected Servers :: %d%nUsers :: %d", ping, JDA.getGatewayPing(), jd.getExecutedCommandCount(), AnimeDB.loadedAnimes(), AnimeDB.getAnimeDBVersion(), JDA.getGuildCache().size(), Jikai.getUserManager().userAmount()), "asciidoc");
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
