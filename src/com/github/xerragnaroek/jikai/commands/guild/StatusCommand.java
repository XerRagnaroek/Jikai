
package com.github.xerragnaroek.jikai.commands.guild;

import static com.github.xerragnaroek.jikai.core.Core.JDA;

import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class StatusCommand implements GuildCommand {
	@Override
	public String getName() {
		return "status";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		Jikai j = Core.JM.get(g);
		JikaiData jd = j.getJikaiData();
		JikaiLocale loc = j.getLocale();
		try {
			TextChannel tc = j.getInfoChannel();
			Instant now = Instant.now();
			Message m = tc.sendMessage("Ping check...").complete();
			long ping = Instant.now().toEpochMilli() - now.toEpochMilli();
			String msg = loc.getStringFormatted("com_g_status_msg", Arrays.asList("ping", "gping", "anime", "lang", "coms", "servers", "users", "enabled"), ping, JDA.getGatewayPing(), AnimeDB.size(), loc.getLanguageName(), jd.getExecutedCommandCount(), JDA.getGuildCache().size(), JikaiUserManager.getInstance().userAmount(), jd.areCommandsEnabled());
			String[] tmp = msg.split("\n");
			// codeblock,title and codeblock end
			String[] nonFormat = { tmp[0], tmp[1], tmp[tmp.length - 1] };
			// all data fields
			tmp = ArrayUtils.subarray(tmp, 2, tmp.length - 2);
			String formatted = BotUtils.padEquallyAndJoin(loc.getString("com_g_status_sep"), "\n", null, tmp);
			formatted = nonFormat[0] + "\n" + nonFormat[1] + "\n" + formatted + "\n" + nonFormat[2];
			m.editMessage(formatted).queue();
		} catch (Exception e) {
			Core.logThrowable(e);
		}
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_g_status_desc");
	}

	@Override
	public boolean isAlwaysEnabled() {
		return true;
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}
}
