/*
 * This file is part of FlightControl, which is licensed under the MIT License.
 *
 * Copyright (c) 2020 Spazzinq
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.spazzinq.flightcontrol;

import lombok.Getter;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.spazzinq.flightcontrol.api.APIManager;
import org.spazzinq.flightcontrol.command.*;
import org.spazzinq.flightcontrol.manager.*;
import org.spazzinq.flightcontrol.multiversion.ParticleManager;
import org.spazzinq.flightcontrol.multiversion.current.ParticleManager13;
import org.spazzinq.flightcontrol.multiversion.old.ParticleManager8;
import org.spazzinq.flightcontrol.object.Category;
import org.spazzinq.flightcontrol.object.FlyPermission;
import org.spazzinq.flightcontrol.object.VersionType;
import org.spazzinq.flightcontrol.util.PlayerUtil;

import java.io.File;
import java.util.HashSet;
import java.util.UUID;

import static org.spazzinq.flightcontrol.util.MessageUtil.msg;

public final class FlightControl extends org.bukkit.plugin.java.JavaPlugin {
    @Getter private final APIManager apiManager = APIManager.getInstance();
    // Storage management
    @Getter private final File storageFolder = new File(getDataFolder() + File.separator + "data");
    @Getter private CategoryManager categoryManager;
    @Getter private ConfManager confManager;
    @Getter private LangManager langManager;
    @Getter private UpdateManager updateManager;
    // Multi-version management
    @Getter private HookManager hookManager;
    @Getter private ParticleManager particleManager;
    // In-game management
    @Getter private FlightManager flightManager;
    @Getter private PlayerManager playerManager;
    @Getter private StatusManager statusManager;
    @Getter private FactionsManager factionsManager;
    @Getter private TrailManager trailManager;

    private final HashSet<String> permissionSuffixCache = new HashSet<>();

    private final PluginManager pm = Bukkit.getPluginManager();
    public static final UUID spazzinqUUID = UUID.fromString("043f10b6-3d13-4340-a9eb-49cbc560f48c");

    public void onEnable() {
        // Create storage folder
        //noinspection ResultOfMethodCallIgnored
        storageFolder.mkdirs();

        registerManagers();
        new Listener(this);
        registerCommands();

        // Ensure all hooks load before managers do
        hookManager.load();
        reloadManagers();

        if (updateManager.getVersion().getVersionType() == VersionType.BETA) {
            getLogger().warning(" \n  _       _       _       _       _       _\n" +
                    " ( )     ( )     ( )     ( )     ( )     ( )\n" +
                    "  X       X       X       X       X       X\n" +
                    "-' `-. ,-' `-. ,-' `-. ,-' `-. ,-' `-. ,-' `-. ,\n" +
                    "      X       X       X       X       X       X\n" +
                    "     (_)     (_)     (_)     (_)     (_)     (_)\n" +
                    " \nVersion " + updateManager.getVersion() + " is currently unstable and should not be run on a " +
                    "production server.\n" +
                    "Thanks for being a FlightControl beta tester!\n \n" +
                    "  _       _       _       _       _       _\n" +
                    " ( )     ( )     ( )     ( )     ( )     ( )\n" +
                    "  X       X       X       X       X       X\n" +
                    "-' `-. ,-' `-. ,-' `-. ,-' `-. ,-' `-. ,-' `-. ,\n" +
                    "      X       X       X       X       X       X\n" +
                    "     (_)     (_)     (_)     (_)     (_)     (_)\n");
        }

        // Update check
        if (confManager.isAutoUpdate()) {
            new BukkitRunnable() {
                @Override public void run() {
                    updateManager.installUpdate(Bukkit.getConsoleSender(), true);
                }
            }.runTaskAsynchronously(this);
        } else {
            new BukkitRunnable() {
                @Override public void run() {
                    if (updateManager.updateExists()) {
                        getLogger().info("Yay! Version " + updateManager.getNewVersion() + " is available for update." +
                                " Perform \"/fc update\" to update and visit https://www.spigotmc" +
                                ".org/resources/55168/ to view the feature changes (the configs automatically update)" +
                                ".");
                    }
                }
            }.runTaskLaterAsynchronously(this, 70);
        }

        // Start file watching service
        new PathWatcher(this, getDataFolder().toPath()).runTaskTimer(this, 0, 10);
        // Start bStats
        new MetricsLite(this);
    }

    private void registerManagers() {
        categoryManager = new CategoryManager(this);
        confManager = new ConfManager(this);
        langManager = new LangManager(this);
        updateManager = new UpdateManager(getDescription().getVersion());

        boolean is1_13 = getServer().getBukkitVersion().contains("1.13")
                || getServer().getBukkitVersion().contains("1.14")
                || getServer().getBukkitVersion().contains("1.15");
        hookManager = new HookManager(this, is1_13);
        particleManager = is1_13 ? new ParticleManager13() : new ParticleManager8();

        flightManager = new FlightManager(this);
        playerManager = new PlayerManager(this);
        statusManager = new StatusManager(this);
        factionsManager = new FactionsManager(this);
        trailManager = new TrailManager(this);
    }

    private void registerCommands() {
        getCommand("tempfly").setExecutor(new TempFlyCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("flightcontrol").setExecutor(new FlightControlCommand(this));
        getCommand("toggletrail").setExecutor(new ToggleTrailCommand(this));
        getCommand("flyspeed").setExecutor(new FlySpeedCommand(this));
    }

    public void reloadManagers() {
        // Prevent permission auto-granting from "*" permission
        for (World w : Bukkit.getWorlds()) {
            String worldName = w.getName();
            registerDefaultPerms(worldName);

            for (String regionName : getHookManager().getWorldGuardHook().getRegionNames(w)) {
                registerDefaultPerms(worldName + "." + regionName);
            }
        }

        categoryManager.reloadCategories();
        confManager.loadConf();
        langManager.loadLang();
        // At end to allow for any necessary migration
        confManager.updateConfig();
        langManager.updateLang();

        playerManager.loadPlayerData();

        checkPlayers();
    }

    public void checkPlayers() {
        trailManager.removeEnabledTrails();
        for (Player p : Bukkit.getOnlinePlayers()) {
            flightManager.check(p);
            if (p.isFlying()) {
                trailManager.trailCheck(p);
            }
        }
    }

    public void debug(CommandSender s, Player p) {
        Location l = p.getLocation();
        World world = l.getWorld();
        String worldName = world.getName(),
                regionName = getHookManager().getWorldGuardHook().getRegionName(l);
        Category category = categoryManager.getCategory(p);

        // config options (settings) and permissions that act upon the same function are listed as
        // setting boolean (space) permission boolean
        msg(s, "&a&lFlightControl &f" + getDescription().getVersion() +
                "\n&eTarget &7» &f" + p.getName() +
                "\n&eCategory &7» &f" + category.getName() +
                (hookManager.getWorldGuardHook().isHooked() ? "\n&eW.RG &7» &f" + worldName + "." + regionName : "") +
                (hookManager.getFactionsHook().isHooked() ? "\n&eFac &7» &f" + category.getFactions() : "") +
                "\n&eWRLDs &7» &f" + category.getWorlds() +
                (hookManager.getWorldGuardHook().isHooked() ? "\n&eRGs &7» &f" + category.getRegions() : "") +
                ("\n&eBypass &7» &f" + (PlayerUtil.hasPermission(p, FlyPermission.BYPASS)
                        || p.getGameMode() == GameMode.SPECTATOR
                        || confManager.isVanishBypass() && hookManager.getVanishHook().vanished(p))).replaceAll("true"
                        , "&atrue"));

        statusManager.evalFlight(p, l, true, s);
    }

    public void registerDefaultPerms(String suffix) {
        if (!permissionSuffixCache.contains(suffix)) {
            registerPerm("flightcontrol.fly." + suffix);
            registerPerm("flightcontrol.nofly." + suffix);

            permissionSuffixCache.add(suffix);
        }
    }

    private void registerPerm(String permString) {
        Permission perm = pm.getPermission(permString);

        if (perm == null) {
            pm.addPermission(new Permission(permString, PermissionDefault.FALSE));
        } else if (perm.getDefault() != PermissionDefault.FALSE) {
            perm.setDefault(PermissionDefault.FALSE);
        }
    }
}
