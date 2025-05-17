package dev.maxwellrose1019.potionfixer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private ProtocolManager protocolManager;
    private Map<String, PotionInfo> potionData = new HashMap<>();
    private int serverMinorVersion = 8;
    private boolean stripNBT = true;
    private boolean bruteforceSync = false;
    private boolean debug = false;

    @Override
    public void onEnable() {
        getLogger().info("[PotionFixer] Enabling...");

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        stripNBT = config.getBoolean("strip-nbt-data", true);
        bruteforceSync = config.getBoolean("bruteforce-sync", false);
        debug = config.getBoolean("debug", false);

        detectServerVersion();
        loadPotionData();

        protocolManager = ProtocolLibrary.getProtocolManager();

        Bukkit.getPluginManager().registerEvents(this, this);

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                ItemStack item = event.getPacket().getItemModifier().read(0);
                if (debug) getLogger().info("[PotionFixer] Intercepted SET_SLOT: " + item);
                ItemStack fixed = fixPotion(item);
                event.getPacket().getItemModifier().write(0, fixed);
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                List<ItemStack> items = event.getPacket().getItemListModifier().read(0);
                List<ItemStack> fixedItems = new ArrayList<>();
                for (ItemStack item : items) {
                    if (debug) getLogger().info("[PotionFixer] Intercepted WINDOW_ITEMS: " + item);
                    fixedItems.add(fixPotion(item));
                }
                event.getPacket().getItemListModifier().write(0, fixedItems);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("testpotion")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            ItemStack hand = player.getInventory().getItemInMainHand();
            ItemStack fixed = fixPotion(hand);
            player.getInventory().setItemInMainHand(fixed);
            player.sendMessage("§aPotion fixed and applied to your hand.");
            return true;
        }
        return false;
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
        // You can populate potionData here
    }

    private ItemStack fixPotion(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return item;

        if (serverMinorVersion <= 8) {
            Potion potion = Potion.fromItemStack(item);
            item = potion.toItemStack(1);
            if (debug) getLogger().info("[PotionFixer] Pre-1.9 potion parsed and returned.");
            return item;
        }

        if (!(item.getItemMeta() instanceof PotionMeta)) return item;

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) return item;

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

        String prefix;
        switch (item.getType()) {
            case SPLASH_POTION:
                prefix = "Splash Potion of ";
                break;
            case LINGERING_POTION:
                prefix = "Lingering Potion of ";
                break;
            default:
                prefix = "Potion of ";
                break;
        }

        cloneMeta.setDisplayName("§f" + prefix + info.name);

        List<String> lore = new ArrayList<>();
        if (!info.duration.isEmpty()) {
            lore.add((info.bad ? "§c" : "§9") + info.name + " (" + info.duration + ")");
        } else {
            lore.add((info.bad ? "§c" : "§9") + info.name);
        }
        lore.add("");
        lore.add("§5When Applied:");
        lore.add((info.bad ? "§c" : "§9") + info.effect);

        if (debug) {
            lore.add("§8§otype:" + item.getType().name().toLowerCase());
        }

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
