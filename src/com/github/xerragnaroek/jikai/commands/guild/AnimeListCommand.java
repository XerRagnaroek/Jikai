package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.list.BigListHandler;
import com.github.xerragnaroek.jikai.anime.list.btn.AnimeListHandler;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AnimeListCommand implements GuildCommand {

    private final Logger log = LoggerFactory.getLogger(AnimeListCommand.class);

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        log.debug("Executing ListCommand");
        Jikai j = Core.JM.get(event.getGuild());
        JikaiLocale loc = j.getLocale();
        AnimeDB.waitUntilLoaded();
        MessageChannel mc = event.getChannel();
        boolean isSending = false;
        if (arguments.length == 0) {
            for (TitleLanguage lang : TitleLanguage.values()) {
                AnimeListHandler h = j.getAnimeListHandler(lang);
                if (!(isSending = h.isSendingList())) {
                    h.sendList(AnimeDB.getLoadedAnime());
                }
            }
            j.getBigListHandlerMap().values().forEach(BigListHandler::sendList);
        } else {
            BigListHandler blh = j.getBigListHandler(arguments[0]);
            if (blh != null) {
                blh.sendList();
            } else {
                TitleLanguage lang = null;
                switch (arguments[0].toLowerCase()) {
                    case "romaji" -> lang = TitleLanguage.ROMAJI;
                    case "english" -> lang = TitleLanguage.ENGLISH;
                    case "native" -> lang = TitleLanguage.NATIVE;
                }
                AnimeListHandler h = j.getAnimeListHandler(lang);
                if (!(isSending = h.isSendingList())) {
                    h.sendList(AnimeDB.getLoadedAnime());
                }
            }
        }
        if (isSending) {
            log.debug("List is already being sent");
            mc.sendMessage(loc.getString("com_g_list_wait")).queue();
        }
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return CommandHandler.MOD_PERMS;
    }

    @Override
    public String getLocaleKey() {
        return "com_g_list";
    }

}
