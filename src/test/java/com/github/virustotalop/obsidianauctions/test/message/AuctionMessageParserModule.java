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
