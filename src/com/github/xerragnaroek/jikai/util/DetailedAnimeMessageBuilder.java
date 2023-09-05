package com.github.xerragnaroek.jikai.util;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.AnimeDate;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUserUpdater;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 0 = Synonyms
 * 1 = Episodes
 * 2 = Start Date
 * 3 = End Date
 * 4 = Next Episode date
 * 5 = Next Ep num
 * 6 = External Links
 * 7 = Popularity
 */
public class DetailedAnimeMessageBuilder {

    private final Anime a;
    private final EmbedBuilder eb;
    private final JikaiLocale loc;
    private boolean ignoreEmpty;
    private static final DateTimeFormatter formatFull = DateTimeFormatter.ofPattern("dd MMMM, yyyy");
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final ZoneId zone;
    private static final String UNKNOWN = "???";

    public DetailedAnimeMessageBuilder(Anime a, ZoneId zone, JikaiLocale loc) {
        this.a = a;
        this.loc = loc;
        this.zone = zone;
        eb = BotUtils.embedBuilder();
    }

    public DetailedAnimeMessageBuilder ignoreEmptyFields() {
        ignoreEmpty = true;
        return this;
    }

    public DetailedAnimeMessageBuilder setTitle(String title, String url) {
        eb.setTitle(title, url);
        return this;
    }

    public DetailedAnimeMessageBuilder setDescription(String desc) {
        eb.setDescription(desc);
        return this;
    }

    public DetailedAnimeMessageBuilder setThumbnail(String url) {
        eb.setThumbnail(url);
        return this;
    }

    public DetailedAnimeMessageBuilder withTitle() {
        List<String> titles = new LinkedList<>();
        List<String> titlesIgnoreCase = new LinkedList<>();
        String title = "";
        if (a.hasTitleRomaji()) {
            titles.add(a.getTitleRomaji());
            titlesIgnoreCase.add(a.getTitleRomaji().toLowerCase());
        }
        if (a.hasTitleEnglish() && !titlesIgnoreCase.contains((title = a.getTitleEnglish()).toLowerCase())) {
            titles.add(title);
            titlesIgnoreCase.add(title.toLowerCase());
        }
        if (a.hasTitleNative() && !titlesIgnoreCase.contains((title = a.getTitleNative()).toLowerCase())) {
            titles.add(title);
        }
        for (String t : titles) {
            if (title.isEmpty()) {
                title = t;
            } else if (title.length() + t.length() + "\n".length() <= 256) {
                title += "\n" + t;
            } else {
                break;
            }
        }

        // title = String.join("\n", titles);
        return setTitle(title, a.getAniUrl());
    }

    public DetailedAnimeMessageBuilder withThumbnail() {
        return setThumbnail(a.getBiggestAvailableCoverImage());
    }

    public DetailedAnimeMessageBuilder withSynonyms() {
        return addField(0, false);
    }

    public DetailedAnimeMessageBuilder withEpisodes() {
        return addField(1, false);
    }

    public DetailedAnimeMessageBuilder withStartDate() {
        return addField(2, false);
    }

    public DetailedAnimeMessageBuilder withEndDate() {
        return addField(3, false);
    }

    public DetailedAnimeMessageBuilder withNextEpisodeDate() {
        return addField(4, false);
    }

    public DetailedAnimeMessageBuilder withNextEpNumber() {
        return addField(5, false);
    }

    public DetailedAnimeMessageBuilder withExternalLinks() {
        return addField(6, false);
    }

    public DetailedAnimeMessageBuilder withPopularity() {
        return addField(7, false);
    }

    public DetailedAnimeMessageBuilder withSynonyms(boolean inline) {
        return addField(0, inline);
    }

    public DetailedAnimeMessageBuilder withEpisodes(boolean inline) {
        return addField(1, inline);
    }

    public DetailedAnimeMessageBuilder withStartDate(boolean inline) {
        return addField(2, inline);
    }

    public DetailedAnimeMessageBuilder withEndDate(boolean inline) {
        return addField(3, inline);
    }

    public DetailedAnimeMessageBuilder withNextEpisodeDate(boolean inline) {
        return addField(4, inline);
    }

