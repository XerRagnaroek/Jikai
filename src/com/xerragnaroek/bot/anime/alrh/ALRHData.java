package com.xerragnaroek.bot.anime.alrh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({	"message_id", "sent_textchannel_id", "sent_ab_version", "title", "role_id", "uni_codepoint",
						"reacted" })
public class ALRHData implements Comparable<ALRHData> {
	private int abVersion;
	private String tcId;
	private String msgId;
	private String title;
	private String roleId;
	private String codepoint;
	private boolean reacted;
	private final Logger log;

	@JsonCreator
	public ALRHData(@JsonProperty("message_id") String mId, @JsonProperty("title") String title,
			@JsonProperty("role_id") String rId, @JsonProperty("uni_codepoint") String cp,
			@JsonProperty("sent_textchannel_id") String tcId, @JsonProperty("sent_ab_version") int abVersion,
			@JsonProperty("reacted") boolean reactedTo) {
		msgId = mId;
		this.title = title;
		roleId = rId;
		codepoint = cp;
		this.tcId = tcId;
		this.abVersion = abVersion;
		reacted = reactedTo;
		log = LoggerFactory.getLogger(ALRHData.class + "#" + title);
	}

	ALRHData(String uniCP, String title) {
		codepoint = uniCP;
		this.title = title;
		log = LoggerFactory.getLogger(ALRHData.class + "#" + title);
	}

	@JsonProperty("message_id")
	public String getMessageId() {
		return msgId;
	}

	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	@JsonProperty("role_id")
	public String getRoleId() {
		return roleId;
	}

	@JsonProperty("uni_codepoint")
	public String getUnicodeCodePoint() {
		return codepoint;
	}

	@JsonProperty("sent_textchannel_id")
	public String getTextChannelId() {
		return tcId;
	}

	@JsonProperty("sent_ab_version")
	public int getABVersion() {
		return abVersion;
	}

	@JsonProperty("reacted")
	public boolean isReacted() {
		return reacted;
	}

	@JsonProperty("message_id")
	public void setMessageId(String mId) {
		msgId = mId;
	}

	@JsonProperty("title")
	public void setTitle(String title) {
		this.title = title;
	}

	@JsonProperty("role_id")
	public void setRoleId(String id) {
		roleId = id;
	}

	@JsonProperty("uni_codepoint")
	public void setUnicodeCodePoint(String uc) {
		codepoint = uc;
	}

	@JsonProperty("reacted")
	public void setReacted(boolean react) {
		reacted = react;
		log.debug("Reacted changed to {}", react);
	}

	@JsonProperty("sent_textchannel_id")
	public void setTextChannelId(String tcId) {
		this.tcId = tcId;
	}

	@JsonProperty("sent_ab_version")
	public void setABVersion(int version) {
		abVersion = version;
	}

	public boolean hasRoleId() {
		return roleId != null;
	}

	@Override
	public int compareTo(ALRHData o) {
		return title.compareTo(o.title);
	}

	@Override
	public int hashCode() {
		return title.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ALRHData) {
			return title.equals(((ALRHData) obj).title);
		}
		return false;
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
