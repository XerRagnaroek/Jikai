package com.github.xerragnaroek.jikai.util.btn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

/**
 * 
 */
public class ButtonedMessage {
	public static final int MAX_BUTTONS = 25;
	private List<List<Button>> btns = new ArrayList<>(5);
	private List<MessageEmbed> mes = new ArrayList<>(10);
	private String msg;

	public ButtonedMessage() {
		for (int i = 0; i < 5; i++) {
			btns.add(new ArrayList<>(5));
		}
	}

	public ButtonedMessage(MessageEmbed embed) {
		this();
		mes.add(embed);
	}

	public ButtonedMessage(Collection<MessageEmbed> embeds) {
		this();
		if (embeds.size() > 10) {
			throw new IllegalArgumentException("A maximum of 10 embeds is allowed! Supplied embeds: " + embeds.size());
		}
		mes.addAll(embeds);
	}

	public ButtonedMessage(String msg) {
		this();
		this.msg = msg;
	}

	public void addButton(int row, Button btn) {
		if (row < 0 && row > 4) {
			throw new IndexOutOfBoundsException("Row index is out of bounds! 0-4");
		}
		btns.get(row).add(btn);
	}

	public void addButton(Button btn) {
		List<Button> row = null;
		for (int i = 0; i < 5; i++) {
			if (btns.get(i).size() < 4) {
				row = btns.get(i);
				break;
			}
		}
		if (row != null) {
			row.add(btn);
		}
	}

	public void addButtons(Collection<Button> col) {
		if (MAX_BUTTONS - buttonAmount() < col.size()) {
			throw new IllegalArgumentException("Supplied collection contains more buttons than the message has space left for!");
		}
		Queue<Button> q = new LinkedList<>();
		q.addAll(col);
		for (int i = 0; i < 5 && !q.isEmpty(); i++) {
			List<Button> row = btns.get(i);
			int spaceLeft = 5 - row.size();
			for (int l = 0; l < spaceLeft && !q.isEmpty(); l++) {
				row.add(q.poll());
			}
		}
	}

	public int buttonAmount() {
		return btns.stream().mapToInt(List::size).sum();
	}

	public void addMessageEmbed(MessageEmbed me) {
		if (mes.size() < 10) {
			mes.add(me);
		} else {
			throw new IndexOutOfBoundsException("Too many embeds! Max is 10");
		}
	}

	public void setMessageEmbeds(MessageEmbed me, MessageEmbed... mes) {
		if (mes.length + 1 > 10) {
			throw new IndexOutOfBoundsException("Too many embeds! Max is 10");
		}
		List<MessageEmbed> mel = new ArrayList<>(10);
		mel.add(me);
		for (MessageEmbed tmp : mes) {
			mel.add(tmp);
		}
		this.mes = mel;
	}

	public void setMessage(String msg) {
		this.msg = msg;
	}

	public Message toMessage() {
		MessageBuilder mb = new MessageBuilder();
		if (!mes.isEmpty()) {
			mb.setEmbeds(mes);
		} else if (msg != null) {
			mb.append(msg);
		}
		/*
		 * else {
		 * throw new IllegalStateException("No Message or MessageEmbeds supplied!");
		 * }
		 */
		List<ActionRow> aRows = new ArrayList<>(5);
		btns.stream().filter(l -> !l.isEmpty()).forEach(l -> aRows.add(ActionRow.of(l)));
		mb.setActionRows(aRows);
		return mb.build();
	}
}
