
package com.github.xerragnaroek.jikai.util;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.schedule.AnimeTable;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.BotData;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.ExportKeyHandler;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class BotUtils {
	private static final Logger log = LoggerFactory.getLogger(BotUtils.class);

	private static Pattern day = Pattern.compile("(\\d+)(?=d)");
	private static Pattern hour = Pattern.compile("(\\d+)(?=h)");
	private static Pattern min = Pattern.compile("(\\d+)(?=m)");

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

	public static CompletableFuture<?> sendToDev(String msg) {
		return sendToDev(new MessageBuilder(msg).buildAll());
	}

	public static CompletableFuture<?> sendToDev(Queue<Message> msgs) {
		log.debug("sending {} messages to dev", msgs.size());
		User dev = getDev();
		if (dev != null) {
			return sendPMsChecked(dev, msgs);
		}
		throw new IllegalStateException("You have supplied an invalid dev id!");
	}

	@Nullable
	public static CompletableFuture<?> sendToDev(Message m) {
		log.debug("Attempting to send a message to the dev");
		User dev = getDev();
		if (dev != null) {
			log.debug("Dev user exists, sending message...");
			return sendPMChecked(dev, m);
		}
		throw new IllegalStateException("You have supplied an invalid dev id!");
	}

	public static void sendThrowableToDev(String msg, Throwable e) {
		log.debug("sending throwable to dev");
		MessageBuilder mb = new MessageBuilder();
		mb.append(msg + "\n" + Objects.requireNonNullElse(e.getMessage(), "") + "\n" + ExceptionUtils.getStackTrace(e));
		User dev = getDev();
		if (dev != null) {
			sendPMsChecked(dev, mb.buildAll(SplitPolicy.NEWLINE));
		}
	}

	public static void logAndSendToDev(Logger log, String msg, Throwable e) {
		log.error(msg, e);
		sendThrowableToDev(msg, e);
	}

	private static User getDev() {
		long devId = Core.DEV_IDS.get(0);
		if (devId != 0) {
			log.debug("DevId != 0");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				return dev;
			}
		}
		return null;
	}

	private static CompletableFuture<?> sendEmbedToDev(MessageEmbed me) {
		log.debug("Attempting to send a message to the dev");
		long devId = Core.DEV_IDS.get(0);
		if (devId != 0) {
			log.debug("DevId != null");
			User dev = Core.JDA.getUserById(devId);
			if (dev != null) {
				log.debug("Dev user exists, sending message...");
				return sendPMChecked(dev, me);
			}
		}
		return null;
	}

	public static CompletableFuture<?> sendFile(User u, String message, byte[] data, String fileName) {
		Logger log = LoggerFactory.getLogger(BotUtils.class.getCanonicalName() + "#" + u.getId());
		log.debug("Sending {} bytes to user", data.length);
		return u.openPrivateChannel().map(pc -> pc.sendFile(data, fileName).append(message)).submit().whenComplete((ma, e) -> {
			log.debug("sendFile to user completed");
			if (e != null) {
				log.error("", e);
			}
		});
	}

	public static CompletableFuture<Message> sendPM(User u, MessageEmbed msg) {
		Logger log = LoggerFactory.getLogger(BotUtils.class.getCanonicalName() + "#" + u.getId());
		log.debug("sending pm");
		CompletableFuture<Message> cf = u.openPrivateChannel().flatMap(pc -> pc.sendMessage(msg)).submit().whenComplete((m, t) -> {
			if (t != null) {
				log.error("Failed sending pm!", t);
			} else {
				log.debug("message sent successfully");
			}
		});
		return cf;
	}

	public static List<CompletableFuture<Message>> sendPM(User u, Message msg) {
		Queue<Message> q = new LinkedList<>();
		q.add(msg);
		return sendPMs(u, q);
	}

	public static List<CompletableFuture<Message>> sendPM(User u, String msg) {
		MessageBuilder mb = new MessageBuilder().append(msg);
		if (mb.length() > 2000) {
			return sendPMs(u, mb.buildAll(SplitPolicy.NEWLINE));
		} else {
			return sendPM(u, mb.build());
		}
	}

	public static List<CompletableFuture<Message>> sendPMs(User u, Queue<Message> msgs) {
		Logger log = LoggerFactory.getLogger(BotUtils.class.getCanonicalName() + "#" + u.getId());
		log.debug("Sending {} messages", msgs.size());
		List<CompletableFuture<Message>> futures;
		try {
			futures = u.openPrivateChannel().submit().thenApply(pc -> {
				List<CompletableFuture<Message>> cfs = new ArrayList<>();
				msgs.forEach(m -> cfs.add(pc.sendMessage(m).submit().thenApply(msg -> {
					log.debug("message sent");
					return msg;
				})));
				return cfs;
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			Core.logThrowable(e);
			futures = Collections.emptyList();
		}
		return futures;
	}

	public static CompletableFuture<Boolean> sendPMChecked(User u, String msg) {
		MessageBuilder mb = new MessageBuilder().append(msg);
		if (mb.length() > 2000) {
			return sendPMsChecked(u, mb.buildAll(SplitPolicy.NEWLINE));
		} else {
			return sendPMChecked(u, mb.build());
		}
	}

	public static CompletableFuture<Boolean> sendPMChecked(User u, Message msg) {
		Queue<Message> q = new LinkedList<>();
		q.add(msg);
		return sendPMsChecked(u, q);
	}

	public static CompletableFuture<Boolean> sendPMChecked(User u, MessageEmbed msg) {
		return sendPM(u, msg).handle((m, e) -> {
			return evalSent(e);
		});
	}

	public static CompletableFuture<Boolean> sendPMsChecked(User u, Queue<Message> msgs) {
		List<CompletableFuture<Message>> cfs = sendPMs(u, msgs);
		try {
			return CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[cfs.size()])).handle((v, e) -> CompletableFuture.completedFuture(evalSent(e))).get();
		} catch (InterruptedException | ExecutionException e) {
			return CompletableFuture.completedFuture(false);
		}
	}

	public static CompletableFuture<Boolean> sendPMsEmbed(User u, Queue<MessageEmbed> msgs) {
		Logger log = LoggerFactory.getLogger(BotUtils.class.getCanonicalName() + "#" + u.getId());
		log.debug("Sending {} embeds", msgs.size());
		return u.openPrivateChannel().submit().thenApply(pc -> {
			List<CompletableFuture<?>> cfs = new ArrayList<>();
			msgs.forEach(m -> cfs.add(pc.sendMessage(m).submit().thenApply(msg -> {
				log.debug("message sent");
				return msg;
			})));
			try {
				return CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[cfs.size()])).handle((v, e) -> CompletableFuture.completedFuture(evalSent(e))).get().get();
			} catch (InterruptedException | ExecutionException e) {
				return false;
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

	public static List<CompletableFuture<?>> sendToAllListChannels(Message msg) {
		return sendToAllChannels(0, msg, null);
	}

	public static List<CompletableFuture<?>> sendToAllScheduleChannels(Message msg) {
		return sendToAllChannels(1, msg, null);
	}

	public static List<CompletableFuture<?>> sendToAllAnimeChannels(Message msg) {
		return sendToAllChannels(2, msg, null);
	}

	public static List<CompletableFuture<?>> sendToAllInfoChannels(Message msg) {
		return sendToAllChannels(3, msg, null);
	}

	public static List<CompletableFuture<?>> sendToAllListChannels(MessageEmbed msg) {
		return sendToAllChannels(0, null, msg);
	}

	public static List<CompletableFuture<?>> sendToAllScheduleChannels(MessageEmbed msg) {
		return sendToAllChannels(1, null, msg);
	}

	public static List<CompletableFuture<?>> sendToAllAnimeChannels(MessageEmbed msg) {
		return sendToAllChannels(2, null, msg);
	}

	public static List<CompletableFuture<?>> sendToAllInfoChannels(MessageEmbed msg) {
		return sendToAllChannels(3, null, msg);
	}

	public static List<CompletableFuture<?>> sendToAllListChannels(String msg) {
		return sendToAllListChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<?>> sendToAllScheduleChannels(String msg) {
		return sendToAllScheduleChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<?>> sendToAllAnimeChannels(String msg) {
		return sendToAllAnimeChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<?>> sendToAllInfoChannels(String msg) {
		return sendToAllInfoChannels(new MessageBuilder(msg).build());
	}

	public static List<CompletableFuture<?>> sendToAllInfoChannelsLocalised(String msgKey, boolean isEmbed) {
		return sendToAllInfoChannelsLocalisedImpl(msgKey, isEmbed, null);
	}

	public static List<CompletableFuture<?>> sendToAllInfoChannelsLocalisedFormatted(String key, boolean isEmbed, List<String> plchldrs, Object... objs) {
		return sendToAllInfoChannelsLocalisedImpl(key, isEmbed, plchldrs, objs);
	}

	private static List<CompletableFuture<?>> sendToAllInfoChannelsLocalisedImpl(String key, boolean isEmbed, List<String> plchldrs, Object... objs) {
		List<CompletableFuture<?>> cf = new ArrayList<>();
		for (Jikai j : Core.JM) {
			try {
				TextChannel tc = j.getInfoChannel();
				String content = null;
				if (plchldrs == null && objs == null) {
					content = j.getLocale().getString(key);
				} else {
					content = j.getLocale().getStringFormatted(key, plchldrs, objs);
				}
				if (content == null) {
					String errorMsg = String.format("Locale '%s' is missing key '%s'!", j.getLocale().getIdentifier(), key);
					Core.ERROR_LOG.error(errorMsg);
					sendToDev(errorMsg);
				} else {
					MessageAction ma = null;
					if (isEmbed) {
						ma = tc.sendMessage(new EmbedBuilder().setDescription(content).build());
					} else {
						ma = tc.sendMessage(content);
					}
					cf.add(ma.submit());
				}
			} catch (Exception e) {
				// info channel doesn't exist, already being handled
			}
		}
		return cf;
	}

	/**
	 * @param channel
	 *            0 = list, 1 = schedule, 2 = anime, 3 = info
	 */
	private static List<CompletableFuture<?>> sendToAllChannels(int channel, Message msg, MessageEmbed me) {
		List<CompletableFuture<?>> cf = new ArrayList<>();
		for (Jikai j : Core.JM) {
			try {
				TextChannel tc = null;
				if (channel == 0) {
					for (TitleLanguage lang : TitleLanguage.values()) {
						tc = j.getListChannel(lang);
						MessageAction mA = (msg == null) ? tc.sendMessage(me) : tc.sendMessage(msg);
						cf.add(mA.submit());
					}
				} else {
					switch (channel) {
						case 1 -> tc = j.getScheduleChannel();
						case 2 -> tc = j.getAnimeChannel();
						case 3 -> tc = j.getInfoChannel();
					}
					MessageAction mA = (msg == null) ? tc.sendMessage(me) : tc.sendMessage(msg);
					cf.add(mA.submit());
				}
			} catch (Exception e) {
				// info channel doesn't exist, already being handled
			}
		}
		return cf;
	}

	public static Iterable<String> collectionToIterableStr(Collection<?> col) {
		return col.stream().map(Object::toString).collect(Collectors.toList());
	}

	public static String formatSeconds(long seconds, JikaiLocale loc) {
		seconds = Math.abs(seconds);
		long days = TimeUnit.SECONDS.toDays(seconds);
		seconds -= TimeUnit.DAYS.toSeconds(days);
		long hours = TimeUnit.SECONDS.toHours(seconds);
		seconds -= TimeUnit.HOURS.toSeconds(hours);
		long minutes = TimeUnit.SECONDS.toMinutes(seconds);
		seconds -= TimeUnit.MINUTES.toSeconds(minutes);
		List<String> time = new LinkedList<>();
		if (days > 0) {
			time.add(days + " " + (days > 1 ? loc.getString("u_days") : loc.getString("u_day")));
		}
		if (hours > 0) {
			time.add(hours + " " + (hours > 1 ? loc.getString("u_hours") : loc.getString("u_hour")));
		}
		if (minutes > 0) {
			time.add(minutes + " " + (minutes > 1 ? loc.getString("u_mins") : loc.getString("u_min")));
		}
		if (seconds > 0) {
			time.add(seconds + " " + (seconds > 1 ? loc.getString("u_secs") : loc.getString("u_sec")));
		}
		return String.join(", ", time);
	}

	public static String formatHours(long hours, JikaiLocale loc) {
		return formatSeconds(hours * 3600, loc);
	}

	public static String formatMinutes(long mins, JikaiLocale loc) {
		return formatSeconds(mins * 60, loc);
	}

	public static String formatMillis(long millis, JikaiLocale loc) {
		if (millis > 1000) {
			long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
			millis -= TimeUnit.SECONDS.toMillis(seconds);
			return String.join(", ", formatSeconds(seconds, loc), millis + " ms");
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

	public static String formatTime(TemporalAccessor tempAccess, String format, Locale locale) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format).localizedBy(locale);
		return dtf.format(tempAccess);
	}

	public static CompletableFuture<?> clearChannel(TextChannel tc) {
		log.debug("Deleting all messages in channel {}", tc.getName());
		AtomicInteger count = new AtomicInteger(0);
		long start = System.currentTimeMillis();
		/*
		 * List<CompletableFuture<?>> cfs = new ArrayList<>();
		 * tc.getIterableHistory().forEach(m -> {
		 * cfs.add(m.delete().submit().thenAccept(v -> {
		 * count.incrementAndGet();
		 * }));
		 * });
		 * return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).whenComplete((v,
		 * t) -> {
		 * if (t == null) {
		 * log.debug("Successfully deleted " + count.get() + " messages in " +
		 * formatMillis(System.currentTimeMillis() - start,
		 * JikaiLocaleManager.getInstance().getLocale("en")));
		 * } else {
		 * Core.logThrowable(t);
		 * }
		 * });
		 */
		return clearImpl(tc, count).whenComplete((v, t) -> {
			if (t == null) {
				log.debug("Successfully deleted " + count.get() + " messages in " + formatMillis(System.currentTimeMillis() - start, JikaiLocaleManager.getInstance().getLocale("en")));
			} else {
				Core.logThrowable(t);
			}
		});
	}

	private static CompletableFuture<Void> clearImpl(TextChannel tc, AtomicInteger count) {
		return tc.getHistoryFromBeginning(100).submit().thenAccept(mh -> {
			if (!mh.isEmpty()) {
				List<CompletableFuture<Void>> cfs = tc.purgeMessages(mh.getRetrievedHistory());
				CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[cfs.size()])).join();
				count.addAndGet(mh.size());
				clearImpl(tc, count).join();
			}
		});
	}

	/**
	 * Pads the given strings with spaces to align whatever is to the right of the separator.
	 * 
	 * @param seperator
	 * @param delimiter
	 * @param strings
	 */
	public static String padEquallyAndJoin(String separator, String delimiter, String title, String... strings) {
		List<String> str = new ArrayList<>(strings.length);
		List<String> fields = new ArrayList<>(strings.length);
		int maxL = 0;
		for (String s : strings) {
			String[] tmp = s.split(separator);
			tmp[0] = tmp[0].trim();
			tmp[1] = tmp[1].trim();
			str.add(tmp[0]);
			fields.add(tmp[1].trim());
			int curL = tmp[0].length();
			maxL = curL > maxL ? curL : maxL;
		}
		maxL++;
		int totalMax = 0;
		List<String> formatted = new ArrayList<>(strings.length);
		for (int i = 0; i < str.size(); i++) {
			String s = String.format("%-" + maxL + "s" + separator + " %s", str.get(i), fields.get(i));
			totalMax = s.length() > totalMax ? s.length() : totalMax;
			formatted.add(s);
		}
		if (title != null) {
			List<String> tmp = new ArrayList<>(formatted.size() + 1);
			tmp.add(String.format("%" + (totalMax / 2 + title.length() / 2) + "s", title));
			tmp.addAll(formatted);
			formatted = tmp;
		}
		return String.join(delimiter, formatted);
	}

	public static EmbedBuilder addJikaiMark(EmbedBuilder eb) {
		BotData d = Core.JM.getJDM().getBotData();
		return eb.setColor(d.getJikaiColor()).setFooter("Made with <3 by Jikai", d.getJikaiImgUrl()).setTimestamp(Instant.now());
	}

	public static Member getHighestPermissionMember(User u) {
		Member m = null;
		long perms = 0;
		for (Guild g : u.getMutualGuilds()) {
			Member tmp = g.getMember(u);
			long p = Permission.getRaw(tmp.getPermissions());
			if (p > perms) {
				m = tmp;
				perms = p;
			}
		}
		return m;
	}

	private static boolean evalSent(Throwable e) {
		if (e == null) {
			log.debug("Successfully sent message");
			return true;
		} else {
			log.error("Failed sending message: {}", e.getMessage());
			return false;
		}
	}

	public static MessageEmbed makeSimpleEmbed(String message) {
		return embedBuilder().setDescription(message).build();
	}

	public static <T> CompletableFuture<T> retryFuture(int times, Supplier<CompletableFuture<T>> futureSup) {
		log.debug("running future, retrying {} times max", times);
		CompletableFuture<T> future = new CompletableFuture<>();
		futureSup.get().thenAccept(a -> future.complete(a)).exceptionally(e -> {
			if (times <= 0) {
				future.completeExceptionally(e);
			} else {
				log.error("Exception while running future!", e);
				retry(times - 1, futureSup, future);
			}
			return null;
		});
		return future;
	}

	private static <T> void retry(int times, Supplier<CompletableFuture<T>> futureSup, CompletableFuture<T> future) {
		log.debug("Retry: {} times left", times);
		futureSup.get().thenAccept(a -> future.complete(a)).exceptionally(ex -> {
			if (times <= 0) {
				future.completeExceptionally(ex);
			} else {
				log.error("Exception while running future!", ex);
				retry(times - 1, futureSup, future);
			}
			return null;
		});
	}

	public static String getTodayDateForJUserFormatted(JikaiUser ju) {
		return formatTime(ZonedDateTime.now(ju.getTimeZone()), "eeee, dd.MM.yy", ju.getLocale().getLocale());
	}

	public static String formatExternalSites(Anime a) {
		return a.getExternalLinks().stream().filter(es -> es.getSite().equals("Twitter") || es.getSite().equals("Official Site")).map(es -> String.format("[**%s**](%s)", es.getSite(), es.getUrl())).collect(Collectors.joining(", "));

	}

	public static <T> List<List<T>> partitionCollection(Collection<T> col, int chunkSize) {
		AtomicInteger counter = new AtomicInteger();
		List<List<T>> list = new LinkedList<>();
		list.addAll(col.stream().collect(Collectors.groupingBy(e -> counter.getAndIncrement() / chunkSize)).values());
		return list;
	}

	public static String processUnicode(String codePoints) {
		return Arrays.stream(codePoints.split("U+")).filter(s -> !s.isEmpty()).map(s -> Integer.parseInt(s, 16)).map(Character::toString).collect(Collectors.joining());
	}

	public static EmbedBuilder embedBuilder() {
		return BotUtils.addJikaiMark(new EmbedBuilder());
	}

	public static MessageEmbed localedEmbedTitleDescription(JikaiLocale loc, String titleKey, String descKey) {
		return embedBuilder().setTitle(loc.getString(titleKey)).setDescription(loc.getString(descKey)).build();
	}

	@SafeVarargs
	public static MessageEmbed localedEmbed(JikaiLocale loc, String locKeyBase, Pair<List<String>, Object[]>... format) {
		EmbedBuilder eb = embedBuilder();
		int[] formatIndex = { 0 };
		Function<String, String> func = key -> loc.isFormattedString(key) ? loc.getStringFormatted(key, format[formatIndex[0]].getLeft(), format[formatIndex[0]++].getRight()) : loc.getString(key);
		eb.setTitle(func.apply(locKeyBase + "_title"), func.apply(locKeyBase + "_url")).setDescription(func.apply(locKeyBase + "_desc"));
		String thumb = func.apply(locKeyBase + "_thumb");
		if (thumb != null) {
			eb.setThumbnail(thumb);
		}
		int field = 1;
		String name = "";
		String tmpField = "";
		while ((name = func.apply((tmpField = locKeyBase + "_field" + field) + "_n")) != null) {
			String tmp = loc.getString(tmpField + "_in");
			boolean inline = tmp != null && tmp.equalsIgnoreCase("true");
			eb.addField(name, func.apply(tmpField + "_v"), inline);
			field++;
		}
		return eb.build();
	}

	public static MessageEmbed titledEmbed(String title, String desc) {
		return embedBuilder().setTitle(title).setDescription(desc).build();
	}

	public static JikaiUser resolveUser(String keyOrId) {
		long id = 0;
		try {
			id = Long.parseLong(keyOrId);
		} catch (NumberFormatException e) {}
		if (id == 0) {
			id = ExportKeyHandler.getInstance().getJikaiUserIdFromKey(keyOrId);
		}
		return JikaiUserManager.getInstance().getUser(id);
	}

	public static List<MessageEmbed> buildFiedldedEmbeds(String title, List<String> strings, boolean inline) {
		return buildFiedldedEmbeds(title, null, strings, inline);
	}

	public static List<MessageEmbed> buildFiedldedEmbeds(String title, String url, List<String> strings, boolean inline) {
		List<EmbedBuilder> ebs = new ArrayList<>();
		EmbedBuilder eb = BotUtils.embedBuilder();
		int cCount = 0;
		int cField = 0;
		for (String s : strings) {
			if (cCount + s.length() < 6000 && cField < 25) {
				eb.addField("", s, inline);
				cCount += s.length();
				cField++;
			} else {
				ebs.add(eb);
				eb = BotUtils.embedBuilder();
				cCount = 0;
				cField = 0;
			}
		}
		if (ebs.isEmpty()) {
			ebs.add(eb);
		}
		if (ebs.size() == 1) {
			eb.setTitle(title, url);
		} else {
			for (int i = 0; i < ebs.size(); i++) {
				ebs.get(i).setTitle(title + " [" + (i + 1) + "/" + ebs.size() + "]", url);
			}
		}
		return ebs.stream().map(EmbedBuilder::build).collect(Collectors.toList());
	}

	public static List<MessageEmbed> buildEmbeds(String title, List<String> strings) {
		List<EmbedBuilder> ebs = new ArrayList<>();
		EmbedBuilder eb = BotUtils.embedBuilder();
		int cCount = 0;
		if (strings == null || strings.isEmpty()) {
			return Collections.emptyList();
		}
		for (String s : strings) {
			if (cCount + s.length() < 2048) {
				/*
				 * if (desc.isEmpty()) {
				 * desc += s;
				 * } else {
				 * desc += (s= "\n" + s);
				 * }
				 */
				eb.appendDescription(s);
				cCount += s.length();
			} else {
				// eb.setDescription(desc);
				ebs.add(eb);
				eb = BotUtils.embedBuilder();
				cCount = 0;
			}
		}
		if (ebs.isEmpty()) {
			ebs.add(eb);
		}
		if (ebs.size() == 1) {
			eb.setTitle(title);
		} else {
			for (int i = 0; i < ebs.size(); i++) {
				ebs.get(i).setTitle(title + " [" + (i + 1) + "/" + ebs.size() + "]");
			}
		}
		return ebs.stream().map(EmbedBuilder::build).collect(Collectors.toList());
	}

	public static String makePrivateMessageLink(long pcId, long msgId) {
		return String.format("https://discord.com/channels/@me/%s/%s", pcId, msgId);
	}

	public static EmbedBuilder makeConfigEmbed(JikaiUser ju) {
		String config = ju.getConfigFormatted();
		String[] tmp = config.split("\n");
		// codeblock, codeblock end
		String[] nonFormat = { "", "" };
		if (tmp[0].contains("```") && tmp[tmp.length - 1].contains("```")) {
			// all data fields
			nonFormat[0] = tmp[0];
			nonFormat[1] = tmp[tmp.length - 1];
			tmp = ArrayUtils.subarray(tmp, 1, tmp.length - 1);
		}
		String formatted = BotUtils.padEquallyAndJoin(ju.getLocale().getString("ju_config_sep"), "\n", null, tmp);
		formatted = nonFormat[0] + "\n" + formatted + "\n" + nonFormat[1];
		EmbedBuilder eb = BotUtils.embedBuilder();
		return eb.setTitle(ju.getLocale().getString("com_ju_config_eb_title")).setDescription(formatted);
	}

	public static int stepStringToMins(String str) {
		AtomicLong mins = new AtomicLong(0);
		Matcher d = day.matcher(str);
		Matcher h = hour.matcher(str);
		Matcher m = min.matcher(str);
		AtomicBoolean noMatch = new AtomicBoolean(true);

		d.results().map(MatchResult::group).findFirst().ifPresent(s -> {
			mins.addAndGet(TimeUnit.DAYS.toMinutes(Long.parseLong(s)));
			noMatch.set(false);
		});
		h.results().map(MatchResult::group).findFirst().ifPresent(s -> {
			mins.addAndGet(TimeUnit.HOURS.toMinutes(Long.parseLong(s)));
			noMatch.set(false);
		});
		m.results().map(MatchResult::group).findFirst().ifPresent(s -> {
			mins.addAndGet(Long.parseLong(s));
			noMatch.set(false);
		});
		if (noMatch.get()) {
			throw new IllegalArgumentException("Invalid step string!");
		}
		return (int) mins.get();
	}

	public static Queue<MessageEmbed> createWeeklySchedule(JikaiUser ju, AnimeTable at) {
		JikaiLocale loc = ju.getLocale();
		StringBuilder bob = new StringBuilder();
		at.toFormatedWeekString(ju.getTitleLanguage(), true, loc.getLocale(), ju).values().forEach(s -> bob.append("\n" + s));
		MessageBuilder mb = new MessageBuilder();
		mb.setContent(bob.toString());
		Queue<MessageEmbed> q = new LinkedList<>();
		Queue<Message> msgs = mb.buildAll(SplitPolicy.NEWLINE);
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setTitle(loc.getString("ju_weekly_sched_msg"));
		Consumer<Message> setAndAdd = m -> {
			eb.setDescription("```asciidoc\n" + m.getContentRaw() + "\n```");
			q.add(eb.build());
		};
		setAndAdd.accept(msgs.poll());
		eb.setTitle(null);
		msgs.forEach(setAndAdd);
		return q;
	}

	public static void validateRoles(Jikai j, JikaiUser ju) {
		try {
			Guild g = j.getGuild();
			Member m = g.getMember(ju.getUser());
			TitleLanguage[] langs = TitleLanguage.values();
			langs = ArrayUtils.remove(langs, ArrayUtils.indexOf(langs, ju.getTitleLanguage(), 0));
			List<String> unwantedLangs = Stream.of(langs).map(tl -> tl.name().toLowerCase()).collect(Collectors.toList());
			String titleLang = ju.getTitleLanguage().name().toLowerCase();
			boolean roleFound = false;
			for (Role r : m.getRoles()) {
				if (unwantedLangs.contains(r.getName())) {
					g.removeRoleFromMember(m, r).queue();
				}
				if (r.getName().equals(titleLang)) {
					roleFound = true;
					break;
				}
			}
			if (!roleFound) {
				Role r = g.getRolesByName(titleLang, false).get(0);
				g.addRoleToMember(m, r).queue(v -> log.debug("successfully added role {} to user {}", r.getName(), ju.getId()));
			}
			setAdultImpl(g, j, m, ju);
			addJikaiUserRole(ju, j);
			g.getMembersWithRoles(g.getRoleById(j.getJikaiData().getJikaiUserRole())).stream().filter(mem -> !JikaiUserManager.getInstance().isKnownJikaiUser(mem.getIdLong())).forEach(mem -> removeJikaiUserRole(m.getIdLong(), j));
		} catch (Exception e) {
			Core.ERROR_LOG.error("couldn't get guild!", e);
		}
	}

	public static void switchTitleLangRole(JikaiUser ju, TitleLanguage old, TitleLanguage newLang) {
		if (old != newLang) {
			log.debug("Swapping roles {}->{}", old, newLang);
			Core.JDA.getMutualGuilds(ju.getUser()).stream().filter(Core.JM::hasManagerFor).map(Core.JM::get).forEach(j -> {
				try {
					Guild g = j.getGuild();
					Role newR = g.getRoleById(j.getJikaiData().getTitleLanguageRole(newLang));
					RestAction<Void> ra = g.addRoleToMember(ju.getId(), newR);
					if (old != null) {
						ra = ra.and(g.removeRoleFromMember(ju.getId(), g.getRoleById(j.getJikaiData().getTitleLanguageRole(old))));
					}
					ra.mapToResult().submit().thenAccept(r -> {
						if (r.isFailure()) {
							log.error("Failed swapping roles! guild={};user={}", g.getId(), ju.getId(), r.getFailure());
						} else {
							log.debug("Swapped roles {}->{} for user {} on guild {}", old, newR.getName(), ju.getId(), g.getId());
						}
					});
				} catch (Exception e) {
					log.error("Couldn't get guild {}!", j.getJikaiData().getGuildId(), e);
				}
			});

		}
	}

	public static void setAdultRole(JikaiUser ju) {
		Core.JDA.getMutualGuilds(ju.getUser()).stream().filter(Core.JM::hasManagerFor).map(Core.JM::get).forEach(j -> {
			try {
				Guild g = j.getGuild();
				Member m = g.getMember(ju.getUser());
				setAdultImpl(g, j, m, ju);
			} catch (Exception e) {
				log.error("Couldn't get guild {}!", j.getJikaiData().getGuildId(), e);
			}
		});
	}

	private static void setAdultImpl(Guild g, Jikai j, Member m, JikaiUser ju) {
		Role adult = g.getRoleById(j.getJikaiData().getAdultRoleId());
		boolean hasAdult = m.getRoles().contains(adult);
		if (hasAdult && !ju.isShownAdult()) {
			g.removeRoleFromMember(m, adult).queue(v -> log.debug("removed false adult role from {}", ju.getId()));
		} else if (!hasAdult && ju.isShownAdult()) {
			g.addRoleToMember(m, adult).queue(v -> log.debug("added missing adult role to {}", ju.getId()));
		}
	}

	public static void addJikaiUserRole(JikaiUser ju) {
		Core.JDA.getMutualGuilds(ju.getUser()).stream().filter(Core.JM::hasManagerFor).map(Core.JM::get).forEach(j -> addJikaiUserRole(ju, j));
	}

	public static void addJikaiUserRole(JikaiUser ju, Jikai j) {
		try {
			Guild g = j.getGuild();
			Member m = g.getMember(ju.getUser());
			Role user = g.getRoleById(j.getJikaiData().getJikaiUserRole());
			if (!m.getRoles().contains(user)) {
				g.addRoleToMember(m, user).queue(v -> log.debug("added user role to {} on {}", ju.getId(), g.getId()));
			}
		} catch (Exception e) {
			log.error("Couldn't get guild {}!", j.getJikaiData().getGuildId(), e);
		}
	}

	public static void removeJikaiUserRole(JikaiUser ju) {
		Core.JDA.getMutualGuilds(ju.getUser()).stream().filter(Core.JM::hasManagerFor).map(Core.JM::get).forEach(j -> removeJikaiUserRole(ju.getId(), j));
	}

	public static void removeJikaiUserRole(long id, Jikai j) {
		try {
			Guild g = j.getGuild();
			Member m = g.getMemberById(id);
			Role user = g.getRoleById(j.getJikaiData().getJikaiUserRole());
			if (m.getRoles().contains(user)) {
				g.removeRoleFromMember(m, user).queue(v -> log.debug("removed user role from {} on {}", id, g.getId()));
			}
		} catch (Exception e) {
			log.error("Couldn't get guild {}!", j.getJikaiData().getGuildId(), e);
		}
	}

	public static void removeTitleLangRole(JikaiUser ju) {
		Core.JDA.getMutualGuilds(ju.getUser()).stream().filter(Core.JM::hasManagerFor).map(Core.JM::get).forEach(j -> removeTitleLangRole(ju.getId(), ju.getTitleLanguage(), j));
	}

	public static void removeTitleLangRole(long id, TitleLanguage lang, Jikai j) {
		try {
			Guild g = j.getGuild();
			Member m = g.getMemberById(id);
			Role user = g.getRoleById(j.getJikaiData().getTitleLanguageRole(lang));
			if (m.getRoles().contains(user)) {
				g.removeRoleFromMember(m, user).queue(v -> log.debug("removed user role from {} on {}", id, g.getId()));
			}
		} catch (Exception e) {
			log.error("Couldn't get guild {}!", j.getJikaiData().getGuildId(), e);
		}
	}

	public static void removeRoles(JikaiUser ju) {
		Core.JDA.getMutualGuilds(ju.getUser()).stream().filter(Core.JM::hasManagerFor).map(Core.JM::get).forEach(j -> {
			removeJikaiUserRole(ju.getId(), j);
			removeTitleLangRole(ju.getId(), ju.getTitleLanguage(), j);
		});
	}
}
