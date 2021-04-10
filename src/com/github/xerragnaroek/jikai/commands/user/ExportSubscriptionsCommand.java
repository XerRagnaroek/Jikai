package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.ExportKeyHandler;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * 
 */
public class ExportSubscriptionsCommand implements JUCommand {

	@Override
	public String getName() {
		return "export";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		boolean overwrite = arguments.length > 0 && arguments[0].equals("new");
		String key = ExportKeyHandler.getInstance().generateExportKey(ju, overwrite);
		JikaiLocale loc = ju.getLocale();
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setTitle(loc.getString(getLocaleKey() + "_eb_title")).setDescription(loc.getStringFormatted(getLocaleKey() + "_eb_msg", Arrays.asList("key"), key));
		ju.sendPM(eb.build());
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_export";
	}

}
