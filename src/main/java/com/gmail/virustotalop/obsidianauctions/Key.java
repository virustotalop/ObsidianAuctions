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

public enum Key {

    ACTION_BAR_TICKS("action-bar-ticks"),
    ALLOW_AUTO_BID("allow-auto-bid"),
    ALLOW_ARENAS("allow-arenas"),
    ALLOW_BUYNOW("allow-buynow"),
    ALLOW_BID_ON_OWN_AUCTION("allow-bid-on-own-auction"),
    ALLOW_GAMEMODE_CHANGE("allow-gamemode-change"),
    ALLOW_GAMEMODE_CREATIVE("allow-gamemode-creative"),
    ALLOW_MAX_BIDS("allow-max-bids"),
    ANTI_SNIPE("anti-snipe"),
    ANTI_SNIPE_PREVENTION_SECONDS("anti-snipe-prevention-seconds"),
    ANTI_SNIPE_TIME_ADDED("anti-snipe-time-added"),
    ARENA_WARNING("arena-warning"),
    AUCTION_CANCEL("auction-cancel"),
    AUCTION_END("auction-end"),
    AUCTION_END_NOBIDS("auction-end-nobids"),
    AUCTION_FAIL_ARENA("auction-fail-arena"),
    AUCTION_HELP("auction-help"),
    AUCTION_ENABLED("auction-enabled"),
    AUCTION_DISABLED("auction-disabled"),
    AUCTION_FAIL_BANNED("auction-fail-banned"),
    AUCTION_FAIL_BLOCKED_BY_OTHER_PLUGIN("auction-fail-blocked-by-other-plugin"),
    AUCTION_FAIL_CANCEL_PREVENTION("auction-fail-cancel-prevention"),
    AUCTION_FAIL_CONSOLE("auction-fail-console"),
    AUCTION_FAIL_DISABLED("auction-fail-disabled"),
    AUCTION_FAIL_GAMEMODE_CREATIVE("auction-fail-gamemode-creative"),
    AUCTION_FAIL_INSUFFICIENT_SUPPLY("auction-fail-insufficient-supply"),
    AUCTION_FAIL_NOT_OWNER_CANCEL("auction-fail-not-owner-cancel"),
    AUCTION_FAIL_NO_SEALED_AUCTION("auction-fail-no-sealed-auctions"),
    AUCTION_FAIL_NO_AUCTION_EXISTS("auction-fail-no-auction-exists"),
    AUCTION_FAIL_NO_AUCTIONS_ALLOWED("auction-fail-no-auctions-allowed"),
    AUCTION_FAIL_HAND_IS_EMPTY("auction-fail-hand-is-empty"),
    AUCTION_FAIL_START_TAX("auction-fail-start-tax"),
    AUCTION_INFO("auction-info"),
    AUCTION_START("auction-start"),
    AUCTION_START_TAX("auction-start-tax"),
    AUCTIONSCOPE_ESCAPE_WARNING("auctionscope-escape-warning"),
    AUCTIONSCOPE_FAIRWELL("auctionscope-fairwell"),
    AUCTIONSCOPE_WELCOME("auctionscope-welcome"),
    AUCTIONSCOPE_WELCOME_ONJOIN("auctionscope-welcome-onjoin"),
    ALLOW_SEALED_AUCTIONS("allow-sealed-auctions"),
    ALLOW_UNSEALED_AUCTIONS("allow-unsealed-auctions"),
    ALLOW_EARLY_END("allow-early-end"),
    AUCTION_END_TAX("auction-end-tax"),
    AUCTION_END_TAX_PERCENT("auction-end-tax-percent"),
    AUCTION_FAIL_NO_EARLY_END("auction-fail-no-early-end"),
    AUCTION_FAIL_NOT_OWNER_END("auction-fail-not-owner-end"),
    AUCTION_FAIL_NO_SCOPE("auction-fail-no-scope"),
    AUCTION_CANCEL_QUEUED("auction-cancel-queued"),
    AUCTION_QUEUE_STATUS_NOT_IN_QUEUE("auction-queue-status-not-in-queue"),
    AUCTION_INFO_NO_AUCTION("auction-info-no-auction"),
    BANNED_ITEMS("banned-items"),
    BID_AUTO_OUTBID("bid-auto-outbid"),
    BID_FAIL_ALREADY_CURRENT_BIDDER("bid-fail-already-current-bidder"),
    BID_FAIL_ARENA("bid-fail-arena"),
    BID_FAIL_AUTO_OUTBID("bid-fail-auto-outbid"),
    BID_FAIL_BLOCKED_BY_OTHER_PLUGIN("bid-fail-blocked-by-other-plugin"),
    BID_FAIL_BID_REQUIRED("bid-fail-bid-required"),
    BID_FAIL_BUYNOW_EXPIRED("bid-fail-buynow-expired"),
    BID_FAIL_CANNOT_ALLOCATE_FUNDS("bid-fail-cant-allocate-funds"),
    BID_FAIL_IS_AUCTION_OWNER("bid-fail-is-auction-owner"),
    BID_FAIL_NO_AUCTION("bid-fail-no-auction"),
    BID_FAIL_NO_BIDDER("bid-fail-no-bidder"),
    BID_FAIL_CONSOLE("bid-fail-console"),
    BID_FAIL_GAMEMODE_CREATIVE("bid-fail-gamemode-creative"),
    BID_FAIL_TOO_LOW("bid-fail-too-low"),
    BID_FAIL_UNDER_STARTING_BID("bid-fail-under-starting-bid"),
    BID_FAIL_OUTSIDE_AUCTIONHOUSE("bid-fail-outside-auctionhouse"),
    BID_SUCCESS_NO_CHALLENGER("bid-success-no-challenger"),
    BID_SUCCESS_OUTBID("bid-success-outbid"),
    BID_SUCCESS_SEALED("bid-success-sealed"),
    BID_SUCCESS_UPDATE_OWN_BID("bid-success-update-own-bid"),
    BID_SUCCESS_UPDATE_OWN_MAXBID("bid-success-update-own-maxbid"),
    BROADCAST_BID_UPDATES("broadcast-bid-updates"),
    CANCEL_PREVENTION_SECONDS("cancel-prevention-seconds"),
    CANCEL_PREVENTION_PERCENT("cancel-prevention-percent"),
    CONFISCATE_FAIL_SELF("confiscate-fail-self"),
    CONFISCATE_FAIL_CONSOLE("confiscate-fail-console"),
    CONFISCATE_SUCCESS("confiscate-success"),
    DEPOSIT_TAX_TO_USER("deposit-tax-to-user"),
    DEFAULT_AUCTION_TIME("default-auction-time"),
    DISABLED_COMMAND_INSCOPE_MESSAGE("disabled-command-inscope"),
    DISABLED_COMMANDS_INSCOPE("disabled-commands-inscope"),
    DISABLED_COMMAND_PARTICIPATING_MESSAGE("disabled-command-participating"),
    DISABLED_COMMANDS_PARTICIPATING("disabled-commands-participating"),
    ENABLE_ACTION_BAR_MESSAGES("enable-actionbar-messages"),
    ENABLE_CHAT_MESSAGES("enable-chat-messages"),
    EXPIRE_BUYNOW_AT_FIRST_BID("expire-buynow-at-first-bid"),
    GAMEMODE_CHANGE_FAIL_PARTICIPATING("gamemodechange-fail-participating"),
    LOG_AUCTIONS("log-auctions"),
    LOT_DROP("lot-drop"),
    LOT_GIVE("lot-give"),
    REMOTE_PLUGIN_PROHIBITION_DISABLED("remote-plugin-prohibition-disabled"),
    REMOTE_PLUGIN_PROHIBITION_ENABLED("remote-plugin-prohibition-enabled"),
    REMOTE_PLUGIN_PROHIBITION_REMINDER("remote-plugin-prohibition-reminder"),
    PARSE_ERROR_INVALID_BID("parse-error-invalid-bid"),
    PARSE_ERROR_INVALID_BUYNOW("parse-error-invalid-buynow"),
    PARSE_ERROR_INVALID_MAX_BID("parse-error-invalid-max-bid"),
    PARSE_ERROR_INVALID_TIME("parse-error-invalid-time"),
    PLUGIN_DISABLED_NO_ECONOMY("plugin-disabled-no-economy"),
    PLUGIN_RELOAD_FAIL_AUCTIONS_RUNNING("plugin-reload-fail-auctions-running"),
    PLUGIN_RELOADED("plugin-reloaded"),
    QUEUE_GUI_ITEM_AUCTIONED_BY("queue-gui-item-auctioned-by"),
    QUEUE_GUI_TITLE("queue-gui-title"),
    SUSPENSION_USER_FAIL_IS_OFFLINE("suspension-user-fail-is-offline"),
    SUSPENSION_USER_IS_ADMIN("suspension-user-fail-is-admin"),
    SUSPENSION_USER_FAIL_ALREADY_SUSPENDED("suspension-user-fail-already-suspended"),
    SUSPENSION_USER("suspension-user"),
    SUSPENSION_USER_SUCCESS("suspension-user-success"),
    SUSPENSION_GLOBAL("suspension-global"),
    SUPPRESS_COUNTDOWN("suppress-countdown"),
    TAXED_ITEMS("taxed-items"),
    TIMER_COUNTDOWN_NOTIFICATION("timer-countdown-notification"),
    USE_OLD_BID_LOGIC("use-old-bid-logic"),
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