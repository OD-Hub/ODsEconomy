# OD's Economy

A comprehensive Paper economy plugin with land claims, dealerships, interest, lottery, gambling, bounties, banknotes, and transaction logging.

**Author:** OD Plugins  
**Supported MC versions:** 1.21 – 26.1.2 (Paper)  
**API:** Paper 1.21+

---

## Features

| Feature | Description | Config Section |
|---------|-------------|----------------|
| **Economy** | Balance, pay, top balances, give/take/set (admin) | `settings` |
| **Sell** | Sell items from your hand for configured prices | `sell-prices` |
| **Land Claims** | Register chunks to players, query ownership | `mregister` / `mcertification` |
| **Dealerships** | Player-owned shops with bulk ordering | `dealerships` |
| **Interest** | Periodic interest on player balances (disabled by default) | `interest` |
| **Lottery** | Ticket-based lottery with auto-draw and house cut | `lottery` |
| **Gambling** | Dice-style double-or-nothing, location-restrictable, command-block support | `gambling` |
| **Bounties** | Place bounties on players; killer collects on death | `bounties` |
| **Banknotes** | Physical money items (PersistentDataContainer-backed) | `banknotes` |
| **Transaction Log** | Logs all balance changes to file (PLAIN or CSV) | `transaction-log` |
| **Dynamic Help** | `/ecohelp` only shows commands the player can use | — |

---

## Commands

### Player Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/balance [player]` | `odseconomy.balance` | Check your or another player's balance |
| `/baltop` | `odseconomy.baltop` | Show richest players |
| `/mtransfer <player> <amount>` | `odseconomy.pay` | Send money |
| `/sell` | `odseconomy.sell` | Sell the item in your hand |
| `/worth` | `odseconomy.worth` | Check sell value of held item |
| `/mcertification <x> <z>` | `odseconomy.certification` | Check chunk owner |
| `/mcertification <player>` | `odseconomy.certification` | List player's chunks |
| `/dorder <item> <amount>` | `odseconomy.order` | Place a dealership order |
| `/dorder confirm` | `odseconomy.order` | Confirm pending order |
| `/dinquire <player\|dept>` | `odseconomy.inquire` | Check dealership info |
| `/dice <amount>` | `odseconomy.dice` | Gamble on a dice roll |
| `/lottery buy [tickets]` | `odseconomy.lottery` | Buy lottery tickets |
| `/lottery info` | `odseconomy.lottery` | Check lottery details |
| `/bounty set <player> <amount>` | `odseconomy.bounty` | Place a bounty |
| `/bounty list [player]` | `odseconomy.bounty` | List active bounties |
| `/mnote <amount>` | `odseconomy.banknote` | Create a physical banknote |

### Admin Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/mgive <player> <amount>` | `odseconomy.admin` | Grant money |
| `/mset <player> <amount>` | `odseconomy.admin` | Set balance |
| `/mtake <player> <amount>` | `odseconomy.admin` | Remove money |
| `/mregister <x> <z> <player>` | `odseconomy.admin` | Register a chunk |
| `/dgive <dept> <player>` | `odseconomy.admin` | Grant a dealership |
| `/mreload` | `odseconomy.admin` | Reload config |

---

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `odseconomy.*` | op | All permissions |
| `odseconomy.admin` | op | Admin commands |
| `odseconomy.balance` | true | /balance |
| `odseconomy.baltop` | true | /baltop |
| `odseconomy.pay` | true | /mtransfer |
| `odseconomy.sell` | true | /sell |
| `odseconomy.worth` | true | /worth |
| `odseconomy.certification` | true | /mcertification |
| `odseconomy.order` | true | /dorder |
| `odseconomy.inquire` | true | /dinquire |
| `odseconomy.dice` | true | /dice |
| `odseconomy.lottery` | true | /lottery |
| `odseconomy.bounty` | true | /bounty |
| `odseconomy.banknote` | true | /mnote |

---

## Configuration

After editing `config.yml`, run `/mreload` to apply changes without restarting.

Key config sections:

```yaml
settings:
  starting-balance: 0.0
  currency-symbol: "$"
  tablist-balance: true

interest:
  enabled: false
  rate: 0.01
  interval-seconds: 3600

lottery:
  enabled: true
  ticket-price: 100.0
  draw-interval-seconds: 86400
  house-cut-percent: 10

gambling:
  enabled: true
  dice-multiplier: 2.0
  require-location: false
  locations: []

bounties:
  enabled: true
  min-amount: 1.0
  max-amount: 100000.0

banknotes:
  enabled: true
  item-material: PAPER
  item-name: "&6Bank Note &7(%amount%)"

transaction-log:
  enabled: false
  format: PLAIN
```

---

## Installation

1. Drop `OdsEconomy.jar` into your server's `plugins/` folder
2. Restart the server (or use `/reload`)
3. Edit `plugins/OdsEconomy/config.yml` to your liking
4. Run `/mreload` to apply changes

---

## Building from Source

```bash
git clone <repo-url>
cd OdsEconomy
mvn clean package
```

Output: `target/OdsEconomy-1.0.0.jar`
