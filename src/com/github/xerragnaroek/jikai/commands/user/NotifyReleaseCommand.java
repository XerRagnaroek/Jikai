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
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class NotifyReleaseCommand implements JUCommand {

	@Override
	public String getName() {
		return "notify_release";
	}

	@Override
	public String getAlternativeName() {
		return "nr";
	}

	@Override
	public String getDescription() {
		return "Enable/Disable the notification upon release of one of your subscribed anime.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setNotifyToRelease(b);
			ju.sendPM(b ? "You will recieve a daily overview of all your subscribed anime releasing on that day!" : "You won't recieve the daily overview.");
		});
	}

	@Override
	public String getUsage() {
		return "notify_release <true|false>";
	}
}
