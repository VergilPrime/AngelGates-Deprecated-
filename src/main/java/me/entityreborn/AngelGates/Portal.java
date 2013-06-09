package me.entityreborn.AngelGates;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.entityreborn.AngelGates.Networks.Network;
import me.entityreborn.AngelGates.events.AngelGatesActivateEvent;
import me.entityreborn.AngelGates.events.AngelGatesCloseEvent;
import me.entityreborn.AngelGates.events.AngelGatesCreateEvent;
import me.entityreborn.AngelGates.events.AngelGatesDeactivateEvent;
import me.entityreborn.AngelGates.events.AngelGatesOpenEvent;
import me.entityreborn.AngelGates.events.AngelGatesPortalEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Button;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

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
public class Portal {
    // Static variables used to store portal lists
    private static final HashMap<Blox, Portal> lookupBlocks = new HashMap<Blox, Portal>();
    private static final HashMap<Blox, Portal> lookupEntrances = new HashMap<Blox, Portal>();
    private static final HashMap<Blox, Portal> lookupControls = new HashMap<Blox, Portal>();
    private static final ArrayList<Portal> allPortals = new ArrayList<Portal>();
    private static final HashMap<String, ArrayList<String>> allPortalsNet = new HashMap<String, ArrayList<String>>();
    private static final HashMap<String, HashMap<String, Portal>> lookupNamesNet = new HashMap<String, HashMap<String, Portal>>();
    
    // Gate location block info
    private Blox topLeft;
    private int modX;
    private int modZ;
    private float rotX;
    // Block references
    private Blox id;
    private Blox button;
    private Blox[] frame;
    private Blox[] entrances;
    // Gate information
    private String name;
    private String destination = "";
    private String lastDest = "";
    private String network;
    private Gate gate;
    private boolean verified;
    private String builtBy;
    // In-use information
    private ArrayList<String> destinations = new ArrayList<String>();
    private boolean isOpen = false;
    private long openTime;
    private long firstEntered;

    private Portal(Blox topLeft, int modX, int modZ,
            float rotX, Blox id, Blox button,
            String name, String builder, 
            boolean verified, String network, Gate gate) {
        this.topLeft = topLeft;
        this.modX = modX;
        this.modZ = modZ;
        this.rotX = rotX;
        this.id = id;
        this.button = button;
        this.verified = verified;
        this.network = network;
        this.name = name;
        this.gate = gate;
        this.builtBy = builder;

        if (verified) {
            this.drawSign();
        }
    }

    /**
     * Option Check Functions
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Getters and Setters
     */
    public float getRotation() {
        return rotX;
    }

    public String getNetworkName() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public long getOpenTime() {
        return openTime;
    }
    
    public long getFirstEnteredTime() {
        return firstEntered;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = filterName(name);
        drawSign();
    }

    public Portal getDestination() {
        return Portal.getByName(destination, getNetworkName());
    }

