package me.entityreborn.AngelGates.events;

import me.entityreborn.AngelGates.Portal;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * AngelGate - A portal plugin for Bukkit
 * Copyright (C) 2011, 2012 Steven "Drakia" Scott <Contact@TheDgtl.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class AngelGatesDestroyEvent extends AngelGatesEvent {
	private Player player;
	private boolean deny;
	private String denyReason;
	private int cost;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public AngelGatesDestroyEvent(Portal portal, Player player, boolean deny, String denyMsg, int cost) {
		super("AngelGatesDestroyEvent", portal);
		this.player = player;
		this.deny = deny;
		this.denyReason = denyMsg;
		this.cost = cost;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public boolean getDeny() {
		return deny;
	}
	
	public void setDeny(boolean deny) {
		this.deny = deny;
	}
	
	public String getDenyReason() {
		return denyReason;
	}
	
	public void setDenyReason(String denyReason) {
		this.denyReason = denyReason;
	}
	
	public int getCost() {
		return cost;
	}
	
	public void setCost(int cost) {
		this.cost = cost;
	}

}
