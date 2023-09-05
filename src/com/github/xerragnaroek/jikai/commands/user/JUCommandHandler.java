package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.BugCommand;
import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.commands.HelpCommand;
import com.github.xerragnaroek.jikai.commands.dev.*;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class JUCommandHandler {

    private static final Set<JUCommand> commands = Collections.synchronizedSet(new TreeSet<>());
    private static final Logger log = LoggerFactory.getLogger(JUCommandHandler.class);
    private static final String prefix = "!";

    static {
        JUCommand[] coms = new JUCommand[]{new SyncAniListsCommand(), new DropAnimeCommand(), new MaxRequestsCommand(), new AniAuthCommand(), new ShowAdultCommand(), new HideAllCommand(), new UnhideAllCommand(), new CustomTitleCommand(), new HideAnimeCommand(), new LinksCommand(), new ChangeLocaleCommand(), new NextEpisodeMsgCommand(), new TestCommand(), new TestEpisodeTrackerCommand(), new ForceSaveCommand(), new EpisodesCommand(), new SubAllCommand(), new UnsubAllCommand(), new UnlinkUserCommand(), new LinkUserCommand(), new SetActivityCommand(), new CodePointTestCommand(), new ImportSubscriptionsCommand(), new ExportSubscriptionsCommand(), new SendPMCommand(), new TestNextEpMessageCommand(), new TestPeriodChangeCommand(), new CancelUpdateThreadCommand(), new UpdateThreadStatusCommand(), new TestReactionCommand(), new UnlinkAniAccountCommand(), new LinkAniAccountCommand(), new ReloadLocalesCommand(), new BugCommand(), new UnregisterCommand(), new WeeklyScheduleCommand(), new ForceDBUpdateCommand(), new TestDailyUpdateCommand(), new TestPostponeCommand(), new SubscriptionsCommand(), new ForceDBUpdateCommand(), new TestNotifyCommand(), new StopCommand(), new HelpCommand(), new ConfigCommand(), new DailyUpdateCommand(), new NotifyReleaseCommand(), new NotificationTimeCommand(), new TimeZoneCommand(), new TitleLanguageCommand()};
        commands.addAll(Arrays.asList(coms));
    }

    private JUCommandHandler() {
    }

    public static Set<JUCommand> getCommands() {
        return commands;
    }

    public static void handleMessage(JikaiUser ju, String content) {
        if (content.startsWith(prefix)) {
            content = content.substring(prefix.length());
            String[] tmp = content.trim().split(" ");
            JUCommand com = ComUtils.findCommand(commands, tmp[0]);
            if (com != null && com.isEnabled() && ComUtils.checkPermissions(com, ju)) {
                log.debug("Found command: '{}'", com.getName());
                // remove the command from the content and execute it
                tmp = ArrayUtils.subarray(tmp, 1, tmp.length);
                try {
                    com.executeCommand(ju, tmp);
                } catch (IllegalArgumentException e) {
                    Core.ERROR_LOG.error("", e);
                    ju.sendPM(ju.getLocale().getStringFormatted("com_ju_invalid", Arrays.asList("input", "usage"), String.join(" ", tmp), com.getUsage(ju.getLocale())));
                }
            }
        }
    }
}
