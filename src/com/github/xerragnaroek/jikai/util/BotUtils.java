
package com.github.xerragnaroek.jikai.util;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

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

	public static CompletableFuture<PrivateChannel> sendToDev(String msg) {
		return sendToDev(new MessageBuilder(msg).buildAll());
	}

	public static CompletableFuture<PrivateChannel> sendToDev(Queue<Message> msgs) {
		User dev = getDev();
		if (dev != null) {
			return sendPMs(dev, msgs);
		}
		throw new IllegalStateException("You have supplied an invalid dev id!");
	}

	@Nullable
	public static CompletableFuture<PrivateChannel> sendToDev(Message m) {
		log.debug("Attempting to send a message to the dev");
		User dev = getDev();
		if (dev != null) {
			log.debug("Dev user exists, sending message...");
			return sendPM(dev, m);
		}
		throw new IllegalStateException("You have supplied an invalid dev id!");
	}

	public static void sendThrowableToDev(String msg, Throwable e) {
		MessageBuilder mb = new MessageBuilder();
		mb.append(Objects.requireNonNullElse(e.getMessage(), "") + "\n" + ExceptionUtils.getStackTrace(e));
		User dev = getDev();
		if (dev != null) {
			sendPMs(dev, mb.buildAll(SplitPolicy.NEWLINE));
		}
	}

	public static void logAndSendToDev(Logger log, String msg, Throwable e) {
		log.error(msg, e);
		sendThrowableToDev(msg, e);
	}

	private static User getDev() {
		long devId = Core.DEV_ID;
		if (devId != 0) {
			log.debug("DevId != null");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				return dev;
			}
		}
		return null;
	}

	private static CompletableFuture<Message> sendEmbedToDev(MessageEmbed me) {
		log.debug("Attempting to send a message to the dev");
		long devId = Core.DEV_ID;
		if (devId != 0) {
			log.debug("DevId != null");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				log.debug("Dev user exists, sending message...");
				return sendPM(dev, me);
			}
		}
		return null;
	}

	public static CompletableFuture<MessageAction> sendFile(User u, String message, byte[] data, String fileName) {
		log.debug("Sending {} bytes to user", data.length);
		return u.openPrivateChannel().map(pc -> pc.sendFile(data, fileName).append(message)).submit().whenComplete((ma, e) -> {
			log.debug("sendFile to user completed");
			if (e != null) {
				log.error("", e);
			}
		});
	}

	public static CompletableFuture<PrivateChannel> sendPM(User u, String msg) {
		MessageBuilder mb = new MessageBuilder().append(msg);
		if (mb.length() > 2000) {
			return sendPMs(u, mb.buildAll(SplitPolicy.NEWLINE));
		} else {
			return sendPM(u, mb.build());
		}
	}

	public static CompletableFuture<PrivateChannel> sendPM(User u, Message msg) {
		Queue<Message> q = new LinkedList<>();
		q.add(msg);
		return sendPMs(u, q);
	}

	public static CompletableFuture<Message> sendPM(User u, MessageEmbed msg) {
		return u.openPrivateChannel().flatMap(pc -> pc.sendMessage(msg)).submit();
	}

	public static CompletableFuture<PrivateChannel> sendPMs(User u, Queue<Message> msgs) {
		return u.openPrivateChannel().submit().whenComplete((pc, ex) -> {
			if (ex != null) {
				log.error("", ex);
			} else {
				msgs.forEach(m -> pc.sendMessage(m).submit().whenComplete((me, e) -> {
					if (e != null) {
						log.error("Failed sending message to {}", u.getName(), e);
					} else {
						log.info("Successfully sent message to {}", u.getName());
					}
				}));
			}
		});
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

	public static List<CompletableFuture<Message>> sendToAllListChannels(Message msg) {
		return sendToAllChannels(0, msg, null);
	}

	public static List<CompletableFuture<Message>> sendToAllScheduleChannels(Message msg) {
		return sendToAllChannels(1, msg, null);
	}

	public static List<CompletableFuture<Message>> sendToAllAnimeChannels(Message msg) {
		return sendToAllChannels(2, msg, null);
	}

	public static List<CompletableFuture<Message>> sendToAllInfoChannels(Message msg) {
		return sendToAllChannels(3, msg, null);
	}

	public static List<CompletableFuture<Message>> sendToAllListChannels(MessageEmbed msg) {
		return sendToAllChannels(0, null, msg);
	}

	public static List<CompletableFuture<Message>> sendToAllScheduleChannels(MessageEmbed msg) {
		return sendToAllChannels(1, null, msg);
	}

	public static List<CompletableFuture<Message>> sendToAllAnimeChannels(MessageEmbed msg) {
		return sendToAllChannels(2, null, msg);
	}

	public static List<CompletableFuture<Message>> sendToAllInfoChannels(MessageEmbed msg) {
		return sendToAllChannels(3, null, msg);
	}

	public static List<CompletableFuture<Message>> sendToAllListChannels(String msg) {
		return sendToAllListChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<Message>> sendToAllScheduleChannels(String msg) {
		return sendToAllScheduleChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<Message>> sendToAllAnimeChannels(String msg) {
		return sendToAllAnimeChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<Message>> sendToAllInfoChannels(String msg) {
		return sendToAllInfoChannels(new MessageBuilder(msg).build());
	}

	/**
	 * @param channel
	 *            0 = list, 1 = schedule, 2 = anime, 3 = info
	 */
	private static List<CompletableFuture<Message>> sendToAllChannels(int channel, Message msg, MessageEmbed me) {
		List<CompletableFuture<Message>> cf = new ArrayList<>();
		for (Jikai j : Core.JM) {
			try {
				TextChannel tc = null;
				switch (channel) {
					case 0 -> tc = j.getListChannel();
					case 1 -> tc = j.getScheduleChannel();
					case 2 -> tc = j.getAnimeChannel();
					case 3 -> tc = j.getInfoChannel();
				}
				MessageAction mA = (msg == null) ? tc.sendMessage(me) : tc.sendMessage(msg);
				cf.add(mA.submit().whenComplete((m, e) -> {
					try {
						log.debug("Sent to infochannel on guild {}: '{}'", j.getGuild().getName(), msg);
					} catch (Exception e1) {
						// guild doesn't exist, already being handled
					}
				}));
			} catch (Exception e) {
				// info channel doesn't exist, already being handled
			}
		}
		return cf;
	}

	public static Iterable<String> collectionToIterableStr(Collection<?> col) {
		return col.stream().map(Object::toString).collect(Collectors.toList());
	}

	public static String formatSeconds(long seconds) {
		seconds = Math.abs(seconds);
		long days = TimeUnit.SECONDS.toDays(seconds);
		seconds -= TimeUnit.DAYS.toSeconds(days);
		long hours = TimeUnit.SECONDS.toHours(seconds);
		seconds -= TimeUnit.HOURS.toSeconds(hours);
		long minutes = TimeUnit.SECONDS.toMinutes(seconds);
		seconds -= TimeUnit.MINUTES.toSeconds(minutes);
		List<String> time = new LinkedList<>();
		if (days > 0) {
			time.add(days + " " + (days > 1 ? "days" : "day"));
		}
		if (hours > 0) {
			time.add(hours + " " + (hours > 1 ? "hours" : "hour"));
		}
		if (minutes > 0) {
			time.add(minutes + " " + (minutes > 1 ? "minutes" : "minute"));
		}
		if (seconds > 0) {
			time.add(seconds + " " + (seconds > 1 ? "seconds" : "second"));
		}
		return String.join(", ", time);
	}

	public static String formatMillis(long millis) {
		if (millis > 1000) {
			long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
			millis -= TimeUnit.SECONDS.toMillis(seconds);
			return String.join(", ", formatSeconds(seconds), millis + " ms");
		} else {
			return millis + " ms";
		}
	}

	public static byte[] imageToByteArray(RenderedImage img) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, "png", baos);
		} catch (IOException e) {
			log.error("", e);
		}
		return baos.toByteArray();
	}

	public static String formatTime(TemporalAccessor tempAccess, String format) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
		return dtf.format(tempAccess);
	}

	public static CompletableFuture<?> clearChannel(TextChannel tc) {
		log.debug("Deleting all messages in channel {}", tc.getName());
		AtomicInteger count = new AtomicInteger(0);
		long start = System.currentTimeMillis();
		List<CompletableFuture<?>> cfs = new ArrayList<>();
		tc.getIterableHistory().forEach(m -> {
			cfs.add(m.delete().submit().thenAccept(v -> {
				count.incrementAndGet();
			}));
		});
		return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).whenComplete((v, t) -> {
			if (t == null) {
				log.debug("Successfully deleted " + count.get() + " messages in " + formatMillis(System.currentTimeMillis() - start));
			} else {
				Core.logThrowable(t);
			}
		});
	}

}
