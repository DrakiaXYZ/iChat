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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class playerListener implements Listener {
	// Use this for permissions checking.
	iChat ichat;
	
	playerListener(iChat ichat) {
		this.ichat = ichat;
	}
	
	@EventHandler
	public void onAsnycPlayerChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		String message = event.getMessage();
		if (message == null) return;
		// Reload variables for this player. Have to do this until there is a PermissionChangeEvent
		ichat.info.addPlayer(player);
		event.setFormat(ichat.API.parseChat(player, message, ichat.chatFormat));
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!ichat.handleMe) return;
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		String command = event.getMessage();
		if (command == null) return;
		
		if (command.toLowerCase().startsWith("/me ")) {
			ichat.info.addPlayer(player);
			String message = command.substring(command.indexOf(" ")).trim();
			String formatted = ichat.API.parseChat(player, message, ichat.meFormat);
			
			// Call iChatMeEvent
			iChatMeEvent meEvent = new iChatMeEvent(player, message);
			ichat.getServer().getPluginManager().callEvent(meEvent);
			
			// Display in console, send to players, and cancel event
			ichat.getServer().broadcastMessage(formatted);
			event.setCancelled(true);
		}
	}
}
