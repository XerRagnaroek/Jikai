
package com.github.xerragnaroek.jikai.commands.user;

import org.apache.commons.lang3.ArrayUtils;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * @author XerRagnaroek
 */
public class ConfigCommand implements JUCommand {

	@Override
	public String getName() {
		return "config";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String config = ju.getConfigFormatted();
		String[] tmp = config.split("\n");
		// codeblock, codeblock end
		String[] nonFormat = { "", "" };
		if (tmp[0].contains("```") && tmp[tmp.length - 1].contains("```")) {
			// all data fields
			nonFormat[0] = tmp[0];
			nonFormat[1] = tmp[tmp.length - 1];
			tmp = ArrayUtils.subarray(tmp, 1, tmp.length - 1);
		}
		String formatted = BotUtils.padEquallyAndJoin(ju.getLocale().getString("ju_config_sep"), "\n", null, tmp);
		formatted = nonFormat[0] + "\n" + formatted + "\n" + nonFormat[1];
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("com_ju_config_eb_title")).setDescription(formatted);
		ju.sendPM(eb.build());
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_config";
	}

}
