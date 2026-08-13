package com.yefeblgn.ultrartp.config;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.EffectSet;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Registries;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * config.yml okuma / yazma katmanı.
 * Sık kullanılan değerler {@link #reload()} sırasında önbelleğe alınır.
 */
public final class ConfigManager {

    private final UltraRTP plugin;

    private FileConfiguration config;

    // general
    private String language;
    private int maxAttempts;
    private int attemptDelayTicks;
    private int maxConcurrentSearches;
    private boolean debug;
    private int autosaveSeconds;

    // hooks
    private boolean vaultEnabled;
    private boolean papiEnabled;
    private boolean papiParseMessages;
    private boolean itemsAdderEnabled;
    private Material itemsAdderFallback;

    // cache
    private boolean cacheEnabled;
    private int cacheSize;
    private int cacheRefillSeconds;
    private int cacheMaxPerCycle;
    private boolean cachePauseWhenEmpty;

    // warmup
    private boolean warmupEnabled;
    private int warmupSeconds;
    private boolean cancelOnMove;
    private double moveThreshold;
    private boolean cancelOnDamage;

    // cooldown
    private boolean cooldownEnabled;
    private int cooldownSeconds;
    private boolean cooldownShared;
    private boolean cooldownPersist;
    private Map<String, Integer> cooldownGroups = new LinkedHashMap<>();

    // cost
    private boolean costEnabled;
    private double costAmount;
    private boolean refundOnFail;

    // misc
    private int invulnerabilitySeconds;
    private boolean resetFallDamage;
    private boolean dismountBefore;
    private boolean backEnabled;
    private double backCost;
    private int backExpireSeconds;

    // safety
    private Set<Material> unsafeBlocks = EnumSet.noneOf(Material.class);
    private Set<Material> unsafeGround = EnumSet.noneOf(Material.class);
    private boolean allowWater;
    private boolean avoidUnderRoof;
    private int roofScanLimit;
    private int requiredAirAbove;
    private int minY;
    private int maxY;
    private int dangerScanRadius;
    private boolean respectWorldBorder;
    private Set<String> blockedBiomes = new HashSet<>();

    // regions
    private Map<String, Region> regions = new LinkedHashMap<>();
    private String defaultRegion;

    // effects
    private Map<String, EffectSet> effects = new HashMap<>();
    private String countdownMode;
    private String bossbarColor;
    private String bossbarOverlay;

    // zone (/rtpzone)
    private Material zoneWandMaterial;
    private List<Integer> zoneWarnSeconds = List.of(5, 3);
    private boolean zoneBlastEffect;

    // gui
    private int guiRows;
    private boolean guiOpenOnPlainCommand;
    private boolean guiFillerEnabled;
    private Material guiFillerMaterial;
    private int guiInfoSlot;
    private int guiRandomSlot;
    private int guiCloseSlot;
    private int guiBackSlot;
    private Map<String, String> guiIcons = new HashMap<>();

    public ConfigManager(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.language = config.getString("general.language", "tr").toLowerCase(Locale.ROOT);
        this.maxAttempts = clamp(config.getInt("general.max-attempts", 45), 1, 500);
        this.attemptDelayTicks = clamp(config.getInt("general.attempt-delay-ticks", 1), 0, 40);
        this.maxConcurrentSearches = clamp(config.getInt("general.max-concurrent-searches", 4), 1, 64);
        this.debug = config.getBoolean("general.debug", false);
        this.autosaveSeconds = clamp(config.getInt("general.autosave-seconds", 300), 30, 7200);

        this.vaultEnabled = config.getBoolean("hooks.vault.enabled", true);
        this.papiEnabled = config.getBoolean("hooks.placeholderapi.enabled", true);
        this.papiParseMessages = config.getBoolean("hooks.placeholderapi.parse-in-messages", true);
        this.itemsAdderEnabled = config.getBoolean("hooks.itemsadder.enabled", true);
        this.itemsAdderFallback = Registries.material(config.getString("hooks.itemsadder.fallback-material"), Material.ENDER_PEARL);

        this.cacheEnabled = config.getBoolean("cache.enabled", true);
        this.cacheSize = clamp(config.getInt("cache.size-per-region", 8), 1, 100);
        this.cacheRefillSeconds = clamp(config.getInt("cache.refill-interval-seconds", 30), 5, 3600);
        this.cacheMaxPerCycle = clamp(config.getInt("cache.max-generate-per-cycle", 2), 1, 20);
        this.cachePauseWhenEmpty = config.getBoolean("cache.pause-when-empty", true);

        this.warmupEnabled = config.getBoolean("teleport.warmup.enabled", true);
        this.warmupSeconds = clamp(config.getInt("teleport.warmup.seconds", 5), 1, 300);
        this.cancelOnMove = config.getBoolean("teleport.warmup.cancel-on-move", true);
        this.moveThreshold = Math.max(0.05D, config.getDouble("teleport.warmup.move-threshold", 0.6D));
        this.cancelOnDamage = config.getBoolean("teleport.warmup.cancel-on-damage", true);

        this.cooldownEnabled = config.getBoolean("teleport.cooldown.enabled", true);
        this.cooldownSeconds = clamp(config.getInt("teleport.cooldown.seconds", 120), 0, 604800);
        this.cooldownShared = config.getBoolean("teleport.cooldown.shared", true);
        this.cooldownPersist = config.getBoolean("teleport.cooldown.persist", true);
        this.cooldownGroups = new LinkedHashMap<>();
        ConfigurationSection groups = config.getConfigurationSection("teleport.cooldown.groups");
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                cooldownGroups.put(key.toLowerCase(Locale.ROOT), groups.getInt(key));
            }
        }

        this.costEnabled = config.getBoolean("teleport.cost.enabled", true);
        this.costAmount = Math.max(0.0D, config.getDouble("teleport.cost.amount", 250.0D));
        this.refundOnFail = config.getBoolean("teleport.cost.refund-on-fail", true);

        this.invulnerabilitySeconds = clamp(config.getInt("teleport.invulnerability-seconds", 5), 0, 300);
        this.resetFallDamage = config.getBoolean("teleport.reset-fall-damage", true);
        this.dismountBefore = config.getBoolean("teleport.dismount-before", true);
        this.backEnabled = config.getBoolean("teleport.back.enabled", true);
        this.backCost = Math.max(0.0D, config.getDouble("teleport.back.cost", 100.0D));
        this.backExpireSeconds = Math.max(0, config.getInt("teleport.back.expire-seconds", 300));

        this.unsafeBlocks = materialSet(config.getStringList("safety.unsafe-blocks"));
        this.unsafeGround = materialSet(config.getStringList("safety.unsafe-ground"));
        this.allowWater = config.getBoolean("safety.allow-water", false);
        this.avoidUnderRoof = config.getBoolean("safety.avoid-under-roof", true);
        this.roofScanLimit = clamp(config.getInt("safety.roof-scan-limit", 45), 1, 320);
        this.requiredAirAbove = clamp(config.getInt("safety.required-air-above", 2), 1, 10);
        this.minY = config.getInt("safety.min-y", -48);
        this.maxY = config.getInt("safety.max-y", 250);
        this.dangerScanRadius = clamp(config.getInt("safety.danger-scan-radius", 2), 0, 6);
        this.respectWorldBorder = config.getBoolean("safety.respect-world-border", true);
        this.blockedBiomes = new HashSet<>();
        for (String biome : config.getStringList("safety.blocked-biomes")) {
            blockedBiomes.add(Region.normalizeBiome(biome));
        }

        this.regions = new LinkedHashMap<>();
        ConfigurationSection regionSection = config.getConfigurationSection("regions");
        if (regionSection != null) {
            for (String key : regionSection.getKeys(false)) {
                Region region = Region.load(key.toLowerCase(Locale.ROOT), regionSection.getConfigurationSection(key));
                if (region != null) {
                    regions.put(region.id(), region);
                }
            }
        }
        this.defaultRegion = config.getString("default-region", "overworld").toLowerCase(Locale.ROOT);

        this.zoneWandMaterial = Registries.material(config.getString("zone.wand-material"), Material.BLAZE_ROD);
        this.zoneBlastEffect = config.getBoolean("zone.blast-effect", true);
        List<Integer> warn = new ArrayList<>();
        for (int second : config.getIntegerList("zone.warn-seconds")) {
            if (second > 0 && !warn.contains(second)) warn.add(second);
        }
        this.zoneWarnSeconds = warn.isEmpty() ? List.of(5, 3) : List.copyOf(warn);

        this.effects = new HashMap<>();
        for (String key : List.of("warmup", "searching", "departure", "arrival", "cancel")) {
            effects.put(key, EffectSet.load(config.getConfigurationSection("effects." + key)));
        }
        // Eski config.yml dosyalarında bu bölümler yoktur -> gömülü varsayılana düş
        effects.put("zone-warning", effectOr("zone-warning", EffectSet.zoneWarningDefault()));
        effects.put("zone-blast", effectOr("zone-blast", EffectSet.zoneBlastDefault()));
        this.countdownMode = config.getString("effects.countdown.mode", "ALL").toUpperCase(Locale.ROOT);
        this.bossbarColor = config.getString("effects.countdown.bossbar-color", "PURPLE").toUpperCase(Locale.ROOT);
        this.bossbarOverlay = config.getString("effects.countdown.bossbar-overlay", "NOTCHED_10").toUpperCase(Locale.ROOT);

        this.guiRows = clamp(config.getInt("gui.main.rows", 5), 1, 6);
        this.guiOpenOnPlainCommand = config.getBoolean("gui.main.open-on-plain-command", true);
        this.guiFillerEnabled = config.getBoolean("gui.main.filler.enabled", true);
        this.guiFillerMaterial = Registries.material(config.getString("gui.main.filler.material"), Material.BLACK_STAINED_GLASS_PANE);
        this.guiInfoSlot = config.getInt("gui.main.info-slot", 4);
        this.guiRandomSlot = config.getInt("gui.main.random-slot", 40);
        this.guiCloseSlot = config.getInt("gui.main.close-slot", 44);
        this.guiBackSlot = config.getInt("gui.main.back-slot", 36);
        this.guiIcons = new HashMap<>();
        ConfigurationSection icons = config.getConfigurationSection("gui.main.icons");
        if (icons != null) {
            for (String key : icons.getKeys(false)) {
                guiIcons.put(key.toLowerCase(Locale.ROOT), icons.getString(key));
            }
        }
    }

    private EffectSet effectOr(String key, EffectSet fallback) {
        ConfigurationSection section = config.getConfigurationSection("effects." + key);
        return section == null ? fallback : EffectSet.load(section);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Set<Material> materialSet(List<String> names) {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) set.add(material);
        }
        return set;
    }

    // ---------------------------------------------------------------- yazma

    public FileConfiguration raw() {
        return config;
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public boolean save() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("config.yml kaydedilemedi: " + ex.getMessage());
            return false;
        }
    }

    /** Değeri yazar, diske kaydeder ve önbelleği tazeler. */
    public void setAndSave(String path, Object value) {
        set(path, value);
        save();
        reload();
    }

    // ---------------------------------------------------------------- getter

    public String language() {
        return language;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public int attemptDelayTicks() {
        return attemptDelayTicks;
    }

    public int maxConcurrentSearches() {
        return maxConcurrentSearches;
    }

    public boolean debug() {
        return debug;
    }

    public int autosaveSeconds() {
        return autosaveSeconds;
    }

    public boolean vaultEnabled() {
        return vaultEnabled;
    }

    public boolean placeholderApiEnabled() {
        return papiEnabled;
    }

    public boolean placeholderParseMessages() {
        return papiParseMessages;
    }

    public boolean itemsAdderEnabled() {
        return itemsAdderEnabled;
    }

    public Material itemsAdderFallback() {
        return itemsAdderFallback;
    }

    public boolean cacheEnabled() {
        return cacheEnabled;
    }

    public int cacheSize() {
        return cacheSize;
    }

    public int cacheRefillSeconds() {
        return cacheRefillSeconds;
    }

    public int cacheMaxPerCycle() {
        return cacheMaxPerCycle;
    }

    public boolean cachePauseWhenEmpty() {
        return cachePauseWhenEmpty;
    }

    public boolean warmupEnabled() {
        return warmupEnabled;
    }

    public int warmupSeconds() {
        return warmupSeconds;
    }

    public boolean cancelOnMove() {
        return cancelOnMove;
    }

    public double moveThreshold() {
        return moveThreshold;
    }

    public boolean cancelOnDamage() {
        return cancelOnDamage;
    }

    public boolean cooldownEnabled() {
        return cooldownEnabled;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean cooldownShared() {
        return cooldownShared;
    }

    public boolean cooldownPersist() {
        return cooldownPersist;
    }

    public Map<String, Integer> cooldownGroups() {
        return Collections.unmodifiableMap(cooldownGroups);
    }

    public boolean costEnabled() {
        return costEnabled;
    }

    public double costAmount() {
        return costAmount;
    }

    public boolean refundOnFail() {
        return refundOnFail;
    }

    public int invulnerabilitySeconds() {
        return invulnerabilitySeconds;
    }

    public boolean resetFallDamage() {
        return resetFallDamage;
    }

    public boolean dismountBefore() {
        return dismountBefore;
    }

    public boolean backEnabled() {
        return backEnabled;
    }

    public double backCost() {
        return backCost;
    }

    public int backExpireSeconds() {
        return backExpireSeconds;
    }

    public Set<Material> unsafeBlocks() {
        return unsafeBlocks;
    }

    public Set<Material> unsafeGround() {
        return unsafeGround;
    }

    public boolean allowWater() {
        return allowWater;
    }

    public boolean avoidUnderRoof() {
        return avoidUnderRoof;
    }

    public int roofScanLimit() {
        return roofScanLimit;
    }

    public int requiredAirAbove() {
        return requiredAirAbove;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int dangerScanRadius() {
        return dangerScanRadius;
    }

    public boolean respectWorldBorder() {
        return respectWorldBorder;
    }

    public Set<String> blockedBiomes() {
        return blockedBiomes;
    }

    public Collection<Region> regions() {
        return regions.values();
    }

    public Region region(String id) {
        return id == null ? null : regions.get(id.toLowerCase(Locale.ROOT));
    }

    public String defaultRegionId() {
        return defaultRegion;
    }

    public EffectSet effect(String key) {
        return effects.getOrDefault(key, EffectSet.empty());
    }

    public String countdownMode() {
        return countdownMode;
    }

    public String bossbarColor() {
        return bossbarColor;
    }

    public String bossbarOverlay() {
        return bossbarOverlay;
    }

    public Material zoneWandMaterial() {
        return zoneWandMaterial;
    }

    public List<Integer> zoneWarnSeconds() {
        return zoneWarnSeconds;
    }

    public boolean zoneBlastEffect() {
        return zoneBlastEffect;
    }

    public int guiRows() {
        return guiRows;
    }

    public boolean guiOpenOnPlainCommand() {
        return guiOpenOnPlainCommand;
    }

    public boolean guiFillerEnabled() {
        return guiFillerEnabled;
    }

    public Material guiFillerMaterial() {
        return guiFillerMaterial;
    }

    public int guiInfoSlot() {
        return guiInfoSlot;
    }

    public int guiRandomSlot() {
        return guiRandomSlot;
    }

    public int guiCloseSlot() {
        return guiCloseSlot;
    }

    public int guiBackSlot() {
        return guiBackSlot;
    }

    public String guiIcon(String key, String fallback) {
        return guiIcons.getOrDefault(key, fallback);
    }
}
