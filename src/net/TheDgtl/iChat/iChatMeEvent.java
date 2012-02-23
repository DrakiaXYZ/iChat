package net.TheDgtl.iChat;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class iChatMeEvent extends Event {
	private String message;
	private Player player;
	
	public iChatMeEvent(final Player player, final String message) {
		this.player = player;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Player getPlayer() {
		return player;
	}
}
