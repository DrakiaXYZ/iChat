package net.TheDgtl.iChat;

/**
 * iChat - A chat formatting plugin for Bukkit.
 * Copyright (C) 2011 Steven "Drakia" Scott <Drakia@Gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

public class playerListener extends PlayerListener {
	// Use this for permissions checking.
	iChat ichat;
	
	playerListener(iChat ichat) {
		this.ichat = ichat;
	}
	
	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (ichat.permissions == null) return;
		Player p = event.getPlayer();
		String msg = event.getMessage();
		
		// Variables we can use in a message
		String prefix = ichat.getPrefix(p);
		String suffix = ichat.getSuffix(p);
		String group = ichat.getGroup(p);
		if (prefix == null) prefix = "";
		if (suffix == null) suffix = "";
		if (group == null) group = "";
		String healthbar = ichat.healthBar(p);
		String health = String.valueOf(p.getHealth());
		String world = p.getWorld().getName();
		// Timestamp support
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(ichat.dateFormat);
		String time = dateFormat.format(now);
		
		// Screwit, adding a space to make color-code glitch not kill us
		msg = msg + " ";
		// We're sending this to String.format, so we need to escape those pesky % symbols
		msg = msg.replaceAll("%", "%%");
		// Censor message
		msg = ichat.censor(p, msg);
		
		String format = ichat.parseVars(ichat.chatFormat, p);
		if (format == null) return;
		// Order is important, this allows us to use all variables in the suffix and prefix! But no variables in the message
		String[] search = new String[] {"+suffix,+s", "+prefix,+p", "+group,+g", "+healthbar,+hb", "+health,+h", "+world,+w", "+time,+t", "+name,+n", "+displayname,+d", "+message,+m"};
		String[] replace = new String[] { suffix, prefix, group, healthbar, health, world, time, "%1$s", p.getDisplayName(), msg };
		event.setFormat( ichat.parse(format, search, replace) );
	}
}
