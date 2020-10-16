
package com.github.xerragnaroek.jikai.commands.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * @author XerRagnaroek
 */
public class ConfigCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(ConfigCommand.class);

	@Override
	public String getName() {
		return "config";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_config_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(ju.getConfigFormatted());
		BotUtils.addJikaiMark(eb);
		ju.sendPM(eb.build());
	}

}
