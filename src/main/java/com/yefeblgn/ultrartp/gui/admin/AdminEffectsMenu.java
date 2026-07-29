package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Parçacık / ses efektleri ve geri sayım gösterimi.
 */
public final class AdminEffectsMenu extends AdminMenu {

    private static final List<String> COUNTDOWN_MODES =
            List.of("NONE", "TITLE", "ACTIONBAR", "BOSSBAR", "BOTH", "ALL");

    public AdminEffectsMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return plugin.messages().get(player, "gui.admin-effects-title");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        toggle(10, "effects.warmup.particle.enabled", "fx-warmup-particle", Material.NETHER_STAR);
        toggle(19, "effects.warmup.sound.enabled", "fx-warmup-sound", Material.NOTE_BLOCK);

        toggle(11, "effects.searching.particle.enabled", "fx-searching-particle", Material.SPYGLASS);
        toggle(20, "effects.searching.sound.enabled", "fx-searching-sound", Material.BELL);

        toggle(12, "effects.departure.particle.enabled", "fx-departure-particle", Material.ENDER_PEARL);
        toggle(21, "effects.departure.sound.enabled", "fx-departure-sound", Material.ENDER_EYE);

        toggle(13, "effects.arrival.particle.enabled", "fx-arrival-particle", Material.END_ROD);
        toggle(22, "effects.arrival.sound.enabled", "fx-arrival-sound", Material.AMETHYST_SHARD);

        toggle(14, "effects.cancel.particle.enabled", "fx-cancel-particle", Material.GUNPOWDER);
        toggle(23, "effects.cancel.sound.enabled", "fx-cancel-sound", Material.REDSTONE_TORCH);

        set(16, countdownItem(), event -> {
            String current = plugin.config().countdownMode();
            int index = COUNTDOWN_MODES.indexOf(current);
            String next = COUNTDOWN_MODES.get((index + 1 + COUNTDOWN_MODES.size()) % COUNTDOWN_MODES.size());
            plugin.config().setAndSave("effects.countdown.mode", next);
            plugin.messages().send(player, "admin.value-set",
                    Text.of("value", plugin.messages().rawString("admin.gui.countdown-mode")),
                    Text.of("count", next));
            rebuild();
        });

        set(25, ItemBuilder.of(Material.FIREWORK_STAR)
                .name("<white><bold>Efekt Önizleme</bold>")
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<gray>Sol tık: <white>varış efekti")
                        .add("<gray>Sağ tık: <white>ayrılış efekti"))
                .hideAll()
                .build(), event -> {
            plugin.effects().play(event.isRightClick() ? "departure" : "arrival", player);
            plugin.effects().ring(event.isRightClick() ? "departure" : "arrival", player.getLocation(), 1.3D, 18);
        });

        backButton(36, new AdminMainMenu(plugin, player));
        closeButton(40);
        fillEmpty();
    }

    private org.bukkit.inventory.ItemStack countdownItem() {
        return ItemBuilder.of(Material.CLOCK)
                .name("<white><bold><name></bold>",
                        Text.of("name", plugin.messages().rawString("admin.gui.countdown-mode")))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Mod: <aqua><value>", Text.of("value", plugin.config().countdownMode()))
                        .blank()
                        .add("<gray>NONE / TITLE / ACTIONBAR")
                        .add("<gray>BOSSBAR / BOTH / ALL")
                        .blank()
                        .add("<yellow>» Tıkla: sıradaki mod"))
                .glow(!plugin.config().countdownMode().equals("NONE"))
                .hideAll()
                .build();
    }

    @Override
    protected void afterChange() {
        // efekt ayarları depoyu etkilemez
    }
}
