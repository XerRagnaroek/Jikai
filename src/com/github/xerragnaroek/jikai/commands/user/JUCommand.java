package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.Command;
import com.github.xerragnaroek.jikai.user.JikaiUser;

public interface JUCommand extends Command {

    void executeCommand(JikaiUser ju, String[] arguments);
}
