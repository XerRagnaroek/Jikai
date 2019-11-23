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
package com.xerragnaroek.jikai.commands.guild.set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Command that changes the trigger String.
 * 
 * @author XerRagnarök
 *
 */
public class SetTriggerCommand implements GuildCommand {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	SetTriggerCommand() {}

	@Override
	public String getName() {
		return "trigger";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		String content = arguments[0];
		//only high ranking lads can use this command
		//pad it with a whitespace at the end so words can be used better: foo bar instead of foobar
		if (content.length() > 1) {
			content += " ";
		}
		if (content.length() >= 1) {
			Jikai j = Core.JM.get(event.getGuild());
			j.getJikaiData().setTrigger(content);
			try {
				j.getInfoChannel().sendMessageFormat("Trigger was changed to \"%s\"", content).queue();
			} catch (Exception e) {}
		}

	}

	@Override
	public String getUsage() {
		return getName() + " <new trigger>";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The string of characters that will trigger a command. Default is \"!\". Can be as long as you want.";
	}
}
