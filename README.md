# ☄️ PotionFixer

![Status](https://img.shields.io/badge/status-beta-red?style=for-the-badge)
![Last Release](https://img.shields.io/badge/release-1.1.2-blue?style=for-the-badge)
![Stability](https://img.shields.io/badge/stability-stable-orange?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-brightgreen?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.8.8--1.20+-blue?style=for-the-badge)
![ProtocolLib](https://img.shields.io/badge/Requires-ProtocolLib-yellow?style=for-the-badge)

A Minecraft plugin to **fix potions that glitch or revert**, especially in **1.8.8 servers and Eaglercraft**. It preserves potion formatting, names, and tooltips when Minecraft tries to reset them. Supports splash, lingering, and regular potions!

---

## 🧪 What it Does

- Intercepts potion items from being reverted (e.g., splash → drinkable).
- Restores proper **name, duration, and effect formatting**.
- Preserves Minecraft’s tooltip style with accurate lore.
- Supports Minecraft versions **1.8.8 to 1.20+**.
- Supports **Eaglercraft 1.8.8** (experimental).
- Adds **debug logging** and a special **gray lore tag** when `debug: true` in `config.yml`.

---

## 🔧 Configuration

```yaml
# - - - - - - - - - - - - - - - - - - - - - - - - - - #
# ______     _   _            ______ _                #
# | ___ \   | | (_)           |  ___(_)               #
# | |_/ /__ | |_ _  ___  _ __ | |_   ___  _____ _ __  #
# |  __/ _ \| __| |/ _ \| '_ \|  _| | \ \/ / _ \ '__| #
# | | | (_) | |_| | (_) | | | | |   | |>  <  __/ |    #
# \_|  \___/ \__|_|\___/|_| |_\_|   |_/_/\_\___|_|    #
#                                                     #
# - - - - - - - - - - - - - - - - - - - - - - - - - - #


# Enable debug mode? Debug mode logs (in console) what is actively happening with the plugin.
# WARNING: This can cause a lot of spam in the console, especially if you have a lot of players. Should only be used for debugging purposes.
#               default settings:
#                   debug: false
debug: false

# Strip NBT data? Clears the "No effects" and other NBT data for cleaner potion displays.
# WARNING: This has not been tested with all server versions yet.
#               default settings:
#                   strip-nbt-data: true
strip-nbt-data: true

# Attempt to force sync with server? Attempts to fix potion types.
# WARNING: This has not been tested with all server versions yet.
#               default settings:
#                   bruteforce-sync: true
bruteforce-sync: true
```

| Option            | Description                                                                 |
|-------------------|-----------------------------------------------------------------------------|
| `debug`           | Enables verbose logging and adds a small gray lore tag at the bottom.       |
| `strip-nbt-data`  | Hides original potion effects from tooltip (removes default effect text).   |
| `bruteforce-sync` | Adds extra lore line with potion type to help force syncing.                |

---

## ⚙️ Compatibility

| Feature         | Supported                        |
|----------------|----------------------------------|
| Minecraft       | ✅ 1.8.8 – 1.20+                  |
| Eaglercraft     | ⚠️ Partial (1.8.8-based only)     |
| Bukkit / Spigot | ✅ Yes                           |
| Paper           | ✅ Yes                           |
| ProtocolLib     | ✅ Required                      |

> ⚠️ You must have ProtocolLib installed or this plugin will not function!

---

## 🪲 Known Bugs

- In Minecraft 1.8.8 and Eaglercraft, splash potions may still revert visually to drinkables.
- Lore and effects can visually desync on some forks or modded clients.
- Pre-1.9 potion items don’t retain metadata properly — best effort patching is used.
- Inventory movements (especially in creative) may trigger unwanted resets.

---

## 📅 Roadmap

- ✅ Proper potion formatting on item display.
- ✅ Splash & lingering potion support.
- 🛠️ Custom potion detection by effect (NBT parsing).
- 🛠️ Fix creative menu potion override glitch.
- 🧪 Cross-version validation and regression test suite.
- 🧪 Support for custom potion recipes (future update).
- 📈 Optional database for potion log analysis (debug mode).

---

## 📫 Contact & Support

Need help or want to report an issue?

💬 DM me on Discord: **skonathan**

I’m always open to testing feedback, bug reports, and contribution offers.

---

## 📁 Development Notes

Written in Java using the Spigot API.

Uses **ProtocolLib** to intercept and patch potions via:

- `SET_SLOT`
- `WINDOW_ITEMS`

✅ Built and tested on:

- Minecraft 1.8.8
- Minecraft 1.20.1
- Eaglercraft 1.8.8
- Paper 1.20.4
- Java 17+

---

## ✅ License

This plugin is currently in **closed alpha testing**.

🛠️ DM me to request usage or contribute to development.

---

Thanks for checking out PotionFixer! ☕🧪
