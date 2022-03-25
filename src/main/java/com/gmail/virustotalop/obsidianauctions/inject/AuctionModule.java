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

package com.gmail.virustotalop.obsidianauctions.inject;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLocationManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManagerListener;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
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
import com.gmail.virustotalop.obsidianauctions.placeholder.Placeholder;
import com.google.inject.Binder;
import com.google.inject.Module;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionModule implements Module {

    private final ObsidianAuctions plugin;
    private final BukkitAudiences adventure;
    private final Configuration config;
    private final Configuration i18nItemConfig;
    private final Class<? extends Placeholder> placeholderClazz;

    public AuctionModule(ObsidianAuctions plugin, BukkitAudiences adventure,
                         Configuration config, Configuration i18nItemConfig,
                         Class<? extends Placeholder> placeholderClazz) {
        this.plugin = plugin;
        this.adventure = adventure;
        this.config = config;
        this.i18nItemConfig = i18nItemConfig;
        this.placeholderClazz = placeholderClazz;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ObsidianAuctions.class).toInstance(this.plugin);
        binder.bind(JavaPlugin.class).toInstance(this.plugin);
        binder.bind(BukkitAudiences.class).toInstance(this.adventure);
        binder.bind(Configuration.class).annotatedWith(Config.class).toInstance(this.config);
        binder.bind(Configuration.class).annotatedWith(I18nItemConfig.class).toInstance(this.i18nItemConfig);
        binder.bind(AuctionManager.class).asEagerSingleton();
        binder.bind(Placeholder.class).to(this.placeholderClazz).asEagerSingleton();
        binder.bind(AuctionLocationManager.class).asEagerSingleton();
        binder.bind(TranslationFactory.class).to(I18nTranslationFactory.class).asEagerSingleton();
        binder.bind(ActionBarManager.class).asEagerSingleton();
        binder.bind(AuctionMessageParser.class).asEagerSingleton();
        binder.bind(MessageManager.class).to(AuctionMessageManager.class).asEagerSingleton();
        binder.bind(AuctionProhibitionManager.class).asEagerSingleton();
        binder.bind(InventoryClickListener.class).asEagerSingleton();
        binder.bind(PlayerListener.class).asEagerSingleton();
        binder.bind(ArenaListener.class).asEagerSingleton();
        binder.bind(AuctionManagerListener.class).asEagerSingleton();
        binder.bind(AuctionCommands.class).asEagerSingleton();
        binder.bind(CommandPermissionHandler.class).asEagerSingleton();
    }
}