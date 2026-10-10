# ⚔️ Duels - Advanced Matchmaking, 1v1 & Team Duels System

A high-performance, fully customizable Minecraft Duels plugin designed for competitive servers. Supports everything from fast-paced **1v1 duels** up to **5v5 team clashes**, featuring automated arena regeneration, persistent SQL data storage, custom kit layouts, and real-time spectating.

---

## 🌟 Key Features

* **⚔️ Multiple Match Formats:** Seamless support for classic **1v1** challenges and large-scale team modes like **2v2, 3v3, or 5v5**.
* **⚡ Matchmaking & Queues:** Fast queue systems for solo players and parties with direct challenge management.
* **👥 Full Party Support:** Robust party coordination designed for team vs. team match setups.
* **🎒 Custom Kit Layouts:** Per-player inventory and hotbar layout editor with instant persistence.
* **🔄 Fast Arena Regeneration:** Region-based terrain rollback that instantly restores arenas after intense matches.
* **👁️ Spectator Mode:** Smooth, interference-free live duel spectating for players and admins.
* **📊 SQL Statistics & History:** Persistent database tracking for player ratings, win/loss stats, match history, and top leaderboards.
* **🛡️ Admin Management:** Complete administrative control over arenas, duels, kits, and matches.

---

## 🎮 Match Types

| Mode | Format | Description |
| :--- | :--- | :--- |
| **Solo Duel** | `1v1` | Direct challenge or ranked queue match |
| **Team / Party Duel** | `2v2` to `5v5` | Coordinated party battles and multi-player arena skirmishes |

---

## 💻 Commands & Permissions

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/duel <player>` | `/1v1`, `/duello`, `/düello` | Send, accept, or decline a duel request | *Default* |
| `/parti` | `/p` | Party management commands for team duels | *Default* |
| `/queue` | `/q` | Join or leave a solo/team matchmaking queue | *Default* |
| `/spectate <player>` | `/spec` | Spectate an active match | *Default* |
| `/kitduzenle` | `/layout`, `/kitedit` | Open the custom kit layout editor | *Default* |
| `/gecmisduellolar` | `/duelhistory`, `/matchhistory` | View previous duel match logs | `duels.history` |
| `/eniyiduellocular` | `/eniyiler`, `/düellocular` | Display top duelist leaderboards | `duels.top` |
| `/arenaregen` | `/ar` | Manage arena reset zones and boundaries | `duels.admin` |
| `/duels` | `/ds` | Administrative management and configuration | `duels.admin` |

---

## 🛠️ Installation

1. Place the compiled `.jar` file into your server's `plugins/` directory.
2. Restart your server to generate configuration files.
3. Configure database settings (`MySQL` / `SQLite`) in `config.yml`.
4. Define arenas and boundaries using `/arenaregen`.
