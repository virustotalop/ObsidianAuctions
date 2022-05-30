package com.gmail.virustotalop.obsidianauctions;

public enum Key {

    ALLOW_GAMEMODE_CREATIVE("allow-gamemode-creative"),
    AUCTION_HELP("auction-help"),
    AUCTION_ENABLED("auction-enabled"),
    AUCTION_DISABLED("auction-disabled"),
    AUCTION_FAIL_CANCEL_PREVENTION("auction-fail-cancel-prevention"),
    AUCTION_FAIL_CONSOLE("auction-fail-console"),
    AUCTION_FAIL_DISABLED("auction-fail-disabled"),
    AUCTION_FAIL_GAMEMODE_CREATIVE("auction-fail-gamemode-creative"),
    AUCTION_FAIL_NOT_OWNER_CANCEL("auction-fail-not-owner-cancel"),
    AUCTION_FAIL_NO_SEALED_AUCTION("auction-fail-no-sealed-auctions"),
    AUCTION_FAIL_NO_AUCTION_EXISTS("auction-fail-no-auction-exists"),
    AUCTION_FAIL_NO_AUCTIONS_ALLOWED("auction-fail-no-auctions-allowed"),
    AUCTION_FAIL_HAND_IS_EMPTY("auction-fail-hand-is-empty"),
    ALLOW_SEALED_AUCTIONS("allow-sealed-auctions"),
    ALLOW_UNSEALED_AUCTIONS("allow-unsealed-auctions"),
    ALLOW_EARLY_END("allow-early-end"),
    AUCTION_FAIL_NO_EARLY_END("auction-fail-no-early-end"),
    AUCTION_FAIL_NOT_OWNER_END("auction-fail-not-owner-end"),
    AUCTION_FAIL_NO_SCOPE("auction-fail-no-scope"),
    AUCTION_CANCEL_QUEUED("auction-cancel-queued"),
    AUCTION_QUEUE_STATUS_NOT_IN_QUEUE("auction-queue-status-not-in-queue"),
    AUCTION_INFO_NO_AUCTION("auction-info-no-auction"),
    BID_FAIL_NO_AUCTION("bid-fail-no-auction"),
    BID_FAIL_CONSOLE("bid-fail-console"),
    BID_FAIL_GAMEMODE_CREATIVE("bid-fail-gamemode-creative"),
    CANCEL_PREVENTION_SECONDS("cancel-prevention-seconds"),
    CANCEL_PREVENTION_PERCENT("cancel-prevention-percent"),
    CONFISCATE_FAIL_SELF("confiscate-fail-self"),
    CONFISCATE_FAIL_CONSOLE("confiscate-fail-console"),
    ENABLE_ACTION_BAR_MESSAGES("enable-actionbar-messages"),
    ENABLE_CHAT_MESSAGES("enable-chat-messages"),
    LOG_AUCTIONS("log-auctions"),
    PLUGIN_DISABLED_NO_ECONOMY("plugin-disabled-no-economy"),
    PLUGIN_RELOAD_FAIL_AUCTIONS_RUNNING("plugin-reload-fail-auctions-running"),
    PLUGIN_RELOADED("plugin-reloaded"),
    SUSPENSION_USER_FAIL_IS_OFFLINE("suspension-user-fail-is-offline"),
    SUSPENSION_USER_IS_ADMIN("suspension-user-fail-is-admin"),
    SUSPENSION_USER_FAIL_ALREADY_SUSPENDED("suspension-user-fail-already-suspended"),
    SUSPENSION_USER("suspension-user"),
    SUSPENSION_USER_SUCCESS("suspension-user-success"),
    SUSPENSION_GLOBAL("suspension-global"),
    UNSUSPENSION_USER_FAIL_IS_OFFLINE("unsuspension-user-fail-is-offline"),
    UNSUSPENSION_FAIL_PERMISSIONS("unsuspension-fail-permissions"),
    UNSUSPENSION_USER_FAIL_NOT_SUSPENDED("unsuspension-user-fail-not-suspended"),
    UNSUSPENSION_USER("unsuspension-user"),
    UNSUSPENSION_USER_SUCCESS("unsuspension-user-success"),
    UNSUSPENSION_GLOBAL("unsuspension-global");

    private final String configKey;

    Key(String configKey) {
        this.configKey = configKey;
    }

    @Override
    public String toString() {
        return this.configKey;
    }
}