package com.gmail.virustotalop.obsidianauctions.item;

import org.bukkit.inventory.ItemStack;

public abstract class WrappedItem {

    private final ItemStack itemStack;

    public WrappedItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }
}