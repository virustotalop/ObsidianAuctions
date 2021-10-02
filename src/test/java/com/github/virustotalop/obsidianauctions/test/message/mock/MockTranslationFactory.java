package com.github.virustotalop.obsidianauctions.test.message.mock;

import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import org.bukkit.inventory.ItemStack;

public class MockTranslationFactory implements TranslationFactory {
    @Override
    public String getTranslation(ItemStack itemStack) {
        return "";
    }
}
