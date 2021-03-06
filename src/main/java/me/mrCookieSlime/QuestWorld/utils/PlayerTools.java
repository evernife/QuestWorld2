package me.mrCookieSlime.QuestWorld.utils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.Translation;
import me.mrCookieSlime.QuestWorld.api.Translator;

public class PlayerTools {
	public static ItemStack getActiveHandItem(Player p) {
		org.bukkit.inventory.PlayerInventory pi = p.getInventory();
		
		ItemStack result = pi.getItemInMainHand();
		if(result == null)
			result = pi.getItemInOffHand();
		
		return result;
	}
	
	public static ItemStack getMainHandItem(Player p) {
		return p.getInventory().getItemInMainHand();
	}
	
	public static int getMaxCraftAmount(CraftingInventory inv) {
		int resultCount = inv.getResult().getAmount();
		int materialCount = Integer.MAX_VALUE;
		
		for(ItemStack is : inv.getMatrix())
			if(is != null && is.getAmount() < materialCount)
				materialCount = is.getAmount();

		return resultCount * materialCount;
	}
	
	public static int fits(ItemStack stack, Inventory inv) {
		ItemStack[] contents = inv.getStorageContents();
		int result = 0;
		
		for(ItemStack is : contents)
			if(is == null)
				result += stack.getMaxStackSize();
			else if(is.isSimilar(stack))
				result += Math.max(stack.getMaxStackSize() - is.getAmount(), 0);
		
		return result;
	}
	
	public static void setActiveHandItem(Player p, ItemStack is) {
		org.bukkit.inventory.PlayerInventory pi = p.getInventory();
		
		if(pi.getItemInMainHand() == null && pi.getItemInOffHand() != null)
			pi.setItemInOffHand(is);
		else
			pi.setItemInMainHand(is);
	}
	
	public static void sendTranslation(CommandSender p, boolean prefixed, Translator key, String... replacements) {
		String text = QuestWorld.translate(key, replacements);
		if(text.isEmpty())
			return;
		
		if(prefixed)
			text = QuestWorld.translate(Translation.default_prefix) + text;
		
		p.sendMessage(Text.colorize(text));
	}
	
	@SuppressWarnings("deprecation")
	public static Player getPlayer(String name) {
		return Bukkit.getPlayerExact(name);
	}
	
	private interface IReflector {
		ItemStack pickBlock(Block block) throws Exception;
	}
	
	private static class Reflector implements IReflector {
		@Override
		public ItemStack pickBlock(Block block) throws Exception {
			World w = block.getWorld();
			Chunk c = block.getChunk();
			Class<?> serverClass = Bukkit.getServer().getClass();
			String CBS = serverClass.getName().replaceFirst("[^.]+$", "");
			String NMS = serverClass.getMethod("getServer").getReturnType().getName().replaceFirst("[^.]+$", "");
			
			Object world = w.getClass().getMethod("getHandle").invoke(w);
			Object chunk = c.getClass().getMethod("getHandle").invoke(c);
			
			Object blockposition = Class.forName(NMS + "BlockPosition")
					.getConstructor(int.class, int.class, int.class)
					.newInstance(block.getX(), block.getY(), block.getZ());
			Object iblockdata = chunk.getClass().getMethod("getBlockData", blockposition.getClass()).invoke(chunk, blockposition);
			
			Class<?> worldClass = Class.forName(NMS + "World");
			@SuppressWarnings("deprecation")
			Object rawblock = Bukkit.getUnsafe().getClass().getMethod("getBlock", Block.class).invoke(null, block);
			Class<?> iblockclass = Class.forName(NMS + "IBlockData");
			Object rawitemstack = rawblock.getClass().getMethod("a", worldClass, blockposition.getClass(), iblockclass)
				.invoke(rawblock, world, blockposition, iblockdata);

			return (ItemStack)Class.forName(CBS + "inventory.CraftItemStack").getMethod("asCraftMirror", rawitemstack.getClass()).invoke(null, rawitemstack);
		}
	}
	
	private static class NullReflector implements IReflector {
		@Override
		public ItemStack pickBlock(Block block) {
			return null;
		}
	}
	
	private static IReflector reflector = null;
	
	public static ItemStack getStackOf(Block block) {
		ItemStack res = null;
		if(reflector == null) {
			reflector = new Reflector();
			try {
				res = reflector.pickBlock(block);
			}
			catch(Exception e) {
				Log.warning("Unable to reflect \"pickBlock\" method, QuestWorld was not fully prepared for your minecraft version");
				Log.warning("Falling back to MaterialData comparison for all future checks. Mining quests may not detect blocks as accurately");
				reflector = new NullReflector();
			}
		}
		else
			try {
				res = reflector.pickBlock(block);
			}
			catch(Exception e) {
				Log.severe("Lost ability to reflect \"pickBlock\" method when we previously could!");
				e.printStackTrace();
				Log.severe("Block was: " + block.getState().getData().toItemStack(1).serialize().toString());
				
				String data = "";
				for(World w : Bukkit.getWorlds()) {
					data += w.getName() + " [" + w.getEnvironment().toString() + "] ";
				}
				
				Log.severe("Worlds: " + data);
			}
		
		if(res == null)
			res = block.getState().getData().toItemStack(1);
		
		return res;
	}
}
