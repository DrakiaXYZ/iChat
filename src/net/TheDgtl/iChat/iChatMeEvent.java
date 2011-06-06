package net.TheDgtl.iChat;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class iChatMeEvent extends Event {
	private static final long serialVersionUID = -4007312028228633239L;
	private String message;
	private Player player;
	
	public iChatMeEvent(final Player player, final String message) {
		super("iChatMeEvent");
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
