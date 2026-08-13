package com.yefeblgn.ultrartp;

import com.yefeblgn.ultrartp.command.RTPAdminCommand;
import com.yefeblgn.ultrartp.command.RTPCommand;
import com.yefeblgn.ultrartp.command.RTPZoneCommand;
import com.yefeblgn.ultrartp.config.ConfigManager;
import com.yefeblgn.ultrartp.config.Messages;
import com.yefeblgn.ultrartp.data.DataStore;
import com.yefeblgn.ultrartp.gui.BedrockMenu;
import com.yefeblgn.ultrartp.gui.ChatInputManager;
import com.yefeblgn.ultrartp.hook.BedrockHook;
import com.yefeblgn.ultrartp.hook.EconomyHook;
import com.yefeblgn.ultrartp.hook.ItemsAdderHook;
import com.yefeblgn.ultrartp.hook.PlaceholderHook;
import com.yefeblgn.ultrartp.listener.GUIListener;
import com.yefeblgn.ultrartp.listener.PlayerListener;
import com.yefeblgn.ultrartp.listener.ZoneWandListener;
import com.yefeblgn.ultrartp.teleport.EffectManager;
import com.yefeblgn.ultrartp.teleport.LocationCache;
import com.yefeblgn.ultrartp.teleport.LocationFinder;
import com.yefeblgn.ultrartp.teleport.SafetyChecker;
import com.yefeblgn.ultrartp.teleport.TeleportManager;
import com.yefeblgn.ultrartp.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * UltraRTP - Paper 1.21.8 için gelişmiş rastgele ışınlanma sistemi.
 *
 * @author yefeblgn
 * @see <a href="https://github.com/yefeblgn">github.com/yefeblgn</a>
 */
public final class UltraRTP extends JavaPlugin {

    private static UltraRTP instance;

    private ConfigManager configManager;
    private Messages messages;
    private DataStore dataStore;

    private EconomyHook economyHook;
    private PlaceholderHook placeholderHook;
    private ItemsAdderHook itemsAdderHook;
    private BedrockHook bedrockHook;
    private BedrockMenu bedrockMenu;

    private SafetyChecker safetyChecker;
    private LocationFinder locationFinder;
    private LocationCache locationCache;
    private TeleportManager teleportManager;
    private EffectManager effectManager;
    private ChatInputManager chatInputManager;
    private ZoneManager zoneManager;

    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        // Mesajlar placeholder kancasını kullandığı için önce oluşturulur
        this.itemsAdderHook = new ItemsAdderHook(this);
        this.economyHook = new EconomyHook(this);
        this.placeholderHook = new PlaceholderHook(this);

        this.messages = new Messages(this);
        this.messages.reload();

        this.dataStore = new DataStore(this);
        this.dataStore.load();

        this.effectManager = new EffectManager(this);
        this.safetyChecker = new SafetyChecker(this);
        this.locationFinder = new LocationFinder(this);
        this.locationCache = new LocationCache(this);
        this.teleportManager = new TeleportManager(this);
        this.chatInputManager = new ChatInputManager(this);

        this.zoneManager = new ZoneManager(this);
        this.zoneManager.load();

        this.bedrockHook = new BedrockHook(this);
        this.bedrockHook.setup();
        this.bedrockMenu = new BedrockMenu(this);

        this.economyHook.setup();
        this.itemsAdderHook.setup();
        this.placeholderHook.setup();

        registerListeners();
        registerCommands();

        this.locationCache.start();
        this.zoneManager.start();
        startAutosave();

