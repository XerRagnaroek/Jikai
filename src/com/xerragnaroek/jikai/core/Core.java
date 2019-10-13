package com.xerragnaroek.jikai.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.alrh.ALRHManager;
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.xerragnaroek.jikai.commands.CommandHandlerManager;
import com.xerragnaroek.jikai.data.GuildDataManager;
import com.xerragnaroek.jikai.timer.RTKManager;
import com.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.RestAction;

public class Core {

	private final static Logger log = LoggerFactory.getLogger(Core.class);
	public static JDA JDA;
	private static String token;
	public static String DEV_ID;
	private static long saveDelay;
	public final static Logger ERROR_LOG = LoggerFactory.getLogger("ERROR");
	public static final GuildDataManager GDM = new GuildDataManager();
	public static final ALRHManager ALRHM = new ALRHManager();
	public static final CommandHandlerManager CHM = new CommandHandlerManager();
	public static final RTKManager RTKM = new RTKManager();
	public static final ScheduleManager SM = new ScheduleManager();

	public static void main(String[] args) throws LoginException, InterruptedException, ExecutionException, ClassNotFoundException, IOException {
		handleArgs(args);
		JDABuilder builder = new JDABuilder(AccountType.BOT);
		builder.setToken(token);
		builder.addEventListeners(new EventListener());
		JDA = builder.build();
		JDA.awaitReady();
		init(args);
	}

	private static void init(String[] args) {
		log.info("Initializing");
		RestAction.setDefaultFailure(e -> BotUtils.logAndSendToDev(ERROR_LOG, "", e));
		GDM.init();
		AnimeDB.init();
		AnimeDB.waitUntilLoaded();
		CHM.init();
		RTKM.init();
		ALRHM.init();
		SM.init();
		GDM.startSaveThread(saveDelay, TimeUnit.MINUTES);
		RTKM.startReleaseUpdateThread();
	}

	private static void handleArgs(String args[]) {
		Iterator<String> it = IteratorUtils.arrayIterator(args);
		while (it.hasNext()) {
			handleArg(it, it.next());
		}
	}

	private static void handleArg(Iterator<String> it, String arg) {
		int t;
		long tmp;
		try {
			switch (arg) {
			case "-token":
				token = it.next();
				log.info("Set token to '{}'", token);
				break;
			case "-dev_id":
				DEV_ID = it.next();
				log.info("Set devId to '{}'", DEV_ID);
				break;
			case "-save_delay":
				saveDelay = Long.parseLong(it.next());
				log.info("Set save_delay to " + saveDelay);
				break;
			case "-commands_default_enabled":
				CHM.setCommandsEnabledDefault(true);
				log.info("Commands are now enabled by default");
				break;
			case "-release_update_rate":
				tmp = Long.parseLong(it.next());
				RTKM.setUpdateRate(tmp);
				log.info("Set release_update_rate to " + tmp);
				break;
			case "-day_threshold":
				t = Integer.parseInt(it.next());
				RTKM.setDayThreshold(t);
				log.info("Set day_threshold to " + t);
				break;
			case "-hour_threshold":
				t = Integer.parseInt(it.next());
				RTKM.setHourThreshold(t);
				log.info("Set hour_threshold to " + t);
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