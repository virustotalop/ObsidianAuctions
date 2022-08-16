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