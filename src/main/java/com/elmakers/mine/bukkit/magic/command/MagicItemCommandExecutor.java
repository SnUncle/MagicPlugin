package com.elmakers.mine.bukkit.magic.command;

import com.elmakers.mine.bukkit.api.item.ItemData;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.integration.VaultController;
import com.elmakers.mine.bukkit.utility.InventoryUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MagicItemCommandExecutor extends MagicTabExecutor {
	
	public MagicItemCommandExecutor(MagicAPI api) {
		super(api);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("delete"))
		{
			if (!api.hasPermission(sender, "Magic.commands.item.delete")) {
				sendNoPermission(sender);
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage("Usage: /mitem delete <itemkey>");
				return true;
			}
			onItemDelete(sender, args[1]);
			return true;
		}

		// Everything beyond this point is is-game only and requires a sub-command
		if (args.length == 0) {
			return false;
		}
		if (!(sender instanceof Player)) {
			return false;
		}

		// All commands past here also require an item being held
		Player player = (Player)sender;
		if (!checkItem(player)) {
			return true;
		}
		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		String subCommand = args[0];
		String[] args2 = Arrays.copyOfRange(args, 1, args.length);
		return processItemCommand(player, itemInHand, subCommand, args2);
	}

	@Override
	public Collection<String> onTabComplete(CommandSender sender, String commandName, String[] args) {
		List<String> options = new ArrayList<String>();

		if (args.length == 1) {
			addIfPermissible(sender, options, "Magic.commands.mitem.", "add");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "remove");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "name");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "describe");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "duplicate");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "save");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "delete");
			addIfPermissible(sender, options, "Magic.commands.mitem.", "worth");
		}

		if (args.length == 2) {
			String subCommand = args[0];
			String subCommandPNode = "Magic.commands.mitem." + subCommand;

			if (!api.hasPermission(sender, subCommandPNode)) {
				return options;
			}
			
			if (subCommand.equalsIgnoreCase("add")) {
				options.add("enchant");
				options.add("attribute");
				options.add("lore");
				options.add("flag");
				options.add("unbreakable");
			}

			if (subCommand.equalsIgnoreCase("remove")) {
				options.add("enchant");
				options.add("attribute");
				options.add("lore");
				options.add("flag");
				options.add("unbreakable");
			}

			if (subCommand.equalsIgnoreCase("delete")) {
				File itemFolder = new File(api.getController().getConfigFolder(), "items");
				if (itemFolder.exists()) {
					File[] files = itemFolder.listFiles();
					for (File file : files) {
						if (file.getName().endsWith(".yml")) {
							options.add(file.getName().replace(".yml", ""));
						}
					}
				}
			}
		}

		if (args.length == 3)
		{
			String subCommand = args[0];
			String subCommand2 = args[1];

			String commandPNode = "Magic.commands.mitem." + subCommand;

			if (!api.hasPermission(sender, commandPNode)) {
				return options;
			}
			String subCommandPNode = "Magic.commands.mitem." + subCommand + "." + subCommand2;
			if (!api.hasPermission(sender, subCommandPNode)) {
				return options;
			}

			boolean isAddRemove = subCommand.equalsIgnoreCase("remove") || subCommand.equalsIgnoreCase("add");
			if (isAddRemove && subCommand2.equalsIgnoreCase("enchant")) {
				for (Enchantment enchantment : Enchantment.values()) {
					options.add(enchantment.getName().toLowerCase());
				}
			}
			if (isAddRemove && subCommand2.equalsIgnoreCase("attribute")) {
				for (Attribute attribute : Attribute.values()) {
					options.add(attribute.name().toLowerCase());
				}
			}
			if (isAddRemove && subCommand2.equalsIgnoreCase("flag")) {
				for (ItemFlag flag : ItemFlag.values()) {
					options.add(flag.name().toLowerCase());
				}
			}
			if (subCommand.equalsIgnoreCase("remove") && subCommand2.equalsIgnoreCase("lore")) {
				options.add("1");
				options.add("2");
				options.add("3");
			}
		}

		return options;
	}

	protected boolean processItemCommand(Player player, ItemStack item, String subCommand, String[] args)
	{
		if (!api.hasPermission(player, "Magic.commands.mitem." + subCommand)) {
			sendNoPermission(player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("add"))
		{
			return onItemAdd(player, item, args);
		}
		if (subCommand.equalsIgnoreCase("remove"))
		{
			return onItemRemove(player, item, args);
		}
		if (subCommand.equalsIgnoreCase("worth"))
		{
			return onItemWorth(player, item);
		}
		if (subCommand.equalsIgnoreCase("duplicate"))
		{
			return onItemDuplicate(player, item);
		}
		if (subCommand.equalsIgnoreCase("save"))
		{
			return onItemSave(player, item, args);
		}
		if (subCommand.equalsIgnoreCase("describe"))
		{
			return onItemDescribe(player, item);
		}
		if (subCommand.equalsIgnoreCase("name"))
		{
			return onItemName(player, item, args);
		}
		
		return false;
	}

	public boolean onItemDescribe(Player player, ItemStack item) {
		YamlConfiguration configuration = new YamlConfiguration();
		configuration.set("item", item);
		String itemString = configuration.saveToString().replace("item:", "").replace(ChatColor.COLOR_CHAR, '&');
		player.sendMessage(itemString);
		return true;
	}

	public boolean onItemWorth(Player player, ItemStack item) {
		MageController controller = api.getController();
		Double worth = controller.getWorth(item);
		if (worth == null) {
			player.sendMessage(ChatColor.RED + "No worth defined for that item");
			return true;
		}
		String worthDescription = null;
		VaultController vault = VaultController.getInstance();
		int amount = item.getAmount();
		double totalWorth = worth * amount;
		if (vault.hasEconomy()) {
			worthDescription = vault.format(totalWorth);
			if (amount > 1) {
				worthDescription = worthDescription + ChatColor.WHITE + 
					" (" + ChatColor.GOLD + vault.format(worth) + ChatColor.WHITE + " each)";
			}
		} else {
			worthDescription = Double.toString(totalWorth);
			if (amount > 1) {
				worthDescription = worthDescription + ChatColor.WHITE +
					" (" + ChatColor.GOLD + Double.toString(worth) + ChatColor.WHITE + " each)";
			}
		}
		 
		player.sendMessage("That item is worth " + ChatColor.GOLD + worthDescription);
		return true;
	}
	
	public boolean onItemDuplicate(Player player, ItemStack item)
	{
		ItemStack newItem = InventoryUtils.getCopy(item);
		api.giveItemToPlayer(player, newItem);

		player.sendMessage(api.getMessages().get("item.duplicated"));
		return true;
	}

	protected boolean checkItem(Player player)
	{
		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		if (itemInHand == null || itemInHand.getType() == Material.AIR) {
			player.sendMessage(api.getMessages().get("item.no_item"));
			return false;
		}
		return true;
	}
	
	public boolean onItemDelete(CommandSender sender, String itemKey) {
		MageController controller = api.getController();
		ItemData existing = controller.getItem(itemKey);
		if (existing == null) {
			sender.sendMessage(ChatColor.RED + "Unknown item: " + itemKey);
			return true;
		}
		boolean hasPermission = true;
		if (sender instanceof Player) {
			Player player = (Player)sender;
			if (!player.hasPermission("Magic.item.overwrite")) {
				if (player.hasPermission("Magic.item.overwrite_own")) {
					String creatorId = existing.getCreatorId();
					hasPermission = creatorId != null && creatorId.equalsIgnoreCase(player.getUniqueId().toString());
				} else {
					hasPermission = false;
				}
			}
		}
		if (!hasPermission) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to delete " + itemKey);
			return true;
		}

		File itemFolder = new File(controller.getConfigFolder(), "items");
		File itemFile = new File(itemFolder, itemKey + ".yml");
		if (!itemFile.exists()) {
			sender.sendMessage(ChatColor.RED + "File doesn't exist: " + itemFile.getName());
			return true;
		}
		itemFile.delete();
		controller.unloadItemTemplate(itemKey);
		sender.sendMessage("Deleted item " + itemKey);
		return true;
	}

	public boolean onItemSave(Player player, ItemStack item, String[] parameters)
	{
		if (parameters.length < 1) {
			player.sendMessage("Use: /mitem save <filename> [worth]");
			return true;
		}

		MageController controller = api.getController();
		String template = parameters[0];
		ItemData existing = controller.getItem(template);
		if (existing != null && !player.hasPermission("Magic.item.overwrite")) {
			String creatorId = existing.getCreatorId();
			boolean isCreator = creatorId != null && creatorId.equalsIgnoreCase(player.getUniqueId().toString());
			if (!player.hasPermission("Magic.item.overwrite_own") || !isCreator) {
				player.sendMessage(ChatColor.RED + "The " + template + " item already exists and you don't have permission to overwrite it.");
				return true;
			}
		}

		double worth = 0;
		if (parameters.length > 1) {
			try {
				worth = Double.parseDouble(parameters[1]);
			} catch (Exception ex) {
				player.sendMessage("Use: /mitem save <filename> [worth]");
				return true;
			}
		} else if (existing != null) {
			worth = existing.getWorth();
		}

		YamlConfiguration itemConfig = new YamlConfiguration();
		ConfigurationSection itemSection = itemConfig.createSection(template);
		itemSection.set("creator_id", player.getUniqueId().toString());
		itemSection.set("creator", player.getName());
		itemSection.set("worth", worth);
		itemSection.set("item", item);

		File itemFolder = new File(controller.getConfigFolder(), "items");
		File itemFile = new File(itemFolder, template + ".yml");
		itemFolder.mkdirs();
		try {
			itemConfig.save(itemFile);
		} catch (IOException ex) {
			ex.printStackTrace();
			player.sendMessage(ChatColor.RED + "Can't write to file " + itemFile.getName());
			return true;
		}
		controller.loadItemTemplate(template, itemSection);
		player.sendMessage(ChatColor.WHITE + "Item saved as " + ChatColor.GOLD + template + " worth " + ChatColor.GREEN + worth);
		if (existing != null) {
			player.sendMessage(ChatColor.YELLOW + " Replaced Worth " + ChatColor.DARK_GREEN + existing.getWorth());
		}
		return true;
	}

	public boolean onItemName(Player player, ItemStack item, String[] parameters)
	{
		String displayName = null;
		if (parameters.length < 1) {
			player.sendMessage(api.getMessages().get("item.rename_clear"));
		} else {
			displayName = ChatColor.translateAlternateColorCodes('&', StringUtils.join(parameters, " "));
			player.sendMessage(api.getMessages().get("item.renamed"));
		}
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(displayName);
		item.setItemMeta(meta);
		return true;
	}

	public boolean onItemAddFlag(Player player, ItemStack item, String flagName)
	{
		ItemFlag flag = null;
		try {
			flag = ItemFlag.valueOf(flagName.toUpperCase());
		} catch (Exception ex) {
			player.sendMessage(ChatColor.RED + "Invalid flag: " + ChatColor.WHITE + flagName);
			return true;
		}
		
		ItemMeta itemMeta = item.getItemMeta();
		itemMeta.addItemFlags(flag);
		item.setItemMeta(itemMeta);

		player.sendMessage(api.getMessages().get("item.flag_added").replace("$flag", flag.name()));
		
		return true;
	}

	public boolean onItemRemoveFlag(Player player, ItemStack item, String flagName)
	{
		ItemFlag flag = null;
		ItemMeta itemMeta = item.getItemMeta();
		if (flagName == null) {
			Set<ItemFlag> flags = itemMeta.getItemFlags();
			if (flags == null || flags.size() == 0) {
				player.sendMessage(api.getMessages().get("item.no_flags"));
				return true;
			}
			flag = flags.iterator().next();
		} else {
			try {
				flag = ItemFlag.valueOf(flagName.toUpperCase());
			} catch (Exception ex) {
				player.sendMessage(ChatColor.RED + "Invalid flag: " + ChatColor.WHITE + flagName);
				return true;
			}
		}

		if (!itemMeta.hasItemFlag(flag)) {
			player.sendMessage(api.getMessages().get("item.no_flag").replace("$flag", flag.name()));
		} else {
			itemMeta.removeItemFlags(flag);
			item.setItemMeta(itemMeta);
			player.sendMessage(api.getMessages().get("item.flag_removed").replace("$flag", flag.name()));
		}

		return true;
	}

	public boolean onItemAddUnbreakable(Player player, ItemStack item)
	{
		if (InventoryUtils.isUnbreakable(item)) {
			player.sendMessage(api.getMessages().get("item.already_unbreakable"));
		} else {
			InventoryUtils.makeUnbreakable(item);
			player.sendMessage(api.getMessages().get("item.add_unbreakable"));
		}

		return true;
	}

	public boolean onItemRemoveUnbreakable(Player player, ItemStack item)
	{
		if (!InventoryUtils.isUnbreakable(item)) {
			player.sendMessage(api.getMessages().get("item.not_unbreakable"));
		} else {
			InventoryUtils.removeUnbreakable(item);
			player.sendMessage(api.getMessages().get("item.remove_unbreakable"));
		}

		return true;
	}

	public boolean onItemAddLore(Player player, ItemStack item, String loreLine)
	{
		ItemMeta itemMeta = item.getItemMeta();
		List<String> lore = itemMeta.getLore();
		if (lore == null) {
			lore = new ArrayList<String>();
		}
		lore.add(loreLine);
		itemMeta.setLore(lore);
		item.setItemMeta(itemMeta);

		player.sendMessage(api.getMessages().get("item.lore_added").replace("$lore", loreLine));
		return true;
	}

	public boolean onItemRemoveLore(Player player, ItemStack item, String loreIndex)
	{
		ItemMeta itemMeta = item.getItemMeta();
		List<String> lore = itemMeta.getLore();
		if (lore == null || lore.isEmpty()) {
			player.sendMessage(api.getMessages().get("item.no_lore"));
			return true;
		}

		int index = 0;
		if (loreIndex != null) {
			try {
				index = Integer.parseInt(loreIndex);
			} catch (Exception ex) {
				player.sendMessage(ChatColor.RED + "Invalid lore line: " + loreIndex);
				return true;
			}
		}

		if (index < 0 || index >= lore.size()) {
			player.sendMessage(ChatColor.RED + "Invalid lore line: " + loreIndex);
			return true;
		}
		
		String line = lore.remove(index);
		itemMeta.setLore(lore);
		item.setItemMeta(itemMeta);
		player.sendMessage(api.getMessages().get("item.lore_removed").replace("$lore", line));
		return true;
	}
	
	public boolean onItemAdd(Player player, ItemStack item, String[] parameters)
	{
		if (parameters.length < 1) {
			return false;
		}
		String addCommand = parameters[0];
		if (addCommand.equalsIgnoreCase("unbreakable")) {
			return onItemAddUnbreakable(player, item);
		}
		if (parameters.length < 2) {
			return false;
		}
		if (addCommand.equalsIgnoreCase("flag")) {
			return onItemAddFlag(player, item, parameters[1]);
		}

		if (addCommand.equalsIgnoreCase("lore")) {
			String[] loreLines = Arrays.copyOfRange(parameters, 1, parameters.length);
			String loreLine = ChatColor.translateAlternateColorCodes('&', StringUtils.join(loreLines, " "));
			return onItemAddLore(player, item, loreLine);
		}
		if (parameters.length < 3) {
			return false;
		}
		if (addCommand.equalsIgnoreCase("enchant")) {
			player.sendMessage(ChatColor.RED + "Not yet implemented!");
			return true;
		}
		if (addCommand.equalsIgnoreCase("attribute")) {
			player.sendMessage(ChatColor.RED + "Not yet implemented!");
			return true;
		}
		return false;
	}

	public boolean onItemRemove(Player player, ItemStack item, String[] parameters)
	{
		if (parameters.length < 1) {
			return false;
		}
		String removeCommand = parameters[0];

		if (removeCommand.equalsIgnoreCase("unbreakable")) {
			return onItemRemoveUnbreakable(player, item);
		}
		
		String firstParameter = parameters.length > 1 ? parameters[1] : null;
		if (removeCommand.equalsIgnoreCase("flag")) {
			return onItemRemoveFlag(player, item, firstParameter);
		}
		if (removeCommand.equalsIgnoreCase("lore")) {
			return onItemRemoveLore(player, item, firstParameter);
		}
		if (removeCommand.equalsIgnoreCase("enchant")) {
			player.sendMessage(ChatColor.RED + "Not yet implemented!");
			return true;
		}
		if (removeCommand.equalsIgnoreCase("attribute")) {
			player.sendMessage(ChatColor.RED + "Not yet implemented!");
			return true;
		}
		return false;
	}
}