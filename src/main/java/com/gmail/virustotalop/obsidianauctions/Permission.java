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

package com.gmail.virustotalop.obsidianauctions;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class Permission {

    public static final String AUCTION_USE = "auction.use";
    public static final String AUCTION_START = "auction.start";
    public static final String AUCTION_END = "auction.end";
    public static final String AUCTION_CANCEL = "auction.cancel";
    public static final String AUCTION_TOGGLE = "auction.toggle";
    public static final String AUCTION_BID = "auction.bid";
    public static final String AUCTION_QUEUE = "auction.queue";
    public static final String AUCTION_INFO = "auction.info";

    public static final String AUCTION_ADMIN_RELOAD = "auction.admin.reload";
    public static final String AUCTION_ADMIN_SUSPEND = "auction.admin.suspend";
    public static final String AUCTION_ADMIN_RESUME = "auction.admin.resume";
    public static final String AUCTION_ADMIN_CANCEL = "auction.admin.cancel";
    public static final String AUCTION_ADMIN_CONFISCATE = "auction.admin.confiscate";

    private Permission() {
    }
}