package com.github.xerragnaroek.jikai.commands.guild;

import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * 
 */
public class SetupJikaiServerCommand implements GuildCommand {

	@Override
	public String getName() {
		return "setup";
	}

	@Override
	public String getDescription() {
		return "Finish the setup for the central jikai server";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		g.createTextChannel("welcome").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL), Arrays.asList(Permission.MESSAGE_WRITE)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0l).submit().thenAccept(this::welcome);
	}

	private void welcome(TextChannel tc) {
		try {
			tc.sendMessage("Welcome to my server!").addFile(BotUtils.imageToByteArray(ImageIO.read(SetupJikaiServerCommand.class.getResourceAsStream("/jikai.png"))), "jikai.png").complete();
		} catch (IOException e) {
			Core.ERROR_LOG.error("Couldn't load jikai image!", e);
		}
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
