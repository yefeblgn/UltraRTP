package com.yefeblgn.ultrartp.gui;

import com.yefeblgn.ultrartp.UltraRTP;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Yönetim panelinde "elle değer gir" akışı için sohbetten girdi toplar.
 * <p>
 * Girdi iptal edilirse ya da süre dolarsa geri çağrıya {@code null} gönderilir.
 */
public final class ChatInputManager implements Listener {

    private static final long TIMEOUT_MILLIS = 60_000L;

    private record Request(Consumer<String> consumer, long expiry) {
    }

    private final UltraRTP plugin;
    private final Map<UUID, Request> pending = new ConcurrentHashMap<>();

    public ChatInputManager(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, Consumer<String> consumer) {
        if (player == null) return;
        player.closeInventory();
        plugin.messages().send(player, "admin.input-prompt");
        pending.put(player.getUniqueId(), new Request(consumer, System.currentTimeMillis() + TIMEOUT_MILLIS));
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Request request = pending.remove(player.getUniqueId());
        if (request == null) return;

        event.setCancelled(true);

        String raw = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (System.currentTimeMillis() > request.expiry()) {
            plugin.messages().send(player, "admin.input-timeout");
            return;
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        boolean cancelled = lower.equals("iptal") || lower.equals("cancel") || lower.equals("q");

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (cancelled) {
                plugin.messages().send(player, "admin.input-cancelled");
                request.consumer().accept(null);
            } else {
                request.consumer().accept(raw);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
