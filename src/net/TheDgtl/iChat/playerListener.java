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
		
		event.setFormat( ichat.parseChat(p, msg) );
	}
}
