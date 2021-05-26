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
	private int aniId;
	private String codepoint;
	private boolean reacted;
	private final Logger log;

	@JsonCreator
	public ALRHData(@JsonProperty("message_id") long mId, @JsonProperty("anime_id") int aniId, @JsonProperty("uni_codepoint") String cp, @JsonProperty("sent_textchannel_id") long tcId, @JsonProperty("reacted") boolean reactedTo) {
		msgId = mId;
		this.aniId = aniId;
		codepoint = cp;
		this.tcId = tcId;
		reacted = reactedTo;
		log = LoggerFactory.getLogger(ALRHData.class + "#" + aniId);
	}

	ALRHData(String uniCP, int aniId) {
		codepoint = uniCP;
		this.aniId = aniId;
		log = LoggerFactory.getLogger(ALRHData.class + "#" + aniId);
	}

	@JsonProperty("message_id")
	public long getMessageId() {
		return msgId;
	}

	@JsonProperty("anime_id")
	public int getAnimeId() {
		return aniId;
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

	@JsonProperty("anime_id")
	public void setAnimeId(int aniId) {
		this.aniId = aniId;
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
		return Integer.compare(aniId, o.aniId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ALRHData) {
			ALRHData d = (ALRHData) obj;
			return aniId == d.aniId && msgId == d.msgId;
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("ALRHData[tcId=%d,msgId=%d,aniId='%s',codepoint=%s,reacted=%b]", tcId, msgId, aniId, codepoint, reacted);
	}
}
