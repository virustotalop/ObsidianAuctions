package com.github.virustotalop.obsidianauctions.test.message;

import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageParser;
import com.google.inject.Guice;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AuctionMessageParserTest {

    @Test
    public void testParseConditionals() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("{true}some text", conditionals);
        assertEquals("some text", parsed);
    }

    @Test
    public void testParseConditionalsNot() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("{!true}some text", conditionals);
        assertEquals("", parsed);
    }

    @Test
    public void testParseConditionalsSkipText() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("false", false);
        String parsed = manager.parseConditionals("{false}some text{end-false}other text", conditionals);
        assertEquals("other text", parsed);
    }

    @Test
    public void testParseConditionalsEnd() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("false", false);
        String parsed = manager.parseConditionals("{false}{end}other text", conditionals);
        assertEquals("", parsed);
    }

    @Test
    public void testParseConditionalsInbetween() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("{true}%other text%{end-true}", conditionals);
        assertEquals("%other text%", parsed);
    }

    @Test
    public void testParseConditionalsDoNotEnd() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("{true}{end}other text", conditionals);
        assertEquals("other text", parsed);
    }

    @Test
    public void testParseConditionalsDoubleEndWithFalseEnd() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        conditionals.put("false", false);
        String parsed = manager.parseConditionals("{!false}{end}{false}{end}other text", conditionals);
        assertEquals("", parsed);
    }

    @Test
    public void testParseConditionalsDoNotEndMultipleConditionals() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        conditionals.put("also-true", true);
        String parsed = manager.parseConditionals("{true}{end}{also-true}{end}other text", conditionals);
        assertEquals("other text", parsed);
    }

    @Test
    public void testParseConditionalsOtherTextWithPercent() {
        AuctionMessageParser manager = Guice.createInjector(new AuctionMessageParserModule())
                .getInstance(AuctionMessageParser.class);
        Map<String, Boolean> conditionals = new HashMap<>();
        conditionals.put("true", true);
        String parsed = manager.parseConditionals("{true}%some-other-text%", conditionals);
        assertEquals("%some-other-text%", parsed);
    }
}