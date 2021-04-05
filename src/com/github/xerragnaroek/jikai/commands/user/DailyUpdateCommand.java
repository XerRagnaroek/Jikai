
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class DailyUpdateCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(DailyUpdateCommand.class);

	@Override
	public String getName() {
		return "daily_update";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("daily");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale en = ju.getLocale();
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setUpdateDaily(b);
			ju.sendPM(b ? en.getString("ju_daily_ov_true") : en.getString("ju_daily_ov_false"));
		});
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_daily_ov";
	}
}
