package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class ChangeLocaleCommand implements JUCommand {

	@Override
	public String getName() {
		return "language";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_changeloc_desc", Arrays.asList("langs"), JikaiLocaleManager.getInstance().getAvailableLocales().toString());
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = JikaiLocaleManager.getInstance().getLocale(arguments[0]);
		if (loc != null && arguments.length > 0) {
			ju.setLocale(loc);
			ju.sendPM(loc.getString("com_ju_changeloc_success"));
		} else {
			ju.sendPM(ju.getLocale().getStringFormatted("com_ju_changeloc_fail", Arrays.asList("langs"), JikaiLocaleManager.getInstance().getAvailableLocales().toString()));
		}
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("locale", "lang");
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_changeloc_use", Arrays.asList("%com%"), getName());
	}
}
