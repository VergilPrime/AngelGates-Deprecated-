package me.entityreborn.AngelGates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.block.Block;

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
public class Gate {
    public static class TypeData {
        private int type;
        private int data = 0;

        public TypeData(int type, int data) {
            this.type = type;
            this.data = data;
        }

        private TypeData(int type) {
            this.type = type;
        }
        
        public int getType() {
            return type;
        }
        
        public int getData() {
            return data;
        }
    }

    public static final int ANYTHING = -1;
    public static final int ENTRANCE = -2;
    public static final int CONTROL = -3;
    public static final int EXIT = -4;
    private static HashMap<String, Gate> gates = new HashMap<String, Gate>();
    private static HashMap<Integer, ArrayList<Gate>> controlBlocks = new HashMap<Integer, ArrayList<Gate>>();
    private static HashSet<Integer> frameBlocks = new HashSet<Integer>();
    private String filename;
    private Character[][] layout;
    private HashMap<Character, TypeData> typedata;
    private RelativeBlockVector[] entrances = new RelativeBlockVector[0];
    private RelativeBlockVector[] border = new RelativeBlockVector[0];
    private RelativeBlockVector[] controls = new RelativeBlockVector[0];
    private RelativeBlockVector exitBlock = null;
    private HashMap<RelativeBlockVector, Integer> exits = new HashMap<RelativeBlockVector, Integer>();
    private int portalBlockOpen = Material.PORTAL.getId();
    private int portalBlockClosed = Material.AIR.getId();
    // iConomy information
    private int useCost = -1;
    private int createCost = -1;
    private int destroyCost = -1;
    private boolean toOwner = false;

    public Gate(String filename, Character[][] layout, HashMap<Character, TypeData> typedata) {
        this.filename = filename;
        this.layout = layout;
        this.typedata = typedata;

        populateCoordinates();
    }

    private void populateCoordinates() {
        ArrayList<RelativeBlockVector> entranceList = new ArrayList<RelativeBlockVector>();
        ArrayList<RelativeBlockVector> borderList = new ArrayList<RelativeBlockVector>();
        ArrayList<RelativeBlockVector> controlList = new ArrayList<RelativeBlockVector>();
        RelativeBlockVector[] relativeExits = new RelativeBlockVector[layout[0].length];
        int[] exitDepths = new int[layout[0].length];
        RelativeBlockVector lastExit = null;

        for (int y = 0; y < layout.length; y++) {
            for (int x = 0; x < layout[y].length; x++) {
                RelativeBlockVector vect = new RelativeBlockVector(x, y, 0);
                Integer id = typedata.get(layout[y][x]).getType();
                
                if (layout[y][x] == '-') {
                    controlList.add(vect);
                }

                if (id == ENTRANCE || id == EXIT) {
                    entranceList.add(vect);
                    exitDepths[x] = y;
                    
                    if (id == EXIT) {
                        this.exitBlock = vect;
                    }
                } else if (id != ANYTHING) {
                    borderList.add(vect);
                }
            }
        }

        for (int x = 0; x < exitDepths.length; x++) {
            relativeExits[x] = new RelativeBlockVector(x, exitDepths[x], 0);
        }

        for (int x = relativeExits.length - 1; x >= 0; x--) {
            if (relativeExits[x] != null) {
                lastExit = relativeExits[x];
            } else {
                relativeExits[x] = lastExit;
            }

            if (exitDepths[x] > 0) {
                this.exits.put(relativeExits[x], x);
            }
        }

        this.entrances = entranceList.toArray(this.entrances);
        this.border = borderList.toArray(this.border);
        this.controls = controlList.toArray(this.controls);
    }

