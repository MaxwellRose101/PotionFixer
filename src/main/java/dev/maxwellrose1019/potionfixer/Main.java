package dev.maxwellrose1019.potionfixer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JavaPlugin {

    private ProtocolManager protocolManager;
    private String potionNameColor;
    private String potionLoreColor;

    private static final Map<PotionType, PotionInfo> potionDurations = new HashMap<>();

    static {
        potionDurations.put(PotionType.SPEED, new PotionInfo(3600, 9600, 1800));
        potionDurations.put(PotionType.SLOWNESS, new PotionInfo(1800, 4800, 400));
        potionDurations.put(PotionType.INSTANT_HEAL, new PotionInfo(1, -1, 1));
        potionDurations.put(PotionType.INSTANT_DAMAGE, new PotionInfo(1, -1, 1));
        potionDurations.put(PotionType.POISON, new PotionInfo(900, 1800, 440));
        potionDurations.put(PotionType.REGEN, new PotionInfo(900, 1800, 440));
        potionDurations.put(PotionType.STRENGTH, new PotionInfo(3600, 9600, 1800));
        potionDurations.put(PotionType.WEAKNESS, new PotionInfo(1800, 4800, -1));
        potionDurations.put(PotionType.LUCK, new PotionInfo(6000, -1, -1));
        potionDurations.put(PotionType.SLOW_FALLING, new PotionInfo(1800, 4800, -1));
        potionDurations.put(PotionType.JUMP, new PotionInfo(3600, 9600, 1800));
        potionDurations.put(PotionType.FIRE_RESISTANCE, new PotionInfo(3600, 9600, -1));
        potionDurations.put(PotionType.NIGHT_VISION, new PotionInfo(3600, 9600, -1));
        potionDurations.put(PotionType.INVISIBILITY, new PotionInfo(3600, 9600, -1));
        potionDurations.put(PotionType.TURTLE_MASTER, new PotionInfo(400, 800, 200));
        potionDurations.put(PotionType.WATER_BREATHING, new PotionInfo(3600, 9600, -1));
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        potionNameColor = parseColor(config.getString("potion-name-color", "&7"));
        potionLoreColor = parseColor(config.getString("potion-lore-color", "&7"));

        protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket().deepClone();
                ItemStack item = packet.getItemModifier().read(0);
                ItemStack fixed = fixPotion(item);
                packet.getItemModifier().write(0, fixed);
                event.setPacket(packet);
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket().deepClone();
                List<ItemStack> items = packet.getItemListModifier().read(0);
                List<ItemStack> fixedList = new ArrayList<>();

                for (ItemStack item : items) {
                    fixedList.add(fixPotion(item));
                }

                packet.getItemListModifier().write(0, fixedList);
                event.setPacket(packet);
            }
        });
    }

    private ItemStack fixPotion(ItemStack item) {
        if (item == null || (!item.getType().toString().endsWith("POTION"))) return item;
        if (!(item.getItemMeta() instanceof PotionMeta)) return item;

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (meta.getBasePotionData() != null) {
            PotionType type = meta.getBasePotionData().getType();
            boolean extended = meta.getBasePotionData().isExtended();
            boolean upgraded = meta.getBasePotionData().isUpgraded();

            PotionEffectType effectType = type.getEffectType();
            if (effectType != null) {
                int amplifier = upgraded ? 1 : 0;
                PotionInfo info = potionDurations.get(type);
                int duration = info == null ? 0 : (upgraded ? info.upgraded : extended ? info.extended : info.normal);

                if (duration > 0) {
                    lore.add(potionLoreColor + effectType.getName().replace("_", " ") + " " + (amplifier + 1));
                    lore.add(potionLoreColor + "Duration: " + formatDuration(duration));
                }
            }
        }

        for (PotionEffect effect : meta.getCustomEffects()) {
            lore.add(potionLoreColor + effect.getType().getName().replace("_", " ") + " " + (effect.getAmplifier() + 1));
            lore.add(potionLoreColor + "Duration: " + formatDuration(effect.getDuration()));
        }

        meta.setDisplayName(potionNameColor + "Potion");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private String parseColor(String code) {
        if (code == null) return ChatColor.RESET.toString();

        code = code.replace("§", "&");

        if (code.matches("&[0-9a-fk-or]")) {
            return ChatColor.translateAlternateColorCodes('&', code);
        }

        Matcher hex = Pattern.compile("#([A-Fa-f0-9]{6})").matcher(code);
        if (hex.find()) {
            return net.md_5.bungee.api.ChatColor.of(hex.group()).toString();
        }

        return ChatColor.RESET.toString();
    }

    private static class PotionInfo {
        int normal;
        int extended;
        int upgraded;

        public PotionInfo(int normal, int extended, int upgraded) {
            this.normal = normal;
            this.extended = extended;
            this.upgraded = upgraded;
        }
    }
}
