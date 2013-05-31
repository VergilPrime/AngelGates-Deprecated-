package me.entityreborn.AngelGates;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.entityreborn.AngelGates.Networks.Network;
import me.entityreborn.AngelGates.events.AngelGatesAccessEvent;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AngelGate - A portal plugin for Bukkit Copyright (C) 2011 Shaun (sturmeh)
 * Copyright (C) 2011 Dinnerbone Copyright (C) 2011, 2012 Steven "Drakia" Scott
 * <Contact@TheDgtl.net>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
public class AngelGates extends JavaPlugin {
    public static Permission permissions = null;
    public static Logger log;
    public static Plugin self;
    public static Server server;
    private static LangLoader lang;
    private static String portalFolder;
    private static String gateFolder;
    private static String langFolder;
    private static String defNetwork = "central";
    public static boolean destroyExplosion = false;
    public static int defaultNetsPerPlayer = 0;
    private static String langName = "en";
    private static int activeTime = 10;
    private static int openTime = 10;
    private static long stayOpenTime = 2;
    public static boolean rememberLastDest = false;
    public static boolean handleVehicles = true;
    public static boolean sortLists = false;
    public static boolean protectEntrance = false;
    public static ChatColor signColor;
    // Temp workaround for snowmen, don't check gate entrance
    public static boolean ignoreEntrance = false;
    // Used for debug
    public static boolean debug = false;
    public static ConcurrentLinkedQueue<Portal> openList = new ConcurrentLinkedQueue<Portal>();
    public static ConcurrentLinkedQueue<Portal> activeList = new ConcurrentLinkedQueue<Portal>();
    // Used for populating gate open/closed material.
    public static Queue<BloxPopulator> blockPopulatorQueue = new LinkedList<BloxPopulator>();
    
    public static void debug(String rout, String msg) {
        if (AngelGates.debug) {
            log.info("[AngelGate::" + rout + "] " + msg);
        } else {
            log.log(Level.FINEST, "[AngelGate::" + rout + "] " + msg);
        }
    }
    
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
        
        if (permissionProvider != null) {
            permissions = permissionProvider.getProvider();
        }
        
