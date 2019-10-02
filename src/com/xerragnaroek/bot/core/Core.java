package com.xerragnaroek.bot.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.alrh.ALRHManager;
import com.xerragnaroek.bot.anime.base.AnimeBase;
import com.xerragnaroek.bot.commands.CommandHandlerManager;
import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.timer.RTKManager;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.RestAction;

public class Core {

	public static final String DEV_ID = "129942311663697921";
	private final static Logger log = LoggerFactory.getLogger(Core.class);
	private static JDA jda;
	private static String token;
	private static String devId;
	private static long saveDelay;
	private final static Logger errorLog = LoggerFactory.getLogger("ERROR");

	public static void main(String[] args)
			throws LoginException, InterruptedException, ExecutionException, ClassNotFoundException, IOException {
		handleArgs(args);
		JDABuilder builder = new JDABuilder(AccountType.BOT);
		//String token = "NjA1MzgzMDU4NDU1MDAzMTU2.XUBVLw.4kUrz7id1T53iqKQar3XbEcpvng";
		builder.setToken(token);
		builder.addEventListeners(new EventListener());
		jda = builder.build();
		jda.awaitReady();
		init(args);
	}

	private static void init(String[] args) {
		log.info("Initializing");
		RestAction.setDefaultFailure(e -> BotUtils.logAndSendToDev(errorLog, "", e));
		GuildDataManager.init();
		AnimeBase.init();
		AnimeBase.waitUntilLoaded();
		CommandHandlerManager.init();
		RTKManager.init();
		ALRHManager.init();
		GuildDataManager.startSaveThread(saveDelay, TimeUnit.SECONDS);
		RTKManager.startReleaseUpdateThread();
	}

	private static void handleArgs(String args[]) {
		Iterator<String> it = IteratorUtils.arrayIterator(args);
		while (it.hasNext()) {
			handleArg(it, it.next());
		}
	}

	private static void handleArg(Iterator<String> it, String arg) {
		switch (arg) {
		case "-token":
			token = it.next();
			break;
		case "-dev_id":
			devId = it.next();
			break;
		case "-save_delay":
			saveDelay = Long.parseLong(it.next());
			break;
		case "-commands_enabled_default":
			CommandHandlerManager.setCommandsEnabledDefault(Boolean.parseBoolean(it.next()));
		default:
			throw new IllegalArgumentException("Unrecognized option '" + arg + "'");
		}
	}

	public static JDA getJDA() {
		return jda;
	}

	public static String getDevId() {
		return devId;
	}

}
