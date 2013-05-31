/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.AngelGates;

import static me.entityreborn.AngelGates.AngelGates.canUseNetwork;
import static me.entityreborn.AngelGates.AngelGates.canUsePortal;
import static me.entityreborn.AngelGates.AngelGates.handleVehicles;
import static me.entityreborn.AngelGates.AngelGates.log;
import static me.entityreborn.AngelGates.AngelGates.openPortal;
import static me.entityreborn.AngelGates.AngelGates.protectEntrance;
import static me.entityreborn.AngelGates.AngelGates.sendMessage;
import static me.entityreborn.AngelGates.AngelGates.server;
import me.entityreborn.AngelGates.events.AngelGatesDestroyEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 *
 * @author import
 */
public class BukkitListener implements Listener {

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!handleVehicles) {
            return;
        }
        Entity passenger = event.getVehicle().getPassenger();
        Vehicle vehicle = event.getVehicle();

        Portal portal = Portal.getByEntrance(event.getTo());
        if (portal == null || !portal.isOpen()) {
            return;
        }

        if (passenger instanceof Player) {
            Player player = (Player) passenger;

            Portal dest = portal.getDestination();
            if (dest == null) {
                return;
            }

            if (!canUsePortal(player, portal, false)) {
                AngelGates.sendMessage(player, AngelGates.getString("denyMsg"));
                return;
            }

            int cost = AngelGates.getGateCost(player, portal, dest);
            if (cost > 0) {
                String target = portal.getGate().getToOwner() ? portal.getNetwork().getOwner() : null;
                if (!AngelGates.chargePlayer(player, target, cost)) {
                    // Insufficient Funds
                    AngelGates.sendMessage(player, AngelGates.getString("ecoInFunds"));
                    return;
                }
                String deductMsg = AngelGates.getString("ecoDeduct");
                deductMsg = AngelGates.replaceVars(deductMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), portal.getName()});
                sendMessage(player, deductMsg, false);
                if (target != null) {
                    Player p = server.getPlayer(target);
                    if (p != null) {
                        String obtainedMsg = AngelGates.getString("ecoObtain");
                        obtainedMsg = AngelGates.replaceVars(obtainedMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), portal.getName()});
                        AngelGates.sendMessage(p, obtainedMsg, false);
                    }
                }
            }

            AngelGates.sendMessage(player, AngelGates.getString("teleportMsg"), false);
            dest.teleport(vehicle);
        } else {
            Portal dest = portal.getDestination();
            if (dest == null) {
                return;
            }
            dest.teleport(vehicle);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.isCancelled()) {
            return;
        }
        // Do a quick check for a AngelGate
        Location from = event.getFrom();
        if (from == null) {
            AngelGates.debug("onPlayerPortal", "From location is null. Stupid Bukkit");
            return;
        }
        World world = from.getWorld();
        int cX = from.getBlockX();
        int cY = from.getBlockY();
        int cZ = from.getBlockZ();
        for (int i = -2; i < 2; i++) {
            for (int j = -2; j < 2; j++) {
                for (int k = -2; k < 2; k++) {
                    Block b = world.getBlockAt(cX + i, cY + j, cZ + k);
                    // We only need to worry about portal mat
                    // Commented out for now, due to new Minecraft insta-nether
                    //if (b.getType() != Material.PORTAL) continue;
                    Portal portal = Portal.getByEntrance(b);
                    if (portal != null) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Check to see if the player actually moved
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Portal portal = Portal.getByEntrance(event.getTo());
        // No portal or not open
        if (portal == null || !portal.isOpen()) {
            return;
        }

        Portal destination = portal.getDestination();
        if (destination == null) {
            return;
        }

        if (!canUsePortal(player, portal, false)) {
            AngelGates.sendMessage(player, AngelGates.getString("denyMsg"));
            portal.teleport(player, portal, event);
            return;
        }

        int cost = AngelGates.getGateCost(player, portal, destination);
        if (cost > 0) {
            String target = portal.getGate().getToOwner() ? portal.getNetwork().getOwner() : null;
            if (!AngelGates.chargePlayer(player, target, cost)) {
                // Insufficient Funds
                AngelGates.sendMessage(player, "Insufficient Funds");
                return;
            }
            String deductMsg = AngelGates.getString("ecoDeduct");
            deductMsg = AngelGates.replaceVars(deductMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), portal.getName()});
            sendMessage(player, deductMsg, false);
            if (target != null) {
                Player p = server.getPlayer(target);
                if (p != null) {
                    String obtainedMsg = AngelGates.getString("ecoObtain");
                    obtainedMsg = AngelGates.replaceVars(obtainedMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), portal.getName()});
                    AngelGates.sendMessage(p, obtainedMsg, false);
                }
            }
        }

        AngelGates.sendMessage(player, AngelGates.getString("teleportMsg"), false);

        destination.teleport(player, portal, event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block;
        
        if (event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_AIR) {
            try {
                block = player.getTargetBlock(null, 5);
            } catch (IllegalStateException ex) {
                // We can safely ignore this exception, it only happens in void or max height
                return;
            }
        } else {
            block = event.getClickedBlock();
        }

        if (block == null) {
            return;
        }

        // Right click
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (block.getType() == Material.WALL_SIGN) {
                Portal portal = Portal.getByBlock(block);
                
                if (portal == null) {
                    return;
                }
                
                // Cancel item use
                event.setUseItemInHand(Event.Result.DENY);
                event.setUseInteractedBlock(Event.Result.DENY);

                boolean deny = false;
                if (!canUseNetwork(player, portal.getNetworkName())) {
                    deny = true;
                }

                if (!canUsePortal(player, portal, deny)) {
                    AngelGates.sendMessage(player, AngelGates.getString("denyMsg"));
                    return;
                }

                if (!portal.isOpen()) {
                    portal.cycleDestination(player);
                }
                return;
            }

            // Implement right-click to toggle a AngelGate, gets around spawn protection problem.
            if ((block.getType() == Material.STONE_BUTTON)) {
                Portal portal = Portal.getByBlock(block);
                if (portal == null) {
                    return;
                }

                // Cancel item use
                event.setUseItemInHand(Event.Result.DENY);
                event.setUseInteractedBlock(Event.Result.DENY);

                boolean deny = false;
                if (!canUseNetwork(player, portal.getNetworkName())) {
                    deny = true;
                }

                if (!canUsePortal(player, portal, deny)) {
                    AngelGates.sendMessage(player, AngelGates.getString("denyMsg"));
                    return;
                }

                openPortal(player, portal);
                
                event.setUseInteractedBlock(Event.Result.ALLOW);
            }
            return;
        }

        // Left click
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Check if we're scrolling a sign
            if (block.getType() == Material.WALL_SIGN) {
                Portal portal = Portal.getByBlock(block);
                
                if (portal == null) {
                    return;
                }

                event.setUseInteractedBlock(Event.Result.DENY);
                // Only cancel event in creative mode
                if (player.getGameMode().equals(GameMode.CREATIVE)) {
                    event.setCancelled(true);
                }

                boolean deny = false;
                if (!canUseNetwork(player, portal.getNetworkName())) {
                    deny = true;
                }

                if (!canUsePortal(player, portal, deny)) {
                    AngelGates.sendMessage(player, AngelGates.getString("denyMsg"));
                    return;
                }

                if (!portal.isOpen()) {
                    portal.cycleDestination(player, -1);
                }
                return;
            }

            // Check if we're pushing a button.
            if (block.getType() == Material.STONE_BUTTON) {
                Portal portal = Portal.getByBlock(block);
                if (portal == null) {
                    return;
                }

                event.setUseInteractedBlock(Event.Result.DENY);
                if (player.getGameMode().equals(GameMode.CREATIVE)) {
                    event.setCancelled(true);
                }

                boolean deny = false;
                if (!canUseNetwork(player, portal.getNetworkName())) {
                    deny = true;
                }

                if (!canUsePortal(player, portal, deny)) {
                    AngelGates.sendMessage(player, AngelGates.getString("denyMsg"));
                    return;
                }
                openPortal(player, portal);
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (block.getType() != Material.WALL_SIGN) {
            return;
        }

        final Portal portal = Portal.createPortal(event, player);
        
        // Not creating a gate, just placing a sign
        if (portal == null) {
            return;
        }

        AngelGates.sendMessage(player, AngelGates.getString("createMsg"), false);
        AngelGates.debug("onSignChange", "Initialized AngelGate: " + portal.getName());
        
        AngelGates.server.getScheduler().scheduleSyncDelayedTask(AngelGates.self, new Runnable() {
            public void run() {
                portal.drawSign();
            }
        }, 1);
    }

    // Switch to HIGHEST priority so as to come after block protection plugins (Hopefully)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Block block = event.getBlock();
        Player player = event.getPlayer();

        Portal portal = Portal.getByBlock(block);
        if (portal == null && protectEntrance) {
            portal = Portal.getByEntrance(block);
        }
        if (portal == null) {
            return;
        }

        boolean deny = false;
        String denyMsg = "";

        if (!AngelGates.canDestroy(player, portal)) {
            denyMsg = "Permission Denied"; // TODO: Change to AngelGates.getString()
            deny = true;
            log.info("" + player.getName() + " tried to destroy gate");
        }

        int cost = AngelGates.getDestroyCost(player, portal.getGate());

        AngelGatesDestroyEvent dEvent = new AngelGatesDestroyEvent(portal, player, deny, denyMsg, cost);
        AngelGates.server.getPluginManager().callEvent(dEvent);
        if (dEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        if (dEvent.getDeny()) {
            AngelGates.sendMessage(player, dEvent.getDenyReason());
            event.setCancelled(true);
            return;
        }

        cost = dEvent.getCost();

        if (cost != 0) {
            if (!AngelGates.chargePlayer(player, null, cost)) {
                AngelGates.debug("onBlockBreak", "Insufficient Funds");
                AngelGates.sendMessage(player, AngelGates.getString("ecoInFunds"));
                event.setCancelled(true);
                return;
            }

            if (cost > 0) {
                String deductMsg = AngelGates.getString("ecoDeduct");
                deductMsg = AngelGates.replaceVars(deductMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), portal.getName()});
                sendMessage(player, deductMsg, false);
            } else if (cost < 0) {
                String refundMsg = AngelGates.getString("ecoRefund");
                refundMsg = AngelGates.replaceVars(refundMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(-cost), portal.getName()});
                sendMessage(player, refundMsg, false);
            }
        }

        portal.unregister(true);
        AngelGates.sendMessage(player, AngelGates.getString("destroyMsg"), false);
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Portal portal = null;

        // Handle keeping portal material and buttons around
        if (block.getTypeId() == 90) {
            portal = Portal.getByEntrance(block);
        } else if (block.getTypeId() == 77) {
            portal = Portal.getByControl(block);
        }
        if (portal != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Portal portal = Portal.getByEntrance(event.getBlock());

        if (portal != null) {
            event.setCancelled((event.getBlock().getY() == event.getToBlock().getY()));
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Portal portal = Portal.getByBlock(block);
            if (portal != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }
        Block affected = event.getRetractLocation().getBlock();
        Portal portal = Portal.getByBlock(affected);
        if (portal != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World w = event.getWorld();
        // We have to make sure the world is actually loaded. This gets called twice for some reason.
        if (w.getBlockAt(w.getSpawnLocation()).getWorld() != null) {
            Portal.loadAllGates(w);
        }
    }

    // We need to reload all gates on world unload, boo
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        AngelGates.debug("onWorldUnload", "Reloading all AngelGates");
        World w = event.getWorld();
        Portal.clearGates();
        for (World world : server.getWorlds()) {
            if (world.equals(w)) {
                continue;
            }
            Portal.loadAllGates(world);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for (Block b : event.blockList()) {
            Portal portal = Portal.getByBlock(b);
            if (portal == null) {
                continue;
            }
            if (AngelGates.destroyExplosion) {
                portal.unregister(true);
            } else {
                AngelGates.blockPopulatorQueue.add(new BloxPopulator(new Blox(b), b.getTypeId(), b.getData()));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (EconomyHandler.setupVault(event.getPlugin())) {
            log.info("Vault v" + EconomyHandler.vault.getDescription().getVersion() + " found");
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (EconomyHandler.checkLost(event.getPlugin())) {
            log.info("Register/Vault plugin lost.");
        }
    }
}
