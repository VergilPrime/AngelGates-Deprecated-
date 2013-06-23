/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.AngelGates;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author import
 */
public class Networks {
    public static class Network {
        private String name;
        private String owner;
        private Set<String> members;
        private Map<String, Portal> portals;

        public Network(String name, String owner) {
            this.name = name;
            this.owner = owner.toLowerCase();
            
            members = new HashSet<String>();
            members.add(owner.toLowerCase());
            
            portals = new HashMap<String, Portal>();
        }
        
        public void registerPortal(Portal portal) {
            portals.put(portal.getName().toLowerCase(), portal);
            Networks.save();
        }
        
        public void unregisterPortal(Portal portal) {
            portals.remove(portal.getName().toLowerCase());
            
            if (portals.isEmpty()) {
                Networks.remove(name);
                Networks.save();
            }
        }
        
        public Portal getPortal(String name) {
            return portals.get(name.toLowerCase());
        }
        
        public void addMember(String name) {
            members.add(name.toLowerCase());
            Networks.save();
        }
        
        public void removeMember(String name) {
            members.remove(name.toLowerCase());
            Networks.save();
        }
        
        public boolean isMember(String name) {
            if (isOwner(name) || members.contains(name.toLowerCase())) {
                return true;
            }
            
            if (AngelGates.permissions != null) {
                Player player = Bukkit.getPlayer(name);
                
                if (player != null) {
                    for (String group : AngelGates.permissions.getPlayerGroups(player)) {
                        if (members.contains("g:" + group.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
            
            if (Towny.getTown(name) != null &&
                    members.contains("t:" + Towny.getTown(name).toLowerCase())) {
                return true;
            }
            
            if (members.contains("~everyone")) {
                return true;
            }
            
            return false;
        }
        
        public Set<String> getMembers() {
            return Collections.unmodifiableSet(members);
        }
        
        public String getOwner() {
            return owner;
        }
        
        public boolean isOwner(String name) {
            if (AngelGates.permissions != null) {
                Player player = Bukkit.getPlayer(name);
                
                if (player != null) {
                    for (String group : AngelGates.permissions.getPlayerGroups(player)) {
                        if (owner.equalsIgnoreCase("g:" + group.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
            
            if (Towny.getTown(name) != null &&
                    members.contains("t:" + Towny.getTown(name).toLowerCase()) && 
                    (Towny.isMayor(name) || Towny.isKing(name))) {
                return true;
            }
            
            return name.equalsIgnoreCase(owner);
        }
        
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name.toLowerCase();
        }

        void setOwner(String other) {
            owner = other.toLowerCase();
            
            for (String pname : Portal.getNetworkPortals(name).keySet()) {
                Portal p = Portal.getByName(pname, name);
                p.drawSign();
            }
            
            Networks.save();
        }
        
        public Map<String, Portal> getPortals() {
            return Collections.unmodifiableMap(portals);
        }
    }
    
    private static Map<String, Network> networks = new HashMap<String, Network>();
    private static Map<String, Integer> networkLimit = new HashMap<String, Integer>();
    private static File file;
    
    public static Map<String, Portal> getPortals(String network) {
        if (!has(network)) {
            return new HashMap<String, Portal>();
        }
        
        return get(network).getPortals();
    }
    
    public static int getNetworkLimit(String player) {
        if (!networkLimit.containsKey(player.toLowerCase())) {
            return AngelGates.defaultNetsPerPlayer;
        }
        
        return networkLimit.get(player.toLowerCase());
    }
    
    public static void setNetworkLimit(String player, int limit) {
        networkLimit.put(player.toLowerCase(), limit);
    }
    
    static void addNetworkLimit(String other, int amount) {
        int limit = getNetworkLimit(other.toLowerCase()) + amount;
        
        networkLimit.put(other.toLowerCase(), limit);
    }
    
    public static Set<Network> getOwnedNetworks(String player) {
        Set<Network> retn = new HashSet<Network>();
        
        for (Network net : networks.values()) {
            if (net.isOwner(player)) {
                retn.add(net);
            }
        }
        
        return Collections.unmodifiableSet(retn);
    }
    
    public static Network add(String name, String owner) {
        Network net = new Network(name, owner);
        networks.put(name.toLowerCase(), net);
        
        save();
        
        return net;
    }
    
    public static Network add(Network net) {
        networks.put(net.getName().toLowerCase(), net);
        
        save();
        
        return net;
    }
    
    public static Map<String, Network> get() {
        return Collections.unmodifiableMap(networks);
    }
    
    public static Network get(String net) {
        return networks.get(net.toLowerCase());
    }
    
    public static boolean has(String net) {
        return networks.containsKey(net.toLowerCase());
    }
    
    public static void remove(String networkName) {
        networks.remove(networkName.toLowerCase());
    }
    
    public static void clear() {
        networks = new HashMap<String, Network>();
        networkLimit = new HashMap<String, Integer>();
    }
    
    public static void load(String dir) {
        file = new File(dir, "networks.yml");
        load();
    }
    
    public static void load() {
        if (file == null) {
            return;
        }
        
        clear();
        
        YamlConfiguration yaml = new YamlConfiguration();
        
        try {
            yaml.load(file);
        } catch (FileNotFoundException fnf) {
            AngelGates.log.info("Could not load network database. File not found.");
            return;
        } catch (Exception ex) {
            AngelGates.log.log(Level.SEVERE, "Could not load network database!", ex);
            return;
        }
        
        if (yaml.contains("users") && yaml.isConfigurationSection("users")) {
            ConfigurationSection limits = yaml.getConfigurationSection("users");
        
            for(String user : limits.getKeys(false)) {
                if (!limits.isConfigurationSection(user)) {
                    continue;
                }

                ConfigurationSection sect = limits.getConfigurationSection(user);

                if (sect.isInt("netlimit")) {
                    networkLimit.put(user.toLowerCase(), sect.getInt("netlimit"));
                } else if (sect.contains("netlimit")) {
                    AngelGates.log.warning("'netlimit' for user '" + user + "' is malformed. Ignoring!");
                }
            }
        }
        
        if (yaml.contains("networks") && yaml.isConfigurationSection("networks")) {
            ConfigurationSection nets = yaml.getConfigurationSection("networks");

            for(String nkey : nets.getKeys(false)) {
                if (!nets.isConfigurationSection(nkey)) {
                    continue;
                }

                ConfigurationSection sect = nets.getConfigurationSection(nkey);

                if (!sect.isString("owner")) {
                    AngelGates.log.warning("'owner' for network '" + nkey + "' is malformed. Skipping network!");
                    continue;
                }

                String owner = sect.getString("owner");
                Network net = new Network(nkey, owner);

                if (sect.isList("members")) {
                    List<String> members = sect.getStringList("members");

                    for(String member : members) {
                        net.members.add(member);
                    }
                }

                networks.put(nkey.toLowerCase(), net);
            }
        }
    }
    
    public static void save() {
        if (file == null) {
            return;
        }
        
        YamlConfiguration yaml = new YamlConfiguration();
        
        ConfigurationSection netsect = yaml.createSection("networks");
        
        for (Network net: networks.values()) {
            ConfigurationSection sect = netsect.createSection(net.getName());
            
            sect.set("owner", net.getOwner());
            
            if (!net.getMembers().isEmpty()) {
                sect.set("members", net.getMembers().toArray());
            }
        }
        
        ConfigurationSection usersect = yaml.createSection("users");
        
        for (String user: networkLimit.keySet()) {
            ConfigurationSection sect = usersect.createSection(user);
            
            sect.set("netlimit", getNetworkLimit(user));
        }
        
        try {
            yaml.save(file);
        } catch (Exception ex) {
            AngelGates.log.log(Level.SEVERE, "Could not save network/userlimit database!", ex);
            return;
        }
    }
    
    public static void save(String dir) {
        file = new File(dir, "networks.yml");
        save();
    }
    
    public static void main(String[] args) {
        Networks.load(".");
        
        for (Network net : Networks.get().values()) {
            System.out.println(net + ", owned by " + net.getOwner());
            System.out.println("Members: " + net.getMembers());
        }
        
        Networks.add("Test1", "Me!");
        Network net = Networks.get("test1");
        net.addMember("__import__");
        
        Networks.save(".");
    }
}
