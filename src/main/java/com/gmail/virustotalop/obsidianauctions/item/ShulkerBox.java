package com.gmail.virustotalop.obsidianauctions.item;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class ShulkerBox extends WrappedItem {

    public ShulkerBox(ItemStack itemStack) {
        super(itemStack);
    }

    public Inventory getInventory() {
        BlockStateMeta meta = (BlockStateMeta) this.getItemStack().getItemMeta();
        org.bukkit.block.ShulkerBox shulker = (org.bukkit.block.ShulkerBox) meta.getBlockState();
        return shulker.getInventory();
    }
}
