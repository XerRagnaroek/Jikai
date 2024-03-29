package com.github.xerragnaroek.jikai.user;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;
import com.github.xerragnaroek.jikai.util.btn.ButtonedMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 *
 */
public class SubListHandlerBtn implements ButtonInteractor {

    private static final String identifier = "slh";
    private final Logger log = LoggerFactory.getLogger(SubListHandlerBtn.class);

    static {
        Core.getEventListener().registerButtonInteractor(new SubListHandlerBtn());
    }

    private SubListHandlerBtn() {
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void handleButtonClick(String[] data, ButtonInteractionEvent event) {
        event.deferEdit().queue();
        long juId = event.getUser().getIdLong();
        log.debug("Handling sublist click for user {}", juId);
        if (!JikaiUserManager.getInstance().isKnownJikaiUser(juId)) {
            log.debug("non JikaiUser clicked on a sub list!");
            return;
        }
        JikaiUser ju = JikaiUserManager.getInstance().getUser(juId);
        int id = Integer.parseInt(data[0]);
        if (ju.isSubscribedTo(id)) {
            ju.unsubscribeAnime(id, ju.getLocale().getString("ju_private_list_unsub_cause"));
            event.editButton(event.getButton().withStyle(ButtonStyle.DANGER)).queue();
        } else {
            ju.subscribeAnime(id, ju.getLocale().getString("ju_private_list_sub_cause"));
            event.editButton(event.getButton().withStyle(ButtonStyle.SUCCESS)).queue();
        }
    }

    public static void sendSubList(JikaiUser ju) {
        BotUtils.sendPMs(ju.getUser(), makeSubMessages(ju));
    }

    private static List<MessageCreateData> makeSubMessages(JikaiUser ju) {
        Set<Anime> anime = ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).collect(Collectors.toCollection(() -> new TreeSet<>(Anime.IGNORE_TITLE_CASE)));
        List<List<Anime>> partitioned = BotUtils.partitionCollection(anime, ButtonedMessage.MAX_BUTTONS);
        List<MessageCreateData> messages = new LinkedList<>();
        String title = ju.getLocale().getStringFormatted("com_ju_subs_eb_title", List.of("anime"), anime.size());
        for (int partition = 0; partition < partitioned.size(); partition++) {
            ButtonedMessage msg = new ButtonedMessage();
            List<Anime> l = partitioned.get(partition);
            EmbedBuilder eb = BotUtils.embedBuilder();
            eb.setTitle(title + (partitioned.size() > 1 ? " " + (partition + 1) + "/" + partitioned.size() : ""));
            for (int i = 0; i < l.size(); i++) {
                Anime a = l.get(i);
                eb.appendDescription(String.format("**%02d**: [**%s**](%s)%n", i + 1, a.getTitle(ju.getTitleLanguage()), a.getAniUrl()));
                msg.addButton(Button.success(identifier + ":" + a.getId(), String.format("%02d", i + 1)));
            }
            msg.addMessageEmbed(eb.build());
            messages.add(msg.toMessage());
        }
        return messages;
    }
}
