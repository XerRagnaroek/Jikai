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

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DebugCommand implements GuildCommand {
	private final Logger log = LoggerFactory.getLogger(DebugCommand.class);

	@Override
	public String getName() {
		return "debug";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		log.debug("Command was called by the author");
		log.info("Executing debug command");
		MessageBuilder bob = new MessageBuilder();
		bob.append("Hello I am debug command");
		try {
			log.debug("Debug message id = {}", bob.sendTo(event.getChannel()).submit().get().getId());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getDescription() {
		return "Prints internal debug information. Only usable by the owner of this bot.";
	}

	@Override
	public boolean isAlwaysEnabled() {
		return true;
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
