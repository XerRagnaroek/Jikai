
package com.github.xerragnaroek.jikai.user;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.ani.AniLinker;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class JikaiUserSetup extends ListenerAdapter {
	private final Logger log;
	private JikaiUser ju;
	private int stage = -1;
	private Jikai j;

	public JikaiUserSetup(JikaiUser ju, Jikai j) {
		this.ju = ju;
		this.j = j;
		log = LoggerFactory.getLogger(JikaiUserSetup.class + "#" + ju.getId());
		Core.JDA.addEventListener(this);
	}

	public void startSetup() {
		log.debug("Running setup for new user");
		CompletableFuture<Boolean> cf = ju.sendPMFormat(ju.getLocale().getStringFormatted("setup_greetings", Arrays.asList("name"), ju.getUser().getName()));
		cf.whenComplete((b, e) -> {
			if (b) {
				nextStage();
			} else {
				JikaiUserManager.getInstance().removeUser(ju.getId());
			}
		});

	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().getIdLong() == ju.getId()) {
			String input = event.getMessage().getContentDisplay();
			boolean successful = false;
			try {
				switch (stage) {
					case 0 -> successful = language(input);
					case 1 -> successful = timeZone(input);
					case 2 -> successful = dailyUpdate(input);
					case 3 -> successful = notifyOnRelease(input);
					case 4 -> successful = sendWeeklySchedule(input);
					case 5 -> successful = titleLanguage(input);
					case 6 -> successful = notifySteps(input);
					// case 7 -> successful = linkAniAccount(input);
				}
				if (successful) {
					nextStage();
				}
			} catch (InterruptedException | ExecutionException e) {
				log.error("Error during setup, repeating stage", e);
			}
		}
	}

	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		if (ju.getId() == event.getUser().getIdLong()) {
			log.debug("User left server before the setup was completed!");
			cancelSetup();
		}
	}

	private boolean language(String input) throws InterruptedException, ExecutionException {
		JikaiLocaleManager jlm = JikaiLocaleManager.getInstance();
		if (input == null) {
			log.debug("Stage Language: First message");
			ju.sendPM(JikaiLocaleManager.getEN().getStringFormatted("setup_lang", Arrays.asList("langs"), jlm.getLocales().stream().map(jl -> String.format("%s[%s]", jl.getLanguageName(), jl.getIdentifier())).collect(Collectors.joining(", "))));
		} else {
			log.debug("Stage Language: Computing input");
			if (jlm.hasLocale(input)) {
				JikaiLocale loc = jlm.getLocale(input);
				ju.setLocale(loc);
				ju.sendPM(loc.getStringFormatted("setup_lang_success", Arrays.asList("lang"), loc.getLanguageName())).get();
				log.debug("Stage Language: Lang set to {}", loc.getIdentifier());
			} else {
				log.debug("Stage Language: Unrecognized input");
				ju.sendPM(JikaiLocaleManager.getEN().getStringFormatted("setup_lang_fail", Arrays.asList("input"), input)).get();
				return false;
			}
		}
		return true;
	}

	private boolean timeZone(String input) throws InterruptedException, ExecutionException {
		JikaiLocale loc = ju.getLocale();
		if (input == null) {
			log.debug("Stage TimeZone: First message");
			ju.sendPM(loc.getStringFormatted("setup_timezone", Arrays.asList("servertz"), j.getJikaiData().getTimeZone().getId()));
		} else {
			try {
				log.debug("Stage TimeZone: Computing input");
				ZoneId z;
				if (input.toLowerCase().equals("ok")) {
					z = j.getJikaiData().getTimeZone();
				} else {
					z = ZoneId.of(input);
				}
				ju.setTimeZone(z);
				ju.sendPM(loc.getStringFormatted("setup_timezone_success", Arrays.asList("zone"), z.getId())).get();
				log.debug("Stage TimeZone: TZ set to {}", z);
			} catch (DateTimeException e) {
				ju.sendPM(loc.getStringFormatted("setup_timezone_fail", Arrays.asList("input"), input)).get();
				log.debug("Stage TimeZone: Malformed zone id");
				return false;
			}
		}
		return true;
	}

	private boolean dailyUpdate(String input) throws InterruptedException, ExecutionException {
		return yesNoOption(input, "setup_daily_update", () -> ju.setUpdateDaily(true), "setup_daily_update_y", () -> ju.setUpdateDaily(false), "setup_daily_update_n");
	}

	private boolean notifyOnRelease(String input) throws InterruptedException, ExecutionException {
		return yesNoOption(input, "setup_daily_notify", () -> ju.setNotifyToRelease(true), "setup_daily_notify_y", () -> ju.setNotifyToRelease(false), "setup_daily_notify_n");
	}

	private boolean sendWeeklySchedule(String input) throws InterruptedException, ExecutionException {
		return yesNoOption(input, "setup_weekly_schedule", () -> ju.setNotifyToRelease(true), "setup_weekly_schedule_y", () -> ju.setNotifyToRelease(false), "setup_weekly_schedule_n");
	}

	private boolean yesNoOption(String input, String message, Runnable yes, String yesMsg, Runnable no, String noMsg) throws InterruptedException, ExecutionException {
		JikaiLocale loc = ju.getLocale();
		if (input == null) {
			log.debug("Stage YesNo: First Message '{}'", message);
			ju.sendPM(loc.getString(message) + " **(yes|y|n|no)**").get();
		} else {
			log.debug("Stage YesNo: Handling input");
			switch (input) {
				case "yes":
				case "y":
					if (yesMsg != null) {
						String msg = loc.getString(yesMsg);
						if (!msg.isEmpty()) {
							ju.sendPM(msg).get();
						}
					}
					yes.run();
					break;
				case "n":
				case "no":
					if (noMsg != null) {
						String msg = loc.getString(yesMsg);
						if (!msg.isEmpty()) {
							ju.sendPM(msg).get();
						}
					}
					no.run();
					break;
				default:
					return false;
			}
			log.debug("Stage YesNo: Sucess");
		}
		return true;
	}

	private boolean titleLanguage(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			log.debug("Stage TitleLanguage: First Message");
			ju.sendPM(ju.getLocale().getString("setup_title_lang")).get();
		} else {
			try {
				int tt = Integer.parseInt(input);
				if (tt > 3) {
					ju.sendPM(ju.getLocale().getString("setup_title_lang_fail")).get();
					return false;
				}
				ju.setTitleLanguage(TitleLanguage.values()[--tt]);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	private boolean notifySteps(String input) throws InterruptedException, ExecutionException {
		JikaiLocale loc = ju.getLocale();
		if (input == null) {
			log.debug("Stage NotifySteps: First Message");
			ju.sendPM(loc.getString("setup_notify_steps")).get();
		} else {
			if (input.equals("n") || input.equals("no")) {
				log.debug("Stage NotifySteps: User wants no steps");
				return true;
			}
			if (!input.contains("d") && !input.contains("h") && !input.contains("m") && !input.contains(",")) {
				ju.sendPM(loc.getString("setup_notify_steps_invalid_format")).get();
				log.debug("Stage NotifySteps: Invalid format string");
				return false;
			} else {
				if (!ju.addReleaseSteps(input)) {
					ju.sendPM(loc.getString("setup_notify_steps_invalid_numbers")).get();
					log.debug("Stage NotifySteps: User didn't supply any valid numbers");
					return false;
				}
				log.debug("Stage NotifySteps: Sucess");
			}
		}
		return true;
	}

	private boolean linkAniAccount(String input) throws InterruptedException, ExecutionException {
		JikaiLocale loc = ju.getLocale();
		if (input == null) {
			log.debug("Stage LinkAccount: First Message");
			ju.sendPM(loc.getString("setup_link_ani")).get();
		} else {
			if (input.equals("no") || input.equals("n")) {
				log.debug("Skip link");
				setupComplete();
			} else {
				AniLinker.linkAniAccount(ju, input).whenComplete((b, e) -> {
					if (b) {
						setupComplete();
					}
				});
			}
		}
		return false;
	}

	private void setupComplete() {
		log.debug("Stage SetupComplete: Finishing setup");
		Core.JDA.removeEventListener(this);
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		JikaiLocale loc = ju.getLocale();
		eb.setTitle(loc.getString("setup_eb_completed_title"));
		eb.setDescription(loc.getStringFormatted("setup_eb_completed_desc", Arrays.asList("settings", "pre"), ju.getConfigFormatted(), Core.JM.getJDM().getBotData().getDefaultPrefix()));
		ju.setSetupCompleted(true);
		ju.sendPM(eb.build());
		log.info("Stage SetupComplete: Completed setup for JikaiUser{" + ju.toString() + "}");
	}

	private void nextStage() {
		log.debug("Next stage");
		try {
			switch (stage) {
				case -1 -> language(null);
				case 0 -> timeZone(null);
				case 1 -> dailyUpdate(null);
				case 2 -> notifyOnRelease(null);
				case 3 -> sendWeeklySchedule(null);
				case 4 -> titleLanguage(null);
				case 5 -> notifySteps(null);
				// case 6 -> linkAniAccount(null);
				case 6 -> setupComplete();
			}
			stage++;
		} catch (InterruptedException | ExecutionException e) {
			BotUtils.logAndSendToDev(log, "Error during stage, notifying user", e);
			ju.sendPM("I'm terribly sorry but there was an internal error! I've notifed the dev but if you happen to see him, send him this:\n||" + e.getMessage() + "||");
			cancelSetup();
		}
	}

	private void cancelSetup() {
		log.debug("Cancelling setup!");
		JikaiUserManager.getInstance().removeUser(ju.getId());
		Core.JDA.removeEventListener(this);
	}

	public static void runSetup(JikaiUser ju, Jikai j) {
		new JikaiUserSetup(ju, j).startSetup();
	}
}
