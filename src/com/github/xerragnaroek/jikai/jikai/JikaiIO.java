
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
