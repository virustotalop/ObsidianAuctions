package com.gmail.virustotalop.obsidianauctions;

public enum Key {

    AUCTION_HELP("auction-help"),
    AUCTION_ENABLED("auction-enabled"),
    AUCTION_DISABLED("auction-disabled"),
    ALLOW_SEALED_AUCTIONS("allow-sealed-auctions"),
    ALLOW_UNSEALED_AUCTIONS("allow-unsealed-auctions"),
    AUCTION_FAIL_NO_SEALED_AUCTION("auction-fail-no-sealed-auctions"),
    AUCTION_FAIL_NO_AUCTION_EXISTS("auction-fail-no-auction-exists"),
    ALLOW_EARLY_END("allow-early-end"),
    AUCTION_FAIL_NO_EARLY_END("auction-fail-no-early-end"),
    AUCTION_FAIL_NOT_OWNER_END("auction-fail-not-owner-end"),
    AUCTION_FAIL_NO_SCOPE("auction-fail-no-scope"),
    AUCTION_CANCEL_QUEUED("auction-cancel-queued"),
    AUCTION_QUEUE_STATUS_NOT_IN_QUEUE("auction-queue-status-not-in-queue"),
    AUCTION_INFO_NO_AUCTION("auction-info-no-auction"),
    CANCEL_PREVENTION_SECONDS("cancel-prevention-seconds"),
    CANCEL_PREVENTION_PERCENT("cancel-prevention-percent"),
    ENABLE_ACTION_BAR_MESSAGES("enable-actionbar-messages"),
    ENABLE_CHAT_MESSAGES("enable-chat-messages"),
    PLUGIN_RELOAD_FAIL_AUCTIONS_RUNNING("plugin-reload-fail-auctions-running"),
    PLUGIN_RELOADED("plugin-reloaded"),

    private final String configKey;

    Key(String configKey) {
        this.configKey = configKey;
    }

    @Override
    public String toString() {
        return this.configKey;
    }
}