package com.github.virustotalop.obsidianauctions.test.bukkit.plugin;

import com.gmail.virustotalop.obsidianauctions.placeholder.NoPlaceholderImpl;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class PapiTest {

    @Test
    public void testNoImplPapi() {
        Player player = Mockito.mock(Player.class);
        NoPlaceholderImpl papi = new NoPlaceholderImpl();
        String test = "test";
        assertEquals(test, papi.replace(player, "test"));
    }
}