        return permissions != null;
    }

    public static void sendMessage(CommandSender player, String message) {
        sendMessage(player, message, true);
    }

    public static void sendMessage(CommandSender player, String message, boolean error) {
        if (message.isEmpty()) {
            return;
        }
        message = message.replaceAll("(&([a-f0-9]))", "\u00A7$2");
        if (error) {
            player.sendMessage(ChatColor.RED + AngelGates.getString("prefix") + ChatColor.WHITE + message);
        } else {
            player.sendMessage(ChatColor.GREEN + AngelGates.getString("prefix") + ChatColor.WHITE + message);
        }
    }

    public static void setLine(Sign sign, int index, String text) {
        sign.setLine(index, AngelGates.signColor + text);
    }

    public static String getSaveLocation() {
        return portalFolder;
    }

    public static String getGateFolder() {
        return gateFolder;
    }

    public static String getDefaultNetwork() {
        return defNetwork;
    }

    public static String getString(String name) {
        return lang.getString(name);
    }

    public static void openPortal(Player player, Portal portal) {
        Portal destination = portal.getDestination();

        // Invalid destination
        if ((destination == null) || (destination == portal)) {
            AngelGates.sendMessage(player, AngelGates.getString("invalidMsg"));
            return;
        }

        // Gate is already open
        if (portal.isOpen()) {
            portal.close(player, false);
            return;
        }

        // Destination blocked
        if (destination.isOpen()) {
            AngelGates.sendMessage(player, AngelGates.getString("blockMsg"));
            return;
        }

        // Open gate
        portal.open(player, false);
    }

    /*
     * Check whether the player has the given permissions.
     */
    public static boolean hasPerm(CommandSender sender, String perm) {
        if (permissions != null) {
            return permissions.has(sender, perm);
        }
        
        return sender.hasPermission(perm);
    }

    /*
     * Call the AngelGateAccessPortal event, used for other plugins to bypass Permissions checks
     */
    public static boolean canUsePortal(Player player, Portal portal, boolean deny) {
        AngelGatesAccessEvent event = new AngelGatesAccessEvent(player, portal, deny);
        AngelGates.server.getPluginManager().callEvent(event);

        if (event.getDeny()) {
            return false;
        }

        return true;
    }

    /*
     * Check if the player can create gates on {network}
     */
    public static boolean canCreate(Player player, String network) {
        // Check for general create
        if (hasPerm(player, "AngelGates.admin")
                || hasPerm(player, "AngelGates.admin.create")) {
            return true;
        }
        
        if (network.equalsIgnoreCase(defNetwork)) {
            sendMessage(player, AngelGates.getString("createNetRestricted"));
            return false;
        }

        if (Networks.has(network)) {
            return Networks.get(network).isMember(player.getName());
        }
        
        int limit = Networks.getNetworkLimit(player.getName());
        
        if (limit == -1) {
            return true;
        }

        if (Networks.getOwnedNetworks(network).size() < limit) {
            return true;
        }

        return false;
    }

    /*
     * Check if the player can destroy this gate
     */
    public static boolean canDestroy(Player player, Portal portal) {
        // Check for general destroy
        if (hasPerm(player, "AngelGates.admin")) {
            return true;
        }

        if (portal.getNetwork().isMember(player.getName())) {
            return true;
        }

        return false;
    }

    /*
     * Charge player for {action} if required, true on success, false if can't afford
     */
    public static boolean chargePlayer(Player player, String target, int cost) {
        // If cost is 0
        if (cost == 0) {
            return true;
        }
        // iConomy is disabled
        if (!EconomyHandler.useEconomy()) {
            return true;
        }
        // Charge player
        return EconomyHandler.chargePlayer(player.getName(), target, cost);
    }

    /*
     * Determine the cost of a gate
     */
    public static int getGateCost(Player player, Portal src, Portal dest) {
        // Not using iConomy
        if (!EconomyHandler.useEconomy()) {
            return 0;
        }
        // Cost is 0 if the player owns this gate and funds go to the owner
        if (src.getGate().getToOwner() && src.getOwner().equalsIgnoreCase(player.getName())) {
            return 0;
        }
        // Player gets free gate use
        if (hasPerm(player, "AngelGates.free") || hasPerm(player, "AngelGates.free.use")
                || hasPerm(player, "AngelGates.admin")) {
            return 0;
        }

        return src.getGate().getUseCost();
    }

    /*
     * Determine the cost to create the gate
     */
    public static int getCreateCost(Player player, Gate gate) {
        // Not using iConomy
        if (!EconomyHandler.useEconomy()) {
            return 0;
        }
        // Player gets free gate destruction
        if (hasPerm(player, "AngelGates.free") || hasPerm(player, "AngelGates.free.create")
                || hasPerm(player, "AngelGates.admin")) {
            return 0;
        }

        return gate.getCreateCost();
    }

    /*
     * Determine the cost to destroy the gate
     */
    public static int getDestroyCost(Player player, Gate gate) {
        // Not using iConomy
        if (!EconomyHandler.useEconomy()) {
            return 0;
        }
        // Player gets free gate destruction
        if (hasPerm(player, "AngelGates.free") || hasPerm(player, "AngelGates.free.destroy")
                || hasPerm(player, "AngelGates.admin")) {
            return 0;
        }

        return gate.getDestroyCost();
    }

    /*
     * Parse a given text string and replace the variables
     */
    public static String replaceVars(String format, String[] search, String[] replace) {
        if (search.length != replace.length) {
            return "";
        }
        for (int i = 0; i < search.length; i++) {
            format = format.replace(search[i], replace[i]);
        }
        return format;
    }

    static boolean canOpenNetwork(Player player, String networkName) {
        if (hasPerm(player, "AngelGates.admin")) {
            return true;
        }

        if (networkName.equalsIgnoreCase(defNetwork)
                || Networks.get(networkName).isMember(player.getName())) {
            return true;
        }

        return false;
    }
    private FileConfiguration newConfig;
    private PluginManager pm;

    @Override
    public void onDisable() {
        Portal.closeAllGates();
        Portal.clearGates();
        getServer().getScheduler().cancelTasks(this);
        Networks.save(getDataFolder().getPath());
    }

    @Override
    public void onEnable() {
        try {
            setupPermissions();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error while setting up perms!", ex);
        }
        
        PluginDescriptionFile pdfFile = this.getDescription();
        self = this;
        server = getServer();

        pm = getServer().getPluginManager();
        newConfig = this.getConfig();
        log = getLogger();

        // Set portalFile and gateFolder to the plugin folder as defaults.
        portalFolder = new File(getDataFolder(), "portals").getPath();
        gateFolder = new File(getDataFolder(), "gates").getPath();
        langFolder = new File(getDataFolder(), "lang").getPath();

        log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");

        // Register events before loading gates to stop weird things happening.
        pm.registerEvents(new BukkitListener(), this);

        this.loadConfig();

        // It is important to load languages here, as they are used during reloadGates()
        lang = new LangLoader(langFolder, langName);

        this.reloadGates();

        // Check to see if iConomy is loaded yet.
        if (EconomyHandler.setupEconomy(pm)) {
            if (EconomyHandler.economy != null) {
                log.info("Vault v" + EconomyHandler.vault.getDescription().getVersion() + " found");
            }
        }

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new SGThread(), 0L, 100L);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new BlockPopulatorThread(), 0L, 1L);

        // Enable Plugin Metrics
        try {
            MetricsLite ml = new MetricsLite(this);
            if (!ml.isOptOut()) {
                ml.start();
                log.info("Plugin metrics enabled.");
            } else {
                log.info("Plugin metrics not enabled.");
            }
        } catch (IOException ex) {
            log.warning("Error enabling plugin metrics: " + ex);
        }
    }

    public void loadConfig() {
        reloadConfig();
        newConfig = this.getConfig();
        // Copy default values if required
        newConfig.options().copyDefaults(true);

        // Load values into variables
        defNetwork = newConfig.getString("default-gate-network").trim();
        destroyExplosion = newConfig.getBoolean("destroy-explosion");
        defaultNetsPerPlayer = newConfig.getInt("nets-per-player");
        langName = newConfig.getString("language");
        rememberLastDest = newConfig.getBoolean("remember-destination");
        ignoreEntrance = newConfig.getBoolean("ignore-entrance");
        handleVehicles = newConfig.getBoolean("handle-vehicles");
        sortLists = newConfig.getBoolean("sort-lists");
        protectEntrance = newConfig.getBoolean("protect-entrance");
        // Sign color
        String sc = newConfig.getString("sign-color");
        try {
            signColor = ChatColor.valueOf(sc.toUpperCase());
        } catch (Exception ignore) {
            log.warning("You have specified an invalid color in your config.yml. Defaulting to BLACK");
            signColor = ChatColor.BLACK;
        }
        // Debug
        debug = newConfig.getBoolean("debug");
        // iConomy
        EconomyHandler.useEconomy = newConfig.getBoolean("use-economy");
        EconomyHandler.createCost = newConfig.getInt("create-cost");
        EconomyHandler.destroyCost = newConfig.getInt("destroy-cost");
        EconomyHandler.useCost = newConfig.getInt("use-cost");
        EconomyHandler.payOwner = newConfig.getBoolean("pay-owner");

        this.saveConfig();
    }

    public void reloadGates() {
        // Close all gates prior to reloading
        for (Portal p : openList) {
            p.close(true);
        }

        Gate.loadGates(gateFolder);
        // Replace nethergate.gate if it doesn't have an exit point.
        if (Gate.getGateByName("nethergate.gate") == null || Gate.getGateByName("nethergate.gate").getExit() == null) {
            Gate.populateDefaults(gateFolder);
        }
        log.info("Loaded " + Gate.getGateCount() + " gate layouts");
        for (World world : getServer().getWorlds()) {
            Portal.loadAllGates(world);
        }

        Networks.load(getDataFolder().getPath());
    }
    
    public boolean onCmdHelp(CommandSender sender, String[] args) {
        String which = (args.length != 0) ? args[0].toLowerCase() : "";
        
        sender.sendMessage(ChatColor.GOLD + "/ag reload");
        sender.sendMessage(ChatColor.WHITE + " - Reload the plugin configuration");
        
        sender.sendMessage(ChatColor.GOLD + "/ag info [player]");
        sender.sendMessage(ChatColor.WHITE + " - Provide info about yourself or another player");
        
        sender.sendMessage(ChatColor.GOLD + "/ag netinfo <network>");
        sender.sendMessage(ChatColor.WHITE + " - Provide info about <network>");
        
        sender.sendMessage(ChatColor.GOLD + "/ag setowner <network> <name>");
        sender.sendMessage(ChatColor.WHITE + " - Set the owner of <network> to <name>");
        
        sender.sendMessage(ChatColor.GOLD + "/ag setnetworks <name> <amount>");
        sender.sendMessage(ChatColor.WHITE + " - Set the networklimit of <player> to <amount>");
        sender.sendMessage(ChatColor.WHITE + "   Use -1 for infinite, and 0 for none");
        
        sender.sendMessage(ChatColor.GOLD + "/ag addnetworks <name> <amount>");
        sender.sendMessage(ChatColor.WHITE + " - Add to the networklimit of <player>");
        
        sender.sendMessage(ChatColor.GOLD + "/ag addmember <network> <name>");
        sender.sendMessage(ChatColor.WHITE + " - Add a user, group or town to <network>");
        
        sender.sendMessage(ChatColor.GOLD + "/ag remmember <network> <name>");
        sender.sendMessage(ChatColor.WHITE + " - Remove a user, group or town from <network>");
        
        sender.sendMessage(ChatColor.BLUE + "Prefix a name with g: to specify a group or");
        sender.sendMessage(ChatColor.WHITE + "t: to specify a town.");
        
        return true;
    }

    public boolean onCmdReload(CommandSender sender, String[] args) {
        if (!hasPerm(sender, "angelgates.commands")
                && !hasPerm(sender, "angelgates.commands.reload")) {
            sendMessage(sender, "Permission Denied");

            return true;
        }

        // Deactivate portals
        for (Portal p : activeList) {
            p.deactivate();
        }

        // Close portals
        for (Portal p : openList) {
            p.close(true);
        }
        // Clear all lists
        activeList.clear();
        openList.clear();

        Portal.clearGates();
        Gate.clearGates();

        // Reload data
        loadConfig();
        reloadGates();

        lang.setLang(langName);
        lang.reload();

        // Load iConomy support if enabled/clear if disabled
        if (EconomyHandler.useEconomy && EconomyHandler.economy == null) {
            if (EconomyHandler.setupEconomy(pm)) {
                if (EconomyHandler.economy != null) {
                    log.info("Vault v" + EconomyHandler.vault.getDescription().getVersion() + " found");
                }
            }
        }

        if (!EconomyHandler.useEconomy) {
            EconomyHandler.vault = null;
            EconomyHandler.economy = null;
        }

        sendMessage(sender, "AngelGate reloaded", false);

        return true;
    }
    
    private boolean onCmdAddMember(CommandSender sender, String[] args) {
        if (args.length == 3) {
            String net = args[1];
            String other = args[2];
            Network network = Networks.get(net);
            
            if (network == null) {
                sendMessage(sender, "Unknown network");
                
                return true;
            }
            
            if (!network.isOwner(sender.getName()) &&
                    !hasPerm(sender, "angelgates.commands") &&
                    !hasPerm(sender, "angelgates.commands.addmember")) {
                sendMessage(sender, "Permission Denied");
                
                return true;
            }
            
            if (!other.startsWith("g:") && !other.startsWith("t:") &&
                    Bukkit.getServer().getOfflinePlayer(other).getFirstPlayed() == 0) {
                sendMessage(sender, other + " has never joined this server!");
                
                return true;
            }
            
            if (network.isMember(other)) {
                sendMessage(sender, other + " is already a member of this network");
                
                return true;
            }
            
            network.addMember(other);
            sendMessage(sender, other + " has been added to the network!", false);
            
            return true;
        }
        
        return false;
    }
    
    private boolean onCmdRemMember(CommandSender sender, String[] args) {
        if (args.length == 3) {
            String net = args[1];
            String other = args[2];
            Network network = Networks.get(net);
            
            if (network == null) {
                sendMessage(sender, "Unknown network");
                
                return true;
            }
            
            if (!network.isOwner(sender.getName()) &&
                    !hasPerm(sender, "angelgates.commands") &&
                    !hasPerm(sender, "angelgates.commands.remmember")) {
                sendMessage(sender, "Permission denied", true);
                
                return true;
            }
            
            if (!network.isMember(other)) {
                sendMessage(sender, other + " is not a member of this network");
                
                return true;
            }
            
            network.removeMember(other);
            sendMessage(sender, other + " has been removed from the network!", false);
            
            return true;
        }
        
        return false;
    }
    
    private boolean onCmdSetOwner(CommandSender sender, String[] args) {
        if (args.length == 3) {
            String net = args[1];
            String other = args[2];
            Network network = Networks.get(net);
            
            if (network == null) {
                sendMessage(sender, "Unknown network");
                
                return true;
            }
            
            if (!network.isOwner(sender.getName()) &&
                    !hasPerm(sender, "angelgates.commands") &&
                    !hasPerm(sender, "angelgates.commands.setowner")) {
                sendMessage(sender, "Permission denied");
                
                return true;
            }
            
            if (!other.startsWith("g:") && !other.startsWith("t:") &&
                    Bukkit.getServer().getOfflinePlayer(other).getFirstPlayed() == 0) {
                sendMessage(sender, other + " has never joined this server");
                
                return true;
            }
            
            if (!network.isMember(other)) {
                sendMessage(sender, other + " must be a member of this network to become owner!");
                
                return true;
            }
            
            if (network.isOwner(sender.getName())) {
                sendMessage(sender, other + " has been set as network owner! You have been demoted to member.", false);
            } else {
                sendMessage(sender, other + " has been set as network owner! " + network.getOwner() + " has been demoted to member.", false);
            }
            
            network.setOwner(other);
            
            return true;
        }
        
        return false;
    }
    
    private boolean onCmdNetInfo(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String net = args[1];
            Network network = Networks.get(net);
            
            if (network == null) {
                sendMessage(sender, "Unknown network");
                
                return true;
            }
            
            sendMessage(sender, "Name: " + network.getName());
            sendMessage(sender, "Owner: " + network.getOwner());
            
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            
            for (String item : network.getMembers()){
               if (first)
                  first = false;
               else
                  sb.append(", ");
               sb.append(item);
            }
            
            sendMessage(sender, "Members: " + sb.toString());
        } 
        
        return true;
    }
    
    private boolean onCmdInfo(CommandSender sender, String[] args) {
        String name = sender.getName();
        
        if (!(sender instanceof Player) && args.length < 2) {
            sendMessage(sender, "You must specify a player's name");
            return true;
        }
        
        if (args.length >= 2) {
            name = args[1];
            
            if (!name.startsWith("g:") && !name.startsWith("t:") && 
                    Bukkit.getServer().getOfflinePlayer(name).getFirstPlayed() == 0) {
                sendMessage(sender, name + " has never joined this server");
                return true;
            }
        }
        
        Set<Network> owned = Networks.getOwnedNetworks(name);
        int limit = Networks.getNetworkLimit(name);
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Network item : owned){
           if (first) {
              first = false;
           } else {
              sb.append(", ");
           }
           
           sb.append(item.getName());
        }
        
        sendMessage(sender, "Owned networks: " + sb.toString());
        sendMessage(sender, owned.size() + " networks of " + (limit > -1 ? limit : "unlimited") + " owned.", false);
        
        return true;
    }
    
    private boolean onCmdSetNetworks(CommandSender sender, String[] args) {
        if (!hasPerm(sender, "angelgates.commands") &&
                !hasPerm(sender, "angelgates.commands.setnetworks")) {
            sendMessage(sender, "Permission denied");

            return true;
        }
        
        if (args.length != 3) {
            return false;
        }
        
        String other = args[1];
        
        if (!other.startsWith("g:") && !other.startsWith("t:") && 
                Bukkit.getServer().getOfflinePlayer(other).getFirstPlayed() == 0) {
            sendMessage(sender, other + " has never joined this server");

            return true;
        }
        
        String samount = args[2];
        int amount;
        
        try {
            amount = Integer.valueOf(samount);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "Must specifiy integer for amount for second argument");
            return true;
        }
        
        if (amount < -1) {
            sendMessage(sender, "Number must be -1 or more!");

            return true;
        }
        
        Networks.setNetworkLimit(other, amount);
        
        sendMessage(sender, "Network limit for " + other + " set to " + (amount != -1 ? amount : "infinite"), false);
        
        return true;
    }
    
    private boolean onCmdAddNetworks(CommandSender sender, String[] args) {
        if (!hasPerm(sender, "angelgates.commands") &&
                !hasPerm(sender, "angelgates.commands.addnetworks")) {
            sendMessage(sender, "Permission denied");

            return true;
        }
        
        if (args.length != 3) {
            return false;
        }
        
        String other = args[1];
        
        if (!other.startsWith("g:") && !other.startsWith("t:") && 
                Bukkit.getServer().getOfflinePlayer(other).getFirstPlayed() == 0) {
            sendMessage(sender, other + " has never joined this server");

            return true;
        }
        
        String samount = args[2];
        int amount;
        
        try {
            amount = Integer.valueOf(samount);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "Must specifiy integer for amount for second argument");
            return true;
        }
        
        if (amount < 1) {
            sendMessage(sender, "Number must be 1 or more!");

            return true;
        }
        
        Networks.addNetworkLimit(other, amount);
        
        int limit = Networks.getNetworkLimit(other);
        
        sendMessage(sender, "Network limit for " + other + " set to " + limit, false);
        
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName();
        
        if (cmd.equalsIgnoreCase("ag") && args.length > 0) {
            if (args[0].equalsIgnoreCase("info")) {
                return onCmdInfo(sender, args);
            }
            
            if (args[0].equalsIgnoreCase("netinfo")) {
                return onCmdNetInfo(sender, args);
            }

            if (args[0].equalsIgnoreCase("reload")) {
                return onCmdReload(sender, args);
            }

            if (args[0].equalsIgnoreCase("addmember")) {
                return onCmdAddMember(sender, args);
            }

            if (args[0].equalsIgnoreCase("remmember")) {
                return onCmdRemMember(sender, args);
            }

            if (args[0].equalsIgnoreCase("setowner")) {
                return onCmdSetOwner(sender, args);
            }

            if (args[0].equalsIgnoreCase("setnetworks")) {
                return onCmdSetNetworks(sender, args);
            }
            
            if (args[0].equalsIgnoreCase("addnetworks")) {
                return onCmdAddNetworks(sender, args);
            }

            return false;
        }

        return false;
    }

    private class BlockPopulatorThread implements Runnable {
        public void run() {
            long sTime = System.nanoTime();
            
            while (System.nanoTime() - sTime < 50000000) {
                BloxPopulator b = AngelGates.blockPopulatorQueue.poll();
                if (b == null) {
                    return;
                }
                b.getBlox().getBlock().setTypeId(b.getMat());
                b.getBlox().getBlock().setData(b.getData());
            }
        }
    }

    private class SGThread implements Runnable {
        public void run() {
            long time = System.currentTimeMillis() / 1000;
            
            // Close open portals
            for (Iterator<Portal> iter = AngelGates.openList.iterator(); iter.hasNext();) {
                Portal p = iter.next();
                
                if (!p.isOpen()) {
                    iter.remove();
                    continue;
                }
                
                if (p.getFirstEnteredTime() == 0 && time > p.getOpenTime() + AngelGates.openTime) {
                    p.close(true);
                    iter.remove();
                }
                
                if (p.getFirstEnteredTime() != 0 && time > p.getFirstEnteredTime() + AngelGates.stayOpenTime) {
                    p.close(true);
                    iter.remove();
                }
            }
            
            // Deactivate active portals
            for (Iterator<Portal> iter = AngelGates.activeList.iterator(); iter.hasNext();) {
                Portal p = iter.next();
                
                if (!p.isActive()) {
                    iter.remove();
                    continue;
                }
                
                if (time > p.getOpenTime() + AngelGates.activeTime) {
                    p.deactivate();
                    iter.remove();
                }
            }
        }
    }
}
