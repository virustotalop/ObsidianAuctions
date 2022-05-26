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

package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.UUID;

/**
 * Structure to hold and process the items being auctioned.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AuctionLot implements Serializable {

    private static final String ITEM_STACK_PATH = "itemstack";

    private static final long serialVersionUID = -1764290458703647129L;

    private UUID ownerUUID;
    private String ownerName;
    private final String itemSerialized;
    private int quantity = 0;

    /**
     * Constructor that sets owner and lot type.
     *
     * @param lotStack
     * @param ownerUUID
     * @param ownerName
     */
    public AuctionLot(ItemStack lotStack, UUID ownerUUID, String ownerName) {
        this.itemSerialized = this.serializeItem(lotStack);
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
    }

    private String serializeItem(ItemStack itemStack) {
        FileConfiguration temp = new YamlConfiguration();
        temp.set(ITEM_STACK_PATH, itemStack);
        return temp.saveToString();
    }

    public ItemStack deserializeItemString(String configContents) {
        FileConfiguration temp = new YamlConfiguration();
        try {
            temp.loadFromString(configContents);
            return temp.getItemStack(ITEM_STACK_PATH);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds items to this lot by removing them from a player.
     *
     * @param addQuantity     amount to move
     * @param removeFromOwner player to take items from
     * @return whether the items were moved
     */
    public boolean addItems(int addQuantity, boolean removeFromOwner) {
        if (removeFromOwner) {
            if (!Items.hasAmount(this.ownerName, addQuantity, getTypeStack())) {
                return false;
            }
            Items.remove(this.ownerName, addQuantity, getTypeStack());
        }
        this.quantity += addQuantity;
        return true;
    }

    /**
     * Public alias for giveLot(String playerName) used when we happen to be giving the lot to an auction winner or authorized confiscator.
     *
     * @param winnerUUID who receives the items
     */
    public void winLot(UUID winnerUUID, String winnerName) {
        this.giveLot(winnerUUID, winnerName);
    }

    /**
     * Cancels the lot by giving the items to the lots original owner.
     */
    public void cancelLot() {
        this.giveLot(this.ownerUUID, this.ownerName);
    }

    /**
     * Gives the items to a player, drops excess on ground or saves all of it to orphanage if the player is offline.
     *
     * @param playerUUID who receives the items
     */
    private void giveLot(UUID playerUUID, String playerName) {
        if (this.quantity == 0) {
            return;
        }
        ItemStack lotTypeLock = getTypeStack();
        Player player = Bukkit.getPlayer(playerUUID);

        this.ownerUUID = playerUUID;
        this.ownerName = playerName;

        int maxStackSize = lotTypeLock.getType().getMaxStackSize();
        if (player != null && player.isOnline()) {
            int amountToGive = 0;
            if (Items.hasSpace(player, this.quantity, lotTypeLock)) {
                amountToGive = this.quantity;
            } else {
                amountToGive = Items.getSpaceForItem(player, lotTypeLock);
            }
            // Give whatever items space permits at this time.
            ItemStack typeStack = getTypeStack();
            if (amountToGive > 0) {
                ObsidianAuctions.get().getMessageManager().sendPlayerMessage("lot-give", playerUUID, (AuctionScope) null);
            }
            while (amountToGive > 0) {
                ItemStack givingItems = lotTypeLock.clone();
                givingItems.setAmount(Math.min(maxStackSize, amountToGive));
                this.quantity -= givingItems.getAmount();
                Items.saferItemGive(player.getInventory(), givingItems);

                amountToGive -= maxStackSize;
            }
            if (this.quantity > 0) {
                // Drop items at player's feet.

                // Move items to drop lot.
                while (this.quantity > 0) {
                    ItemStack cloneStack = typeStack.clone();
                    cloneStack.setAmount(Math.min(this.quantity, Items.getMaxStackSize(typeStack)));
                    quantity -= cloneStack.getAmount();

                    // Drop lot.
                    Item drop = player.getWorld().dropItemNaturally(player.getLocation(), cloneStack);
                    drop.setItemStack(cloneStack);
                }
                ObsidianAuctions.get().getMessageManager().sendPlayerMessage("lot-drop", playerUUID, (AuctionScope) null);
            }
        } else {
            // Player is offline, queue lot for give on login.
            // Create orphaned lot to try to give when inventory clears up.
            final AuctionLot orphanLot = new AuctionLot(lotTypeLock, playerUUID, playerName);

            // Move items to orphan lot
            orphanLot.addItems(this.quantity, false);
            this.quantity = 0;

            // Queue for distribution on space availability.
            ObsidianAuctions.get().saveOrphanLot(orphanLot);
        }
    }

    /**
     * Gets a stack of a single item having the properties of all the items in this lot.
     *
     * @return item stack of one item
     */
    public ItemStack getTypeStack() {
        ItemStack lotTypeLock = this.deserializeItemString(this.itemSerialized);
        lotTypeLock.setAmount(1);
        return lotTypeLock;
    }

    /**
     * Gets the name of the owner of this lot.
     *
     * @return name of lot owner
     */
    public String getOwner() {
        return this.ownerName;
    }

    /**
     * Gets the quantity of items in this lot.
     *
     * @return quantity of items in lot
     */
    public int getQuantity() {
        return this.quantity;
    }
}
