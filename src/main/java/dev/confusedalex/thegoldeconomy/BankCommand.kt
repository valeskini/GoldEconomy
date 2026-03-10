package dev.confusedalex.thegoldeconomy

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import dev.confusedalex.thegoldeconomy.TheGoldEconomy.base
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem

import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import java.util.*

@Suppress("DEPRECATION")
@CommandAlias("bank")
class BankCommand(plugin: TheGoldEconomy) : BaseCommand() {
    private var bundle: ResourceBundle = plugin.eco.bundle
    private var eco: EconomyImplementer = plugin.eco
    var util: Util = plugin.util
    var config: FileConfiguration = plugin.config

    @HelpCommand
    fun help(help: CommandHelp) {
        help.showHelp()
    }

    @Default
    @CommandPermission("thegoldeconomy.gui")
    @Description("{@@command.info.gui}")
    fun gui(commandSender: CommandSender) {
        val playerOptional = util.isPlayer(sender)
        if (playerOptional.isEmpty) return

        val player = playerOptional.get()
        val uuid = player.uniqueId

        if (util.isBankingRestrictedToPlot(player)) return

        val gui = Gui.gui().title(Component.text("Bank")).rows(5).disableAllInteractions().create()

        // Materials per amount for withdraw (all red concrete)
        val withdrawMaterials = mapOf(
            100 to Material.RED_CONCRETE,
            20 to Material.RED_CONCRETE,
            1 to Material.RED_CONCRETE
        )

        // Materials per amount for deposit (all lime concrete)
        val depositMaterials = mapOf(
            100 to Material.LIME_CONCRETE,
            20 to Material.LIME_CONCRETE,
            1 to Material.LIME_CONCRETE
        )

        fun getMaterialForDigit(digit: Char): Material = when(digit) {
            '1' -> Material.BEE_SPAWN_EGG
            '2' -> Material.PIG_SPAWN_EGG
            '3' -> Material.MULE_SPAWN_EGG
            '4' -> Material.PANDA_SPAWN_EGG
            '5' -> Material.SHEEP_SPAWN_EGG
            '6' -> Material.HORSE_SPAWN_EGG
            '7' -> Material.PARROT_SPAWN_EGG
            '8' -> Material.WOLF_SPAWN_EGG
            '9' -> Material.CHICKEN_SPAWN_EGG
            '0' -> Material.FOX_SPAWN_EGG
            else -> Material.FOX_SPAWN_EGG
        }

        fun createBalanceRowItems(balance: Int): List<GuiItem> {
            val balanceStr = balance.toString()
            val maxLength = 7
            val paddedBalance = balanceStr.padStart(maxLength, '0')
            return paddedBalance.map { digit ->
                val material = getMaterialForDigit(digit)
                ItemBuilder.from(material)
                    .name(Component.text("§lBalance"))
                    .asGuiItem()
            }
        }

        fun updateBalance() {
            val balanceAmount = eco.bank.getAccountBalance(uuid)
            val balanceItems = createBalanceRowItems(balanceAmount)
            for (i in balanceItems.indices) {
                gui.updateItem(2, 2 + i, balanceItems[i])  // row 2, cols 2-8, 1-based indexing
            }
        }

        fun createWithdrawItem(amount: Int): GuiItem {
            val material = withdrawMaterials[amount] ?: Material.RED_CONCRETE
            return ItemBuilder.from(material)
                .name(Component.text("Withdraw §l${amount}$"))
                .asGuiItem {
                    val balance = eco.bank.getAccountBalance(uuid)
                    if (amount > balance) {
                        util.sendMessageToPlayer(bundle.getString("error.notEnough"), player)
                    } else {
                        util.sendMessageToPlayer(String.format(bundle.getString("info.withdraw"), amount), player)
                        eco.converter.withdraw(player, amount, base)
                        updateBalance()
                    }
                }
        }

        fun createDepositItem(amount: Int): GuiItem {
            val material = depositMaterials[amount] ?: Material.LIME_CONCRETE
            return ItemBuilder.from(material)
                .name(Component.text("Deposit: §l${amount}$"))
                .asGuiItem {
                    val inventoryValue = eco.converter.getInventoryValue(player, base)
                    if (amount > inventoryValue) {
                        util.sendMessageToPlayer(bundle.getString("error.notEnough"), player)
                    } else {
                        util.sendMessageToPlayer(String.format(bundle.getString("info.deposit"), amount), player)
                        eco.converter.deposit(player, amount, base)
                        updateBalance()
                    }
                }
        }

        val withdrawAmounts = listOf(100, 20, 1)
        val depositAmounts = listOf(100, 20, 1)

        val withdrawItems = withdrawAmounts.map { createWithdrawItem(it) }
        val depositItems = depositAmounts.map { createDepositItem(it) }

        val balanceAmount = eco.bank.getAccountBalance(uuid)
        val balanceItems = createBalanceRowItems(balanceAmount)

        for (i in balanceItems.indices) {
            gui.setItem(2, 2 + i, balanceItems[i])
        }

        gui.setItem(4, 2, depositItems[0]) // Deposit 100
        gui.setItem(4, 3, depositItems[1]) // Deposit 20
        gui.setItem(4, 4, depositItems[2]) // Deposit 1

        gui.setItem(4, 6, withdrawItems[0]) // Withdraw 100
        gui.setItem(4, 7, withdrawItems[1]) // Withdraw 20
        gui.setItem(4, 8, withdrawItems[2]) // Withdraw 1

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem())

