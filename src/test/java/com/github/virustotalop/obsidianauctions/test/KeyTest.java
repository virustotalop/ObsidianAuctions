package com.github.virustotalop.obsidianauctions.test;

import com.gmail.virustotalop.obsidianauctions.Key;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class KeyTest {

    @Test
    public void duplicateValueTest() {
        Set<String> values = new HashSet<>();
        for (Key key : Key.values()) {
            assertTrue(values.add(key.toString()), () -> "Duplicate key: " + key.name());
        }
    }
}