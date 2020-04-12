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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PingCommand implements GuildCommand {

	@Override
	public String getName() {
		return "ping";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] content) {
		MessageChannel channel = event.getChannel();
		long time = System.currentTimeMillis();
		channel.sendMessage("Pong") /* => RestAction<Message> */
				.queue(response /* => Message */ -> {
					response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue(msg -> msg.addReaction("U+1F44C").queue());
				});
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

	@Override
	public String getDescription() {
		return "Shows the latency of the bot.";
	}

}
