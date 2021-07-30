package com.github.xerragnaroek.jikai.util;

import java.io.IOException;
import java.io.Writer;

import com.github.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.entities.PrivateChannel;

/**
 * 
 */
public class PMWriter extends Writer {

	private long id;
	private PrivateChannel pc;

	public PMWriter(long userId) {
		id = userId;
		pc = Core.JDA.getUserById(id).openPrivateChannel().complete();
	}

	public PMWriter(long userId, PrivateChannel channel) {
		id = userId;
		pc = channel;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		pc.sendMessage(String.valueOf(cbuf, off, len)).queue();
	}

	@Override
	public void flush() throws IOException {}

	@Override
	public void close() throws IOException {}

}
