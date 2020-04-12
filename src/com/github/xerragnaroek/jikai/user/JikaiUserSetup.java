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
package com.github.xerragnaroek.jikai.user;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class JikaiUserSetup extends ListenerAdapter {
	private final Logger log;
	private JikaiUser ju;
	private int stage = -1;

	public JikaiUserSetup(JikaiUser ju) {
		this.ju = ju;
		log = LoggerFactory.getLogger(JikaiUserSetup.class + "#" + ju.getId());
		Core.JDA.addEventListener(this);
	}

	//TODO think of some nice words
	public void startSetup() {
		log.debug("Running setup for new user");
		try {
			ju.sendPMFormat("<PLACEHOLDER GREETINGS>, %s.%nBefore you can subscribe to anime and be sent updates, you need to complete a short setup first.", ju.getUser().getName()).get();
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
							successful = titleLanguage(input);
							break;
						case 4:
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
			log.debug("Stage TimeZone: First message");
			ju.sendPM("What is your time zone? E.g. 'Europe/Berlin', 'GMT' or  'America/Chicago'\n||See the column 'TZ database name' here: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones||");
		} else {
			try {
				log.debug("Stage TimeZone: Computing input");
				ZoneId z = ZoneId.of(input);
				ju.setTimeZone(z);
				log.debug("Stage TimeZone: TZ set to {}", z);
			} catch (DateTimeException e) {
				ju.sendPM("That is not a known time zone. Check the link above for yours.").get();
				log.debug("Stage TimeZone: Malformed zone id");
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
			log.debug("Stage YesNo: First Message '{}'", message);
			ju.sendPM(message + " (yes|y|n|no)").get();
		} else {
			log.debug("Stage YesNo: Handling input");
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
			log.debug("Stage YesNo: Sucess");
		}
		return true;
	}

	private boolean titleLanguage(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			log.debug("Stage TitleLanguage: First Message");
			ju.sendPM("How do you want your titles to be displayed?\n**1. English** - 'Is This a Zombie?'\n**2. Native** - 'これはゾンビですか?'\n**3. Romaji** - 'Kore wa Zombie Desu ka?'").get();
		} else {
			try {
				int tt = Integer.parseInt(input);
				if (tt > 3) {
					ju.sendPM("1, 2 or 3.").get();
					return false;
				}
				ju.setTitleLanguage(TitleLanguage.values()[--tt]);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	private boolean releaseSteps(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			log.debug("Stage ReleaseSteps: First Message");
			ju.sendPM("You can define times until a relase when I will notify you.\nFor example:\nTo be sent a message 3 days,1 day, 1 hour and 15 minutes before an anime releases, type \"3d,1d,1h,15m\"\nThis will automatically ignore any input above a week because that's just overkill.\nIf you don't want any of that, type \"no\" or \"n\"").get();
		} else {
			if (input.equals("n") || input.equals("no")) {
				log.debug("Stage ReleaseSteps: User wants no steps");
				return true;
			}
			if (!input.contains("d") && !input.contains("h") && !input.contains("m") && !input.contains(",")) {
				ju.sendPM("That isn't a valid format string. Please try again.").get();
				log.debug("Stage ReleaseSteps: Invalid format string");
				return false;
			} else {
				if (!ju.addReleaseSteps(input)) {
					ju.sendPM("None of those was a number. Try again or type \"no\" to not add any steps.").get();
					log.debug("Stage ReleaseSteps: User didn't supply any valid numbers");
					return false;
				}
				log.debug("Stage ReleaseSteps: Sucess");
			}
		}
		return true;
	}

	private void setupComplete() {
		log.debug("Stage SetupComplete: Finishing setup");
		Core.JDA.removeEventListener(this);
		StringBuilder bob = new StringBuilder();
		bob.append("Setup complete!\nHere are your current settings:\n");
		bob.append("```asciidoc\n");
		bob.append("Time zone :: " + ju.getTimeZone() + "\n");
		bob.append("Send daily overview :: " + (ju.isUpdatedDaily() ? "yes" : "no") + "\n");
		bob.append("Notified on release :: " + (ju.isNotfiedOnRelease() ? "yes" : "no") + "\n");
		bob.append("Title language :: " + ju.getTitleLanguage() + "\n");
		Set<Integer> steps = ju.getPreReleaseNotifcationSteps();
		bob.append("Release update steps :: " + (steps.isEmpty() ? "None" : StringUtils.join(steps, "min, ") + "min before a release") + "\n");
		bob.append("```\n");
		bob.append("You can now subscribe to anime on any server running Jikai!\nFor a list of all commands use the `" + Core.JM.getJDM().getBotData().getDefaultTrigger() + "help` command ");
		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(bob);
		ju.setSetupCompleted(true);
		ju.sendPM(eb.build());
		log.info("Stage SetupComplete: Completed setup for JikaiUser{" + ju.toString() + "}");
	}

	private void nextStage() {
		log.debug("Next stage");
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
					titleLanguage(null);
					break;
				case 3:
					releaseSteps(null);
					break;
				case 4:
					setupComplete();
					break;
			}
			stage++;
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error during stage, repeating", e);
			nextStage();
		}
	}

	public static void runSetup(JikaiUser ju) {
		new JikaiUserSetup(ju).startSetup();
	}
}