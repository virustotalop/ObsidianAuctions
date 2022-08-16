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

import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageParser;
import com.google.inject.Guice;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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