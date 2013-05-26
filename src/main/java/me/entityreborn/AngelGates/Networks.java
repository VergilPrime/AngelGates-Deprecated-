/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.AngelGates;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author import
 */
public class Networks {

    
    public static class Network {
        private String name;
        private String owner;
        private Set<String> members;

        public Network(String name, String owner) {
            this.name = name;
            this.owner = owner;
            members = new HashSet<String>();
        }
        
        public void addMember(String name) {
            members.add(name.toLowerCase());
        }
        
        public void removeMember(String name) {
            members.remove(name.toLowerCase());
        }
        
        public boolean isMember(String name) {
            if (name.equalsIgnoreCase(owner) || members.contains(name.toLowerCase())) {
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
        
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name.toLowerCase();
        }
    }
    
    private Map<String, Network> networks = new HashMap<String, Network>();
    
    public Network add(String name, String owner) {
        Network net = new Network(name, owner);
        networks.put(name.toLowerCase(), net);
        
        return net;
    }
    
    public Network add(Network net) {
        networks.put(net.getName().toLowerCase(), net);
        
        return net;
    }
    
    public Map<String, Network> get() {
        return Collections.unmodifiableMap(networks);
    }
    
    public Network get(String net) {
        return networks.get(net.toLowerCase());
    }
    
    public boolean has(String net) {
        return networks.containsKey(net.toLowerCase());
    }
    
    public void load(String dir) {
        File config = new File(dir, "networks.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        try {
            yaml.load(config);
        } catch (FileNotFoundException fnf) {
            // Fine.
        } catch (Exception ex) {
            AngelGates.log.log(Level.SEVERE, "Could not load network database!", ex);
            return;
        }
        
        for(String key : yaml.getKeys(false)) {
            if (!yaml.isConfigurationSection(key)) {
                continue;
            }
            
            ConfigurationSection netconfig = yaml.getConfigurationSection(key);
            
            if (!netconfig.isString("owner")) {
                AngelGates.log.warning("'owner' for network '" + key + "' is malformed. Skipping network!");
                continue;
            }
            
            String owner = netconfig.getString("owner");
            Network net = new Network(key, owner);
            
            if (netconfig.isList("members")) {
                List<String> members = netconfig.getStringList("members");
                
                for(String member : members) {
                    net.addMember(member);
                }
            }
            
            networks.put(key.toLowerCase(), net);
        }
    }
    
    public void save(String dir) {
        File config = new File(dir, "networks.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(config);
        } catch (Exception ex) {
            // This is fine, whatever.
        }
        
        for (Network net: networks.values()) {
            yaml.createSection(net.getName());
            ConfigurationSection sect = yaml.getConfigurationSection(net.getName());
            
            sect.set("owner", net.getOwner());
            
            if (!net.getMembers().isEmpty()) {
                sect.set("members", net.getMembers().toArray());
            }
        }
        
        
        try {
            yaml.save(config);
        } catch (Exception ex) {
            AngelGates.log.log(Level.SEVERE, "Could not save network database!", ex);
            return;
        }
    }
    
    public static void main(String[] args) {
        Networks nets = new Networks();
        nets.load(".");
        for (Network net : nets.get().values()) {
            System.out.println(net + ", owned by " + net.getOwner());
            System.out.println("Members: " + net.getMembers());
        }
        nets.add("Test1", "Me!");
        Network net = nets.get("test1");
        net.addMember("__import__");
        nets.save(".");
    }
}
