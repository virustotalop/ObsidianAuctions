package com.gmail.virustotalop.obsidianauctions.inject;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.arena.ArenaManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScopeManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScopeManagerListener;
import com.gmail.virustotalop.obsidianauctions.command.AuctionCommands;
import com.gmail.virustotalop.obsidianauctions.command.CommandPermissionHandler;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.Config;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.I18nItemConfig;
import com.gmail.virustotalop.obsidianauctions.language.I18nTranslationFactory;
import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import com.gmail.virustotalop.obsidianauctions.listener.ArenaListener;
import com.gmail.virustotalop.obsidianauctions.listener.InventoryClickListener;
import com.gmail.virustotalop.obsidianauctions.listener.PlayerListener;
import com.gmail.virustotalop.obsidianauctions.message.ActionBarManager;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageParser;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.papi.PlaceholderAPI;
import com.google.inject.Binder;
import com.google.inject.Module;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionModule implements Module {

    private final ObsidianAuctions plugin;
    private final BukkitAudiences adventure;
    private final Configuration config;
    private final Configuration i18nItemConfig;
    private final Class<? extends PlaceholderAPI> papiClazz;

    public AuctionModule(ObsidianAuctions plugin, BukkitAudiences adventure,
                         Configuration config, Configuration i18nItemConfig,
                         Class<? extends PlaceholderAPI> papiClazz) {
        this.plugin = plugin;
        this.adventure = adventure;
        this.config = config;
        this.i18nItemConfig = i18nItemConfig;
        this.papiClazz = papiClazz;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ObsidianAuctions.class).toInstance(this.plugin);
        binder.bind(JavaPlugin.class).toInstance(this.plugin);
        binder.bind(BukkitAudiences.class).toInstance(this.adventure);
        binder.bind(Configuration.class).annotatedWith(Config.class).toInstance(this.config);
        binder.bind(Configuration.class).annotatedWith(I18nItemConfig.class).toInstance(this.i18nItemConfig);
        binder.bind(AuctionScopeManager.class).asEagerSingleton();
        binder.bind(PlaceholderAPI.class).to(this.papiClazz).asEagerSingleton();
        binder.bind(ArenaManager.class).asEagerSingleton();
        binder.bind(TranslationFactory.class).to(I18nTranslationFactory.class).asEagerSingleton();
        binder.bind(ActionBarManager.class).asEagerSingleton();
        binder.bind(AuctionMessageParser.class).asEagerSingleton();
        binder.bind(MessageManager.class).to(AuctionMessageManager.class).asEagerSingleton();
        binder.bind(AuctionProhibitionManager.class).asEagerSingleton();
        binder.bind(InventoryClickListener.class).asEagerSingleton();
        binder.bind(PlayerListener.class).asEagerSingleton();
        binder.bind(ArenaListener.class).asEagerSingleton();
        binder.bind(AuctionScopeManagerListener.class).asEagerSingleton();
        binder.bind(AuctionCommands.class).asEagerSingleton();
        binder.bind(CommandPermissionHandler.class).asEagerSingleton();
    }
}