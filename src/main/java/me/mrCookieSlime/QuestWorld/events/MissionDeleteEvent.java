package me.mrCookieSlime.QuestWorld.events;

import org.bukkit.event.HandlerList;

import me.mrCookieSlime.QuestWorld.quests.Mission;

public class MissionDeleteEvent extends CancellableEvent {
	private Mission mission;
	
	public MissionDeleteEvent(Mission mission) {
		this.mission = mission;
	}
	
	public Mission getMission() {
		return mission;
	}
	
	// Boilerplate copy/paste from CancellableEvent
	@Override
	public HandlerList getHandlers() { return handlers;	}
	public static HandlerList getHandlerList() { return handlers; }
	private static final HandlerList handlers = new HandlerList();
}
