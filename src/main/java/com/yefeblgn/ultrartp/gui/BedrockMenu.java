package com.yefeblgn.ultrartp.gui;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Formatter;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Bedrock (Geyser) oyuncular icin yerel Form menusu.
 *
 * <p>Cam panelli sandik menusu Bedrock'ta kotu gorunur; bunun yerine
 * Bedrock'un kendi buton listesi kullanilir. Isinlanma mantigi degismez,
 * ayni {@code TeleportManager} cagrilir - yalnizca sunum farklidir.</p>
 *
 * <p>Form cevaplari Geyser'in ag is parcaciginda gelir; isinlanma
 * cagrilari ana is parcacigina aktarilir.</p>
 */
public final class BedrockMenu {

    private final UltraRTP plugin;
    private boolean warned;

    public BedrockMenu(UltraRTP plugin) {
        this.plugin = plugin;
    }

    /** Cumulus + Floodgate siniflari yuklenebiliyor mu? */
    public boolean isAvailable() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Class.forName("org.geysermc.cumulus.form.SimpleForm");
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    /**
     * Ana menuyu form olarak acar.
     *
     * @return gonderildiyse true; false ise cagiran taraf sandik menusune duser
     */
    public boolean openMain(Player player) {
        try {
            return build(player);
        } catch (Throwable throwable) {
            if (!warned) {
                plugin.getLogger().warning("Bedrock menusu olusturulamadi: "
                        + throwable.getClass().getName() + ": " + throwable.getMessage()
                        + " - sandik menusune donuluyor.");
                warned = true;
            }
            return false;
        }
    }

    private boolean build(Player player) {
        List<Region> regions = new ArrayList<>();
        for (Region region : plugin.config().regions()) {
            if (region.enabled() && Bukkit.getWorld(region.worldName()) != null) {
                regions.add(region);
            }
        }

        // Basliklar gradyanli; Bedrock'ta harf harf renk degistigi icin
        // okunmuyor -> duz metin + tek renk.
        // Menude SADECE bolge kartlari var; "Rastgele Bolge" ve "Onceki
        // Konuma Don" kaldirildi (komutla erisilebiliyorlar: /rtp, /rtp back).
        SimpleForm.Builder form = SimpleForm.builder()
                .title(flat(plugin.messages().get(player, "gui.main-title"), "§b§l"))
                .content(summary(player));

        for (Region region : regions) {
            addRegionButton(form, player, region);
        }

        form.validResultHandler(response -> {
            int id = response.clickedButtonId();
            Bukkit.getScheduler().runTask(plugin, () -> handle(player, regions, id));
        });

        return send(player, form.build());
    }

    private void handle(Player player, List<Region> regions, int id) {
        if (id < 0 || id >= regions.size()) {
            return;
        }
        Region region = regions.get(id);
        if (!plugin.teleports().hasAccess(player, region)) {
            plugin.messages().send(player, "general.no-permission");
            return;
        }
        plugin.teleports().request(player, region);
    }

    // ------------------------------------------------------------- butonlar

    private void addRegionButton(SimpleForm.Builder form, Player player, Region region) {
        // Etiket DOGRUDAN kodda kuruluyor, dil dosyasi sablonundan degil.
        // Sebep: sunucuda onceden olusmus lang dosyalari guncellenmiyor
        // (mevcut anahtarlar korunuyor) ve eski sablondaki <world> gibi
        // yer tutucular ham haliyle ekranda kaliyordu.
        String plainName = Text.plain(Text.mm(region.displayName()));
        String status = Text.plain(Text.mm(statusText(player, region)));
        String label = "§f" + plainName + "\n§7" + status;

        String icon = region.bedrockIcon();
        if (icon == null || icon.isBlank()) {
            form.button(label);
            return;
        }
        FormImage.Type type = icon.startsWith("http") ? FormImage.Type.URL : FormImage.Type.PATH;
        form.button(label, type, icon);
    }

    private String statusText(Player player, Region region) {
        if (!plugin.teleports().hasAccess(player, region)) {
            return plugin.messages().rawString("bedrock.locked");
        }
        long cooldown = plugin.teleports().remainingCooldown(player, region);
        if (cooldown > 0) {
            return plugin.messages().rawString("bedrock.cooldown")
                    .replace("<time>", Formatter.time(cooldown));
        }
        double cost = plugin.teleports().effectiveCost(player, region);
        if (cost > 0) {
            return plugin.messages().rawString("bedrock.cost")
                    .replace("<cost>", plugin.economy().format(cost));
        }
        return plugin.messages().rawString("bedrock.free");
    }


    private String summary(Player player) {
        return legacy(plugin.messages().get(player, "bedrock.summary",
                Text.of("player", player.getName()),
                Text.of("balance", plugin.economy().format(plugin.economy().balance(player))),
                Text.num("count", plugin.data().teleportCount(player.getUniqueId()))));
    }

    // -------------------------------------------------------------- altyapi

    /**
     * MiniMessage Component -> Bedrock metni.
     *
     * <p><b>Gradyanlar duz metne cevrilir.</b> {@code <gradient:...>} her
     * harfe ayri renk verir; Java'da yumusak gecis gorunur ama Bedrock'ta
     * harf harf zipliyan gokkusagi gibi durur ve okunakligi bozar.
     * Bu yuzden Bedrock tarafinda renkleri atip tek renk uyguluyoruz.</p>
     */
    private static String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /** Renksiz duz metin + tek renk kodu. */
    private static String flat(Component component, String color) {
        return color + Text.plain(component);
    }

    private boolean send(Player player, Form form) {
        try {
            if (FloodgateApi.getInstance().sendForm(player.getUniqueId(), form)) {
                return true;
            }
        } catch (Throwable throwable) {
            if (!warned) {
                plugin.getLogger().warning("Bedrock formu gonderilemedi: " + throwable.getMessage());
                warned = true;
            }
            return false;
        }
        if (!warned) {
            plugin.getLogger().warning("Floodgate formu kabul etmedi (oyuncu kaydi yok). "
                    + "Proxy kurulumunda 'send-floodgate-data: true' gerekir.");
            warned = true;
        }
        return false;
    }
}
