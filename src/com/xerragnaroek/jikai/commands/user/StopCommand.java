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
package com.xerragnaroek.jikai.commands.user;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.jikai.JikaiIO;
import com.xerragnaroek.jikai.user.JikaiUser;
import com.xerragnaroek.jikai.util.BotUtils;

/**
 * @author XerRagnaroek
 *
 */
public class StopCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(StopCommand.class);

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public String getDescription() {
		return "Stops the bot";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		log.info("Shutting the bot down...\n Saving data...");
		JikaiIO.save(true);
		BotUtils.sendToAllInfoChannels("The dev has shut the bot down. Downtime shouldn't be long. ||(Hopefully)||").forEach(CompletableFuture::join);
		log.info("Goodbye :)");
		System.exit(1);
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
