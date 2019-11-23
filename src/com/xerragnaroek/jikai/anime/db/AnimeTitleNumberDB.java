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
package com.xerragnaroek.jikai.anime.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualTreeBidiMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;

/**
 * @author XerRagnaroek
 *
 */
public class AnimeTitleNumberDB {
	private Path loc = Paths.get(Core.DATA_LOC.toAbsolutePath().toString(), "/titles.txt");
	private BidiMap<String, Integer> map = new DualTreeBidiMap<>();
	private AtomicInteger entryNum = new AtomicInteger(0);
	private final Logger log = LoggerFactory.getLogger(AnimeTitleNumberDB.class);

	AnimeTitleNumberDB() {
		init();
	}

	private void init() {
		if (Files.exists(loc)) {
			log.debug("File exists, loading");
			load();
		} else {
			try {
				log.debug("File doesn't exist, creating it");
				Files.createFile(loc);
			} catch (IOException e) {
				log.error("Failed creating title num file", e);
			}
		}
	}

	private void load() {
		try {
			Files.lines(loc).peek(str -> entryNum.incrementAndGet()).forEach(str -> {
				String num = StringUtils.substringBefore(str, "=").trim();
				String title = StringUtils.substringAfter(str, "=").trim();
				log.info("Loaded " + str);
				map.put(title, Integer.parseInt(num));
			});
		} catch (IOException e) {
			log.error("Failed loading the title num file", e);
		}
	}

	void store(String title) {
		if (!map.containsKey(title)) {
			map.put(title, entryNum.incrementAndGet());
			log.info("Stored new anime: {} ,#{}", title, entryNum.get());
			Core.EXEC.execute(() -> save());
		}
	}

	int getNumber(String title) {
		if (map.containsKey(title)) {
			return map.get(title);
		}
		return -1;
	}

	String getTitle(int num) {
		return map.getKey(num);
	}

	private void save() {
		StringBuilder bob = new StringBuilder();
		synchronized (map) {
			map.values().forEach(n -> bob.append(n + "=" + map.getKey(n) + "\n"));
		}
		try {
			Files.writeString(loc, bob);
			log.debug("Saved {} entries", map.size());
		} catch (IOException e) {
			log.error("Failed saving", e);
		}
	}
}