    public DetailedAnimeMessageBuilder withNextEpNumber(boolean inline) {
        return addField(5, inline);
    }

    public DetailedAnimeMessageBuilder withExternalLinks(boolean inline) {
        return addField(6, inline);
    }

    public DetailedAnimeMessageBuilder withPopularity(boolean inline) {
        return addField(7, inline);
    }

    public DetailedAnimeMessageBuilder withAll(boolean inline) {
        withTitle();
        withThumbnail();
        for (int i = 0; i <= 7; i++) {
            addField(i, inline);
        }
        return this;
    }

    private void addField(String locKey, boolean inline, String format, String replace) {
        locKey = "dam_" + locKey;
        String title = loc.getString(locKey + "_n");
        String val = loc.getStringFormatted(locKey + "_v", Collections.singletonList(format), replace);
        eb.addField(title, val, inline);
    }

    private void synonymField(boolean inline) {
        addField("synonyms", inline, "synonyms", String.join("\n", a.getSynonyms()));
    }

    private void episodesField(boolean inline) {
        int eps = a.getEpisodes();
        addField("episodes", inline, "eps", eps > 0 ? String.valueOf(a.getEpisodes()) : UNKNOWN);
    }

    private void startDateField(boolean inline) {
        addField("start_date", inline, "date", makeDateString(a.getStartDate()));
    }

    private void endDateField(boolean inline) {
        addField("end_date", inline, "date", makeDateString(a.getEndDate()));
    }

    private void nextEpDateField(boolean inline) {
        addField("next_ep_date", inline, "nextEp", makeNextEpString());
    }

    private void nextEpNumField(boolean inline) {
        int nextEp = a.getNextEpisodeNumber();
        addField("next_ep_num", inline, "nextEpNum", nextEp > 0 ? String.valueOf(nextEp) : UNKNOWN);
    }

    private void externalLinksField(boolean inline) {
        addField("external", inline, "extLinks", a.getExternalLinks().stream().map(es -> String.format("[%s](%s)", es.getSite(), es.getUrl())).collect(Collectors.joining("\n")));
    }

    private void popularityField(boolean inline) {
        addField("popularity", inline, "pop", String.valueOf(a.getPopularity()));
    }

    private DetailedAnimeMessageBuilder addField(int f, boolean inline) {
        switch (f) {
            case 0 -> {
                if (!ignoreEmpty || !a.getSynonyms().isEmpty()) {
                    synonymField(inline);
                }
            }
            case 1 -> {
                if (!ignoreEmpty || a.getEpisodes() > 0) {
                    episodesField(inline);
                }
            }
            case 2 -> {
                if (!ignoreEmpty || a.getStartDate().isUsable()) {
                    startDateField(inline);
                }
            }
            case 3 -> {
                if (!ignoreEmpty || a.getEndDate().isUsable()) {
                    endDateField(inline);
                }
            }
            case 4 -> {
                if (!ignoreEmpty || a.hasDataForNextEpisode()) {
                    nextEpDateField(inline);
                }
            }
            case 5 -> {
                if (!ignoreEmpty || a.getNextEpisodeNumber() > 0) {
                    nextEpNumField(inline);
                }
            }
            case 6 -> {
                if (!ignoreEmpty || !a.getExternalLinks().isEmpty()) {
                    externalLinksField(inline);
                }
            }
            case 7 -> {
                if (!ignoreEmpty || a.getPopularity() > 0) {
                    popularityField(inline);
                }
            }
        }
        return this;
    }

    public MessageEmbed build() {
        return eb.build();
    }

    private String makeDateString(AnimeDate ad) {
        if (ad.hasAll()) {
            return ad.toZDT(zone).format(formatFull);
        } else if (ad.hasYear() && ad.hasMonth()) {
            return LocalDateTime.of(ad.getYear(), ad.getMonth(), 1, 1, 1).format(format);
        }
        return UNKNOWN;
    }

    private String makeNextEpString() {
        if (a.hasDataForNextEpisode()) {
            return JikaiUserUpdater.formatAirDateTime(a.getNextEpisodeDateTime(zone).get(), loc.getLocale());
        } else {
            return "TBA";
        }
    }

}
