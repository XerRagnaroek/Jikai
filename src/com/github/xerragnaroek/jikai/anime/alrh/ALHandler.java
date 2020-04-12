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

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * 
 * Handles the sending and updating of the anime list. <br>
 * Only used by the ALRHandler.
 * 
 * @author XerRagnaroek
 *
 */
class ALHandler {
	private ALRHandler alrh;
	private int expectedNumSuccesses = 0;
	private final Logger log;
	private ALRHDataBase alrhDB;
	private AtomicBoolean sending = new AtomicBoolean(false);
	private AtomicInteger successes = new AtomicInteger(0); //Successful RestActions

	ALHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ALHandler.class.getName() + "#" + alrh.gId);
		alrhDB = alrh.alrhDB;
	}

	/**
	 * Whether the list is currently being sent, or not.
	 * 
	 * @return true - if the list is currently being sent <br>
	 *         false - nothing is being sent
	 */
	boolean isSending() {
		return sending.get();
	}

	/**
	 * Send the anime list.<br>
	 * Deletes the old list (if existing) and replaces it with the new one. <br>
	 * Once all RestActions have successfully completed, ALRH is set to updated.
	 */
	void sendList() {
		sending.set(true);
		try {
			Set<DTO> dtos = Core.JM.getALHRM().getListMessages();
			successes.set(0);
			expectedNumSuccesses = calcExpectedSuccesses(dtos);
			log.info("Deleting old messages and data");
			TextChannel tc = alrh.j.getGuild().getTextChannelById(alrhDB.getSentTextChannelId());
			if (tc != null) {
				alrhDB.forEachMessage((id, dat) -> {
					tc.deleteMessageById(id).queue(v -> log.info("Deleted old list message"));
				});
			}
			alrhDB.clearUcMsgMap();
			TextChannel lc = alrh.j.getListChannel();
			log.info("Sending list messages to channel {}", lc.getName() + "#" + alrh.tcId);
			dtos.forEach(dto -> handleDTO(lc, dto));
		} catch (Exception e) {}
		sending.set(false);
	}

	/**
	 * Calculates how many RestActions will be made by sending the list and thus how many successes
	 * are to be expected.
	 * 
	 * @param dtos
	 *            - a set containing the DTOs with the data to send
	 * @return How many successful RestActions will be had once everything is sent
	 */
	private int calcExpectedSuccesses(Set<DTO> dtos) {
		int c = 0;
		for (DTO dto : dtos) {
			//roles,reactions and 1 for the message
			//c += dto.getALRHData().size() * 2 + 1;
			c += dto.getALRHData().size() + 1;
		}
		return c;
	}

	/**
	 * Handles the actual sending of the list messages.
	 * 
	 * @param g
	 *            - The Guild to send to
	 * @param tc
	 *            - The TextChannel to send to
	 * @param dto
	 *            - The DTO containing the data to send
	 */
	private void handleDTO(TextChannel tc, DTO dto) {
		MessageEmbed me = dto.getMessage();
		Set<ALRHData> data = dto.getALRHData();
		log.debug("Sending list message...");
		tc.sendMessage(me).queue(m -> {
			incrementSuccesses();
			data.forEach(alrhd -> {
				if (alrhDB.isReacted(alrhd)) {
					alrhd.setReacted(true);
				}
				alrhd.setTextChannelId(tc.getIdLong());
				/*
				 * List<Role> roles = g.getRolesByName(title, false); if (roles.isEmpty()) {
				 * log.info("Creating role for {}", title);
				 * g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r ->
				 * { if (r != null) { log.debug("Storing role {} with id {}", r.getName(),
				 * r.getId()); alrhd.setRoleId(r.getId()); alrhDB.addALRHData(alrhd);
				 * incrementSuccesses(); } }, rerr -> log.error("Failed creating role for {}",
				 * title, rerr)); } else { log.debug("Role {} already exists, getting id", title);
				 * alrhd.setRoleId(roles.get(0).getId()); incrementSuccesses(); }
				 */
				String uniCP = alrhd.getUnicodeCodePoint();
				log.debug("Adding reaction {} to message", uniCP);
				m.addReaction(uniCP).queue(v -> {
					incrementSuccesses();
					log.debug("Added reaction {} to message", uniCP);
					if (successes.get() == expectedNumSuccesses) {
						alrh.dataChanged();
						successes.set(0);
						alrh.jData.save(true);
						sending.set(false);
					}
				});
			});
			alrhDB.setDataForMessage(m.getIdLong(), data);
		});
	}

	private void incrementSuccesses() {
		log.debug("Successful sends: {}/{}", successes.incrementAndGet(), expectedNumSuccesses);
	}

	void update() {
		sendList();
	}
}
