package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Handles simple civilian tasks (lanterns, well, bells).
 */
public class TaskManager {
    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;

    private final Set<Location> ritualLanterns = new HashSet<>();
    private final Set<Location> activatedLanterns = new HashSet<>();

    private final Set<Location> signalBells = new HashSet<>();
    private Location wellLocation;

    private final NamespacedKey livingWaterKey;
    private final Random random = new Random();

    public TaskManager(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.livingWaterKey = new NamespacedKey(plugin, "living_water");
    }

    public void loadFromConfig(FileConfiguration config) {
        ritualLanterns.clear();
        activatedLanterns.clear();
        signalBells.clear();
        wellLocation = null;

        loadLocations(config.getConfigurationSection("ritualLanterns"), ritualLanterns);
        loadLocations(config.getConfigurationSection("signalBells"), signalBells);
        ConfigurationSection wellSection = config.getConfigurationSection("wellLocation");
        if (wellSection != null) {
            wellLocation = parseLocation(wellSection);
        }
    }

    private void loadLocations(ConfigurationSection section, Set<Location> target) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            Location loc = parseLocation(entry);
            if (loc != null) {
                target.add(loc);
            }
        }
    }

    private Location parseLocation(ConfigurationSection section) {
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (world == null || plugin.getServer().getWorld(world) == null) {
            return null;
        }
        return new Location(plugin.getServer().getWorld(world), x, y, z);
    }

    public boolean handleLanternInteract(Player player, Location clicked) {
        for (Location lantern : ritualLanterns) {
            if (lantern.equals(clicked)) {
                if (activatedLanterns.contains(lantern)) {
                    player.sendMessage(Component.text("This ritual lantern is already burning.", NamedTextColor.YELLOW));
                } else {
                    activatedLanterns.add(lantern);
                    player.sendMessage(Component.text("You ignite a ritual lantern.", NamedTextColor.GOLD));
                    updateManiacBlindness();
                }
                return true;
            }
        }
        return false;
    }

    private void updateManiacBlindness() {
        boolean allActive = !ritualLanterns.isEmpty() && activatedLanterns.containsAll(ritualLanterns);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (roleManager.isManiac(player)) {
                if (allActive) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, true, false));
                } else {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                }
            }
        }
    }

    public boolean handleBellInteract(Player player, Location clicked) {
        for (Location bell : signalBells) {
            if (bell.equals(clicked)) {
                giveBellHint(player);
                return true;
            }
        }
        return false;
    }

    private void giveBellHint(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Signal Note");
        meta.setAuthor("Town Watch");
        List<Component> pages = new ArrayList<>();
        pages.add(Component.text(randomHint()));
        meta.pages(pages);
        book.setItemMeta(meta);
        player.getInventory().addItem(book);
        player.sendMessage(Component.text("You receive a hastily scribbled note from the bell.", NamedTextColor.AQUA));
    }

    private String randomHint() {
        String[] hints = new String[] {
                "The Maniac was seen in the north area.",
                "A marked player was near the center recently.",
                "Someone carrying Living Water headed east.",
                "Lanterns burn brighter when friends are close.",
                "Bells echo louder when danger is near."
        };
        return hints[random.nextInt(hints.length)];
    }

    public boolean handleWellInteract(Player player, Location clicked) {
        if (wellLocation == null) {
            return false;
        }
        if (wellLocation.equals(clicked)) {
            giveLivingWater(player);
            return true;
        }
        return false;
    }

    private void giveLivingWater(Player player) {
        ItemStack water = new ItemStack(Material.POTION, 1);
        ItemMeta meta = water.getItemMeta();
        meta.displayName(Component.text("Living Water", NamedTextColor.AQUA));
        meta.lore(List.of(Component.text("Use to cleanse normal marks.", NamedTextColor.GRAY)));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(livingWaterKey, PersistentDataType.STRING, "living_water");
        water.setItemMeta(meta);
        player.getInventory().addItem(water);
        player.sendMessage(Component.text("You draw Living Water from the well.", NamedTextColor.AQUA));
    }

    public boolean tryConsumeLivingWater(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(livingWaterKey, PersistentDataType.STRING)) {
            return false;
        }
        markManager.clearNormalMarks(player);
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(Component.text("Your normal marks are washed away.", NamedTextColor.GREEN));
        return true;
    }

    public void resetLanterns() {
        activatedLanterns.clear();
        updateManiacBlindness();
    }

    public void giveStartingSigns(Player player) {
        ItemStack signs = new ItemStack(Material.OAK_SIGN, 6);
        player.getInventory().addItem(signs);
    }
}
