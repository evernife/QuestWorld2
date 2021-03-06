package me.mrCookieSlime.QuestWorld.extensions.builtin;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.CSCoreLibPlugin.general.String.StringUtils;
import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.MissionChange;
import me.mrCookieSlime.QuestWorld.api.MissionType;
import me.mrCookieSlime.QuestWorld.api.interfaces.IMission;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;

public class FishMission extends MissionType implements Listener {
	public FishMission() {
		super("FISH", true, true, new ItemStack(Material.FISHING_ROD));
	}
	
	@Override
	public ItemStack userDisplayItem(IMission instance) {
		return instance.getMissionItem().clone();
	}
	
	@Override
	protected String userInstanceDescription(IMission instance) {
		return "&7Fish up " + instance.getAmount() + "x " + StringUtils.formatItemName(instance.getDisplayItem(), false);
	}
	
	@EventHandler
	public void onFish(PlayerFishEvent e) {
		if (!(e.getCaught() instanceof Item)) return;
		ItemStack caught = ((Item)e.getCaught()).getItemStack();

		QuestWorld.getInstance().getManager(e.getPlayer()).forEachTaskOf(this, mission -> {
			return QuestWorld.getInstance().isItemSimiliar(caught, mission.getMissionItem());
		}, caught.getAmount(), false);
	}
	
	@Override
	protected void layoutMenu(MissionChange changes) {
		super.layoutMenu(changes);
		putButton(10, MissionButton.item(changes));
		putButton(17, MissionButton.amount(changes));
	}
}
