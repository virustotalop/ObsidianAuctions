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

package com.github.virustotalop.obsidianauctions.test.message;

import com.github.virustotalop.obsidianauctions.test.message.mock.MockTranslationFactory;
import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageParser;
import com.gmail.virustotalop.obsidianauctions.placeholder.NoPlaceholderImpl;
import com.gmail.virustotalop.obsidianauctions.placeholder.Placeholder;
import com.google.inject.Binder;
import com.google.inject.Module;

public class AuctionMessageParserModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(TranslationFactory.class).to(MockTranslationFactory.class).asEagerSingleton();
        binder.bind(AuctionMessageParser.class).asEagerSingleton();
        binder.bind(Placeholder.class).to(NoPlaceholderImpl.class).asEagerSingleton();
    }
}
