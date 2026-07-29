package com.yefeblgn.ultrartp.hook;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Formatter;
import com.yefeblgn.ultrartp.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI genişletmesi.
 *
 * <pre>
 *  %ultrartp_cooldown%                 -> kalan saniye (sayı)
 *  %ultrartp_cooldown_formatted%       -> "1dk 20sn"
 *  %ultrartp_cooldown_&lt;bölge&gt;%        -> o bölgenin kalan süresi
 *  %ultrartp_can_teleport%             -> true/false
 *  %ultrartp_teleports%                -> toplam ışınlanma sayısı
 *  %ultrartp_cost%                     -> varsayılan ücret
 *  %ultrartp_cost_&lt;bölge&gt;%            -> bölge ücreti
 *  %ultrartp_regions%                  -> erişilebilen bölge sayısı
 *  %ultrartp_regions_total%            -> toplam açık bölge sayısı
 *  %ultrartp_region_name_&lt;bölge&gt;%     -> bölgenin görünen adı (renksiz)
 *  %ultrartp_warmup%                   -> ayarlı gecikme (sn)
 *  %ultrartp_warmup_remaining%         -> devam eden ışınlanmanın kalan süresi
 *  %ultrartp_in_warmup%                -> true/false
 *  %ultrartp_back_available%           -> true/false
 *  %ultrartp_cache_total%              -> depodaki toplam hazır konum
 *  %ultrartp_cache_&lt;bölge&gt;%           -> bölge deposundaki konum sayısı
 *  %ultrartp_language%                 -> aktif dil
 *  %ultrartp_version%                  -> eklenti sürümü
 * </pre>
 */
public final class RTPExpansion extends PlaceholderExpansion {

    private final UltraRTP plugin;

    public RTPExpansion(UltraRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ultrartp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "yefeblgn";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        String param = params.toLowerCase(Locale.ROOT);
        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();

        switch (param) {
            case "version" -> {
                return plugin.getPluginMeta().getVersion();
            }
            case "language" -> {
                return plugin.config().language();
            }
            case "regions_total" -> {
                return String.valueOf(plugin.config().regions().stream().filter(Region::enabled).count());
            }
            case "cost" -> {
                return plugin.economy().format(plugin.config().costAmount());
            }
            case "warmup" -> {
                return String.valueOf(plugin.config().warmupSeconds());
            }
            case "cache_total" -> {
                return String.valueOf(plugin.cache().totalSize());
            }
            default -> {
                // aşağıda devam
            }
        }

        if (param.startsWith("cache_")) {
            return String.valueOf(plugin.cache().size(param.substring(6)));
        }
        if (param.startsWith("region_name_")) {
            Region region = plugin.config().region(param.substring(12));
            return region == null ? "" : Text.plain(Text.mm(region.displayName()));
        }
        if (param.startsWith("cost_")) {
            Region region = plugin.config().region(param.substring(5));
            if (region == null) return "";
            double cost = region.cost() >= 0 ? region.cost() : plugin.config().costAmount();
            return plugin.economy().format(cost);
        }

        if (offlinePlayer == null) return "";

        switch (param) {
            case "teleports" -> {
                return String.valueOf(plugin.data().teleportCount(offlinePlayer.getUniqueId()));
            }
            case "back_available" -> {
                return String.valueOf(plugin.data().back(offlinePlayer.getUniqueId()) != null);
            }
            default -> {
                // aşağıda devam
            }
        }

        if (player == null) return "";

        switch (param) {
            case "cooldown" -> {
                return String.valueOf(plugin.teleports().remainingCooldown(player, defaultRegion()));
            }
            case "cooldown_formatted" -> {
                long left = plugin.teleports().remainingCooldown(player, defaultRegion());
                return left <= 0 ? plugin.messages().rawString("time.now") : Formatter.time(left);
            }
            case "can_teleport" -> {
                return String.valueOf(plugin.teleports().remainingCooldown(player, defaultRegion()) <= 0
                        && !plugin.teleports().isBusy(player.getUniqueId()));
            }
            case "in_warmup" -> {
                return String.valueOf(plugin.teleports().isWarmingUp(player.getUniqueId()));
            }
            case "warmup_remaining" -> {
                return String.valueOf(plugin.teleports().warmupRemaining(player.getUniqueId()));
            }
            case "regions" -> {
                return String.valueOf(plugin.teleports().accessibleRegions(player).size());
            }
            default -> {
                // aşağıda devam
            }
        }

        if (param.startsWith("cooldown_formatted_")) {
            Region region = plugin.config().region(param.substring(19));
            if (region == null) return "";
            long left = plugin.teleports().remainingCooldown(player, region);
            return left <= 0 ? plugin.messages().rawString("time.now") : Formatter.time(left);
        }
        if (param.startsWith("cooldown_")) {
            Region region = plugin.config().region(param.substring(9));
            if (region == null) return "";
            return String.valueOf(plugin.teleports().remainingCooldown(player, region));
        }

        return null;
    }

    private Region defaultRegion() {
        Region region = plugin.config().region(plugin.config().defaultRegionId());
        if (region != null) return region;
        return plugin.config().regions().stream().filter(Region::enabled).findFirst().orElse(null);
    }
}
