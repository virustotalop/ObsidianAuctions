/*
 *     ObsidianAuctions
 *     Copyright (C) 2012-2022 flobi and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.virustotalop.obsidianauctions.util;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class FileUtil {

    /**
     * Load an object from a file
     *
     * @param fileName   the file to be loaded
     * @param loadIfNull the object to be used if the loaded value is null
     * @return the resulting string list
     */

    @SuppressWarnings({"unchecked"})
    public static <T> T load(String fileName, T loadIfNull) {
        Object importedObject = null;
        File loadFile = new File(ObsidianAuctions.get().getDataFolder(), fileName);
        if (loadFile.exists()) {
            try {
                InputStream inputStream = new FileInputStream(loadFile.getAbsolutePath());
                InputStream buffer = new BufferedInputStream(inputStream);
                ObjectInput input = new ObjectInputStream(buffer);
                importedObject = input.readObject();
                input.close();
                buffer.close(); //make sure these are closed
                inputStream.close(); //make sure these are closed
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        if (importedObject == null) {
            importedObject = loadIfNull;
        }
        return (T) importedObject;
    }

    /**
     * Saves an object to a file.
     *
     * @param object   object to save
     * @param fileName name of file
     */

    public static void save(Object object, String fileName) {
        File saveFile = new File(ObsidianAuctions.get().getDataFolder(), fileName);
        try {
            if (saveFile.exists()) {
                saveFile.delete();
            }
            FileOutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            try {
                output.writeObject(object);
            } finally {
                output.close();
                buffer.close(); //make sure these are closed
                file.close(); //make sure these are closed
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private FileUtil() {
    }
}