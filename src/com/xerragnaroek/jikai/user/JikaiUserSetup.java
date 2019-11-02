package com.xerragnaroek.jikai.user;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class JikaiUserSetup extends ListenerAdapter {
	private final Logger log;
	private JikaiUser ju;
	private int stage = -1;

	public JikaiUserSetup(JikaiUser ju) {
		this.ju = ju;
		log = LoggerFactory.getLogger(JikaiUserSetup.class + "" + ju.getId());
		Core.JDA.addEventListener(this);
	}

	//TODO think of some nice words
	public void startSetup() {
		log.debug("Running setup for new user");
		try {
			ju.sendPMFormat("<PLACEHOLDER GREETINGS>, %s.%nBefore you can subscribe to anime and be sent updates, you need to complete a short setup first.").get();
			nextStage();
		} catch (InterruptedException | ExecutionException e) {
			log.error("", e);
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			if (event.getAuthor().getIdLong() == ju.getId()) {
				String input = event.getMessage().getContentDisplay();
				boolean successful = false;
				try {
					switch (stage) {
					case 0:
						successful = timeZone(input);
						break;
					case 1:
						successful = dailyUpdate(input);
						break;
					case 2:
						successful = notifyOnRelease(input);
						break;
					case 3:
						successful = releaseSteps(input);
						break;
					default:
						return;
					}
					if (successful) {
						nextStage();
					}
				} catch (InterruptedException | ExecutionException e) {
					log.error("Error during setup, repeating stage", e);
				}
			}
		}
	}

	private boolean timeZone(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			ju.sendPM("What is your time zone? E.g. 'Europe/Berlin', 'GMT' or  'America/Chicago'\n||See the column 'TZ database name' here: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones||");
		} else {
			try {
				ZoneId z = ZoneId.of(input);
				ju.setTimeZone(z);

			} catch (DateTimeException e) {
				ju.sendPMFormat("\"%s\" is not a known time zone. Check the link above for yours.").get();
				return false;
			}
		}
		return true;
	}

	private boolean dailyUpdate(String input) throws InterruptedException, ExecutionException {
		return yesNoOption(input, "Do you want to recieve a daily overview of which anime will air that day?", () -> ju.setUpdateDaily(true), "You'll recieve a daily overview at midnight.", () -> ju.setUpdateDaily(false), null);
	}

	private boolean notifyOnRelease(String input) throws InterruptedException, ExecutionException {
		return yesNoOption(input, "Should I send you a message when an anime releases?", () -> ju.setNotifyToRelease(true), null, () -> ju.setNotifyToRelease(false), null);
	}

	private boolean yesNoOption(String input, String message, Runnable yes, String yesMsg, Runnable no, String noMsg) throws InterruptedException, ExecutionException {
		if (input == null) {
			ju.sendPM(message + " (yes|y|n|no)").get();
		} else {
			switch (input) {
			case "yes":
			case "y":
				if (yesMsg != null) {
					ju.sendPM(yesMsg).get();
				}
				yes.run();
				break;
			case "n":
			case "no":
				if (noMsg != null) {
					ju.sendPM(noMsg).get();
				}
				no.run();
				break;
			default:
				return false;
			}
		}
		return true;
	}

	private boolean releaseSteps(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			ju.sendPM("You can define times until a relase when I will notify you.\nFor example:\nTo be sent a message 3 days,1 day, 1 hour and 15 minutes before an anime releases, type \"3d,1d,1h,15m\"\nThis will automatically ignore any input above a week because that's just overkill.\nIf you don't want any of that, type \"no\" or \"n\"").get();
		} else {
			if (input.equals("n") || input.equals("no")) {
				return true;
			}
			if (!input.contains("d") && !input.contains("h") && !input.contains("m") && !input.contains(",")) {
				ju.sendPM("That isn't a valid format string. Please try again.").get();
				return false;
			} else {
				String[] tmp = input.split(",");
				int nfe = 0;
				for (String s : tmp) {
					char end = s.charAt(s.length());
					s = StringUtils.chop(s);
					try {
						long l = Long.parseLong(s);
						switch (end) {
						case 'd':
							if (l <= 7) {
								ju.addPreReleaseNotificaionStep(TimeUnit.DAYS.toMinutes(l));
							}
							break;
						case 'h':
							if (l <= 168) {
								ju.addPreReleaseNotificaionStep(l * 60);
							}
							break;
						case 'm':
							if (l <= 10080) {
								ju.addPreReleaseNotificaionStep(l);
							}
							break;
						}
					} catch (NumberFormatException e) {
						nfe++;
					}
				}
				if (nfe == tmp.length) {
					ju.sendPM("None of those was a number. Try again or type \"no\" to not add any steps.").get();
					return false;
				}
			}
		}
		return true;
	}

	private void setupComplete() {
		Core.JDA.removeEventListener(this);
		StringBuilder bob = new StringBuilder();
		bob.append("Setup complete!\nHere are your current settings:\n");
		bob.append("```asciidoc\n");
		bob.append("Time zone :: " + ju.getTimeZone() + "\n");
		bob.append("Send daily overview :: " + (ju.isUpdatedDaily() ? "yes" : "no") + "\n");
		bob.append("Notified on release :: " + (ju.isNotfiedOnRelease() ? "yes" : "no") + "\n");
		Set<Long> steps = ju.getPreReleaseNotifcationSteps();
		bob.append("Release update steps :: " + (steps.isEmpty() ? "None" : StringUtils.join(steps, "min, ") + "min before a release") + "\n");
		bob.append("```\n");
		bob.append("You can now subscribe to anime on any server running Jikai!\nFor a list of all commands use the `" + Core.JM.getJDM().getBotData().getDefaultTrigger() + "help` command ");
		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(bob);
		ju.setSetupCompleted(true);
		ju.sendPM(eb.build());
		log.info("Completed setup!");
	}

	private void nextStage() {
		try {
			switch (stage) {
			case -1:
				timeZone(null);
				break;
			case 0:
				dailyUpdate(null);
				break;
			case 1:
				notifyOnRelease(null);
				break;
			case 2:
				releaseSteps(null);
				break;
			case 3:
				setupComplete();
				break;
			}
			stage++;
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error during setup, repeating stage", e);
			nextStage();
		}
	}

	public static void runSetup(JikaiUser ju) {
		ForkJoinPool.commonPool().submit(() -> new JikaiUserSetup(ju).startSetup());
	}
}
