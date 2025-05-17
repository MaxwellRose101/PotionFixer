package dev.maxwellrose1019.potionfixer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;

public class Main extends JavaPlugin {

    private ProtocolManager protocolManager;
    private Map<String, PotionInfo> potionData = new HashMap<>();
    private int serverMinorVersion = 8;
    private boolean stripNBT = true;

    @Override
    public void onEnable() {
        getLogger().info("[PotionFixer] Enabling...");

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        stripNBT = config.getBoolean("strip-nbt-data", true);

        detectServerVersion();
        loadPotionData();

        protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket().deepClone();
                ItemStack item = packet.getItemModifier().read(0);
                packet.getItemModifier().write(0, fixPotion(item));
                event.setPacket(packet);
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket().deepClone();
                List<ItemStack> items = packet.getItemListModifier().read(0);
                List<ItemStack> fixedItems = new ArrayList<>();
                for (ItemStack item : items) {
                    fixedItems.add(fixPotion(item));
                }
                packet.getItemListModifier().write(0, fixedItems);
                event.setPacket(packet);
            }
        });

        getLogger().info("[PotionFixer] Enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(this);
        }
        getLogger().info("[PotionFixer] Disabled.");
    }

    private void detectServerVersion() {
        String version = Bukkit.getBukkitVersion().split("-", 2)[0];
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            try {
                serverMinorVersion = Integer.parseInt(parts[1]);
                getLogger().info("[PotionFixer] Detected server minor version: " + serverMinorVersion);
            } catch (NumberFormatException e) {
                getLogger().warning("[PotionFixer] Could not parse server version, defaulting to 8");
            }
        }
    }

    private void loadPotionData() {
        add("swiftness", "Swiftness", "+20% Speed", "3:00", false, 8);
        add("swiftness_upgraded", "Swiftness II", "+40% Speed", "1:30", false, 8);
        add("swiftness_extended", "Swiftness", "+20% Speed", "8:00", false, 8);
        add("slowness", "Slowness", "-15% Speed", "1:30", true, 8);
        add("slowness_extended", "Slowness", "-15% Speed", "4:00", true, 8);
        add("harming", "Harming", "Instant Damage", "", true, 8);
        add("harming_upgraded", "Harming II", "More Instant Damage", "", true, 8);
        add("healing", "Healing", "Instant Health", "", false, 8);
        add("healing_upgraded", "Healing II", "More Instant Health", "", false, 8);
        add("strength", "Strength", "+130% Melee Damage", "3:00", false, 8);
        add("strength_upgraded", "Strength II", "+260% Melee Damage", "1:30", false, 8);
        add("strength_extended", "Strength", "+130% Melee Damage", "8:00", false, 8);
        add("weakness", "Weakness", "-4 Attack Damage", "1:30", true, 8);
        add("weakness_extended", "Weakness", "-4 Attack Damage", "4:00", true, 8);
        add("regeneration", "Regeneration", "+4❤ / 5s", "0:45", false, 8);
        add("regeneration_upgraded", "Regeneration II", "+8❤ / 5s", "0:22", false, 8);
        add("regeneration_extended", "Regeneration", "+4❤ / 5s", "1:30", false, 8);
        add("fire_resistance", "Fire Resistance", "Immunity to fire and lava", "3:00", false, 8);
        add("fire_resistance_extended", "Fire Resistance", "Immunity to fire and lava", "8:00", false, 8);
        add("night_vision", "Night Vision", "Brighter vision", "3:00", false, 8);
        add("night_vision_extended", "Night Vision", "Brighter vision", "8:00", false, 8);
        add("invisibility", "Invisibility", "Invisible to others", "3:00", false, 8);
        add("invisibility_extended", "Invisibility", "Invisible to others", "8:00", false, 8);
        add("poison", "Poison", "Damage over time", "0:45", true, 8);
        add("poison_upgraded", "Poison II", "More damage over time", "0:21", true, 8);
        add("poison_extended", "Poison", "Damage over time", "1:30", true, 8);
        add("luck", "Luck", "Improved loot", "5:00", false, 9);
        add("slow_falling", "Slow Falling", "Slower descent", "1:30", false, 13);
        add("turtle_master", "Turtle Master", "+Resistance -60% Speed", "0:20", false, 13);
        add("water_breathing", "Water Breathing", "Breathe underwater", "3:00", false, 8);
        add("water_breathing_extended", "Water Breathing", "Breathe underwater", "8:00", false, 8);
        add("jump_boost", "Jump Boost", "+50% Jump Height", "3:00", false, 8);
        add("jump_boost_upgraded", "Jump Boost II", "+100% Jump Height", "1:30", false, 8);
        add("jump_boost_extended", "Jump Boost", "+50% Jump Height", "8:00", false, 8);
    }

    private void add(String key, String name, String effect, String duration, boolean bad, int minVersion) {
        potionData.put(key, new PotionInfo(name, effect, duration, bad, minVersion));
    }

    private ItemStack fixPotion(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return item;
        if (!(item.getItemMeta() instanceof PotionMeta)) return item;

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        PotionData base = meta.getBasePotionData();
        String key = base.getType().name().toLowerCase();
        if (base.isUpgraded()) key += "_upgraded";
        else if (base.isExtended()) key += "_extended";

        if (key.contains("awkward") || key.contains("mundane") || key.contains("thick") || key.contains("water"))
            return item;

        PotionInfo info = potionData.get(key);
        if (info == null || serverMinorVersion < info.minVersion) return item;

        ItemStack clone = item.clone();
        PotionMeta cloneMeta = (PotionMeta) clone.getItemMeta();
        if (cloneMeta == null) return item;

        cloneMeta.setDisplayName("\u00a7fPotion of " + info.name);

        List<String> lore = new ArrayList<>();
        if (!info.duration.isEmpty()) {
            lore.add((info.bad ? "\u00a7c" : "\u00a79") + info.name + " (" + info.duration + ")");
        } else {
            lore.add((info.bad ? "\u00a7c" : "\u00a79") + info.name);
        }
        lore.add(""); // Blank line
        lore.add("\u00a75When Applied:");
        lore.add((info.bad ? "\u00a7c" : "\u00a79") + info.effect);
        cloneMeta.setLore(lore);

        if (stripNBT) {
            cloneMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }

        clone.setItemMeta(cloneMeta);
        return clone;
    }

    static class PotionInfo {
        String name, effect, duration;
        boolean bad;
        int minVersion;

        PotionInfo(String name, String effect, String duration, boolean bad, int minVersion) {
            this.name = name;
            this.effect = effect;
            this.duration = duration;
            this.bad = bad;
            this.minVersion = minVersion;
        }
    }
}
