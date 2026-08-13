package com.yefeblgn.ultrartp.hook;

import com.yefeblgn.ultrartp.UltraRTP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Bedrock (Geyser/Floodgate) oyuncu tespiti.
 *
 * <p><b>Neden gerekli:</b> Bedrock istemcisi Java resource pack'ini almaz ve
 * sandik menusunu farkli cizer; cam panelli menuler orada kotu gorunur.
 * Bedrock oyuncusuna kendi yerel Form arayuzunu gostermek gerekir.</p>
 *
 * <p>Tespit iki sinyalin <b>VEYA</b>'sidir:</p>
 * <ol>
 *   <li>UUID deseni - Floodgate, Bedrock oyunculara ust 64 biti sifir olan
 *       UUID verir. Surumden bagimsiz, en guvenilir isaret.</li>
 *   <li>Floodgate API - hesap baglama gibi durumlarda UUID normal gorunebilir.</li>
 * </ol>
 *
 * <p>API'nin cevabi tek basina yeterli sayilmaz: proxy kurulumlarinda
 * backend Floodgate'i oyuncuyu tanimayabiliyor ve false donebiliyor.</p>
 */
public final class BedrockHook {

    private static final String FLOODGATE_API = "org.geysermc.floodgate.api.FloodgateApi";

    private final UltraRTP plugin;
    private Object floodgateApi;
    private Method isFloodgatePlayer;
    private boolean warned;

    public BedrockHook(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        floodgateApi = null;
        isFloodgatePlayer = null;

        boolean present = Bukkit.getPluginManager().getPlugin("floodgate") != null
                || Bukkit.getPluginManager().getPlugin("Floodgate") != null
                || Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;
        if (!present) {
            return;
        }

        try {
            Class<?> api = Class.forName(FLOODGATE_API);
            floodgateApi = api.getMethod("getInstance").invoke(null);
            isFloodgatePlayer = api.getMethod("isFloodgatePlayer", UUID.class);
        } catch (Exception ex) {
            plugin.getLogger().info("Floodgate API'sine erisilemedi, UUID yontemi kullanilacak ("
                    + ex.getClass().getSimpleName() + ").");
        }
    }

    public boolean isAvailable() {
        return floodgateApi != null;
    }

    /** Oyuncu Bedrock istemcisinden mi bagli? */
    public boolean isBedrock(Player player) {
        if (player == null) {
            return false;
        }
        if (hasBedrockUuid(player)) {
            return true;
        }
        return apiSaysBedrock(player);
    }

    /** Floodgate UUID deseni: ust 64 bit sifir. */
    public boolean hasBedrockUuid(Player player) {
        return player != null && player.getUniqueId().getMostSignificantBits() == 0L;
    }

    public boolean apiSaysBedrock(Player player) {
        if (player == null || floodgateApi == null || isFloodgatePlayer == null) {
            return false;
        }
        try {
            Object result = isFloodgatePlayer.invoke(floodgateApi, player.getUniqueId());
            return result instanceof Boolean bedrock && bedrock;
        } catch (Exception ex) {
            if (!warned) {
                plugin.getLogger().warning("Floodgate sorgusu basarisiz: " + ex.getMessage());
                warned = true;
            }
            return false;
        }
    }
}
