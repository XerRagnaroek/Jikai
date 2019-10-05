package com.xerragnaroek.bot.util;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
		u.openPrivateChannel().submit().thenAccept(pc -> {
			msgs.forEach(m -> futures.add(pc.sendMessage(m).submit().whenComplete((me, e) -> {
				if (e != null) {
					log.info("Successfully sent message to {}", u.getName());
				} else {
					log.error("Failed sending message to {}", u.getName(), e);
				}
			})));
		});
		return futures;
	}

	public static TextChannel getTextChannelChecked(Guild g, String id) throws BotException {
		if (id != null) {
			TextChannel tc = g.getTextChannelById(id);
			if (tc != null) {
				return tc;
			}
		}
		throw new BotException((id == null ? "Id can not be null!" : "Id is invalid!"), null, BotException.UTIL);
	}

	public static TextChannel getTextChannelChecked(String gId, String id) throws BotException {
		return getTextChannelChecked(Core.JDA.getGuildById(gId), id);
	}
}
