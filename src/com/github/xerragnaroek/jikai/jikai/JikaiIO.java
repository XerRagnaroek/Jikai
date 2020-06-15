
package com.github.xerragnaroek.jikai.jikai;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

public class JikaiIO {
	private final static Logger log = LoggerFactory.getLogger(JikaiIO.class);

	public static void save(boolean now) {
		JM.getJDM().save(now);
		try {
			JikaiUserManager.getInstance().save();
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed saving user db", e);
		}
	}

	public static void load() {
		log.info("Loading data...");
		Path loc = Core.DATA_LOC;
		JikaiLocaleManager jlm = JikaiLocaleManager.getInstance();
		try {
			if (Files.exists(loc)) {
				Files.walk(loc).filter(Files::isRegularFile).forEach(path -> {
					log.info("Found file {}", path.toAbsolutePath());
					String fileName = path.getFileName().toString();
					if (!path.getParent().getFileName().toString().equals("locales")) {
						switch (fileName) {
							case "BOT.json" -> JM.getJDM().loadBotData(path);
							case "user.db" -> JikaiUserManager.getInstance().load(path);
							default -> {
								if (!fileName.equals("jikai.png")) {
									JM.getJDM().loadData(path);
								}
							}
						}
					}
				});
			} else {
				log.debug("Creating config directory");
				log.info("No configurations found, falling back to default settings");

				Files.createDirectory(loc);
			}
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed loading the data!", e);
		}
	}

	public static void startSaveThread(long delay, TimeUnit unit) {
		Core.EXEC.scheduleAtFixedRate(() -> JikaiIO.save(false), delay, delay, unit);
	}

}