    public void setDestination(Portal destination) {
        setDestination(destination.getName());
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestinationName() {
        return destination;
    }

    public Gate getGate() {
        return gate;
    }

    public Blox[] getEntrances() {
        if (entrances == null) {
            RelativeBlockVector[] space = gate.getEntrances();
            entrances = new Blox[space.length];
            int i = 0;

            for (RelativeBlockVector vector : space) {
                entrances[i++] = getBlockAt(vector);
            }
        }
        return entrances;
    }

    public Blox[] getFrame() {
        if (frame == null) {
            RelativeBlockVector[] border = gate.getBorder();
            frame = new Blox[border.length];
            int i = 0;

            for (RelativeBlockVector vector : border) {
                frame[i++] = getBlockAt(vector);
            }
        }

        return frame;
    }

    public Block getSign() {
        return id.getBlock();
    }

    public World getWorld() {
        return topLeft.getWorld();
    }

    public Block getButton() {
        if (button == null) {
            return null;
        }
        return button.getBlock();
    }

    public void setButton(Blox button) {
        this.button = button;
    }

    public static ArrayList<String> getNetwork(String network) {
        return allPortalsNet.get(network.toLowerCase());
    }
    
    public Network getNetwork() {
        return Networks.get(getNetworkName());
    }

    public boolean open(boolean force) {
        return open(null, force);
    }

    public boolean open(Player openFor, boolean force) {
        // Call the AngelGateOpenEvent
        AngelGatesOpenEvent event = new AngelGatesOpenEvent(openFor, this, force);
        AngelGates.server.getPluginManager().callEvent(event);
        if (!force && event.isCancelled()) {
            return false;
        }
        
        force = event.getForce();

        if (isOpen() && !force) {
            return false;
        }

        getWorld().loadChunk(getWorld().getChunkAt(topLeft.getBlock()));

        int openType = gate.getPortalBlockOpen();
        for (Blox inside : getEntrances()) {
            AngelGates.blockPopulatorQueue.add(new BloxPopulator(inside, openType));
        }

        isOpen = true;
        openTime = System.currentTimeMillis() / 1000;
        AngelGates.openList.add(this);
        AngelGates.activeList.remove(this);

        Portal end = getDestination();
        
        if (end != null && !end.isOpen()) {
            end.setDestination(this);
            end.open(openFor, false);
            
            if (end.isVerified()) {
                end.drawSign();
            }
        }
        
        firstEntered = 0;

        return true;
    }
    
    public void close(boolean force) {
        close(null, force);
    }

    public void close(Player closer, boolean force) {
        if (!isOpen) {
            return;
        }
        
        // Call the AngelGateCloseEvent
        AngelGatesCloseEvent event = new AngelGatesCloseEvent(closer, this);
        AngelGates.server.getPluginManager().callEvent(event);
        
        if (!force && event.isCancelled()) {
            return;
        }
        
        // Close this gate, then the dest gate.
        int closedType = gate.getPortalBlockClosed();
        
        for (Blox inside : getEntrances()) {
            AngelGates.blockPopulatorQueue.add(new BloxPopulator(inside, closedType));
        }
        
        isOpen = false;
        AngelGates.openList.remove(this);
        AngelGates.activeList.remove(this);

        Portal end = getDestination();

        if (end != null && end.isOpen()) {
            end.deactivate(); // Clear it's destination first.
            end.close(false);
        }

        deactivate();
    }

    public boolean isPowered() {
        RelativeBlockVector[] controls = gate.getControls();

        for (RelativeBlockVector vector : controls) {
            MaterialData mat = getBlockAt(vector).getBlock().getState().getData();

            if (mat instanceof Button && ((Button) mat).isPowered()) {
                return true;
            }
        }

        return false;
    }

    public void teleport(Player player, Portal origin, PlayerMoveEvent event) {
        Location traveller = player.getLocation();
        Location exit = getExit(traveller);

        exit.setYaw(origin.getRotation() - traveller.getYaw() + this.getRotation() + 180);

        // Call the AngelGatePortalEvent to allow plugins to change destination
        if (!origin.equals(this)) {
            AngelGatesPortalEvent pEvent = new AngelGatesPortalEvent(player, origin, this, exit);
            AngelGates.server.getPluginManager().callEvent(pEvent);
            
            // Teleport is cancelled
            if (pEvent.isCancelled()) {
                origin.teleport(player, origin, event);
                return;
            }
            // Update exit if needed
            exit = pEvent.getExit();
        }

        // If no event is passed in, assume it's a teleport, and act as such
        if (event == null) {
            exit.setYaw(this.getRotation());
            player.teleport(exit);
        } else {
            // The new method to teleport in a move event is set the "to" field.
            event.setTo(exit);
        }
        
        if (firstEntered == 0) {
            firstEntered = System.currentTimeMillis() / 1000;
        }
    }

    public void teleport(final Vehicle vehicle) {
        Location traveller = new Location(getWorld(), vehicle.getLocation().getX(), vehicle.getLocation().getY(), vehicle.getLocation().getZ());
        Location exit = getExit(traveller);

        double velocity = vehicle.getVelocity().length();

        // Stop and teleport
        vehicle.setVelocity(new Vector());

        // Get new velocity
        final Vector newVelocity = new Vector();
        
        switch ((int) id.getBlock().getData()) {
            case 2:
                newVelocity.setZ(-1);
                break;
            case 3:
                newVelocity.setZ(1);
                break;
            case 4:
                newVelocity.setX(-1);
                break;
            case 5:
                newVelocity.setX(1);
                break;
        }
        
        newVelocity.multiply(velocity);

        final Entity passenger = vehicle.getPassenger();
        
        if (passenger != null) {
            final Vehicle v = exit.getWorld().spawn(exit, vehicle.getClass());
            
            vehicle.eject();
            vehicle.remove();
            passenger.teleport(exit);
            
            AngelGates.server.getScheduler().scheduleSyncDelayedTask(AngelGates.self, new Runnable() {
                public void run() {
                    v.setPassenger(passenger);
                    v.setVelocity(newVelocity);
                }
            }, 1);
        } else {
            Vehicle mc = exit.getWorld().spawn(exit, vehicle.getClass());
            
            if (mc instanceof StorageMinecart) {
                StorageMinecart smc = (StorageMinecart) mc;
                smc.getInventory().setContents(((StorageMinecart) vehicle).getInventory().getContents());
            }
            
            mc.setVelocity(newVelocity);
            vehicle.remove();
        }
        
        if (firstEntered == 0) {
            firstEntered = System.currentTimeMillis() / 1000;
        }
    }

    public Location getExit(Location traveller) {
        Location loc = null;
        // Check if the gate has an exit block
        if (gate.getExit() != null) {
            Blox exit = getBlockAt(gate.getExit());
            loc = exit.modRelativeLoc(0D, 0D, 1D, traveller.getYaw(), traveller.getPitch(), modX, 1, modZ);
        } else {
            AngelGates.log.warning("Missing destination point in .gate file " + gate.getFilename());
        }

        if (loc != null) {
            if (getWorld().getBlockTypeIdAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) == Material.STEP.getId()) {
                loc.setY(loc.getY() + 0.5);
            }

            loc.setPitch(traveller.getPitch());
            return loc;
        }
        return traveller;
    }

