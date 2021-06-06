package com.gmail.virustotalop.obsidianauctions.inject;

import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.listener.InventoryClickListener;
import com.gmail.virustotalop.obsidianauctions.listener.PlayerListener;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.google.inject.Binder;
import com.google.inject.Module;

public class AuctionModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(MessageManager.class).to(AuctionMessageManager.class);
        binder.bind(AuctionProhibitionManager.class).asEagerSingleton();
        binder.bind(InventoryClickListener.class).asEagerSingleton();
        binder.bind(PlayerListener.class).asEagerSingleton();
    }
}