    public void save(String gateFolder) {
        try {
            File fname = new File(gateFolder, filename);
            fname.getParentFile().mkdirs();
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(fname));

            writeConfig(bw, "portal-open", portalBlockOpen);
            writeConfig(bw, "portal-closed", portalBlockClosed);
            
            if (useCost != -1) {
                writeConfig(bw, "use-cost", useCost);
            }
            
            if (createCost != -1) {
                writeConfig(bw, "create-cost", createCost);
            }
            
            if (destroyCost != -1) {
                writeConfig(bw, "destroy-cost", destroyCost);
            }
            
            writeConfig(bw, "pay-owner", toOwner);

            for (Character type : typedata.keySet()) {
                Integer value = typedata.get(type).getType();
                // Skip control values
                if (value < 0) {
                    continue;
                }

                bw.append(type);
                bw.append('=');
                bw.append(value.toString());
                Integer mData = typedata.get(type).getData();
                // Append metadata
                if (mData != 0) {
                    bw.append(':');
                    bw.append(mData.toString());
                }
                bw.newLine();
            }

            bw.newLine();

            for (int y = 0; y < layout.length; y++) {
                for (int x = 0; x < layout[y].length; x++) {
                    Character symbol = layout[y][x];
                    bw.append(symbol);
                }
                bw.newLine();
            }

            bw.close();
        } catch (IOException ex) {
            AngelGates.log.log(Level.SEVERE, "Could not save Gate " + filename + " - " + ex.getMessage());
        }
    }

    private void writeConfig(BufferedWriter bw, String key, int value) throws IOException {
        bw.append(String.format("%s=%d", key, value));
        bw.newLine();
    }

    private void writeConfig(BufferedWriter bw, String key, boolean value) throws IOException {
        bw.append(String.format("%s=%b", key, value));
        bw.newLine();
    }

    public Character[][] getLayout() {
        return layout;
    }

    public HashMap<Character, TypeData> getTypeData() {
        return typedata;
    }

    public RelativeBlockVector[] getEntrances() {
        return entrances;
    }

    public RelativeBlockVector[] getBorder() {
        return border;
    }

    public RelativeBlockVector[] getControls() {
        return controls;
    }

    public HashMap<RelativeBlockVector, Integer> getExits() {
        return exits;
    }

    public RelativeBlockVector getExit() {
        return exitBlock;
    }

    public int getControlBlock() {
        return typedata.get('-').getType();
    }

    public String getFilename() {
        return filename;
    }

    public int getPortalBlockOpen() {
        return portalBlockOpen;
    }

    public void setPortalBlockOpen(int type) {
        portalBlockOpen = type;
    }

    public int getPortalBlockClosed() {
        return portalBlockClosed;
    }

    public void setPortalBlockClosed(int type) {
        portalBlockClosed = type;
    }

    public int getUseCost() {
        if (useCost < 0) {
            return EconomyHandler.useCost;
        }
        return useCost;
    }

    public Integer getCreateCost() {
        if (createCost < 0) {
            return EconomyHandler.createCost;
        }
        return createCost;
    }

    public Integer getDestroyCost() {
        if (destroyCost < 0) {
            return EconomyHandler.destroyCost;
        }
        return destroyCost;
    }

    public Boolean getToOwner() {
        return toOwner;
    }

    public boolean matches(Blox topleft, int modX, int modZ) {
        return matches(topleft, modX, modZ, false);
    }

    public boolean matches(Blox topleft, int modX, int modZ, boolean onCreate) {
        for (int y = 0; y < layout.length; y++) {
            for (int x = 0; x < layout[y].length; x++) {
                int id = typedata.get(layout[y][x]).getType();

                if (id == ENTRANCE || id == EXIT) {
                    // TODO: Remove once snowmanTrailEvent is added
                    if (AngelGates.ignoreEntrance) {
                        continue;
                    }

                    int type = topleft.modRelative(x, y, 0, modX, 1, modZ).getType();

                    // Ignore entrance if it's air and we're creating a new gate
                    if (onCreate && type == Material.AIR.getId()) {
                        continue;
                    }

                    if (type != portalBlockClosed && type != portalBlockOpen) {
                        // Special case for water gates
                        if (portalBlockOpen == Material.WATER.getId() || portalBlockOpen == Material.STATIONARY_WATER.getId()) {
                            if (type == Material.WATER.getId() || type == Material.STATIONARY_WATER.getId()) {
                                continue;
                            }
                        }
                        // Special case for lava gates
                        if (portalBlockOpen == Material.LAVA.getId() || portalBlockOpen == Material.STATIONARY_LAVA.getId()) {
                            if (type == Material.LAVA.getId() || type == Material.STATIONARY_LAVA.getId()) {
                                continue;
                            }
                        }
                        AngelGates.debug("Gate::Matches", "Entrance/Exit Material Mismatch: " + type);
                        return false;
                    }
                } else if (id != ANYTHING) {
                    if (topleft.modRelative(x, y, 0, modX, 1, modZ).getType() != id) {
                        AngelGates.debug("Gate::Matches", "Block Type Mismatch: " + topleft.modRelative(x, y, 0, modX, 1, modZ).getType() + " != " + id);
                        return false;
                    }
                    Integer mData = typedata.get(layout[y][x]).getData();
                    if (mData != 0 && topleft.modRelative(x, y, 0, modX, 1, modZ).getData() != mData) {
                        AngelGates.debug("Gate::Matches", "Block Data Mismatch: " + topleft.modRelative(x, y, 0, modX, 1, modZ).getData() + " != " + mData);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void registerGate(Gate gate) {
        gates.put(gate.getFilename(), gate);

        int blockID = gate.getControlBlock();

        if (!controlBlocks.containsKey(blockID)) {
            controlBlocks.put(blockID, new ArrayList<Gate>());
        }

        controlBlocks.get(blockID).add(gate);
    }

    public static Gate loadGate(File file) {
        Scanner scanner = null;
        boolean designing = false;
        ArrayList<ArrayList<Character>> design = new ArrayList<ArrayList<Character>>();
        HashMap<Character, TypeData> typedata = new HashMap<Character, TypeData>();
        HashMap<String, String> config = new HashMap<String, String>();
        HashSet<Integer> frameTypes = new HashSet<Integer>();
        int cols = 0;

        // Init types map
        typedata.put('.', new TypeData(ENTRANCE));
        typedata.put('*', new TypeData(EXIT));
        typedata.put(' ', new TypeData(ANYTHING));

        try {
            scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (designing) {
                    ArrayList<Character> row = new ArrayList<Character>();

                    if (line.length() > cols) {
                        cols = line.length();
                    }

                    for (Character symbol : line.toCharArray()) {
                        if ((symbol.equals('?')) || (!typedata.containsKey(symbol))) {
                            AngelGates.log.log(Level.SEVERE, "Could not load Gate " + file.getName() + " - Unknown symbol '" + symbol + "' in diagram");
                            return null;
                        }
                        row.add(symbol);
                    }

                    design.add(row);
                } else {
                    if ((line.isEmpty()) || (!line.contains("="))) {
                        designing = true;
                    } else {
                        String[] split = line.split("=");
                        String key = split[0].trim();
                        String value = split[1].trim();

                        if (key.length() == 1) {
                            Character symbol = key.charAt(0);
                            int meta = 0;
                            // Check for metadata
                            if (value.contains(":")) {
                                split = value.split(":");
                                value = split[0].trim();
                                String mData = split[1].trim();
                                meta = Integer.parseInt(mData);
                            }
                            Integer id = Integer.parseInt(value);

                            typedata.put(symbol, new TypeData(id, meta));
                            frameTypes.add(id);
                        } else {
                            config.put(key, value);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            AngelGates.log.log(Level.SEVERE, "Could not load Gate " + file.getName() + " - Invalid block ID given");
            return null;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        Character[][] layout = new Character[design.size()][cols];

        for (int y = 0; y < design.size(); y++) {
            ArrayList<Character> row = design.get(y);
            Character[] result = new Character[cols];

            for (int x = 0; x < cols; x++) {
                if (x < row.size()) {
                    result[x] = row.get(x);
                } else {
                    result[x] = ' ';
                }
            }

            layout[y] = result;
        }

        Gate gate = new Gate(file.getName(), layout, typedata);

        gate.portalBlockOpen = readConfig(config, gate, file, "portal-open", gate.portalBlockOpen);
        gate.portalBlockClosed = readConfig(config, gate, file, "portal-closed", gate.portalBlockClosed);
        gate.useCost = readConfig(config, gate, file, "use-cost", -1);
        gate.destroyCost = readConfig(config, gate, file, "destroy-cost", -1);
        gate.createCost = readConfig(config, gate, file, "create-cost", -1);
        gate.toOwner = (config.containsKey("pay-owner") ? Boolean.valueOf(config.get("pay-owner")) : EconomyHandler.payOwner);

        if (gate.getControls().length != 2) {
            AngelGates.log.log(Level.SEVERE, "Could not load Gate " + file.getName() + " - Gates must have exactly 2 control points.");
            return null;
        }

        // Merge frame types, add open mat to list
        frameBlocks.addAll(frameTypes);

        gate.save(file.getParent() + "/"); // Updates format for version changes
        return gate;
    }

    private static int readConfig(HashMap<String, String> config, Gate gate, File file, String key, int def) {
        if (config.containsKey(key)) {
            try {
                return Integer.parseInt(config.get(key));
            } catch (NumberFormatException ex) {
                AngelGates.log.log(Level.WARNING, String.format("%s reading %s: %s is not numeric", ex.getClass().getName(), file, key));
            }
        }

        return def;
    }

    public static void loadGates(String gateFolder) {
        File dir = new File(gateFolder);
        File[] files;

        if (dir.exists()) {
            files = dir.listFiles(new AngelGateFilenameFilter());
        } else {
            files = new File[0];
        }

        if (files.length == 0) {
            dir.mkdir();
            populateDefaults(gateFolder);
        } else {
            for (File file : files) {
                Gate gate = loadGate(file);
                if (gate != null) {
                    registerGate(gate);
                }
            }
        }
    }

    public static void populateDefaults(String gateFolder) {
        int obby = Material.OBSIDIAN.getId();
        Character[][] layout = new Character[][]{
            {' ', 'X', 'X', ' '},
            {'X', '.', '.', 'X'},
            {'-', '.', '.', '-'},
            {'X', '*', '.', 'X'},
            {' ', 'X', 'X', ' '},};
        HashMap<Character, TypeData> typedata = new HashMap<Character, TypeData>();
        typedata.put('.', new TypeData(ENTRANCE));
        typedata.put('*', new TypeData(EXIT));
        typedata.put(' ', new TypeData(ANYTHING));
        typedata.put('X', new TypeData(obby));
        typedata.put('-', new TypeData(obby));
        Gate gate = new Gate("nethergate.gate", layout, typedata);
        gate.save(gateFolder);
        registerGate(gate);
    }

    public static Gate[] getGatesByControlBlock(Block block) {
        return getGatesByControlBlock(block.getTypeId());
    }

    public static Gate[] getGatesByControlBlock(int type) {
        Gate[] result = new Gate[0];
        ArrayList<Gate> lookup = controlBlocks.get(type);

        if (lookup != null) {
            result = lookup.toArray(result);
        }

        return result;
    }

    public static Gate getGateByName(String name) {
        return gates.get(name);
    }

    public static int getGateCount() {
        return gates.size();
    }

    public static boolean isGateBlock(int type) {
        return frameBlocks.contains(type);
    }

    static class AngelGateFilenameFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".gate");
        }
    }

    public static void clearGates() {
        gates.clear();
        controlBlocks.clear();
        frameBlocks.clear();
    }
}
