package com.github.virustotalop.obsidianauctions.test.util.mock;

import com.github.virustotalop.obsidianauctions.test.util.mock.listener.MockListenerOne;
import com.github.virustotalop.obsidianauctions.test.util.mock.listener.MockListenerTwo;
import com.google.inject.Binder;
import com.google.inject.Module;

public class MockInjectorModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(MockListenerOne.class).toInstance(new MockListenerOne());
        binder.bind(MockListenerTwo.class).toInstance(new MockListenerTwo());
    }
}