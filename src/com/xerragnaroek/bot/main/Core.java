package com.xerragnaroek.bot.main;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.ALRHManager;
import com.xerragnaroek.bot.anime.AnimeBase;
import com.xerragnaroek.bot.commands.CommandHandler;
import com.xerragnaroek.bot.config.ConfigManager;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Core {

	private final static Logger log = LoggerFactory.getLogger(Core.class);
	private static JDA jda;

	public static void main(String[] args) throws LoginException, InterruptedException, ExecutionException, ClassNotFoundException, IOException {
		init();
		JDABuilder builder = new JDABuilder(AccountType.BOT);
		//String token = "NjA1MzgzMDU4NDU1MDAzMTU2.XUBVLw.4kUrz7id1T53iqKQar3XbEcpvng";
		builder.setToken(args[0]);
		builder.addEventListeners(CommandHandler.getInstance(), new ReactionRoleTest());
		jda = builder.build();
		jda.awaitReady();
		/*Instant start = Instant.now();
		AnimeSearch as = new AnimeSearch().setStatus(AnimeStati.AIRING);
		AnimePage ap = as.get().get();
		int page = 1;
		AtomicInteger c = new AtomicInteger(0);
		while (!ap.animes.isEmpty()) {
			System.out.println("Page " + page);
			ap.animes.forEach(a -> c.incrementAndGet());
			Thread.sleep(1000);
			as.setPage(page++);
			ap = as.get().get();
		}
		System.out.println("MAL lists a totla of " + c.get() + " currently airing animes. Listing was completed in " + Duration.between(start,
																																		Instant.now()).toMillis() + "ms.");*/
		/*Schedule s = new Connector().getCurrentSchedule().get();
		Consumer<SubAnime> printTitle = a -> System.out.println(a.title);
		System.out.println("\nMonday:\n");
		s.monday.forEach(printTitle);
		System.out.println("\nTuesday:\n");
		s.tuesday.forEach(printTitle);
		System.out.println("\nWednesday:\n");
		s.wednesday.forEach(printTitle);
		System.out.println("\nThursday:\n");
		s.thursday.forEach(printTitle);
		System.out.println("\nFriday:\n");
		s.friday.forEach(printTitle);
		System.out.println("\nSaturday:\n");
		s.saturday.forEach(printTitle);
		System.out.println("\nSunday:\n");
		s.sunday.forEach(printTitle);
		System.out.println("\nOthers:\n");
		s.others.forEach(printTitle);
		System.out.println("\nUnknown:\n");
		s.unknown.forEach(printTitle);*/
	}

	private static void init() {
		log.info("Initializing");
		ConfigManager.init();
		CommandHandler.init();
		AnimeBase.init();
		AnimeBase.waitUntilLoaded();
		ALRHManager.init();
	}

	public static JDA getJDA() {
		return jda;
	}

}
