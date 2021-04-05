
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.ComUtils;
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
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setNotifyToRelease(b);
		});
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_notify_release";
	}
}
