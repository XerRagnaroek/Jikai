package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.SubscriptionExportHandler;

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
		String key = SubscriptionExportHandler.getInstance().generateExportKey(ju, overwrite);
		ju.sendPM(key);
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_export";
	}

}
