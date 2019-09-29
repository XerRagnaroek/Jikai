package com.xerragnaroek.bot.core;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.base.AnimeBase;
import com.xerragnaroek.bot.commands.CommandHandlerManager;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Core {

	private final static Logger log = LoggerFactory.getLogger(Core.class);
	private static JDA jda;

	public static void main(String[] args)
			throws LoginException, InterruptedException, ExecutionException, ClassNotFoundException, IOException {
		init();
		JDABuilder builder = new JDABuilder(AccountType.BOT);
		//String token = "NjA1MzgzMDU4NDU1MDAzMTU2.XUBVLw.4kUrz7id1T53iqKQar3XbEcpvng";
		builder.setToken(args[0]);
		builder.addEventListeners(new EventListener());
		jda = builder.build();
		jda.awaitReady();
	}

	private static void init() {
		log.info("Initializing");
		GuildDataManager.init();
		AnimeBase.init();
		AnimeBase.waitUntilLoaded();
		CommandHandlerManager.init();

		/*ALRHManager.init(); is called in the onReady of the EventListener*/
	}

	public static JDA getJDA() {
		return jda;
	}

}
