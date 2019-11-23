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
package com.xerragnaroek.jikai.util;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class BotUtils {
	private static final Logger log = LoggerFactory.getLogger(BotUtils.class);

	public static TextChannel getChannelOrDefault(String channel, String gId) {
		TextChannel tc = null;
		Guild g = Core.JDA.getGuildById(gId);
		if (channel != null) {
			tc = g.getTextChannelById(channel);
		}
		if (tc == null) {
			tc = g.getTextChannelsByName("general", true).get(0);
		}
		return tc;
	}

	public static boolean isToday(ZonedDateTime zdt) {
		if (zdt == null) {
			return false;
		}
		return ZonedDateTime.now().toLocalDate().equals(zdt.toLocalDate());
	}

	public static List<CompletableFuture<Message>> sendToDev(String msg) {
		return sendToDev(new MessageBuilder(msg).buildAll());
	}

	public static List<CompletableFuture<Message>> sendToDev(Queue<Message> msgs) {
		User dev = getDev();
		List<CompletableFuture<Message>> list = new LinkedList<>();
		if (dev != null) {
			list.addAll(sendPMs(dev, msgs));
		}
		return list;
	}

	@Nullable
	public static CompletableFuture<Message> sendToDev(Message m) {
		log.debug("Attempting to send a message to the dev");
		User dev = getDev();
		if (dev != null) {
			log.debug("Dev user exists, sending message...");
			return sendPM(dev, m);
		}
		return null;
	}

	public static List<CompletableFuture<Message>> sendThrowableToDev(String msg, Throwable e) {
		MessageBuilder mb = new MessageBuilder();
		mb.append(Objects.requireNonNullElse(e.getMessage(), "") + "\n" + ExceptionUtils.getStackTrace(e));
		User dev = getDev();
		List<CompletableFuture<Message>> list = new LinkedList<>();
		if (dev != null) {
			list.addAll(sendPMs(dev, mb.buildAll(SplitPolicy.NEWLINE)));
		}
		return list;
	}

	public static List<CompletableFuture<Message>> logAndSendToDev(Logger log, String msg, Throwable e) {
		log.error(msg, e);
		return sendThrowableToDev(msg, e);
	}

	@Nullable
	private static User getDev() {
		String devId = Core.DEV_ID;
		if (devId != null) {
			log.debug("DevId != null");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				return dev;
			}
		}
		return null;
	}

	@Nullable
	private static CompletableFuture<Message> sendEmbedToDev(MessageEmbed me) {
		log.debug("Attempting to send a message to the dev");
		String devId = Core.DEV_ID;
		if (devId != null) {
			log.debug("DevId != null");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				log.debug("Dev user exists, sending message...");
				return sendPM(dev, me);
			}
		}
		return null;
	}

	public static CompletableFuture<Message> sendPM(User u, String msg) {
		return sendPM(u, new MessageBuilder().append(msg).build());
	}

	public static CompletableFuture<Message> sendPM(User u, Message msg) {
		Queue<Message> q = new LinkedList<>();
		q.add(msg);
		return sendPMs(u, q).get(0);
	}

	public static CompletableFuture<Message> sendPM(User u, MessageEmbed msg) {
		return u.openPrivateChannel().submit().thenCompose(pc -> pc.sendMessage(msg).submit()).whenComplete((m, e) -> {
			if (e != null) {
				log.info("Successfully sent message to {}", u.getName());
			} else {
				log.error("Failed sending message to {}", u.getName(), e);
			}
		});
	}

	public static List<CompletableFuture<Message>> sendPMs(User u, Queue<Message> msgs) {
		List<CompletableFuture<Message>> futures = new LinkedList<>();
		PrivateChannel pc = u.openPrivateChannel().complete();
		msgs.forEach(m -> futures.add(pc.sendMessage(m).submit().whenComplete((me, e) -> {
			if (e != null) {
				log.error("Failed sending message to {}", u.getName(), e);
			} else {
				log.info("Successfully sent message to {}", u.getName());
			}
		})));
		return futures;
	}

	public static TextChannel getTextChannelChecked(Guild g, long id) throws Exception {
		if (id != 0) {
			TextChannel tc = g.getTextChannelById(id);
			if (tc != null) {
				return tc;
			}
		}
		throw new Exception((id == 0 ? "Id can not be null!" : "Id is invalid!"));
	}

	public static TextChannel getTextChannelChecked(long gId, long id) throws Exception {
		return getTextChannelChecked(Core.JDA.getGuildById(gId), id);
	}

	public static List<CompletableFuture<Message>> sendToAllInfoChannels(String msg) {
		List<CompletableFuture<Message>> cf = new ArrayList<>();
		for (Jikai j : Core.JM) {
			try {
				cf.add(j.getInfoChannel().sendMessage(msg).submit().whenComplete((m, e) -> {
					try {
						log.debug("Sent to infochannel on guild {}: '{}'", j.getGuild().getName(), msg);
					} catch (Exception e1) {
						//guild doesn't exist, already being handled
					}
				}));
			} catch (Exception e) {
				//info channel doesn't exist, already being handled
			}
		}
		return cf;
	}

	public static Iterable<String> collectionToIterableStr(Collection<?> col) {
		return col.stream().map(Object::toString).collect(Collectors.toList());
	}
}
