package dev.confusedalex.thegoldeconomy

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot

class AtmHandler(
    private val bankCommand: BankCommand,
        private val plugin: JavaPlugin
) : Listener {

    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return

            if (event.action == Action.RIGHT_CLICK_BLOCK && event.clickedBlock != null) {
                if (event.clickedBlock!!.type == Material.RESIN_BRICK_STAIRS) {
                    val player = event.player
                    if (!event.isCancelled) {
                        event.isCancelled = true
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                        player.swingMainHand()
                        bankCommand.gui(player)

                        // Kurzer Delay, um Spam-Klicks zu verhindern
                        plugin.server.scheduler.runTaskLater(plugin, Runnable {
                            // Hier ist Vorsicht geboten: Das Event abzubrechen und später wieder
                            // auf false zu setzen macht technisch wenig Sinn, da das Event hier schon vorbei ist.
                        }, 10L)
                    }
                }
            }
    }

    fun createCustomCraftingRecipe() {
        val resinBlock = ItemStack(Material.RESIN_BRICK_STAIRS)

        // FIX: NamespacedKey hinzufügen
        val key = NamespacedKey(plugin, "atm_recipe")
        val recipe = ShapedRecipe(key, resinBlock)

        recipe.shape(
            "BCB",
            "ADA",
            "AAA"
        )

        recipe.setIngredient('A', Material.GRAY_CONCRETE)
        recipe.setIngredient('B', Material.STONE_BUTTON)
        recipe.setIngredient('C', Material.TORCH)
        recipe.setIngredient('D', Material.IRON_NUGGET)

        // Rezept registrieren
        plugin.server.addRecipe(recipe)

        plugin.logger.info("Custom Resin Block crafting recipe added with key: ${key.toString()}")
    }
}