    public boolean isChunkLoaded() {
        return getWorld().isChunkLoaded(topLeft.getBlock().getChunk());
    }

    public void loadChunk() {
        getWorld().loadChunk(topLeft.getBlock().getChunk());
    }

    public boolean isVerified() {
        verified = true;
        for (RelativeBlockVector control : gate.getControls()) {
            verified = verified && getBlockAt(control).getBlock().getTypeId() == gate.getControlBlock();
        }
        return verified;
    }

    public boolean wasVerified() {
        return verified;
    }

    public boolean checkIntegrity() {
        return gate.matches(topLeft, modX, modZ);
    }

    public ArrayList<String> getDestinations(Player player, String network) {
        ArrayList<String> dests = new ArrayList<String>();
        for (String dest : allPortalsNet.get(network.toLowerCase())) {
            Portal portal = getByName(dest, network);

            if (dest.equalsIgnoreCase(getName())) {
                continue;
            }
            // Allow random use by non-players (Minecarts)
            if (player == null) {
                dests.add(portal.getName());
                continue;
            }
            dests.add(portal.getName());
        }
        return dests;
    }

    public boolean activate(Player player) {
        destinations.clear();
        destination = "";
        
        AngelGates.activeList.add(this);
        
        destinations = getDestinations(player, network);
        
        if (AngelGates.sortLists) {
            Collections.sort(destinations);
        }
        
        if (AngelGates.rememberLastDest && !lastDest.isEmpty() && destinations.contains(lastDest)) {
            destination = lastDest;
        }

        AngelGatesActivateEvent event = new AngelGatesActivateEvent(this, player, destinations, destination);
        AngelGates.server.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            AngelGates.activeList.remove(this);
            return false;
        }
        
