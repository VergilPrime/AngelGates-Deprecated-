/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.AngelGates;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import org.bukkit.Bukkit;

/**
 *
 * @author import
 */
public class Towny {
    public static String getTown(String name) {
        try {
            if (Bukkit.getPlayer(name) != null) {
                name = Bukkit.getPlayer(name).getName();
            }
            
            Resident resy = TownyUniverse.getDataSource().getResident(name);
            Town twn = resy.getTown();
            String townName = twn.getName();
            
            return townName;
        } catch (Exception ex) {
        }
        
        return null;
    }
    
    public static boolean isMayor(String name) {
        try {
            if (Bukkit.getPlayer(name) != null) {
                name = Bukkit.getPlayer(name).getName();
            }
            
            Resident resy = TownyUniverse.getDataSource().getResident(name);
            return resy.isMayor();
        } catch (Exception ex) {
        }
        
        return false;
    }
    
    public static boolean isKing(String name) {
        try {
            if (Bukkit.getPlayer(name) != null) {
                name = Bukkit.getPlayer(name).getName();
            }
            
            Resident resy = TownyUniverse.getDataSource().getResident(name);
            return resy.isKing();
        } catch (Exception ex) {
        }
        
        return false;
    }
}