        gui.open(player)
    }

    @Subcommand("balance")
    @CommandAlias("balance")
    @Description("{@@command.info.balance}")
    @CommandPermission("thegoldeconomy.balance")
    fun balance(commandSender: CommandSender, @Optional player: OfflinePlayer?) {
        val senderOptional = util.isPlayer(commandSender)
        if (senderOptional.isEmpty) return

        val sender = senderOptional.get()
        val uuid = sender.uniqueId

        if (player == null) {
            util.sendMessageToPlayer(
                String.format(
                    bundle.getString("info.balance"),
                    eco.getBalance(uuid.toString()).toInt(),
                    eco.bank.getAccountBalance(uuid),
                    eco.converter.getInventoryValue(sender, base)
                ), sender
            )
        } else if (sender.hasPermission("thegoldeconomy.balance.others")) {
            util.sendMessageToPlayer(
                String.format(
                    bundle.getString("info.balance.other"), player.name, eco.getBalance(player).toInt()
                ), sender
            )
        } else {
            util.sendMessageToPlayer(bundle.getString("error.noPermission"), sender)
        }
    }

    @Subcommand("baltop")
    @CommandPermission("thegoldeconomy.balance.top")
    @Description("{@@command.info.baltop}")
    fun balanceTop(commandSender: CommandSender) {
        val senderOptional = util.isPlayer(commandSender)
        if (senderOptional.isEmpty) return
        val sender = senderOptional.get()

        val allBalances = eco.bank.getAllPlayerAccounts()

        if (allBalances.isEmpty()) {
            util.sendMessageToPlayer(bundle.getString("info.balance.top.empty"), sender)
            return
        }

        val topBalances = allBalances.entries.sortedByDescending { it.value }.take(10)

        util.sendMessageToPlayer("§6§l--- Top 10 Richest Players ---", sender)

        topBalances.forEachIndexed { index, entry ->
            val uuid = entry.key
            val balance = entry.value
            val playerName = Bukkit.getOfflinePlayer(uuid)?.name ?: "Unknown"

            // Format balance with commas for thousands (nice readability)
            val balanceFormatted = "%,d".format(balance)

            // Make the rank (index + 1) two digits wide for nice alignment, e.g. " 1.", "10."
            val rank = String.format("%2d.", index + 1)

            // Build the message: rank in gold, name in green bold, balance in yellow with currency
            val message = "§e$rank §a§l$playerName §7- §6$balanceFormatted Gold"

            util.sendMessageToPlayer(message, sender)
        }

        util.sendMessageToPlayer("§6§l----------------------------", sender)
    }



    @Subcommand("pay")
    @Description("{@@command.info.pay}")
    @CommandPermission("thegoldeconomy.pay")
    fun pay(commandSender: CommandSender, target: OfflinePlayer, amount: Int) {
        val playerOptional = util.isPlayer(commandSender)
        if (playerOptional.isEmpty) return
        val sender = playerOptional.get()
        val senderuuid = sender.uniqueId
        val targetuuid = target.uniqueId

        when {
            util.isBankingRestrictedToPlot(sender) -> return
            amount == 0 -> {
                util.sendMessageToPlayer(bundle.getString("error.zero"), sender)
                return
            }

            amount < 0 -> {
                util.sendMessageToPlayer(bundle.getString("error.negative"), sender)
                return
            }

            amount > eco.bank.getTotalPlayerBalance(senderuuid) -> {
                util.sendMessageToPlayer(bundle.getString("error.notEnough"), sender)
                return
            }

            senderuuid == targetuuid -> {
                util.sendMessageToPlayer(bundle.getString("error.payYourself"), sender)
                return
            }

            util.isOfflinePlayer(target.name.toString()).isEmpty -> {
                util.sendMessageToPlayer(bundle.getString("error.noPlayer"), sender)
                return
            }

            else -> {
                eco.withdrawPlayer(sender, amount.toDouble())
                util.sendMessageToPlayer(
                    String.format(bundle.getString("info.sendMoneyTo"), amount, target.name), sender
                )
                if (target.isOnline) {
                    util.sendMessageToPlayer(
                        String.format(
                            bundle.getString("info.moneyReceived"), amount, sender.name
                        ), Bukkit.getPlayer(target.uniqueId)
                    )
                    eco.bank.setAccountBalance(target.uniqueId, eco.bank.getTotalPlayerBalance(targetuuid) + amount)
                } else {
                    eco.depositPlayer(target, amount.toDouble())
                }
            }
        }
    }

    @Subcommand("deposit")
    @Description("{@@command.info.deposit}")
    @CommandPermission("thegoldeconomy.deposit")
    fun deposit(commandSender: CommandSender, @Optional nuggets: String?) {
        val playerOptional = util.isPlayer(commandSender)
        if (playerOptional.isEmpty) return

        val player = playerOptional.get()
        val inventoryValue = eco.converter.getInventoryValue(player, base)

        if (util.isBankingRestrictedToPlot(player)) {
            return
        }
        if (nuggets == null || nuggets == "all") {
            if (inventoryValue <= 0) {
                util.sendMessageToPlayer(bundle.getString("error.zero"), player)
                return
            }
            util.sendMessageToPlayer(String.format(bundle.getString("info.deposit"), inventoryValue), player)
            eco.converter.deposit(player, inventoryValue, base)
            return
        }

        val amount: Int
        try {
            amount = nuggets.toInt()
        } catch (e: NumberFormatException) {
            commandHelp.showHelp()
            return
        }

        if (amount == 0) {
            util.sendMessageToPlayer(bundle.getString("error.zero"), player)
        } else if (amount < 0) {
            util.sendMessageToPlayer(bundle.getString("error.negative"), player)
        } else if (amount > inventoryValue) {
            util.sendMessageToPlayer(bundle.getString("error.notEnough"), player)
        } else {
            util.sendMessageToPlayer(String.format(bundle.getString("info.deposit"), amount), player)
            eco.converter.deposit(player, nuggets.toInt(), base)
        }
    }

    @Subcommand("withdraw")
    @Description("{@@command.info.withdraw}")
    @CommandPermission("thegoldeconomy.withdraw")
    fun withdraw(commandSender: CommandSender, @Optional nuggets: String?) {
        val playerOptional = util.isPlayer(commandSender)
        if (playerOptional.isEmpty) return

        val player = playerOptional.get()

        if (util.isBankingRestrictedToPlot(player)) return
        if (nuggets == null || nuggets == "all") {
            val accountBalance = eco.bank.getAccountBalance(player.uniqueId)
            util.sendMessageToPlayer(String.format(bundle.getString("info.withdraw"), accountBalance), player)
            eco.converter.withdraw(player, eco.bank.getAccountBalance(player.uniqueId), base)
            return
        }

        val amount: Int
        try {
            amount = nuggets.toInt()
        } catch (e: NumberFormatException) {
            commandHelp.showHelp()
            return
        }

        if (amount == 0) {
            util.sendMessageToPlayer(bundle.getString("error.zero"), player)
        } else if (amount < 0) {
            util.sendMessageToPlayer(bundle.getString("error.negative"), player)
        } else if (amount > eco.bank.getAccountBalance(player.uniqueId)) {
            util.sendMessageToPlayer(bundle.getString("error.notEnough"), player)
        } else {
            util.sendMessageToPlayer(String.format(bundle.getString("info.withdraw"), amount), player)
            eco.converter.withdraw(player, amount, base)
        }
    }


    @Subcommand("set")
    @CommandPermission("thegoldeconomy.set")
    @Description("{@@command.info.set}")
    fun set(commandSender: CommandSender?, target: OfflinePlayer, gold: Int) {
        if (commandSender is Player) {
            util.sendMessageToPlayer(
                String.format(bundle.getString("info.sender.moneyset"), target.name, gold), commandSender
            )
        }

        eco.bank.setAccountBalance(target.uniqueId, gold)
        util.sendMessageToPlayer(
            String.format(bundle.getString("info.target.moneySet"), gold), Bukkit.getPlayer(target.uniqueId)
        )
    }

    @Subcommand("add")
    @CommandPermission("thegoldeconomy.add")
    @Description("{@@command.info.add}")
    fun add(commandSender: CommandSender?, target: OfflinePlayer, gold: Int) {
        if (commandSender is Player) {
            util.sendMessageToPlayer(
                String.format(bundle.getString("info.sender.addmoney"), gold, target.name), commandSender
            )
        }

        eco.depositPlayer(target, gold.toDouble())
        util.sendMessageToPlayer(
            String.format(bundle.getString("info.target.addMoney"), gold), Bukkit.getPlayer(target.uniqueId)
        )
    }

    @Subcommand("remove")
    @CommandPermission("thegoldeconomy.remove")
    @Description("{@@command.info.remove}")
    fun remove(commandSender: CommandSender?, target: OfflinePlayer, gold: Int) {
        if (commandSender is Player) {
            util.sendMessageToPlayer(
                String.format(bundle.getString("info.sender.remove"), gold, target.name), commandSender
            )
        }

        eco.withdrawPlayer(target, gold.toDouble())
        util.sendMessageToPlayer(
            String.format(bundle.getString("info.target.remove"), gold), Bukkit.getPlayer(target.uniqueId)
        )
    }

}