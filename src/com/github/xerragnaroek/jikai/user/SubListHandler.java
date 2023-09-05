package com.github.xerragnaroek.jikai.user;

import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Deprecated
public class SubListHandler {

    private static final Map<Long, Pair<Integer, PrivateList>> map = Collections.synchronizedMap(new HashMap<>());
    private final static Logger log = LoggerFactory.getLogger(SubListHandler.class);

    public static void sendSubList(JikaiUser ju) {
        map.compute(ju.getId(), (id, p) -> {
            if (p == null) {
                p = sendListImpl(ju);
            } else if (p.getLeft() != ju.getSubscribedAnime().size()) {
                p.getRight().expire(false);
                p = sendListImpl(ju);
            } else {
                ju.sendPM(BotUtils.localedEmbed(ju.getLocale(), "ju_sub_list_dupe_eb", Pair.of(List.of("link"), new Object[]{BotUtils.makePrivateMessageLink(ju.getUser().openPrivateChannel().complete().getIdLong(), p.getRight().getFirstMessageId())})));
            }
            return p;
        });
    }

    private static Pair<Integer, PrivateList> sendListImpl(JikaiUser ju) {
        SubscriptionSet set = ju.getSubscribedAnime();
        PrivateList pl = new PrivateList(ju, ju.getLocale().getStringFormatted("com_ju_subs_eb_title", List.of("anime"), set.size()), null, true);
        pl.runOnExpire(() -> {
            map.remove(ju.getId());
            MDC.put("id", "" + ju.getId());
            log.debug("Removed SubList");
            MDC.remove("id");
        });
        pl.sendList(set);
        return Pair.of(set.size(), pl);
    }

}
