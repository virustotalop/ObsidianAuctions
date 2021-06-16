package com.gmail.virustotalop.obsidianauctions.inject;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.I18nItemConfig;
import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import com.gmail.virustotalop.obsidianauctions.listener.InventoryClickListener;
import com.gmail.virustotalop.obsidianauctions.listener.PlayerListener;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class AuctionModule implements Module {

    private final BukkitAudiences adventure;
    private final Configuration i18nItemConfig;

    public AuctionModule(BukkitAudiences adventure, Configuration i18nItemConfig) {
        this.adventure = adventure;
        this.i18nItemConfig = i18nItemConfig;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(BukkitAudiences.class).toInstance(this.adventure);
        binder.bind(Configuration.class).annotatedWith(I18nItemConfig.class).toInstance(this.i18nItemConfig);
        binder.bind(TranslationFactory.class).asEagerSingleton();
        binder.bind(MessageManager.class).to(AuctionMessageManager.class);
        binder.bind(AuctionProhibitionManager.class).asEagerSingleton();
        binder.bind(InventoryClickListener.class).asEagerSingleton();
        binder.bind(PlayerListener.class).asEagerSingleton();
    }
}
