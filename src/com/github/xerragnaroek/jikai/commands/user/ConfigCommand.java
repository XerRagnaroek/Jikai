
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * @author XerRagnaroek
 *
 */
public class ConfigCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(ConfigCommand.class);

	@Override
	public String getName() {
		return "config";
	}

	@Override
	public String getDescription() {
		return "Shows your current configuration.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		StringBuilder bob = new StringBuilder();
		bob.append("```asciidoc\n");
		bob.append("Time zone :: " + ju.getTimeZone() + "\n");
		bob.append("Send daily overview :: " + (ju.isUpdatedDaily() ? "yes" : "no") + "\n");
		bob.append("Notified on release :: " + (ju.isNotfiedOnRelease() ? "yes" : "no") + "\n");
		bob.append("Title language :: " + ju.getTitleLanguage() + "\n");
		Set<Integer> steps = ju.getPreReleaseNotifcationSteps().stream().map(i -> i / 60).collect(Collectors.toSet());
		bob.append("Release update steps :: " + (steps.isEmpty() ? "None" : StringUtils.join(steps, "min, ") + "min before a release") + "\n```");
		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(bob);
		ju.setSetupCompleted(true);
		ju.sendPM(eb.build());
	}

}
