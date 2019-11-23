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
package com.xerragnaroek.jikai.commands.guild;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RequestAssistanceCommand implements GuildCommand {

	@Override
	public String getName() {
		return "request_assistance";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		TextChannel tc = event.getTextChannel();
		long devId = Core.DEV_ID;
		User author = event.getAuthor();
		if (devId == 0) {
			tc.sendMessage("I'm sorry " + author.getAsMention() + " but whoever is hosting this bot didn't supply a developer id.").queue();
			return;
		} else {
			User dev = Core.JDA.getUserById(devId);
			if (dev == null) {
				tc.sendMessage("I'm sorry " + author.getAsMention() + " but whoever hosts this bot has supplied an invalid dev id.").queue();
			} else {
				Guild g = event.getGuild();
				BotUtils.sendPM(dev, String.format("%s from guild \"%s\"#%s has an issue:%n%s", author.getAsTag(), g.getName(), g.getId(), String.join(" ", arguments)));
				tc.sendMessage("A message has been sent to dev " + dev.getAsTag()).queue();
			}
		}
	}

	@Override
	public String getUsage() {
		return "request_assistance <message>";
	}

	@Override
	public String getDescription() {
		return "Notifies the dev that you require assistance with your given issue.";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

}
