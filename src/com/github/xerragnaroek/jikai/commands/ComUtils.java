package com.github.xerragnaroek.jikai.commands;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Consumer;

public class ComUtils {

    private static final Logger log = LoggerFactory.getLogger(ComUtils.class);

    public static boolean checkPermissions(GuildCommand c, Member m) {
        boolean tmp = false;
        Permission[] perms = c.getRequiredPermissions();
        if (perms.length == 0) {
            tmp = true;
        } else {
            for (Permission p : perms) {
                if (m.hasPermission(p)) {
                    return true;
                }
            }
        }
        // this is mostly to stop non jus from using commands but allowing them to still use commands like
        // help and register
        // user has the necessary perms
        if (tmp) {
            if (c.isJikaiUserOnly()) {
                // only known jus can use this command
                tmp = JikaiUserManager.getInstance().isKnownJikaiUser(m.getIdLong());
            }
        }
        if (c.isDevOnly()) {
            tmp = Core.DEV_IDS.contains(m.getIdLong());
        }
        log.debug("Member has {}sufficient permission for command {}", (tmp ? "" : "in"), c.getName());
        return tmp;
    }

    public static boolean checkPermissions(JUCommand c, JikaiUser ju) {
        boolean tmp = true;
        if (c.isDevOnly()) {
            tmp = Core.DEV_IDS.contains(ju.getId());
        }
        log.debug("JikaiUser has {}sufficient permission for command {}", (tmp ? "" : "in"), c.getName());
        return tmp;
    }

    public static <T extends Command> T findCommand(Set<T> data, String content) {
        for (T c : data) {
            if (c.getName().equals(content)) {
                return c;
            } else if (c.hasAlternativeNames()) {
                if (c.getAlternativeNames().stream().anyMatch(str -> str.equals(content))) {
                    return c;
                }
            }
        }
        return null;
    }

    public static void trueFalseCommand(String input, JikaiUser ju, Consumer<Boolean> con) {
        String str = input;
        switch (str.toLowerCase()) {
            case "true":
            case "false":
                con.accept(Boolean.parseBoolean(str));
                break;
            default:
                ju.sendPMFormat("Invalid input: '%s' !", str);
        }
    }
}
