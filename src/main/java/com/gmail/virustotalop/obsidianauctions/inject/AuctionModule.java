package com.gmail.virustotalop.obsidianauctions.inject;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.command.AuctionCommands;
import com.gmail.virustotalop.obsidianauctions.command.CommandPermissionHandler;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.I18nItemConfig;
import com.gmail.virustotalop.obsidianauctions.language.I18nTranslationFactory;
import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import com.gmail.virustotalop.obsidianauctions.listener.InventoryClickListener;
import com.gmail.virustotalop.obsidianauctions.listener.PlayerListener;
import com.gmail.virustotalop.obsidianauctions.message.ActionBarManager;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageParser;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionModule implements Module {

    private final ObsidianAuctions plugin;
    private final BukkitAudiences adventure;
    private final Configuration i18nItemConfig;

    public AuctionModule(ObsidianAuctions plugin, BukkitAudiences adventure, Configuration i18nItemConfig) {
        this.plugin = plugin;
        this.adventure = adventure;
        this.i18nItemConfig = i18nItemConfig;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ObsidianAuctions.class).toInstance(this.plugin);
        binder.bind(JavaPlugin.class).toInstance(this.plugin);
        binder.bind(BukkitAudiences.class).toInstance(this.adventure);
        binder.bind(Configuration.class).annotatedWith(I18nItemConfig.class).toInstance(this.i18nItemConfig);
        binder.bind(TranslationFactory.class).to(I18nTranslationFactory.class).asEagerSingleton();
        binder.bind(ActionBarManager.class).asEagerSingleton();
        binder.bind(AuctionMessageParser.class).asEagerSingleton();
        binder.bind(MessageManager.class).to(AuctionMessageManager.class).asEagerSingleton();
        binder.bind(AuctionProhibitionManager.class).asEagerSingleton();
        binder.bind(InventoryClickListener.class).asEagerSingleton();
        binder.bind(PlayerListener.class).asEagerSingleton();
        binder.bind(AuctionCommands.class).asEagerSingleton();
        binder.bind(CommandPermissionHandler.class).asEagerSingleton();
    }
}