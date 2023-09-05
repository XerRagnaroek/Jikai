package com.github.xerragnaroek.jikai.util.pagi;

import com.github.xerragnaroek.jikai.core.Core;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Allows for up to 10 options
 */
public abstract class Pagination extends ListenerAdapter {
    static final String FORWARD_CP = "U+25b6";
    static final String BACKWARD_CP = "U+25c0";
    // private static final List<String> codePoints = Arrays.asList("U+31U+fe0fU+20e3",
    // "U+32U+fe0fU+20e3", "U+33U+fe0fU+20e3", "U+34U+fe0fU+20e3", "U+35U+fe0fU+20e3",
    // "U+36U+fe0fU+20e3", "U+37U+fe0fU+20e3", "U+38U+fe0fU+20e3", "U+39U+fe0fU+20e3", "U+1f51f");
    protected int stage = 0;
    protected List<PaginationStage> stages = new LinkedList<>();
    protected MessageChannel channel;
    protected long msgId;
    protected ListenerAdapter adapter;

    Pagination() {
        setAdapter();
    }

    abstract void setAdapter();

    public void addStage(MessageEmbed embed, List<String> codePoints, Consumer<String> reactionAddHandler, Consumer<String> reactionRemHandler, boolean externalAdvance, boolean externalRetreat, IntConsumer onStageChange) {
        stages.add(new PaginationStage(embed, codePoints, Optional.ofNullable(reactionAddHandler), Optional.ofNullable(reactionRemHandler), externalAdvance, externalRetreat, onStageChange));
    }

    public void addStage(MessageEmbed embed, List<String> codePoints, Consumer<String> reactionAddHandler, Consumer<String> reactionRemHandler, boolean externalAdvance, boolean externalRetreat) {
        addStage(embed, codePoints, reactionAddHandler, reactionRemHandler, externalAdvance, externalRetreat, null);
    }

    public void addStage(MessageEmbed embed, List<String> codePoints, Consumer<String> reactionAddHandler, Consumer<String> reactionRemHandler, boolean externalAdvance) {
        addStage(embed, codePoints, reactionAddHandler, reactionRemHandler, externalAdvance, false);
    }

    public void addStage(MessageEmbed embed, List<String> codePoints, Consumer<String> reactionAddHandler, Consumer<String> reactionRemHandler) {
        addStage(embed, codePoints, reactionAddHandler, reactionRemHandler, false);
    }

    public void addStage(MessageEmbed embed, List<String> codePoints, Consumer<String> reactionAddHandler) {
        addStage(embed, codePoints, reactionAddHandler, null);
    }

    public void addStage(MessageEmbed embed, List<String> codePoints) {
        addStage(embed, codePoints, null);
    }

    public void addStage(MessageEmbed embed) {
        addStage(embed, Collections.emptyList());
    }

    public PaginationStage getCurrentStage() {
        return stages.get(stage);
    }

    public int getCurrentStageInt() {
        return stage;
    }

    public void send(MessageChannel channel) {
        this.channel = channel;
        doStage();
    }

    public CompletableFuture<Message> editCurrentMessage(MessageEmbed meb) {
        return channel.editMessageEmbedsById(msgId, meb).submit();
    }

    void doStage() {
        unregisterListener();
        PaginationStage ps;
        if ((ps = stages.get(stage)).onStageChange() != null) {
            ps.onStageChange().accept(stage);
        }
        doStageImpl().thenAccept(v -> {
            registerListener();
        });
    }

    public void editStage(int stage, MessageEmbed edit) {
        PaginationStage ps = stages.get(stage);
        stages.set(stage, new PaginationStage(edit, ps.reactionCodePoints(), ps.reactionAddHandler(), ps.reactionRemHandler(), ps.externalAdvance(), ps.externalRetreat(), ps.onStageChange()));
        if (stage == getCurrentStageInt()) {
            editCurrentMessage(edit);
        }
    }

    abstract CompletableFuture<?> doStageImpl();

    CompletableFuture<?> addReactions(Message m) {
        int c = 0;
        PaginationStage pStage = stages.get(stage);
        List<String> cps = pStage.reactionCodePoints();
        RestAction<Void> ra = null;
        if (cps.isEmpty()) {
            if (stage == 0) {
                if (!pStage.externalAdvance()) {
                    ra = m.addReaction(Emoji.fromUnicode(FORWARD_CP));
                }
            } else {
                if (!pStage.externalRetreat()) {
                    ra = m.addReaction(Emoji.fromUnicode(BACKWARD_CP));
                }
            }
        } else {
            if (stage > 0 && !pStage.externalRetreat()) {
                ra = m.addReaction(Emoji.fromUnicode(BACKWARD_CP));
            } else {
                ra = m.addReaction(Emoji.fromUnicode(cps.get(c++)));
            }
            for (; c < cps.size(); c++) {
                ra = ra.and(m.addReaction(Emoji.fromUnicode(cps.get(c))));
            }
        }
        if (stage + 1 < stages.size() && !pStage.externalAdvance()) {
            ra = ra.and(m.addReaction(Emoji.fromUnicode(FORWARD_CP)));
        }
        return ra.submit();
    }

    private void registerListener() {
        Core.JDA.addEventListener(adapter);
    }

    private void unregisterListener() {
        Core.JDA.removeEventListener(adapter);
    }

    public void nextStage() {
        stage++;
        doStage();
    }

    public void previousStage() {
        stage--;
        doStage();
    }

    void setMsgId(long id) {
        msgId = id;
    }

    public void skipToStage(int stage) {
        this.stage = stage;
        doStage();
    }

    public int getStages() {
        return stages.size();
    }

    public void end() {
        unregisterListener();
    }
}

record PaginationStage(MessageEmbed embed, List<String> reactionCodePoints,
                       Optional<Consumer<String>> reactionAddHandler, Optional<Consumer<String>> reactionRemHandler,
                       boolean externalAdvance, boolean externalRetreat, IntConsumer onStageChange) {
}