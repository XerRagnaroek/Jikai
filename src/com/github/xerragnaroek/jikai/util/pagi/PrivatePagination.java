package com.github.xerragnaroek.jikai.util.pagi;

import java.util.concurrent.CompletableFuture;

import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent;

/**
 * 
 */
public class PrivatePagination extends Pagination {

	public PrivatePagination() {
		super();
	}

	@Override
	CompletableFuture<?> doStageImpl() {
		PaginationStage pStage = stages.get(stage);
		if (msgId != 0) {
			channel.deleteMessageById(msgId).submit();
		}
		return channel.sendMessageEmbeds(pStage.embed()).submit().thenCompose(m -> {
			setMsgId(m.getIdLong());
			return addReactions(m);
		});
	}

	@Override
	void setAdapter() {
		this.adapter = this;
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		if (event.getMessageIdLong() == msgId && !event.getUser().isBot()) {
			String cp = event.getReactionEmote().getAsCodepoints();
			switch (cp) {
				case FORWARD_CP: {
					nextStage();
					break;
				}
				case BACKWARD_CP: {
					previousStage();
					break;
				}
				default:
					stages.get(stage).reactionAddHandler().ifPresent(c -> c.accept(event.getReactionEmote().getAsCodepoints()));
			}

		}
	}

	@Override
	public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event) {
		if (event.getMessageIdLong() == msgId && !event.getUser().isBot()) {
			stages.get(stage).reactionRemHandler().ifPresent(c -> c.accept(event.getReactionEmote().getAsCodepoints()));
		}
	}

}
