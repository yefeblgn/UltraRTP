package com.yefeblgn.ultrartp.hook;

import com.yefeblgn.ultrartp.UltraRTP;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault ekonomi entegrasyonu.
 * Vault veya bir ekonomi eklentisi yoksa tüm işlemler "ücretsiz" gibi davranır.
 */
public final class EconomyHook {

    private final UltraRTP plugin;
    private Economy economy;

    public EconomyHook(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        this.economy = null;

        if (!plugin.config().vaultEnabled()) {
            plugin.getLogger().info("Vault entegrasyonu config'te kapalı.");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault bulunamadı - ücret sistemi devre dışı.");
            return;
        }

        try {
            RegisteredServiceProvider<Economy> provider =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (provider == null) {
                plugin.getLogger().warning("Vault var fakat kayıtlı bir ekonomi eklentisi yok.");
                return;
            }
            this.economy = provider.getProvider();
            plugin.getLogger().info("Vault ekonomisi bağlandı: " + economy.getName());
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Vault ekonomisine bağlanılamadı: " + throwable.getMessage());
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public String currencyName() {
        if (economy == null) return "";
        return economy.currencyNamePlural();
    }

    public double balance(OfflinePlayer player) {
        if (economy == null || player == null) return 0.0D;
        try {
            return economy.getBalance(player);
        } catch (Throwable throwable) {
            return 0.0D;
        }
    }

    public String format(double amount) {
        if (economy == null) return com.yefeblgn.ultrartp.util.Formatter.money(amount);
        try {
            return economy.format(amount);
        } catch (Throwable throwable) {
            return com.yefeblgn.ultrartp.util.Formatter.money(amount);
        }
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0.0D) return true;
        try {
            return economy.has(player, amount);
        } catch (Throwable throwable) {
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0.0D) return true;
        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response != null && response.transactionSuccess();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Para çekilemedi: " + throwable.getMessage());
            return false;
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0.0D) return true;
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response != null && response.transactionSuccess();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Para yatırılamadı: " + throwable.getMessage());
            return false;
        }
    }
}
