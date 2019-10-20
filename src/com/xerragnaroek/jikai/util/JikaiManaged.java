package com.xerragnaroek.jikai.util;

import com.xerragnaroek.jikai.data.JikaiGuild;

public class JikaiManaged {
	private JikaiGuild jg;

	protected JikaiManaged() {}

	public void setJikaiGuild(JikaiGuild jig) {
		if (jg == null) {
			jg = jig;
		}
	}

	public JikaiGuild getJikaiGuild() {
		return jg;
	}
}
