package com.xerragnaroek.bot.util;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
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

	public static CompletableFuture<Message> sendToDev(String msg) {
		log.debug("Attempting to send a message to the dev");
		String devId = Core.DEV_ID;
		if (devId != null) {
			log.debug("DevId != null");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				log.debug("Dev user exists, sending message...");
				return sendPM(dev, new MessageBuilder().append(msg).build());
			}
		}
		return null;
	}

	public static CompletableFuture<Message> sendThrowableToDev(String msg, Throwable e) {
		EmbedBuilder eb = new EmbedBuilder();
		if (msg.length() > 256) {
			sendToDev(msg);
		} else {
			eb.setTitle(msg);
		}
		eb.addField(Objects.requireNonNullElse(e.getMessage(), ""), ExceptionUtils.getStackTrace(e), false).setColor(Color.RED);
		return sendEmbedToDev(eb.build());
	}

	public static CompletableFuture<Message> logAndSendToDev(Logger log, String msg, Throwable e) {
		log.error(msg, e);
		return sendThrowableToDev(msg, e);
	}

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
		return u.openPrivateChannel().submit().thenCompose(pc -> pc.sendMessage(msg).submit()).whenComplete((m, e) -> {
			if (e != null) {
				log.info("Successfully sent message to {}", u.getName());
			} else {
				log.error("Failed sending message to {}", u.getName(), e);
			}
		});
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
}
