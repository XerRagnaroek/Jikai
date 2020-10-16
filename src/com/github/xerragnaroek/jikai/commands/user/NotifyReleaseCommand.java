
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class NotifyReleaseCommand implements JUCommand {

	@Override
	public String getName() {
		return "notify_release";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("nr");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_notify_release_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setNotifyToRelease(b);
		});
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_notify_release_use", Arrays.asList("com"), getName());
	}
}
