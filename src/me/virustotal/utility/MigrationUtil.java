package me.virustotal.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

import com.flobi.floAuction.floAuction;

public class MigrationUtil {

	public static void migrateOldData(floAuction plugin)
	{
		String path = plugin.getDataFolder().getAbsolutePath();
		String strippedPath = path.substring(0, path.lastIndexOf(File.separator));

		for(File file : new File(strippedPath).listFiles())
		{
			if(file.isFile())
			{
				String fileName = file.getName();
				if(fileName.contains("floAuction") && fileName.endsWith(".jar"))
				{
					plugin.getLogger().log(Level.INFO, "Disabling floAuction");
					try 
					{
						MigrationUtil.copyFile(file, new File(file.getAbsolutePath() + ".dis"));
						file.delete();
						break;
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
						break;
					}
				}
			}
		}

		File pathFolder = new File(path);
		plugin.getLogger().log(Level.INFO, "Checking to see if migration is needed...");
		if(pathFolder.exists())
		{
			plugin.getLogger().log(Level.INFO, "Migration not needed, skipping migration!");
		}
		else
		{

			String floAuctionPath = strippedPath + File.separator + "floAuction";
			File floAuctionFolder = new File(floAuctionPath);
			if(floAuctionFolder.exists())
			{
				pathFolder.mkdirs();
				for(File file : floAuctionFolder.listFiles())
				{
					if(file.getName().toLowerCase().endsWith(".yml"))
					{
						try 
						{
							File newFile = new File(path + File.separator + file.getName());
							MigrationUtil.copyFile(file, newFile);
						} 
						catch (IOException e) 
						{
							e.printStackTrace();
						}
						plugin.getLogger().log(Level.INFO, "Migrated file " + file.getName() + " to the ObsidianAuctions folder!");
					}
				}
			}
			else
			{
				plugin.getLogger().log(Level.INFO, "No data to migrate, will create default data!");
			}
		}
	}

	private static void copyFile(File sourceFile, File destFile) throws IOException 
	{
		if(!destFile.exists()) 
		{
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try 
		{
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally 
		{
			if(source != null) 
			{
				source.close();
			}
			if(destination != null) 
			{
				destination.close();
			}
		}
	}
}
