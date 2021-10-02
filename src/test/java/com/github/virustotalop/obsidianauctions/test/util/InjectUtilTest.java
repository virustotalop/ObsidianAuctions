package com.github.virustotalop.obsidianauctions.test.util;

import com.github.virustotalop.obsidianauctions.test.util.mock.MockInjectorModule;
import com.github.virustotalop.obsidianauctions.test.util.mock.MockListener;
import com.github.virustotalop.obsidianauctions.test.util.mock.listener.MockListenerOne;
import com.github.virustotalop.obsidianauctions.test.util.mock.listener.MockListenerTwo;
import com.gmail.virustotalop.obsidianauctions.util.InjectUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InjectUtilTest {

    @Test
    public void testCollect() {
        Injector injector = Guice.createInjector(new MockInjectorModule());
        List<MockListener> collected = InjectUtil.collect(MockListener.class, injector);
        assertEquals(2, collected.size());
        assertEquals(MockListenerOne.class, collected.get(0).getClass());
        assertEquals(MockListenerTwo.class, collected.get(1).getClass());
    }
}