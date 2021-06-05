package com.github.virustotalop.obsidianauctions.test;

import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AuctionMessageManagerTest {

    @Test
    public void testParseConditionals() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%true%some text", conditionals);
        assertEquals("some text", parsed);
    }

    @Test
    public void testParseConditionalsNot() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%!true%some text", conditionals);
        assertEquals("", parsed);
    }

    @Test
    public void testParseConditionalsSkipText() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("false", false);
        String parsed = manager.parseConditionals("%false%some text%end-false%other text", conditionals);
        assertEquals("other text", parsed);
    }

    @Test
    public void testParseConditionalsEnd() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("false", false);
        String parsed = manager.parseConditionals("%false%%end%other text", conditionals);
        assertEquals("", parsed);
    }

    @Test
    public void testParseConditionalsInbetween() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%true%%other text%%end-true%", conditionals);
        assertEquals("%other text%", parsed);
    }

    @Test
    public void testParseConditionalsDoNotEnd() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%true%%end%other text", conditionals);
        assertEquals("other text", parsed);
    }

    @Test
    public void testParseConditionalsDoNotEndMultipleConditionals() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        conditionals.put("also-true", true);
        String parsed = manager.parseConditionals("%true%%end%%also-true%%end%other text", conditionals);
        assertEquals("other text", parsed);
    }

    @Test
    public void testParseConditionalsOtherTextWithPercent() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%true%%some-other-text%", conditionals);
        assertEquals("%some-other-text%", parsed);
    }

    @Test
    public void testParseConditionalsDanglingPercent() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("% a", conditionals);
        assertEquals("% a", parsed);
    }

    @Test
    public void testParseConditionalsDanglingPercentWithOtherText() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%a", conditionals);
        assertEquals("%a", parsed);
    }

    @Test
    public void testParseConditionalsDanglingPercentWithOtherTextWithSpaceAtEnd() {
        AuctionMessageManager manager = new AuctionMessageManager();
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("%a ", conditionals);
        assertEquals("%a ", parsed);
    }
}