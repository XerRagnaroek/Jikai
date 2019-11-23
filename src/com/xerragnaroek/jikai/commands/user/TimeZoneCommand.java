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
package com.xerragnaroek.jikai.commands.user;

import java.time.ZoneId;

import com.xerragnaroek.jikai.user.JikaiUser;

public class TimeZoneCommand implements JUCommand {

	@Override
	public String getName() {
		return "timezone";
	}

	@Override
	public String getAlternativeName() {
		return "tz";
	}

	@Override
	public String getDescription() {
		return "Your timezone. This effects when your notifcations are sent, so set it correctly!";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String id = arguments[0];
		try {
			ZoneId z = ZoneId.of(id);
			ju.setTimeZone(z);
		} catch (Exception e) {
			ju.sendPMFormat("'%s' isn't a known timezone.%nSee the column 'TZ database name' here: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones", id);
		}
	}

	@Override
	public String getUsage() {
		return "timezone <zone id>";
	}

}
