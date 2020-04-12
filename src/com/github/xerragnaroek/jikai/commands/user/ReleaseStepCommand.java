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

import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class ReleaseStepCommand implements JUCommand {

	@Override
	public String getName() {
		return "release_step";
	}

	@Override
	public String getAlternativeName() {
		return "rs";
	}

	@Override
	public String getDescription() {
		return "Add or remove release steps (certain times until the release of an anime where you'll be notfied)./nExample: `!release_step add 2d,12h,45m` to add a step at 2 days, at 12 hours and at 45 minutes before a release.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String tmp = arguments[1];
		if (!tmp.contains("d") && !tmp.contains("h") && !tmp.contains("m") && !tmp.contains(",")) {
			ju.sendPMFormat("'%s' isn't a valid format string! Seperate your steps with a ',' and 'd' = days, 'h' = hours, 'm' = minutes. E.g. `1d,6h,30m`", tmp);
			return;
		}
		if (arguments[0].equals("add")) {
			ju.addReleaseSteps(tmp);
		} else if (arguments[0].equals("remove") || arguments[0].equals("rem")) {
			ju.removeReleaseSteps(tmp);
		}
	}

	@Override
	public String getUsage() {
		return "release_step <add|remove> <steps> ";
	}
}
