package me.natnm.dispenserfill;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class DispenserFillCommand implements CommandExecutor {
    Plugin plugin;

    private final HashMap<UUID, Long> cooldowns = new HashMap<>(); // cooldown map
    private final int COOLDOWN_TIME = 5; // Cooldown time in seconds

    /**
     * Constructs a new DispenserFillCommand instance.
     *
     * @param plugin The plugin instance.
     */
    public DispenserFillCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    /**
     * Executes the /filldispensers command, filling nearby dispensers with the held item.
     *
     * @param sender  The command's sender.
     * @param command The command that was executed.
     * @param label   The alias of the command that was used.
     * @param args    The command arguments.
     * @return true if the command was processed successfully, otherwise false.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // verify that the player has permission to use the command
        if (!player.hasPermission("dispenserfill.fill")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        // Cooldown check
        if(cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = ((cooldowns.get(player.getUniqueId())/1000)+COOLDOWN_TIME) - (System.currentTimeMillis()/1000);
            if(timeLeft > 0) {
                // Player is still on cooldown
                player.sendMessage("You must wait " + timeLeft + " seconds to use this command again.");
                return true;
            }
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage("You must be holding an item to fill the dispensers.");
            return true;
        }

        int radius = 5; // this will change if an argument for radius is provided
        if (args.length > 0) {
            try {
                radius = Integer.parseInt(args[0]);

                if(radius < 0) {
                    player.sendMessage("Invalid radius specified. Please enter a valid number.");
                    return true;
                }
                if(radius > 40) {
                    player.sendMessage("Radius is too large. Using the maximum radius of 40 blocks.");
                    radius = 40;
                }

                // Do the dispenser fill operations Asynchronously so the server doesn't crash
                scheduleDispenserFill(player, heldItem, radius);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid radius specified. Please enter a valid number.");
            }
        }else {
            player.sendMessage("No radius specified. Using default radius of 5 blocks.");

            // Do the dispenser fill operations Asynchronously so the server doesn't crash
            scheduleDispenserFill(player, heldItem, radius);

        }

        return true;
    }
    /**
     * starts the dispenser filling process on the main server thread so the server doesn't crash.
     *
     * @param player    The player executing the command.
     * @param heldItem  The item in the player's main hand.
     * @param radius    The radius within which to fill dispensers.
     */
    private void scheduleDispenserFill(Player player, ItemStack heldItem, int radius) {
        new BukkitRunnable() {
            public void run() {
                fillDispensers(player, heldItem, radius);
            }
        }.runTask(plugin);

        // After command logic, update the player's cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    /**
     * Fills all dispensers within a specified radius of the player with the held item.
     * This method is called on the main server thread to ensure thread safety.
     *
     * @param player    The player executing the command.
     * @param heldItem  The item to fill the dispensers with.
     * @param radius    The radius within which to fill dispensers.
     */
    private void fillDispensers(Player player, ItemStack heldItem, int radius) {
        Location playerLoc = player.getLocation();
        int startX = playerLoc.getBlockX() - radius;
        int startY = playerLoc.getBlockY() - radius;
        int startZ = playerLoc.getBlockZ() - radius;
        int endX = playerLoc.getBlockX() + radius;
        int endY = playerLoc.getBlockY() + radius;
        int endZ = playerLoc.getBlockZ() + radius;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    if (block.getState() instanceof Dispenser) {
                        Dispenser dispenser = (Dispenser) block.getState();
                        Inventory dispenserInv = dispenser.getInventory();

                        dispenserInv.clear(); // Clear the inventory before filling it

                        // Clone the held item to preserve its data
                        ItemStack itemToFill = heldItem.clone();

                        // Calculate the max stack size for the held item (e.g. 64 for most items, but not for all items)
                        int maxStackSize = itemToFill.getMaxStackSize();

                        // Set the amount of the item's clone to the max stack size
                        itemToFill.setAmount(maxStackSize);

                        // Fill all slots in the dispenser with the max stack size of the held item
                        for (int slot = 0; slot < dispenserInv.getSize(); slot++) {
                            dispenserInv.setItem(slot, itemToFill);
                        }
                    }
                }
            }
        }

        player.sendMessage("Filled all dispensers within " + radius + " blocks.");
    }
}
