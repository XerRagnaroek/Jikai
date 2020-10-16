package com.github.xerragnaroek.jikai.anime.alrh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "message_id", "sent_textchannel_id", "title", "uni_codepoint", "reacted" })
public class ALRHData implements Comparable<ALRHData> {
	private long tcId;
	private long msgId;
	private String title;
	private String codepoint;
	private boolean reacted;
	private final Logger log;

	@JsonCreator
	public ALRHData(@JsonProperty("message_id") long mId, @JsonProperty("title") String title, @JsonProperty("uni_codepoint") String cp, @JsonProperty("sent_textchannel_id") long tcId, @JsonProperty("reacted") boolean reactedTo) {
		msgId = mId;
		this.title = title;
		codepoint = cp;
		this.tcId = tcId;
		reacted = reactedTo;
		log = LoggerFactory.getLogger(ALRHData.class + "#" + title);
	}

	ALRHData(String uniCP, String title) {
		codepoint = uniCP;
		this.title = title;
		log = LoggerFactory.getLogger(ALRHData.class + "#" + title);
	}

	@JsonProperty("message_id")
	public long getMessageId() {
		return msgId;
	}

	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	@JsonProperty("uni_codepoint")
	public String getUnicodeCodePoint() {
		return codepoint;
	}

	@JsonProperty("sent_textchannel_id")
	public long getTextChannelId() {
		return tcId;
	}

	@JsonProperty("reacted")
	public boolean isReacted() {
		return reacted;
	}

	@JsonProperty("message_id")
	public void setMessageId(long mId) {
		msgId = mId;
	}

	@JsonProperty("title")
	public void setTitle(String title) {
		this.title = title;
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
	public void setTextChannelId(long tcId) {
		this.tcId = tcId;
	}

	@Override
	public int compareTo(ALRHData o) {
		return String.CASE_INSENSITIVE_ORDER.compare(title, o.title);
	}

	@Override
	public int hashCode() {
		return title.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ALRHData) {
			ALRHData d = (ALRHData) obj;
			return title.equals(d.title) && msgId == d.msgId;
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("ALRHData[tcId=%d,msgId=%d,title='%s',codepoint=%s,reacted=%b]", tcId, msgId, title, codepoint, reacted);
	}
}
