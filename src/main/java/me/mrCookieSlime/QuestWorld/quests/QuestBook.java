package me.mrCookieSlime.QuestWorld.quests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuOpeningHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.CategoryChange;
import me.mrCookieSlime.QuestWorld.api.Manual;
import me.mrCookieSlime.QuestWorld.api.MissionChange;
import me.mrCookieSlime.QuestWorld.api.MissionType;
import me.mrCookieSlime.QuestWorld.api.QuestChange;
import me.mrCookieSlime.QuestWorld.api.Translation;
import me.mrCookieSlime.QuestWorld.api.menu.Buttons;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;
import me.mrCookieSlime.QuestWorld.containers.PagedMapping;
import me.mrCookieSlime.QuestWorld.listeners.Input;
import me.mrCookieSlime.QuestWorld.listeners.InputType;
import me.mrCookieSlime.QuestWorld.managers.PlayerManager;
import me.mrCookieSlime.QuestWorld.parties.Party;
import me.mrCookieSlime.QuestWorld.utils.ItemBuilder;
import me.mrCookieSlime.QuestWorld.utils.PlayerTools;
import me.mrCookieSlime.QuestWorld.utils.Text;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

public class QuestBook {
	
	public static void openMainMenu(Player p) {
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(null);
		
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.title"));
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().QuestClick().playTo(p);
			}
		});
		
		addPartyMenuButton(menu, p);
		
		PagedMapping view = new PagedMapping(45, 9);
		view.hackNav(4);
		for(Category category : QuestWorld.getInstance().getCategories()) {
			if (!category.isHidden()) {
				if (category.isWorldEnabled(p.getWorld().getName())) {
					if ((category.getParent() != null && !QuestWorld.getInstance().getManager(p).hasFinished(category.getParent())) || !category.hasPermission(p)) {
						view.addItem(category.getID(), new CustomItem(new MaterialData(Material.BARRIER), category.getName(), "", QuestWorld.getInstance().getBookLocal("quests.locked")));
						view.addButton(category.getID(), new MenuClickHandler() {
							
							@Override
							public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
								return false;
							}
						});
					}
					else {
						ItemStack item = category.getItem();
						ItemMeta im = item.getItemMeta();
						List<String> lore = new ArrayList<String>();
						lore.add("");
						lore.add(category.getProgress(p));
						lore.add("");
						lore.add(Text.colorize("&7" + category.getQuests().size() + QuestWorld.getInstance().getBookLocal("category.desc.total")));
						lore.add(Text.colorize("&a" + category.getFinishedQuests(p).size() + QuestWorld.getInstance().getBookLocal("category.desc.completed")));
						lore.add(Text.colorize("&b" + category.getQuests(p, QuestStatus.AVAILABLE).size() + QuestWorld.getInstance().getBookLocal("category.desc.available")));
						lore.add(Text.colorize("&e" + category.getQuests(p, QuestStatus.ON_COOLDOWN).size() + QuestWorld.getInstance().getBookLocal("category.desc.cooldown")));
						lore.add(Text.colorize("&5" + category.getQuests(p, QuestStatus.REWARD_CLAIMABLE).size() + QuestWorld.getInstance().getBookLocal("category.desc.claimable_reward")));
						im.setLore(lore);
						item.setItemMeta(im);
						view.addItem(category.getID(), item);
						view.addNavButton(category.getID(), new MenuClickHandler() {
							
							@Override
							public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
								QuestWorld.getInstance().getManager(p).putPage(0);
								openCategory(p, category, true);
								return false;
							}
						});
					}
				}
				else {
					view.addItem(category.getID(), new CustomItem(new MaterialData(Material.BARRIER), category.getName(), "", QuestWorld.getInstance().getBookLocal("quests.locked-in-world")));
					view.addButton(category.getID(), new MenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
							return false;
						}
					});
				}
			}
		}
		view.build(menu, p);
		menu.open(p);
	}
	
	public static void openLastMenu(Player p) {
		QuestingObject last = QuestWorld.getInstance().getManager(p).getLastEntry();
		if (last != null) {			
			if(last instanceof Quest) {
				Quest q = (Quest)last;
				
				if(q.isValid()) {
					QuestBook.openQuest(p, q, true, true);
					return;
				}
				else
					last = q.getCategory();
			}
			
			if (last instanceof Category) {
				Category c = (Category)last;

				if(c.isValid()) {
					QuestBook.openCategory(p, c, true);
					return;
				}
			}
		}
		
		QuestBook.openMainMenu(p);
	}
	
	private static void addPartyMenuButton(ChestMenu menu, Player p) {
		if (QuestWorld.getInstance().getCfg().getBoolean("party.enabled")) {
			ItemStack skullItem = new ItemBuilder(SkullType.PLAYER)
				.display(QuestWorld.getInstance().getBookLocal("gui.party"))
				.lore(QuestWorld.getInstance().getManager(p).getProgress(), "", QuestWorld.getInstance().getBookLocal("button.open"))
				.get();
			menu.addItem(4, skullItem);
			menu.addMenuClickHandler(4, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					openPartyMenu(arg0);
					return false;
				}
			});
		}
		else {
			menu.addItem(4, new ItemBuilder(Material.ENCHANTED_BOOK)
					.display("&eQuest Book")
					.lore("", QuestWorld.getInstance().getManager(p).getProgress()).get());
			menu.addMenuClickHandler(4, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
		}
	}

	public static void openPartyMembers(final Player p) {
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.party"));
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().PartyClick().playTo(p);
			}
		});
		ItemBuilder skull = new ItemBuilder(SkullType.PLAYER);
		menu.addItem(4, skull.display(QuestWorld.getInstance().getBookLocal("gui.party")).lore("", QuestWorld.getInstance().getBookLocal("button.back.party")).get());
		menu.addMenuClickHandler(4, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
				openPartyMenu(arg0);
				return false;
			}
		});
		
		final Party party = QuestWorld.getInstance().getManager(p).getParty();
		if (party != null) {
			for (int i = 0; i < party.getSize(); i++) {
				final OfflinePlayer player = Bukkit.getOfflinePlayer(party.getPlayers().get(i));
				if (!party.isLeader(p)) {
					
					ItemStack item = skull.skull(player.getName()).display("&e" + player.getName()).lore("", (party.isLeader(player) ? "&4Lider da Party": "&eMembro da Party"")).get();
					menu.addItem(i + 9, item);
					menu.addMenuClickHandler(i + 9, new MenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
							return false;
						}
					});
				}
				else {
					ItemStack item = skull.skull(player.getName())
							.display("&e" + player.getName())
							.lore("", (party.isLeader(player) ? "&5&lLider da Party": "&e&lMembro da Party"), "", (party.isLeader(player) ? "": "&7&oClique aqui para kikar esse membro do seu time"))
							.get();
					menu.addItem(i + 9, item);
					menu.addMenuClickHandler(i + 9, new MenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
							if (!party.isLeader(player)) {
								party.kickPlayer(player.getName());
								openPartyMembers(p);
							}
							return false;
						}
					});
				}
			}
		}
		
		menu.open(p);
	}

	public static void openPartyMenu(final Player p) {
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.party"));
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().PartyClick().playTo(p);
			}
		});
		
		menu.addItem(4, new CustomItem(new MaterialData(Material.MAP), QuestWorld.getInstance().getBookLocal("gui.title"), "", QuestWorld.getInstance().getBookLocal("button.back.quests")));
		menu.addMenuClickHandler(4, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
				openMainMenu(p);
				return false;
			}
		});
		
		final Party party = QuestWorld.getInstance().getManager(p).getParty();
		
		ItemBuilder wool = new ItemBuilder(Material.WOOL);
		
		if (party == null) {
			menu.addItem(9, wool.color(DyeColor.GREEN).display("&a&l۩ Criar Party ۩").lore("", "&a&lCrie um TIME para você!", "", "&3Com um time criado você pode", "&3convidar seus amigos para jogar", "&3e assim progredir junto nas quests :D").getNew());
			menu.addMenuClickHandler(9, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					Party.create(p);
					openPartyMenu(p);
					return false;
				}
			});
		}
		else {
			if (party.isLeader(p)) {
				menu.addItem(9, wool.color(DyeColor.GREEN).display("&a&lConvidar um Amigo").lore("", "&3Convida um jogador para o seu TIME", "&rMáximo de membros é: &e" + QuestWorld.getInstance().getCfg().getInt("party.max-members")).getNew());
				menu.addMenuClickHandler(9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						if (party.getSize() >= QuestWorld.getInstance().getCfg().getInt("party.max-members"))
							PlayerTools.sendTranslation(p, true, Translation.party_errorfull);
						else {
							PlayerTools.sendTranslation(p, true, Translation.party_playerpick);
							QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.PARTY_INVITE, party));
							p.closeInventory();
						}
						return false;
					}
				});
				
				menu.addItem(17, wool.color(DyeColor.RED).display("&4&lDeletar sua Party").lore("", "&cClicando aqui você ira deletar seu TIME", "", "&cTOME CUIDADO COM ESSA OPÇÂO!").getNew());
				menu.addMenuClickHandler(17, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						party.abandon();
						openPartyMenu(p);
						return false;
					}
				});
			}
			else {
				menu.addItem(17, wool.color(DyeColor.RED).display("&4&lSair da Party").lore("", "&cClicando aqui você ira sair do seu TIME", "", "&cTOME CUIDADO COM ESSA OPÇÂO!").getNew());
				menu.addMenuClickHandler(17, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						party.kickPlayer(p.getName());
						openPartyMenu(p);
						return false;
					}
				});
			}
			ItemStack skullItem = new ItemBuilder(SkullType.PLAYER).display("&eLista de Membros").lore("", "&3Mostra todos os membros da sua Party").get();
			menu.addItem(13, skullItem);
			menu.addMenuClickHandler(13, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					openPartyMembers(p);
					return false;
				}
			});
		}
		
		menu.open(p);
	}

	public static void openCategory(Player p, Category category, final boolean back) {
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(category);
		
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.title"));
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().QuestClick().playTo(p);
			}
		});
		
		addPartyMenuButton(menu, p);
		
		if (back) {
			menu.addItem(0, new ItemBuilder(Material.MAP).display(QuestWorld.getInstance().getBookLocal("button.back.general")).get());
			menu.addMenuClickHandler(0, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					openMainMenu(arg0);
					return false;
				}
			});
		}
		
		ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE).color(DyeColor.RED);
		
		PagedMapping view = new PagedMapping(45, 9);
		view.hackNav(4);
		for (final Quest quest: category.getQuests()) {
			glassPane.display(quest.getName());
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED) || !quest.isWorldEnabled(p.getWorld().getName())) {
				view.addItem(quest.getID(), glassPane.lore("", QuestWorld.getInstance().getBookLocal("quests.locked")).getNew());
				view.addButton(quest.getID(), new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED_NO_PARTY)) {
				view.addItem(quest.getID(), glassPane.lore("", "&4Você precisa sair da sua Party atual").getNew());
				view.addButton(quest.getID(), new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED_PARTY_SIZE)) {
				view.addItem(quest.getID(), glassPane.lore("", "&4Você só pode fazer essa missão em uma Party", "&4que tenha ao menos &c" + quest.getPartySize() + " &4membros").getNew());
				view.addButton(quest.getID(), new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else {
				ItemStack item = quest.getItem();
				ItemMeta im = item.getItemMeta();
				List<String> lore = new ArrayList<String>();
				lore.add("");
				lore.add(quest.getProgress(p));
				lore.add("");
				lore.add(Text.colorize("&7") + quest.getFinishedTasks(p).size() + "/" + quest.getMissions().size() + QuestWorld.getInstance().getBookLocal("quests.tasks_completed"));
				if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
					lore.add("");
					lore.add(QuestWorld.getInstance().getBookLocal("quests.state.reward_claimable"));
				}
				else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
					lore.add("");
					lore.add(QuestWorld.getInstance().getBookLocal("quests.state.cooldown"));
				}
				else if (QuestWorld.getInstance().getManager(p).hasFinished(quest)) {
					lore.add("");
					lore.add(QuestWorld.getInstance().getBookLocal("quests.state.completed"));
				}
				im.setLore(lore);
				item.setItemMeta(im);
				view.addItem(quest.getID(), item);
				view.addNavButton(quest.getID(), new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						openQuest(p, quest, back, true);
						return false;
					}
				});
			}
		}
		view.build(menu, p);
		menu.open(p);
	}
	
	public static void openQuest(final Player p, final Quest quest, final boolean categoryBack, final boolean back) {
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(quest);
		
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.title"));
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().QuestClick().playTo(p);
			}
		});
		
		if (back) {
			menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), QuestWorld.getInstance().getBookLocal("button.back.general")));
			menu.addMenuClickHandler(0, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					openCategory(p, quest.getCategory(), categoryBack);
					return false;
				}
			});
		}
		
		if (quest.getCooldown() >= 0) {
			String cooldown = quest.getFormattedCooldown();
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
				long remaining = (QuestWorld.getInstance().getManager(p).getCooldownEnd(quest) - System.currentTimeMillis() + 59999) / 60 / 1000;
				cooldown = (remaining / 60) + "h " + (remaining % 60) + "m remaining";
			}
			menu.addItem(8, new CustomItem(new MaterialData(Material.WATCH), QuestWorld.getInstance().getBookLocal("quests.display.cooldown"), "", "&b" + cooldown));
			menu.addMenuClickHandler(8, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
		}
		
		int rewardIndex = 2;
		if (quest.getMoney() > 0 && QuestWorld.getInstance().getEconomy() != null) {
			menu.addItem(rewardIndex, new CustomItem(new MaterialData(Material.GOLD_INGOT), QuestWorld.getInstance().getBookLocal("quests.display.monetary"), "", "&6$" + quest.getMoney()));
			menu.addMenuClickHandler(rewardIndex, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
			rewardIndex++;
		}
		
		if (quest.getXP() > 0) {
			menu.addItem(rewardIndex, new CustomItem(new MaterialData(Material.EXP_BOTTLE), QuestWorld.getInstance().getBookLocal("quests.display.exp"), "", "&a" + quest.getXP() + " Level"));
			menu.addMenuClickHandler(rewardIndex, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
			rewardIndex++;
		}
		
		ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE);
		
		int index = 9;
		for (final Mission mission: quest.getMissions()) {
			if (QuestWorld.getInstance().getManager(p).hasUnlockedTask(mission)) {

				ItemBuilder entryItem = new ItemBuilder(mission.getDisplayItem()).display(mission.getText());
				
				if(mission.getType() instanceof Manual) {
					String label = ((Manual) mission.getType()).getLabel();
					entryItem.lore("", mission.getProgress(p), "", "&r> Click for Manual " + label);
				}
				else
					entryItem.lore("", mission.getProgress(p));

				menu.addItem(index, entryItem.get());
			}
			else {
				
				menu.addItem(index, glassPane.color(DyeColor.RED).display("&7&kSOMEWEIRDMISSION").lore("", QuestWorld.getInstance().getBookLocal("task.locked")).get());
			}
			
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					PlayerManager manager = QuestWorld.getInstance().getManager(p);
					
					if (!manager.hasUnlockedTask(mission)) return false;
					if (manager.getStatus(quest).equals(QuestStatus.AVAILABLE) && quest.isWorldEnabled(p.getWorld().getName())) {
						if (manager.hasCompletedTask(mission)) return false;
						
						if(mission.getType() instanceof Manual) {
							Manual m = (Manual) mission.getType();
							int progress = m.onManual(p, mission);
							if(progress != Manual.FAIL) {
								manager.setProgress(mission, progress);
								openQuest(p, quest, categoryBack, back);
							}
						}
					}
					return false;
				}
			});
			index++;
		}
		
		for (int i = 0; i < 9; i++) {
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
				menu.addItem(i + 18, glassPane.color(DyeColor.PURPLE).display(QuestWorld.getInstance().getBookLocal("quests.state.reward_claim")).get());
				menu.addMenuClickHandler(i + 18, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						quest.handoutReward(p);
						QuestWorld.getSounds().muteNext();
						openQuest(p, quest, categoryBack, back);
						return false;
					}
				});
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
				menu.addItem(i + 18, glassPane.color(DyeColor.YELLOW).display(QuestWorld.getInstance().getBookLocal("quests.state.cooldown")).get());
				menu.addMenuClickHandler(i + 18, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else {
				menu.addItem(i + 18, glassPane.color(DyeColor.GRAY).display(QuestWorld.getInstance().getBookLocal("quests.display.rewards")).get());
				menu.addMenuClickHandler(i + 18, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
		}
		
		int slot = 27;
		for (ItemStack reward: quest.getRewards()) {
			menu.addItem(slot, reward);
			menu.addMenuClickHandler(slot, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					return false;
				}
			});
			slot++;
		}
		
		menu.open(p);
	}

	
	/*
	 * 
	 * 			Quest Editor
	 * 
	 */
	public static void openEditor(Player p) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		ItemBuilder defaultItem = new ItemBuilder(Material.STAINED_GLASS_PANE)
				.color(DyeColor.RED).display("&7&l> Nova categoria");

		PagedMapping view = new PagedMapping(45);
		view.touch(0); // Dummy, force a page to exist
		
		int found = 0, categoryCount = QuestWorld.getInstance().getCategories().size();
		for(int i = 0; i < view.getCapacity(); ++i) {
			if(found < categoryCount)
				view.touch(i + view.getPageCapacity());
			
			Category category = QuestWorld.getInstance().getCategory(i);
			if(category != null) {
				String[] lore = {
						"",
						"&c&oClick-Esquerdo para editar",
						"§c§oSHIFT + Click-Esquerdo para abrir",
						"&c&oClick-Direito para deletar"
				};
				int quests = category.getQuests().size();
				if(quests > 0) {
					int j = 0;
					List<String> lines = new ArrayList<>();
					for(Quest q : category.getQuests()) {
						lines.add("&7- " + q.getName());
						if(++j >= 5)
							break;
					}
					if(j < quests)
						lines.add("&7&oand "+(quests-j)+" more...");
					String[] newLore = lines.toArray(new String[lines.size() + lore.length]);
					for(j = 0; j < lore.length; ++j)
						newLore[lines.size() + j] = lore[j];
					lore = newLore;
				}
				
				++found;
				view.addItem(i, new ItemBuilder(category.getItem()).lore(lore).get());
				view.addNavButton(i, Buttons.onCategory(category));
			}
			else {
				view.addItem(i, defaultItem.get());
				view.addNavButton(i, Buttons.newCategory(i));
			}
		}

		view.build(menu, p);
		menu.open(p);
	}

	public static void openCategoryQuestEditor(Player p, final Category category) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c&l<&c<&l< Voltar"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openEditor(p);
				return false;
			}
		});
		
		ItemBuilder defaultItem = new ItemBuilder(Material.STAINED_GLASS_PANE)
				.color(DyeColor.RED).display("&3&l> Nova Quest");
		
		PagedMapping view = new PagedMapping(45);
		view.touch(0); // Dummy, force a page to exist
		
		int found = 0, questCount = category.getQuests().size();
		for (int i = 0; i < view.getCapacity(); ++i) {
			if(found < questCount)
				view.touch(i + view.getPageCapacity());
			
			Quest quest = category.getQuest(i);
			if (quest != null) {
				int missions = quest.getMissions().size();
				String[] lore = {
					"",
					"&c&oClick-Esquerdo para editar",
					"&c&oClick-Direito para deletar"
				};
				
				if(missions > 0) {
					int j = 0;
					List<String> lines = new ArrayList<>();
					for(Mission m : quest.getMissions()) {
						lines.add("&7- " + m.getText());
						if(++j >= 5)
							break;
					}
					if(j < missions)
						lines.add("&7&oand "+(missions-j)+" more...");
					String[] newLore = lines.toArray(new String[lines.size() + lore.length]);
					for(j = 0; j < lore.length; ++j)
						newLore[lines.size() + j] = lore[j];
					lore = newLore;
				}
				
				++found;
				view.addItem(i, new ItemBuilder(quest.getItem()).lore(lore).get());
				view.addNavButton(i, Buttons.onQuest(quest));
			}
			else {
				view.addItem(i, defaultItem.getNew());
				view.addNavButton(i, Buttons.newQuest(category.getID(), i));
			}
		}
		view.build(menu, p);
		menu.open(p);
	}

	public static void openCategoryEditor(Player p, final Category category) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		CategoryChange changes = new CategoryChange(category);
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c&l<&c<&l< Voltar"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openEditor(p);
				return false;
			}
		});
		
		ItemStack item = category.getItem().clone();
		ItemMeta im = item.getItemMeta();
		im.setLore(Arrays.asList("", "§e> Clique para mudar o Display-Item para", "§eo item que você esta segurando"));
		item.setItemMeta(im);
		
		menu.addItem(9, item);
		menu.addMenuClickHandler(9, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				ItemStack hand = PlayerTools.getActiveHandItem(p);
				if (hand != null) {
					changes.setItem(hand);
					if(changes.sendEvent())
						changes.apply();
					openCategoryEditor(p, category);
				}
				return false;
			}
		});
		
		menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), category.getName(), "", "§e> Clique para mudar o Nome"));
		menu.addMenuClickHandler(10, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.CATEGORY_RENAME, category));
				PlayerTools.sendTranslation(p, true, Translation.category_namechange, category.getName());
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(11, new CustomItem(new MaterialData(Material.BOOK_AND_QUILL), "§7Quest Requirement:", "", (category.getParent() != null ? "§r" + category.getParent().getName(): "§7§oNenhum"), "", "§rClick-Esquerdo: §eMuda os requerimentos da Quest", "§rClick-Direito: §eRemove os requerimentos da quest"));
		menu.addMenuClickHandler(11, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (action.isRightClicked()) {
					changes.setParent(null);
					if(changes.sendEvent())
						changes.apply();
					openCategoryEditor(p, category);
				}
				else {
					QuestWorld.getInstance().getManager(p).putPage(0);
					QBDialogue.openQuestRequirementChooser(p, category);
				}
				return false;
			}
		});
		
		menu.addItem(12, new CustomItem(new MaterialData(Material.NAME_TAG), "§r" + (category.getPermission().equals("") ? "None": category.getPermission()), "", "§e> Click to change the rquired Permission Node"));
		menu.addMenuClickHandler(12, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.CATEGORY_PERMISSION, category));
				PlayerTools.sendTranslation(p, true, Translation.category_permchange, category.getName(), category.getPermission());
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(13, new CustomItem(new MaterialData(Material.GOLDEN_CARROT), "§rMostrar no QuestBook: " + (!category.isHidden() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Clique para mudar se esta categoria ira", "&eou não aparecer no QuestBook"));
		menu.addMenuClickHandler(13, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				changes.setHidden(!category.isHidden());
				if(changes.sendEvent())
					changes.apply();
				openCategoryEditor(p, category);
				return false;
			}
		});
		
		menu.addItem(14, new CustomItem(new MaterialData(Material.GRASS), "§7World Blacklist", "", "§e> Clique para configurar em quais mundos", "&eessa categoria esta ativada!"));
		menu.addMenuClickHandler(14, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openWorldEditor(p, category);
				return false;
			}
		});
		
		menu.addItem(17, ItemBuilder.Proto.RED_WOOL.get().display("&4Delete Database").lore(
				"",
				"&rIsso ira deletar todo o banco de dados",
				"&rde todas as Quests dentro dessa categoria",
				"&re ira limpar todos os progressos associados a ela",
				"&rde todos os jogadores.").get());
		menu.addMenuClickHandler(17, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				for (Quest quest: category.getQuests()) {
					PlayerManager.clearAllQuestData(quest);
				}
				QuestWorld.getSounds().DestructiveClick().playTo(p);
				return false;
			}
		});
		
		menu.open(p);
	}

	public static void openQuestEditor(Player p, final Quest quest) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		QuestChange changes = new QuestChange(quest);
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c&l<&c<&l< Voltar"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openCategoryQuestEditor(p, quest.getCategory());
				return false;
			}
		});
		
		ItemStack item = quest.getItem().clone();
		ItemMeta im = item.getItemMeta();
		im.setLore(Arrays.asList("", "§e> Clique para mudar o Display-Item para", "§eo item que você esta segurando"));
		item.setItemMeta(im);
		
		menu.addItem(9, item);
		menu.addMenuClickHandler(9, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				ItemStack mainItem = p.getInventory().getItemInMainHand();
				if (mainItem != null) {
					changes.setItem(mainItem);
					if(changes.sendEvent())
						changes.apply();
					
					openQuestEditor(p, quest);
				}
				return false;
			}
		});
		
		menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), quest.getName(), "", "§e> Clique para mudar o Nome"));
		menu.addMenuClickHandler(10, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.QUEST_RENAME, quest));
				PlayerTools.sendTranslation(p, true, Translation.quest_namechange, quest.getName());
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(11, new CustomItem(new MaterialData(Material.CHEST), "§rRecompença §7(Em Itens)", "", "§e> Clique para editar as recompenças", "§epara os itens da sua §a§lHOTBAR"));
		menu.addMenuClickHandler(11, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				changes.setItemRewards(p);
				if(changes.sendEvent())
					changes.apply();
				
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(12, new CustomItem(new MaterialData(Material.WATCH), "§7Cooldown: §b" + quest.getFormattedCooldown(), "", "§rLeft Click: §e+1m", "§rRight Click: §e-1m", "§rShift + Left Click: §e+1h", "§rShift + Right Click: §e-1h"));
		menu.addMenuClickHandler(12, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				long cooldown = quest.getCooldown();
				long delta = action.isShiftClicked() ? 60: 1;
				if (action.isRightClicked()) delta = -delta;

				// Force a step at 0, so you can't jump from 59 -> -1 or -1 -> 59
				if(cooldown + delta < 0) {
					if(cooldown <= 0) 
						cooldown = -1;
					else
						cooldown = 0;
				}
				else if(cooldown == -1)
					cooldown = 0;
				else
					cooldown += delta;
				
				changes.setCooldown(cooldown);
				if(changes.sendEvent())
					changes.apply();
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		if (QuestWorld.getInstance().getEconomy() != null) {
			menu.addItem(13, new CustomItem(new MaterialData(Material.GOLD_INGOT), "§7Recompença §7(Em Dinheiro): §6$" + quest.getMoney(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+100", "§rShift + Right Click: §e-100"));
			menu.addMenuClickHandler(13, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int money = quest.getMoney();
					if (action.isRightClicked()) money = money - (action.isShiftClicked() ? 100: 1);
					else money = money + (action.isShiftClicked() ? 100: 1);
					if (money < 0) money = 0;
					changes.setMoney(money);
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor(p, quest);
					return false;
				}
			});
		}
		
		menu.addItem(14, new CustomItem(new MaterialData(Material.EXP_BOTTLE), "§7Recompença §7(Em XP): §b" + quest.getXP() + " Level", "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+10", "§rShift + Right Click: §e-10"));
		menu.addMenuClickHandler(14, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				int xp = quest.getXP();
				if (action.isRightClicked()) xp = xp - (action.isShiftClicked() ? 10: 1);
				else xp = xp + (action.isShiftClicked() ? 10: 1);
				if (xp < 0) xp = 0;
				changes.setXP(xp);
				if(changes.sendEvent())
					changes.apply();
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(15, new CustomItem(new MaterialData(Material.BOOK_AND_QUILL), "§7Quests Requeridas:", "", (quest.getParent() != null ? "§r" + quest.getParent().getName(): "§7§oNenhum"), "", "§rClick-Esquerdo: §eMuda os requerimentos da Quest", "§rClick-Direito: §eRemove os requerimentos da quest"));
		menu.addMenuClickHandler(15, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (action.isRightClicked()) {
					changes.setParent(null);
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor(p, quest);
				}
				else {
					QuestWorld.getInstance().getManager(p).putPage(0);
					QBDialogue.openQuestRequirementChooser(p, quest);
				}
				return false;
			}
		});
		
		menu.addItem(16, new CustomItem(new MaterialData(Material.COMMAND), "§7Comandos executados assim que a quest for completada", "", "§rLeft Click: §eAbrir editor de comando"));
		menu.addMenuClickHandler(16, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				p.closeInventory();
				QBDialogue.openCommandEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(17, new CustomItem(new MaterialData(Material.NAME_TAG), "§r" + (quest.getPermission().equals("") ? "None": quest.getPermission()), "", "§e> Click to change the required Permission Node"));
		menu.addMenuClickHandler(17, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.QUEST_PERMISSION, quest));
				PlayerTools.sendTranslation(p, true, Translation.quest_permchange, quest.getName(), quest.getPermission());
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(18, new CustomItem(new MaterialData(Material.FIREWORK), "§rParty Support: " + (quest.supportsParties() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Clique para mudar se essa quest pode ou nao ser feita em Partys"));
		menu.addMenuClickHandler(18, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				changes.setPartySupport(quest.supportsParties());
				if(changes.sendEvent())
					changes.apply();
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(19, new CustomItem(new MaterialData(Material.COMMAND), "§rCompletada na ordem certa: " + (quest.isOrdered() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Clique para mudar se as tarefas", "§edevem ser feitas na ordem que foram arranjadas"));
		menu.addMenuClickHandler(19, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				changes.setOrdered(!quest.isOrdered());
				if(changes.sendEvent())
					changes.apply();
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(20, new CustomItem(new MaterialData(Material.CHEST), "§rPegar Recompença Automaticamente: " + (quest.isAutoClaiming() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Clique para mudar se a recompença da quest", "§esera dada automaticamente ou ela precisa ser", "§ecoletada manualmente"));
		menu.addMenuClickHandler(20, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				changes.setAutoClaim(!changes.isAutoClaiming());
				if(changes.sendEvent())
					changes.apply();
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(21, new CustomItem(new MaterialData(Material.GRASS), "§7World Blacklist", "", "§e> Click to configure in which Worlds", "&ethis Quest is able to be completed"));
		menu.addMenuClickHandler(21, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openWorldEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(22, new CustomItem(new MaterialData(Material.FIREWORK), "§rMinimal Party Size: " + (quest.getPartySize() < 1 ? "§4Players aren't allowed be in a Party": (quest.getPartySize() == 1 ? ("§ePlayers can but don't have to be in a Party") : ("§aPlayers need to be in a Party of " + quest.getPartySize() + " or more"))), "", "§eChange the min. Amount of Players in", "§ea Party needed to start this Quest", "", "§r1 = §7Players can but don't have to be in a Party", "§r0 = §7Players aren't allowed to be in a Party", "", "§rLeft Click: §e+1", "§rRight Click: §e-1"));
		menu.addMenuClickHandler(22, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				int size = quest.getPartySize();
				if (action.isRightClicked()) size--;
				else size++;
				if (size < 0) size = 0;
				changes.setPartySize(size);
				if(changes.sendEvent())
					changes.apply();
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(26, ItemBuilder.Proto.RED_WOOL.get().display("&4Delete Database").lore(
				"",
				"&rIsso ira deletar todo o banco de dados DESSA QUEST",
				"&rre ira limpar todo os progressos associados a essa QUEST",
				"&rde todos os jogadores.").get());
		menu.addMenuClickHandler(26, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				PlayerManager.clearAllQuestData(quest);
				QuestWorld.getSounds().DestructiveClick().playTo(p);
				return false;
			}
		});
		
		int index = 36;
		for (ItemStack reward: quest.getRewards()) {
			menu.addItem(index, reward);
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
			index++;
		}
		
		for (int i = 0; i < 9; i++) {
			final Mission mission = quest.getMission(i);
			if (mission == null) {
				menu.addItem(45 + i, new CustomItem(new MaterialData(Material.PAPER), "&7&o> Nova Tarefa"));;
				menu.addMenuClickHandler(45 + i, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						changes.addMission(new Mission(quest, String.valueOf(slot - 36), MissionType.valueOf("SUBMIT"), EntityType.PLAYER, "", new ItemStack(Material.STONE), p.getLocation().getBlock().getLocation(), 1, null, 0, false, 0, true, "Hey there! Do this Quest."));
						if(changes.sendEvent())
							changes.apply();
						openQuestEditor(p, quest);
						return false;
					}
				});
			}
			else {
				ItemStack stack = new CustomItem(new MaterialData(Material.BOOK), mission.getText());
				ItemMeta meta = stack.getItemMeta();
				meta.setLore(Arrays.asList("", "§c§oClick-Esquerdo para editar", "§c§oClick-Direito para deletar"));
				stack.setItemMeta(meta);
				menu.addItem(45 + i, stack);
				menu.addMenuClickHandler(45 + i, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						if (action.isRightClicked()) QBDialogue.openDeletionConfirmation(p, mission);
						else openQuestMissionEditor(p, mission);
						return false;
					}
				});
			}
		}
		
		menu.open(p);
	}

	public static void openWorldEditor(Player p, final Quest quest) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c&l<&c<&l< Voltar"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		int index = 9;
		for (final World world: Bukkit.getWorlds()) {
			menu.addItem(index, new CustomItem(new MaterialData(Material.GRASS), "&r" + world.getName() + ": " + (quest.isWorldEnabled(world.getName()) ? "§2§l\u2714": "§4§l\u2718")));
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestChange changes = new QuestChange(quest);
					changes.toggleWorld(world.getName());
					if(changes.sendEvent())
						changes.apply();
					openWorldEditor(p, quest);
					return false;
				}
			});
			index++;
		}
		
		menu.open(p);
	}

	public static void openWorldEditor(Player p, final Category category) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c&l<&c<&l< Voltar"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openCategoryEditor(p, category);
				return false;
			}
		});
		
		int index = 9;
		for (final World world: Bukkit.getWorlds()) {
			menu.addItem(index, new CustomItem(new MaterialData(Material.GRASS), "&r" + world.getName() + ": " + (category.isWorldEnabled(world.getName()) ? "§2§l\u2714": "§4§l\u2718")));
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					CategoryChange changes = new CategoryChange(category);
					changes.toggleWorld(world.getName());
					if(changes.sendEvent())
						changes.apply();
					
					openWorldEditor(p, category);
					return false;
				}
			});
			index++;
		}
		
		menu.open(p);
	}

	public static void openQuestMissionEditor(Player p, final Mission mission) {
		final ChestMenu menu = new ChestMenu("§3§lFinalCraft Quest Editor");
		MissionChange changes = new MissionChange(mission);
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c&l<&c<&l< Voltar"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openQuestEditor(p, mission.getQuest());
				return false;
			}
		});
		
		// Mission types now handle their own menu data!
		mission.getType().buildMenu(changes, menu);
		
		ItemStack missionSelector = new ItemBuilder(mission.getType().getSelectorItem())
				.display("&7" + mission.getType().toString())
				.lore(
						"",
						"&e> Click to change the Mission Type").get();
		
		menu.addItem(9, missionSelector);
		menu.addMenuClickHandler(9, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openMissionSelector(p, changes.getSource());
				return false;
			}
		});
		
		menu.open(p);
	}

	public static void openMissionSelector(Player p, Mission mission) {
		final ChestMenu menu = new ChestMenu(Text.colorize("&3Mission Selector: " + mission.getQuest().getName()));
		MissionChange changes = new MissionChange(mission);
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), QuestWorld.getInstance().getBookLocal("button.back.general")));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
				openQuestMissionEditor(arg0, mission);
				return false;
			}
		});
		
		PagedMapping view = new PagedMapping(45, 9);
		int i = 0;
		for(MissionType type : QuestWorld.getInstance().getMissionTypes().values()) {
			String name = Text.niceName(type.getName());
			view.addItem(i, new ItemBuilder(type.getSelectorItem()).display("&f" + name).get());
			view.addButton(i, MissionButton.simpleHandler(changes, event -> changes.setType(type) ));
			++i;
		}
		view.setBackButton(Buttons.simpleHandler(event -> openQuestMissionEditor(p, mission)));
		view.build(menu, p);
		
		menu.open(p);
	}
}
