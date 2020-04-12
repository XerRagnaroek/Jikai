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
package com.github.xerragnaroek.jikai.jikai;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

public class JikaiIO {
	private final static Logger log = LoggerFactory.getLogger(JikaiIO.class);

	public static void save(boolean now) {
		JM.getJDM().save(now);
		try {
			Jikai.getUserManager().save();
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed saving user db", e);
		}
	}

	public static void load() {
		log.info("Loading configurations...");
		Path loc = Core.DATA_LOC;
		try {
			if (Files.exists(loc)) {
				Files.walk(loc).filter(Files::isRegularFile).forEach(path -> {
					log.info("Found file {}", path.toAbsolutePath());
					switch (path.getFileName().toString()) {
					case "BOT.json":
						JM.getJDM().loadBotData(path);
						break;
					case "user.db":
						Jikai.getUserManager().load(path);
						break;
					case "titles.txt":
						break;
					default:
						JM.getJDM().loadData(path);
					}
				});
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				Files.createDirectory(loc);
			}
		} catch (IOException e) {
			BotUtils.logAndSendToDev(log, "Failed loading the configurations", e);
		}
	}

	public static void startSaveThread(long delay, TimeUnit unit) {
		Core.EXEC.scheduleAtFixedRate(() -> JikaiIO.save(false), delay, delay, unit);
	}

}
