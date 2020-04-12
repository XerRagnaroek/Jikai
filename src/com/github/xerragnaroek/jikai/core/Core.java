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
package com.github.xerragnaroek.jikai.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.JikaiManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Core {

	private final static Logger log = LoggerFactory.getLogger(Core.class);
	public static JDA JDA;
	private static String token;
	public static long DEV_ID;
	private static long saveDelay;
	public final static Logger ERROR_LOG = LoggerFactory.getLogger("ERROR");
	public static final JikaiManager JM = new JikaiManager();
	public static final Path DATA_LOC = Paths.get("./data/");
	public static final ScheduledExecutorService EXEC = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);

	public static void main(String[] args) throws LoginException, InterruptedException, ExecutionException, ClassNotFoundException, IOException {
		long mem = Runtime.getRuntime().freeMemory();
		handleArgs(args);
		JDABuilder builder = JDABuilder.create(token, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS);
		builder.disableCache(Arrays.asList(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS));
		builder.addEventListeners(new EventListener());
		JDA = builder.build();
		JDA.awaitReady();
		init(args);
		log.info("Initialized, using a total of " + ((mem - Runtime.getRuntime().freeMemory()) / 1024) + " kb of memory");
	}

	private static void init(String[] args) {
		log.info("Initializing");
		//RestAction.setDefaultFailure(e -> BotUtils.logAndSendToDev(ERROR_LOG, "", e));
		JM.init();
		JM.startSaveThread(saveDelay);
		BotUtils.sendToAllInfoChannels("I'm up and running again, please treat me kindly ;3 ||OwO I can see your bulgy wulgy... ~nya||");
	}

	private static void handleArgs(String args[]) {
		Iterator<String> it = IteratorUtils.arrayIterator(args);
		while (it.hasNext()) {
			handleArg(it, it.next());
		}
	}

	private static void handleArg(Iterator<String> it, String arg) {
		long tmp;
		try {
			switch (arg) {
				case "-token":
					token = it.next();
					log.info("Set token to '{}'", token);
					break;
				case "-dev_id":
					DEV_ID = Long.parseLong(it.next());
					log.info("Set devId to '{}'", DEV_ID);
					break;
				case "-save_delay":
					saveDelay = Long.parseLong(it.next());
					log.info("Set save_delay to " + saveDelay);
					break;
				case "-anime_base_update_rate":
					tmp = Long.parseLong(it.next());
					AnimeDB.setUpdateRate(tmp);
					log.info("Set anime_base_update_rate to " + tmp);
					break;
				default:
					ERROR_LOG.error("Unrecognized option '" + arg + "'");
					throw new IllegalArgumentException("Unrecognized option '" + arg + "'");
			}
		} catch (Exception e) {
			ERROR_LOG.error("Malformed argument: " + e.getMessage(), e);
			throw new IllegalArgumentException("Unrecognized option '" + arg + "'");
		}
	}

	public static void logThrowable(Throwable e) {
		ERROR_LOG.error("", e);
	}

}