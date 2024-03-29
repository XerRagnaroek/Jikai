package com.github.xerragnaroek.jikai.commands.guild.set;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.list.btn.AnimeListHandler;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;


/**
 * Command that sets the channel the bot posts the role list in.
 *
 * @author XerRagnarök
 */
public class SetListChannelCommand implements GuildCommand {
    SetListChannelCommand() {
    }

    @Override
    public String getName() {
        return "list_channel";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        Guild g = event.getGuild();
        Jikai j = Core.JM.get(g);
        TextChannel textC = event.getChannel().asTextChannel();
        if (arguments.length >= 2) {
            switch (arguments[0]) {
                case "adult" -> {
                    setAdult(g, j, textC, arguments);
                }
                case "big" -> {
                    setBig(g, j, textC, arguments);
                }
                default -> setLang(g, j, textC, arguments);
            }
        }
    }

    private void setAdult(Guild g, Jikai j, TextChannel textC, String[] arguments) {
        textC = validateChannel(g, arguments[1], textC, j);
        if (textC == null) {
            return;
        }
        j.getJikaiData().setListChannelAdultId(textC.getIdLong());
        try {
            j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_list_success", Arrays.asList("channel", "name"), textC.getAsMention(), "ADULT")).queue();
        } catch (Exception ignored) {
        }
    }

    private void setBig(Guild g, Jikai j, TextChannel textC, String[] arguments) {
        textC = validateChannel(g, arguments[1], textC, j);
        if (textC == null) {
            return;
        }
        j.getJikaiData().setListChannelBigId(textC.getIdLong());
        try {
            j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_list_success", Arrays.asList("channel", "name"), textC.getAsMention(), "BIG")).queue();
        } catch (Exception ignored) {
        }
    }

    private void setLang(Guild g, Jikai j, TextChannel textC, String[] arguments) {
        TitleLanguage lang = TitleLanguage.ROMAJI;
        switch (arguments[0].toLowerCase()) {
            case "romaji" -> lang = TitleLanguage.ROMAJI;
            case "native" -> lang = TitleLanguage.NATIVE;
            case "english" -> lang = TitleLanguage.ENGLISH;
        }
        textC = validateChannel(g, arguments[1], textC, j);
        if (textC == null) {
            return;
        }
        boolean firstTimeSet = !j.hasListChannelSet(lang);
        j.getJikaiData().setListChannelId(textC.getIdLong(), lang);
        if (firstTimeSet) {
            AnimeListHandler alh = null;
            switch (lang) {
                case ENGLISH -> alh = j.makeEnglishListHandler(textC);
                case NATIVE -> alh = j.makeNativeListHandler(textC);
                case ROMAJI -> alh = j.makeRomajiListHandler(textC);
            }
            alh.sendList(AnimeDB.getLoadedAnime());
        }
        try {
            j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_list_success", Arrays.asList("channel", "name"), textC.getAsMention(), lang.toString())).queue();
        } catch (Exception e) {
        }
    }

    private TextChannel validateChannel(Guild g, String name, TextChannel textC, Jikai j) {
        List<TextChannel> tcs = g.getTextChannelsByName(name, false);
        if (!tcs.isEmpty()) {
            return tcs.get(0);
        } else {
            textC.sendMessage(j.getLocale().getStringFormatted("com_g_set_list_fail", List.of("channel"), name)).queue();
            return null;
        }
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER};
    }

    @Override
    public List<String> getAlternativeNames() {
        return Arrays.asList("list_chan", "l_chan", "lc");
    }

    @Override
    public String getLocaleKey() {
        return "com_g_set_list";
    }
}
