package com.github.xerragnaroek.jikai.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.internal.utils.EncodingUtil;

/**
 * 
 */
public class ReleaseMessageReactionHandler {

	private static ReleaseMessageReactionHandler instance;
	private static String emojiUnicode = "U+1f440";
	private static String codePoints = EncodingUtil.encodeCodepoints(emojiUnicode);
	private Set<Long> releaseMessageIds = Collections.synchronizedSet(new TreeSet<>());
	private final Logger log = LoggerFactory.getLogger(ReleaseMessageReactionHandler.class);

	private ReleaseMessageReactionHandler() {
		codePoints = EncodingUtil.encodeCodepoints(emojiUnicode);
	}

	public synchronized static ReleaseMessageReactionHandler getRMRH() {
		if (instance == null) {
			instance = new ReleaseMessageReactionHandler();
		}
		return instance;
	}

	public static String getWatchedEmojiUnicode() {
		return emojiUnicode;
	}

	public static String getWatchedEmojiCodePoints() {
		return codePoints;
	}

	public boolean registerNewReleaseMessage(long id) {
		log.debug("Registered new release message: {}", id);
		return releaseMessageIds.add(id);
	}

	public boolean isRegisteredReleaseMessage(long id) {
		return releaseMessageIds.contains(id);
	}

	public boolean removeReleaseMessage(long id) {
		log.debug("Removed release message: {}", id);
		return releaseMessageIds.remove(id);
	}

	public void handleEmojiReacted(PrivateMessageReactionAddEvent event) {
		User u = event.getUser();
		if (!u.isBot()) {
			long msgId = event.getMessageIdLong();
			log.debug("Handling emoji reacted for msg {}", msgId);
			if (isRegisteredReleaseMessage(msgId)) {
				JikaiUser ju = JikaiUserManager.getInstance().getUser(u.getIdLong());
				JikaiLocale jLoc = ju.getLocale();
				event.getChannel().retrieveMessageById(msgId).flatMap(m -> {
					log.debug("Editing release notify message to show that user watched it!");
					MessageEmbed me = m.getEmbeds().get(0);
					EmbedBuilder bob = new EmbedBuilder(me);
					bob.setDescription(me.getDescription() + "\n" + jLoc.getStringFormatted("ju_eb_notify_release_watched", Arrays.asList("date"), BotUtils.getTodayDateForJUserFormatted(ju)));
					return m.editMessage(bob.build());
				}).flatMap(m -> {
					log.debug("Successfully edited msg: {}", m.getId());
					removeReleaseMessage(msgId);
					return m.clearReactions();
				}).queue(v -> log.debug("Removed now obsolete watched reaction"));
			} else {
				log.debug("Msg isn't a registered release notify message");
			}
		}
	}

	public Set<Long> getReleaseMessageIds() {
		synchronized (releaseMessageIds) {
			return new TreeSet<>(releaseMessageIds);
		}
	}

	public void setReleaseMessageIds(Set<Long> ids) {
		releaseMessageIds = Collections.synchronizedSet(ids);
	}
}
