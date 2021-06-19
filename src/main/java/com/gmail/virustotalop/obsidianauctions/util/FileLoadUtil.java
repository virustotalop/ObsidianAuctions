package com.gmail.virustotalop.obsidianauctions.util;

import com.gmail.virustotalop.obsidianauctions.auction.AuctionLot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FileLoadUtil {

    private FileLoadUtil() {}

    /**
     * Load a String array from a file.
     *
     * @param saveFile the file to be saved
     * @return the resulting string list
     */
    @SuppressWarnings({"unchecked", "finally"})
    public static List<String> loadStringList(File saveFile) {
        List<String> importedObjects = new ArrayList<String>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (List<String>) input.readObject();
            input.close();
            buffer.close(); //make sure these are closed
            file.close(); //make sure these are closed
        } finally {
            return importedObjects;
        }
    }

    /**
     * Load a UUID array from a file.
     *
     * @param saveFile the file to be saved
     * @return the resulting string list
     */
    @SuppressWarnings({"unchecked", "finally"})
    public static List<UUID> loadUUIDList(File saveFile) {
        List<UUID> importedObjects = new ArrayList<>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (List<UUID>) input.readObject();
            input.close();
            buffer.close(); //make sure these are closed
            file.close(); //make sure these are closed
        } finally {
            return importedObjects;
        }
    }

    /**
     * Load a String String map from a file.
     *
     * @param saveFile the file to be saved
     * @return the resulting string string map
     */
    @SuppressWarnings({"unchecked", "finally"})
    public static Map<String, String[]> loadMapStringStringArray(File saveFile) {
        Map<String, String[]> importedObjects = new HashMap<>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (Map<String, String[]>) input.readObject();
            input.close();
            buffer.close();//make sure these are closed
            file.close();//make sure these are closed
        } finally {
            return importedObjects;
        }
    }

    /**
     * Load a UUID String map from a file.
     *
     * @param saveFile the file to be saved
     * @return the resulting string string map
     */
    @SuppressWarnings({"unchecked", "finally"})
    public static Map<UUID, String[]> loadMapUUIDStringArray(File saveFile) {
        Map<UUID, String[]> importedObjects = new HashMap<>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (Map<UUID, String[]>) input.readObject();
            input.close();
            buffer.close();//make sure these are closed
            file.close();//make sure these are closed
        } finally {
            return importedObjects;
        }
    }

    /**
     * Load a list of AuctionLot from a file.
     *
     * @param saveFile the file to be saved
     * @return the loaded list
     */
    @SuppressWarnings("unchecked")
    public static List<AuctionLot> loadListAuctionLot(File saveFile) {
        List<AuctionLot> importedObjects = new ArrayList<>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (List<AuctionLot>) input.readObject();
            input.close();
            buffer.close(); //make sure these are closed
            file.close();   //make sure these are closed
        } catch(IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return importedObjects;
    }

}