        destination = event.getDestination();
        destinations = event.getDestinations();
        
        drawSign();
        
        return true;
    }

    public void deactivate() {
        AngelGatesDeactivateEvent event = new AngelGatesDeactivateEvent(this);
        AngelGates.server.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return;
        }

        AngelGates.activeList.remove(this);
        
        destinations.clear();
        destination = "";
        
        drawSign();
    }

    public boolean isActive() {
        return destinations.size() > 0;
    }

    public void cycleDestination(Player player) {
        cycleDestination(player, 1);
    }

    public void cycleDestination(Player player, int dir) {
        Boolean activate = false;
        
        if (!isActive()) {
            // If the event is cancelled, return
            if (!activate(player)) {
                return;
            }
            
            AngelGates.debug("cycleDestination", "Network Size: " + allPortalsNet.get(network.toLowerCase()).size());
            AngelGates.debug("cycleDestination", "Player has access to: " + destinations.size());
            
            activate = true;
        }

        if (destinations.isEmpty()) {
            AngelGates.sendMessage(player, AngelGates.getString("destEmpty"));
            return;
        }

        if (!AngelGates.rememberLastDest || !activate || lastDest.isEmpty()) {
            int index = destinations.indexOf(destination);
            index += dir;
            
            if (index >= destinations.size()) {
                index = 0;
            } else if (index < 0) {
                index = destinations.size() - 1;
            }
            
            destination = destinations.get(index);
            lastDest = destination;
        }
        
        openTime = System.currentTimeMillis() / 1000;
        drawSign();
    }

    public final void drawSign() {
        Material sMat = id.getBlock().getType();
        
        if (sMat != Material.SIGN && sMat != Material.WALL_SIGN && sMat != Material.SIGN_POST) {
            AngelGates.log.warning("Sign block is not a Sign object");
            AngelGates.debug("Portal::drawSign", "Block: " + id.getBlock().getType() + " @ " + id.getBlock().getLocation());
            
            return;
        }
        
        Sign sign = (Sign) id.getBlock().getState();
        
        int max = destinations.size() - 1;
        int done = 0;

        if (!isActive()) {
            AngelGates.setLine(sign, done, "-" + getNetwork().getName() + "-");
            AngelGates.setLine(sign, ++done, "-" + name + "-");
            AngelGates.setLine(sign, ++done, "-" + getNetwork().getOwner() + "-");
        } else {
            int index = destinations.indexOf(destination);
            AngelGates.setLine(sign, done, "-" + name + "-");
            if ((index == max) && (max > 1) && (++done <= 3)) {
                AngelGates.setLine(sign, done, destinations.get(index - 2));
            }

            if ((index > 0) && (++done <= 3)) {
                AngelGates.setLine(sign, done, destinations.get(index - 1));
            }

            if (++done <= 3) {
                AngelGates.setLine(sign, done, " >" + destination + "< ");
            }

            if ((max >= index + 1) && (++done <= 3)) {
                AngelGates.setLine(sign, done, destinations.get(index + 1));
            }

            if ((max >= index + 2) && (++done <= 3)) {
                AngelGates.setLine(sign, done, destinations.get(index + 2));
            }
        }

        for (done++; done <= 3; done++) {
            sign.setLine(done, "");
        }

        sign.update();
    }

    public void unregister(boolean removeAll) {
        AngelGates.debug("Unregister", "Unregistering gate " + getName());
        close(true);

        for (Blox block : getFrame()) {
            lookupBlocks.remove(block);
        }
        
        // Include the sign and button
        lookupBlocks.remove(id);
        
        if (button != null) {
            lookupBlocks.remove(button);
        }

        lookupControls.remove(id);
        
        if (button != null) {
            lookupControls.remove(button);
        }

        for (Blox entrance : getEntrances()) {
            lookupEntrances.remove(entrance);
        }

        if (removeAll) {
            allPortals.remove(this);
        }

        lookupNamesNet.get(getNetworkName().toLowerCase()).remove(getName().toLowerCase());
        allPortalsNet.get(getNetworkName().toLowerCase()).remove(getName().toLowerCase());

        for (String originName : allPortalsNet.get(getNetworkName().toLowerCase())) {
            Portal origin = Portal.getByName(originName, getNetworkName());
            
            if (origin == null ||
                    !origin.getDestinationName().equalsIgnoreCase(getName()) ||
                    !origin.isVerified()) {
                continue;
            }
            
            origin.close(true);
        }

        if (id.getBlock().getType() == Material.WALL_SIGN && id.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) id.getBlock().getState();
            sign.setLine(0, getName());
            sign.setLine(1, "");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
        }

        saveAllGates(getWorld());
    }

    private Blox getBlockAt(RelativeBlockVector vector) {
        return topLeft.modRelative(vector.getRight(), vector.getDepth(), vector.getDistance(), modX, 1, modZ);
    }

    private void register() {
        String netname = getNetworkName().toLowerCase();
        // Check if network exists in our network list
        if (!lookupNamesNet.containsKey(netname)) {
            AngelGates.debug("register", "Network " + getNetworkName() + " not in lookupNamesNet, adding");
            lookupNamesNet.put(netname, new HashMap<String, Portal>());
        }
        
        lookupNamesNet.get(netname).put(getName().toLowerCase(), this);
        
        // Check if this network exists
        if (!allPortalsNet.containsKey(netname)) {
            AngelGates.debug("register", "Network " + getNetworkName() + " not in allPortalsNet, adding");
            allPortalsNet.put(netname, new ArrayList<String>());
        }
        
        allPortalsNet.get(netname).add(getName().toLowerCase());

        for (Blox block : getFrame()) {
            lookupBlocks.put(block, this);
        }
        
        // Include the sign and button
        lookupBlocks.put(id, this);
        
        if (button != null) {
            lookupBlocks.put(button, this);
        }

        lookupControls.put(id, this);
        
        if (button != null) {
            lookupControls.put(button, this);
        }

        for (Blox entrance : getEntrances()) {
            lookupEntrances.put(entrance, this);
        }

        allPortals.add(this);
    }

    public static Portal createPortal(SignChangeEvent event, Player player) {
        Blox id = new Blox(event.getBlock());
        Block idParent = id.getParent();
        
        if (idParent == null) {
            return null;
        }

        if (Gate.getGatesByControlBlock(idParent).length == 0) {
            return null;
        }

        if (Portal.getByBlock(idParent) != null) {
            AngelGates.debug("createPortal", "idParent belongs to existing gate");
            return null;
        }

        Blox parent = new Blox(player.getWorld(), idParent.getX(), idParent.getY(), idParent.getZ());
        Blox topleft = null;
        String network = filterName(event.getLine(0));
        String name = filterName(event.getLine(1));

        // Moved the layout check so as to avoid invalid messages when not making a gate
        int modX = 0;
        int modZ = 0;
        float rotX = 0f;
        int facing = 0;

        if (idParent.getX() > id.getBlock().getX()) {
            modZ -= 1;
            rotX = 90f;
            facing = 2;
        } else if (idParent.getX() < id.getBlock().getX()) {
            modZ += 1;
            rotX = 270f;
            facing = 1;
        } else if (idParent.getZ() > id.getBlock().getZ()) {
            modX += 1;
            rotX = 180f;
            facing = 4;
        } else if (idParent.getZ() < id.getBlock().getZ()) {
            modX -= 1;
            rotX = 0f;
            facing = 3;
        }

        Gate[] possibleGates = Gate.getGatesByControlBlock(idParent);
        Gate gate = null;
        RelativeBlockVector buttonVector = null;

        for (Gate possibility : possibleGates) {
            if (gate == null && buttonVector == null) {
                RelativeBlockVector[] vectors = possibility.getControls();
                RelativeBlockVector otherControl = null;

                for (RelativeBlockVector vector : vectors) {
                    Blox tl = parent.modRelative(-vector.getRight(), -vector.getDepth(), -vector.getDistance(), modX, 1, modZ);

                    if (gate == null) {
                        if (possibility.matches(tl, modX, modZ, true)) {
                            gate = possibility;
                            topleft = tl;

                            if (otherControl != null) {
                                buttonVector = otherControl;
                            }
                        }
                    } else if (otherControl != null) {
                        buttonVector = vector;
                    }

                    otherControl = vector;
                }
            }
        }

        if (gate == null || buttonVector == null) {
            AngelGates.debug("createPortal", "Could not find matching gate layout");
            return null;
        }

        if (network.length() < 1 || network.length() > 11) {
            network = AngelGates.getDefaultNetwork();
        }

        boolean deny = false;
        String denyMsg = "";

        // Check if the player can create gates on this network
        if (!AngelGates.canCreate(player, network)) {
            AngelGates.debug("createPortal", "Player does not have access to network");
            deny = true;
            denyMsg = AngelGates.getString("createNetDeny");
        }

        // Check to make sure none of this gate belongs to another gate.
        for (RelativeBlockVector v : gate.getBorder()) {
            Blox b = topleft.modRelative(v.getRight(), v.getDepth(), v.getDistance(), modX, 1, modZ);
            
            if (Portal.getByBlock(b.getBlock()) != null) {
                AngelGates.debug("createPortal", "Gate conflicts with existing gate");
                AngelGates.sendMessage(player, AngelGates.getString("createConflict"));
                
                return null;
            }
        }

        Blox button = null;
        Portal portal;
        
        portal = new Portal(topleft, modX, modZ, rotX, id, button, name, 
                player.getName(), false, network, gate);

        int cost = AngelGates.getCreateCost(player, gate);

        // Call AngelGateCreateEvent
        AngelGatesCreateEvent cEvent = new AngelGatesCreateEvent(player, portal, event.getLines(), deny, denyMsg, cost);
        AngelGates.server.getPluginManager().callEvent(cEvent);
        
        if (cEvent.isCancelled()) {
            return null;
        }
        
        if (cEvent.isDenied()) {
            AngelGates.sendMessage(player, cEvent.getDenyReason());
            
            return null;
        }

        cost = cEvent.getCost();

        // Name & Network can be changed in the event, so do these checks here.
        if (portal.getName().length() < 1 || portal.getName().length() > 11) {
            AngelGates.debug("createPortal", "Name length error");
            AngelGates.sendMessage(player, AngelGates.getString("createNameLength"));
            
            return null;
        }

        if (getByName(portal.getName(), portal.getNetworkName()) != null) {
            AngelGates.debug("createPortal", "Name Error");
            AngelGates.sendMessage(player, AngelGates.getString("createExists"));
            
            return null;
        }

        if (cost > 0) {
            if (!AngelGates.chargePlayer(player, null, cost)) {
                String inFundMsg = AngelGates.getString("ecoInFunds");
                inFundMsg = AngelGates.replaceVars(inFundMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), name});
                AngelGates.sendMessage(player, inFundMsg);
                AngelGates.debug("createPortal", "Insufficient Funds");
                
                return null;
            }
            
            String deductMsg = AngelGates.getString("ecoDeduct");
            deductMsg = AngelGates.replaceVars(deductMsg, new String[]{"%cost%", "%portal%"}, new String[]{EconomyHandler.format(cost), name});
            AngelGates.sendMessage(player, deductMsg, false);
        }
        
        if (!Networks.has(network)) {
            Networks.add(network, player.getName());
        }

        button = topleft.modRelative(buttonVector.getRight(), buttonVector.getDepth(), buttonVector.getDistance() + 1, modX, 1, modZ);
        button.setType(Material.STONE_BUTTON.getId());
        button.setData(facing);

        portal.setButton(button);
        portal.register();
        portal.drawSign();

        for (Blox inside : portal.getEntrances()) {
            inside.setType(portal.getGate().getPortalBlockClosed());
        }

        for (String originName : allPortalsNet.get(portal.getNetworkName().toLowerCase())) {
            Portal origin = Portal.getByName(originName, portal.getNetworkName());
            
            if (origin == null || 
                    !origin.getDestinationName().equalsIgnoreCase(portal.getName()) || 
                    !origin.isVerified()) {
                continue;
            }
        }

        saveAllGates(portal.getWorld());

        return portal;
    }

    public static Portal getByName(String name, String network) {
        if (!lookupNamesNet.containsKey(network.toLowerCase())) {
            return null;
        }
        
        return lookupNamesNet.get(network.toLowerCase()).get(name.toLowerCase());

    }

    public static Portal getByEntrance(Location location) {
        return lookupEntrances.get(new Blox(location));
    }

    public static Portal getByEntrance(Block block) {
        return lookupEntrances.get(new Blox(block));
    }

    public static Portal getByControl(Block block) {
        return lookupControls.get(new Blox(block));
    }

    public static Portal getByBlock(Block block) {
        return lookupBlocks.get(new Blox(block));
    }

    public static void saveAllGates(World world) {
        File file = new File(AngelGates.getSaveLocation(), world.getName() + ".yml");
        file.getParentFile().mkdirs();
        
        YamlConfiguration yaml = new YamlConfiguration();
        
        try {
            if (file.exists()) {
                yaml.load(file);
            }
        } catch (Exception ex) {
            Logger.getLogger(Portal.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ConfigurationSection root = yaml.createSection("portals");
        
        for (Portal portal : allPortals) {
            String wName = portal.getWorld().getName();
            
            if (!wName.equalsIgnoreCase(world.getName())) {
                continue;
            }
                
            ConfigurationSection portalsect = root.createSection(portal.getName());
            
            Blox sign = new Blox(portal.id.getBlock());
            Blox button = portal.button;

            portalsect.set("sign", sign.toString());
            portalsect.set("button", (button != null) ? button.toString() : "");
            portalsect.set("modX", portal.modX);
            portalsect.set("modZ", portal.modZ);
            portalsect.set("rotX", portal.rotX);
            portalsect.set("topLeft", portal.topLeft.toString());
            portalsect.set("gate", portal.gate.getFilename());
            portalsect.set("network", portal.getNetworkName());
            portalsect.set("world", portal.getWorld().getName());
            portalsect.set("builtBy", portal.builtBy);
        }

        try {
            yaml.save(file);
        } catch (Exception e) {
            AngelGates.log.log(Level.SEVERE, "Exception while writing AngelGates to " + file + ": " + e);
        }
    }

    public static void clearGates() {
        lookupBlocks.clear();
        lookupNamesNet.clear();
        lookupEntrances.clear();
        lookupControls.clear();
        allPortals.clear();
        allPortalsNet.clear();
    }

    public static void loadAllGates(World world) {
        File file = new File(AngelGates.getSaveLocation(), world.getName() + ".yml");
        
        if (!file.exists()) {
            AngelGates.log.info("{" + world.getName() + "} No AngelGates for world ");
            return;
        }
        
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (FileNotFoundException ex) {
            return;
        } catch (IOException ex) {
            AngelGates.log.log(Level.SEVERE, "Error loading " + file.getPath(), ex);
            return;
        } catch (InvalidConfigurationException ex) {
            AngelGates.log.log(Level.SEVERE, "Error loading " + file.getPath(), ex);
            return;
        }
        
        if (!yaml.isConfigurationSection("portals")) {
            AngelGates.log.info("{" + world.getName() + "} No AngelGates for world ");
            return;
        }
        
        if (!yaml.isConfigurationSection("portals")) {
            AngelGates.log.info("{" + world.getName() + "} Malformed world file. Skipping. ");
            return;
        }
        
        ConfigurationSection sect = yaml.getConfigurationSection("portals");
        int portalCount = 0;
        
        try {
            for (String key : sect.getKeys(false)) {
                try {
                    ConfigurationSection portalsect = sect.getConfigurationSection(key);

                    Blox sign = new Blox(world, portalsect.getString("sign"));

                    if (!(sign.getBlock().getState() instanceof Sign)) {
                        AngelGates.log.info("Sign for " + key + " doesn't exist. BlockType = " + sign.getBlock().getType());
                        continue;
                    }
                
                    String sbutton = portalsect.getString("button");
                    Blox button = (sbutton.length() > 0) ? new Blox(world, sbutton) : null;

                    String sgate = portalsect.getString("gate");
                    Gate gate = (sgate.contains(";")) ? Gate.getGateByName("nethergate.gate") : Gate.getGateByName(sgate);
                    
                    if (gate == null) {
                        AngelGates.log.info("Gate layout for " + key + " does not exist [" + sgate + "]");
                        continue;
                    }

                    String network = portalsect.getString("network").trim();
                    if (network.isEmpty()) {
                        network = AngelGates.getDefaultNetwork();
                    }
                    
                    if (!Networks.has(network)) {
                        AngelGates.log.info("Network " + network + " for " 
                                + key + " does not exist. Skipping for now.");
                        continue;
                    }
                    
                    int modX = Integer.parseInt(portalsect.getString("modX"));
                    int modZ = Integer.parseInt(portalsect.getString("modZ"));
                    float rotX = Float.parseFloat(portalsect.getString("rotX"));
                    Blox topLeft = new Blox(world, portalsect.getString("topLeft"));
                    String builder = Networks.get(network).getOwner();
                            
                    if (portalsect.isString("buildBy")) {
                        builder = portalsect.getString("builtBy");
                    }
                    
                    Portal portal = new Portal(topLeft, modX, modZ, rotX, sign, 
                            button, key, builder, false, network, gate);
                    portal.register();
                    portal.close(true);
                    
                    portalCount++;
                } catch (Exception e) {
                    AngelGates.log.log(Level.SEVERE, "Malformed data for portal " 
                            + key + " in " + file.getName() + ": " + e.getMessage());
                }
            }

            AngelGates.log.info("{" + world.getName() + "} Loaded " 
                    + portalCount + " AngelGates.");
        } catch (Exception e) {
            AngelGates.log.log(Level.SEVERE, "Exception while reading AngelGates from " 
                    + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Open any always-on gates. Do this here as it should be more efficient than in the loop.
        for (Iterator<Portal> iter = allPortals.iterator(); iter.hasNext();) {
            Portal portal = iter.next();
            if (portal == null) {
                continue;
            }

            // Verify portal integrity/register portal
            if (!portal.wasVerified()) {
                if (!portal.isVerified() || !portal.checkIntegrity()) {
                    // DEBUG
                    for (RelativeBlockVector control : portal.getGate().getControls()) {
                        if (portal.getBlockAt(control).getBlock().getTypeId() != portal.getGate().getControlBlock()) {
                            AngelGates.debug("loadAllGates", "Control Block Type == " + portal.getBlockAt(control).getBlock().getTypeId());
                        }
                    }
                    portal.unregister(false);
                    iter.remove();
                    AngelGates.log.info("Destroying AngelGate at " + portal.toString());
                    continue;
                } else {
                    portal.drawSign();
                    portalCount++;
                }
            }
        }
    }

    public static void closeAllGates() {
        AngelGates.log.info("Closing all AngelGates.");
        
        for (Portal p : allPortals) {
            if (p == null) {
                continue;
            }
            
            p.close(true);
        }
    }

    public static String filterName(String input) {
        return input.replaceAll("[\\|:#]", "").trim();
    }

    @Override
    public String toString() {
        return String.format("Portal [id=%s, network=%s name=%s, type=%s]", id, network, name, gate.getFilename());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((network == null) ? 0 : network.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Portal other = (Portal) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equalsIgnoreCase(other.name)) {
            return false;
        }
        if (network == null) {
            if (other.network != null) {
                return false;
            }
        } else if (!network.equalsIgnoreCase(other.network)) {
            return false;
        }
        return true;
    }
}
