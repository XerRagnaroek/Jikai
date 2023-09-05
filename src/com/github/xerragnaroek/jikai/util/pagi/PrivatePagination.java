package com.github.xerragnaroek.jikai.util.pagi;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

import java.util.concurrent.CompletableFuture;

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
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE && event.getMessageIdLong() == msgId && !event.getUser().isBot()) {
            String cp = event.getEmoji().asUnicode().getAsCodepoints();
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
                    stages.get(stage).reactionAddHandler().ifPresent(c -> c.accept(event.getEmoji().asUnicode().getAsCodepoints()));
            }

        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE && event.getMessageIdLong() == msgId && !event.getUser().isBot()) {
            stages.get(stage).reactionRemHandler().ifPresent(c -> c.accept(event.getEmoji().asUnicode().getAsCodepoints()));
        }
    }

}
