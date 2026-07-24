# OD Eco v1.1.1

A comprehensive Paper economy plugin with GUI-driven tax management, player dealerships, sell shops, interest, lottery, gambling, bounties, banknotes, auctions, shared accounts, and transaction logging.

**Author:** OD Plugins  
**Supported MC versions:** 1.21+ (Paper)  
**API:** Paper 1.21+ (uses Paper-specific `PlayerProfile` API)  
**Requires:** Paper (will not run on Spigot/Bukkit/Vanilla)

---

## Features

| Feature | Description | Config Section |
|---------|-------------|----------------|
| **Economy** | Balance, pay, tab list balance display, starting balance | `settings` |
| **Sell** | Sell items from your hand for configured prices; admin-configurable sell prices via `/dealershipsetup` | `sell` / `worth` |
| **Dealerships** | Player-owned shops with custom items, stock, prices, and access control — all GUI-managed | `dealerships` |
| **Shared Accounts** | Joint accounts with deposit, withdrawal, invite, and kick — all GUI-managed | `shared-accounts` |
| **Interest** | Periodic interest on player balances (configurable rate and interval) | `interest` |
| **Lottery** | Ticket-based lottery with payout percentage | `lottery` |
| **Gambling** | Dice-style double-or-nothing with configurable max bet | `dice` |
| **Bounties** | Place bounties on players; killer collects on death | `bounties` |
| **Auctions** | Buy and sell items at the Auction House with listing fees and tax | `auctions` |
| **Banknotes** | Physical money items backed by PersistentDataContainer | `banknotes` |
| **Tax System** | Income tax, set tax, or percentage-based balance tax with grace periods, auto-deduct, banknote delivery, or manual payment | `taxes` |
| **Transaction Log** | Logs all balance changes to file with configurable retention | `transaction-logging` |
| **Eco Panel** | Full GUI hub for all player features — `/eco` | — |
| **Admin Panel** | Full GUI for all admin features — `/ecoadmin` | — |
| **Tax Setup Wizard** | Step-by-step `/taxsetup` command to configure tax settings interactively | `taxes` |
| **Dealership Setup** | Create, delete, edit members, and set custom icons via `/dealershipsetup` | `dealerships` |
| **Sell Setup** | Add, edit, and remove sellable items via the admin sell setup wizard | `worth` |
| **Chat Input System** | All prompts close the GUI, accept input in chat, and reopen automatically | — |

---

## Commands

### Player Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/eco` | `odeco.use` | Open the Economy Panel (aliases: `/ecopanel`, `/balance`, `/bal`) |
| `/pay <player> <amount>` | `odeco.use` | Send money to another player |
| `/sell` | `odeco.use` | Sell the item in your hand |
| `/worth [item]` | `odeco.use` | Check sell value of held item |
| `/baltop [page]` | `odeco.use` | View richest players on the server |
| `/lottery` | `odeco.use` | Buy lottery tickets and view lottery info |
| `/dice <amount>` | `odeco.use` | Roll the dice — double or nothing |
| `/banknote <amount>` | `odeco.use` | Create a physical banknote from your balance |
| `/auction` | `odeco.use` | Open the Auction House |
| `/interest` | `odeco.use` | View your current interest details |
| `/bounty <set\|check\|list>` | `odeco.use` | Place, check, or list bounties |
| `/taxmanager [pay <amount\|all>\|check]` | `odeco.taxmanager` | Check tax debt and pay taxes |
| `/dealership [name]` | `odeco.use` | Open a dealership shop |

### Admin Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/ecoadmin <give\|take\|set\|reload>` | `odeco.admin` | Admin economy commands (also opens Admin Panel GUI) |
| `/taxsetup` | `odeco.admin` | Open the Tax Setup Wizard — step-by-step tax configuration |
| `/dealershipsetup` | `odeco.admin` | Manage dealerships — create, delete, edit members, set icons |

---

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `odeco.use` | true | All player commands |
| `odeco.admin` | op | Admin commands (includes `odeco.use`) |
| `odeco.taxmanager` | op | Tax manager access (includes `odeco.use`) |

---

## Configuration

After editing `config.yml`, run `/ecoadmin reload` to apply changes without restarting.

Key config sections:

```yaml
currency-singular: Dollar
currency-plural: Dollars
currency-symbol: $
starting-balance: 100

interest:
  enabled: true
  rate: 2.5
  interval-minutes: 60

sell:
  enabled: true
  multiplier: 1.0

lottery:
  enabled: true
  ticket-price: 100
  payout-percentage: 80

dice:
  enabled: true
  max-bet: 50000

banknotes:
  enabled: true
  fee-percent: 2

auctions:
  enabled: true
  listing-fee: 0
  tax-percent: 5
  duration-hours: 24

bounties:
  enabled: true
  minimum: 100
  tax-percent: 5

shared-accounts:
  enabled: true

dealerships:
  enabled: true

transaction-logging:
  enabled: true
  keep-days: 30

tab-list:
  enabled: true
  update-interval-ticks: 100

taxes:
  enabled: false
  mode: NONE                    # NONE, INCOME_TAX, SET_TAX, PERCENTAGE_BALANCE
  income-tax-rate: 10           # % of income earned since last assessment
  set-tax-amount: 100           # Fixed amount per assessment
  balance-tax-rate: 1           # % of total balance
  interval-minutes: 60          # How often taxes are assessed (0 = disable)
  grace-period-minutes: 30      # Time to pay after assessment (0 = none)
  payment-method: AUTO          # AUTO, MANUAL, BANKNOTE
  auto-pay-to: SERVER           # SERVER or SHARED_ACCOUNT
  tax-shared-account: "Taxes"
  banknote-delivery-targets: []
  show-info-button: true
  overdue-penalty-percent: 10

admin:
  server-account: "Server"
```

---

## Installation

1. Drop `ODEco-1.1.0.jar` into your server's `plugins/` folder
2. Restart the server
3. Edit `plugins/ODE-Eco/config.yml` to your liking
4. Run `/ecoadmin reload` to apply changes

---

## Building from Source

```bash
# Compile (requires Paper 1.21+ on classpath)
javac --release 21 -cp <paper-api.jar> -d out src/main/java/com/odeco/*.java src/main/java/com/odeco/**/*.java

# Package
jar cf ODEco-1.1.0.jar -C out . -C src/main/resources .
```
