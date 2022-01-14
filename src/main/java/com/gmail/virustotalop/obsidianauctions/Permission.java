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