        banner();
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) teleportManager.cancelAll();
        if (zoneManager != null) {
            zoneManager.stop();
            zoneManager.save();
        }
        if (locationCache != null) locationCache.stop();
        if (autosaveTask != null) autosaveTask.cancel();
        if (placeholderHook != null) placeholderHook.shutdown();
        if (dataStore != null) dataStore.save();

        getLogger().info("UltraRTP kapatıldı.");
        instance = null;
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(chatInputManager, this);
        Bukkit.getPluginManager().registerEvents(new ZoneWandListener(this), this);
    }

    private void registerCommands() {
        RTPCommand rtp = new RTPCommand(this);
        PluginCommand rtpCommand = getCommand("rtp");
        if (rtpCommand != null) {
            rtpCommand.setExecutor(rtp);
            rtpCommand.setTabCompleter(rtp);
        }

        RTPAdminCommand admin = new RTPAdminCommand(this);
        PluginCommand adminCommand = getCommand("rtpadmin");
        if (adminCommand != null) {
            adminCommand.setExecutor(admin);
            adminCommand.setTabCompleter(admin);
        }

        RTPZoneCommand zone = new RTPZoneCommand(this);
        PluginCommand zoneCommand = getCommand("rtpzone");
        if (zoneCommand != null) {
            zoneCommand.setExecutor(zone);
            zoneCommand.setTabCompleter(zone);
        }
    }

    private void startAutosave() {
        if (autosaveTask != null) autosaveTask.cancel();
        long interval = configManager.autosaveSeconds() * 20L;
        this.autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this, () -> dataStore.saveIfDirty(), interval, interval);
    }

    /**
     * config.yml, dil dosyaları ve tüm entegrasyonları yeniden yükler.
     */
    public void reloadEverything() {
        configManager.reload();
        messages.reload();

        economyHook.setup();
        itemsAdderHook.setup();
        placeholderHook.setup();

        locationCache.restart();
        zoneManager.load();
        zoneManager.restart();
        startAutosave();
    }

    private void banner() {
        String version = getPluginMeta().getVersion();
        getLogger().info("");
        getLogger().info("  UltraRTP v" + version + "  |  github.com/yefeblgn");
        getLogger().info("  Dil: " + configManager.language()
                + "  |  Bölge: " + configManager.regions().size()
                + "  |  RTP Zone: " + zoneManager.zones().size());
        getLogger().info("  Vault: " + yesNo(economyHook.isAvailable())
                + "  |  PlaceholderAPI: " + yesNo(placeholderHook.isAvailable())
                + "  |  ItemsAdder: " + yesNo(itemsAdderHook.isAvailable()));
        getLogger().info("");
    }

    private String yesNo(boolean value) {
        return value ? "✔" : "✖";
    }

    // ------------------------------------------------------------ erişimciler

    public static UltraRTP instance() {
        return instance;
    }

    public ConfigManager config() {
        return configManager;
    }

    public Messages messages() {
        return messages;
    }

    public DataStore data() {
        return dataStore;
    }

    public EconomyHook economy() {
        return economyHook;
    }

    public PlaceholderHook placeholders() {
        return placeholderHook;
    }

    public ItemsAdderHook itemsAdder() {
        return itemsAdderHook;
    }

    public BedrockHook bedrock() {
        return bedrockHook;
    }

    public BedrockMenu bedrockMenu() {
        return bedrockMenu;
    }

    /**
     * Menuyu acar: Bedrock oyuncusuna form, digerlerine sandik menusu.
     * Form gonderilemezse sandik menusune duser, oyuncu bos ekranla kalmaz.
     */
    public void openMenu(org.bukkit.entity.Player player) {
        boolean useForm = getConfig().getBoolean("gui.bedrock-form", true)
                && bedrockMenu != null
                && bedrockMenu.isAvailable()
                && bedrockHook.isBedrock(player);

        if (useForm && bedrockMenu.openMain(player)) {
            return;
        }
        new com.yefeblgn.ultrartp.gui.MainMenu(this, player).open();
    }

    public SafetyChecker safety() {
        return safetyChecker;
    }

    public LocationFinder finder() {
        return locationFinder;
    }

    public LocationCache cache() {
        return locationCache;
    }

    public TeleportManager teleports() {
        return teleportManager;
    }

    public EffectManager effects() {
        return effectManager;
    }

    public ChatInputManager chatInput() {
        return chatInputManager;
    }

    public ZoneManager zones() {
        return zoneManager;
    }
}
