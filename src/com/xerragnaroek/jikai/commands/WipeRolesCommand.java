package com.xerragnaroek.jikai.commands;

import java.util.concurrent.atomic.AtomicInteger;

import com.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class WipeRolesCommand implements Command {

	@Override
	public String getName() {
		return "wipe";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		Jikai j = Core.JM.get(g);
		try {
			Message m1 = j.getInfoChannel().sendMessage("Deleting all anime roles...").complete();
			AtomicInteger count = new AtomicInteger(0);
			ALRHandler alrh = Core.JM.get(g).getALRHandler();
			g.getRoles().stream().filter(r -> r.getPermissionsRaw() == 0l).peek(r -> count.incrementAndGet()).forEach(r -> alrh.deleteRole(r));
			m1.editMessageFormat("Deleted %d roles", count.get()).queue();
		} catch (Exception e) {}
	}

	@Override
	public String getIdentifier() {
		return "wrc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_ROLES };
	}

	@Override
	public String getDescription() {
		return "Wipes all roles with no permissions. Should only be called if there are stray anime roles. Wille delete more than just this bot's roles if they also hav 0 perms. You have been warned.";
	}
}
