package net.TheDgtl.iChat;

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
		
		// Screwit, adding a space to make color-code glitch not kill us
		msg = msg + " ";
		// We're sending this to String.format, so we need to escape those pesky % symbols
		msg = msg.replaceAll("%", "%%");
		// Censor message
		msg = ichat.censor(msg);
		
		
		String format = ichat.chatFormat;
		String[] search = new String[] {"+group,+g", "+name,+n", "+suffix,+s", "+prefix,+p", "+message,+m", "+healthbar,+hb", "+health,+h"};
		String[] replace = new String[] { group, "%1$s", suffix, prefix, msg, healthbar, health };
		event.setFormat( ichat.parse(format, search, replace) );
	}
}
