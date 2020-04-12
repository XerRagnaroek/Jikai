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
package com.github.xerragnaroek.jikai.anime.alrh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "message_id", "sent_textchannel_id", "sent_ab_version", "title", "role_id", "uni_codepoint", "reacted" })
public class ALRHData implements Comparable<ALRHData> {
	private int abVersion;
	private long tcId;
	private long msgId;
	private String title;
	private String codepoint;
	private boolean reacted;
	private final Logger log;

	@JsonCreator
	public ALRHData(@JsonProperty("message_id") long mId, @JsonProperty("title") String title, @JsonProperty("uni_codepoint") String cp, @JsonProperty("sent_textchannel_id") long tcId, @JsonProperty("sent_ab_version") int abVersion, @JsonProperty("reacted") boolean reactedTo) {
		msgId = mId;
		this.title = title;
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

	@JsonProperty("sent_ab_version")
	public int getABVersion() {
		return abVersion;
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

	@JsonProperty("sent_ab_version")
	public void setABVersion(int version) {
		abVersion = version;
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