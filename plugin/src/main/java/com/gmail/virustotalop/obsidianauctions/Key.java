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
public final class Key {

    public static final Key ACTION_BAR_TICKS = new Key("action-bar-ticks");
    public static final Key ALLOW_AUTO_BID = new Key("allow-auto-bid");
    public static final Key ALLOW_ARENAS = new Key("allow-arenas");
    public static final Key ALLOW_BUYNOW = new Key("allow-buynow");
    public static final Key ALLOW_BID_ON_OWN_AUCTION = new Key("allow-bid-on-own-auction");
    public static final Key ALLOW_DAMAGED_ITEMS = new Key("allow-damaged-items");
    public static final Key ALLOW_GAMEMODE_CHANGE = new Key("allow-gamemode-change");
    public static final Key ALLOW_GAMEMODE_CREATIVE = new Key("allow-gamemode-creative");
    public static final Key ALLOW_MAX_BIDS = new Key("allow-max-bids");
    public static final Key ALLOW_MOBSPAWNERS = new Key("allow-mobspawners");
    public static final Key ALLOW_RENAMED_ITEMS = new Key("allow-renamed-items");
    public static final Key ANTI_SNIPE = new Key("anti-snipe");
    public static final Key ANTI_SNIPE_PREVENTION_SECONDS = new Key("anti-snipe-prevention-seconds");
    public static final Key ANTI_SNIPE_TIME_ADDED = new Key("anti-snipe-time-added");
    public static final Key ARENA_WARNING = new Key("arena-warning");
    public static final Key AUCTION_CANCEL = new Key("auction-cancel");
    public static final Key AUCTION_CANCEL_PERMISSIONS = new Key("auction-cancel-permissions");
    public static final Key AUCTION_END = new Key("auction-end");
    public static final Key AUCTION_END_NOBIDS = new Key("auction-end-nobids");
    public static final Key AUCTION_END_PERMISSIONS = new Key("auction-end-permissions");
    public static final Key AUCTION_FAIL_ARENA = new Key("auction-fail-arena");
    public static final Key AUCTION_HELP = new Key("auction-help");
    public static final Key AUCTION_ENABLED = new Key("auction-enabled");
    public static final Key AUCTION_DISABLED = new Key("auction-disabled");
    public static final Key AUCTION_FAIL_AUCTION_EXISTS = new Key("auction-fail-auction-exists");
    public static final Key AUCTION_FAIL_BANNED = new Key("auction-fail-banned");
    public static final Key AUCTION_FAIL_BLACKLIST_NAME = new Key("auction-fail-blacklist-name");
    public static final Key AUCTION_FAIL_BLOCKED_BY_OTHER_PLUGIN = new Key("auction-fail-blocked-by-other-plugin");
    public static final Key AUCTION_FAIL_CANCEL_PREVENTION = new Key("auction-fail-cancel-prevention");
    public static final Key AUCTION_FAIL_CONSOLE = new Key("auction-fail-console");
    public static final Key AUCTION_FAIL_DAMAGED_ITEM = new Key("auction-fail-damaged-item");
    public static final Key AUCTION_FAIL_DISABLED = new Key("auction-fail-disabled");
    public static final Key AUCTION_FAIL_GAMEMODE_CREATIVE = new Key("auction-fail-gamemode-creative");
    public static final Key AUCTION_FAIL_INSUFFICIENT_SUPPLY = new Key("auction-fail-insufficient-supply");
    public static final Key AUCTION_FAIL_NOT_OWNER_CANCEL = new Key("auction-fail-not-owner-cancel");
    public static final Key AUCTION_FAIL_NO_SEALED_AUCTION = new Key("auction-fail-no-sealed-auctions");
    public static final Key AUCTION_FAIL_NO_AUCTION_EXISTS = new Key("auction-fail-no-auction-exists");
    public static final Key AUCTION_FAIL_NO_AUCTIONS_ALLOWED = new Key("auction-fail-no-auctions-allowed");
    public static final Key AUCTION_FAIL_HAND_IS_EMPTY = new Key("auction-fail-hand-is-empty");
    public static final Key AUCTION_FAIL_PERMISSIONS = new Key("auction-fail-permissions");
    public static final Key AUCTION_FAIL_QUANTITY_TOO_LOW = new Key("auction-fail-quantity-too-low");
    public static final Key AUCTION_FAIL_RENAMED_ITEM = new Key("auction-fail-renamed-item");
    public static final Key AUCTION_FAIL_START_TAX = new Key("auction-fail-start-tax");
    public static final Key AUCTION_FAIL_STARTING_BID_TOO_HIGH = new Key("auction-fail-starting-bid-too-high");
    public static final Key AUCTION_FAIL_STARTING_BID_TOO_LOW = new Key("auction-fail-starting-bid-too-low");
    public static final Key AUCTION_FAIL_SPAWNER = new Key("auction-fail-spawner");
    public static final Key AUCTION_INFO = new Key("auction-info");
    public static final Key AUCTION_INFO_BIDDER_NOONE = new Key("auction-info-bidder-noone");
    public static final Key AUCTION_INFO_ENCHANTMENT_NONE = new Key("auction-info-enchantment-none");
    public static final Key AUCTION_INFO_ENCHANTMENT_SEPARATOR = new Key("auction-info-enchantment-separator");
    public static final Key AUCTION_INFO_PAYLOAD_SEPARATOR = new Key("auction-info-payload-separator");
    public static final Key AUCTION_INFO_PERMISSIONS = new Key("auction-info-permissions");
    public static final Key AUCTION_START = new Key("auction-start");
    public static final Key AUCTION_START_TAX = new Key("auction-start-tax");
    public static final Key AUCTION_TOGGLE_PERMISSIONS = new Key("auction-toggle-permissions");
    public static final Key AUCTION_USE_PERMISSIONS = new Key("auction-use-permissions");
    public static final Key AUCTIONSCOPE_ESCAPE_WARNING = new Key("auctionscope-escape-warning");
    public static final Key AUCTIONSCOPE_FAIRWELL = new Key("auctionscope-fairwell");
    public static final Key AUCTIONSCOPE_WELCOME = new Key("auctionscope-welcome");
    public static final Key AUCTIONSCOPE_WELCOME_ONJOIN = new Key("auctionscope-welcome-onjoin");
    public static final Key ALLOW_SEALED_AUCTIONS = new Key("allow-sealed-auctions");
    public static final Key ALLOW_UNSEALED_AUCTIONS = new Key("allow-unsealed-auctions");
    public static final Key ALLOW_EARLY_END = new Key("allow-early-end");
    public static final Key AUCTION_END_TAX = new Key("auction-end-tax");
    public static final Key AUCTION_END_TAX_PERCENT = new Key("auction-end-tax-percent");
    public static final Key AUCTION_FAIL_BANNED_LORE = new Key("auction-fail-banned-lore");
    public static final Key AUCTION_FAIL_BUYNOW_TOO_HIGH = new Key("auction-fail-buynow-too-high");
    public static final Key AUCTION_FAIL_BUYNOW_TOO_LOW = new Key("auction-fail-buynow-too-low");
    public static final Key AUCTION_FAIL_INCREMENT_TOO_HIGH = new Key("auction-fail-increment-too-high");
    public static final Key AUCTION_FAIL_INCREMENT_TOO_LOW = new Key("auction-fail-increment-too-low");
    public static final Key AUCTION_FAIL_INVALID_OWNER = new Key("auction-fail-invalid-owner");
    public static final Key AUCTION_FAIL_NO_EARLY_END = new Key("auction-fail-no-early-end");
    public static final Key AUCTION_FAIL_NOT_OWNER_END = new Key("auction-fail-not-owner-end");
    public static final Key AUCTION_FAIL_NO_SCOPE = new Key("auction-fail-no-scope");
    public static final Key AUCTION_FAIL_TIME_TOO_HIGH = new Key("auction-fail-time-too-high");
    public static final Key AUCTION_FAIL_TIME_TOO_LOW = new Key("auction-fail-time-too-low");
    public static final Key AUCTION_CANCEL_QUEUED = new Key("auction-cancel-queued");
    public static final Key AUCTION_QUEUE_FAIL_CURRENT_AUCTION = new Key("auction-queue-fail-current-auction");
    public static final Key AUCTION_QUEUE_FAIL_FULL = new Key("auction-queue-fail-full");
    public static final Key AUCTION_QUEUE_FAIL_IN_QUEUE = new Key("auction-queue-fail-in-queue");
    public static final Key AUCTION_QUEUE_ENTER = new Key("auction-queue-enter");
    public static final Key AUCTION_QUEUE_STATUS_NOT_IN_QUEUE = new Key("auction-queue-status-not-in-queue");
    public static final Key AUCTION_QUEUE_PERMISSIONS = new Key("auction-queue-permissions");
    public static final Key AUCTION_INFO_NO_AUCTION = new Key("auction-info-no-auction");
    public static final Key BANNED_ITEMS = new Key("banned-items");
    public static final Key BANNED_LORE = new Key("banned-lore");
    public static final Key BID_AUTO_OUTBID = new Key("bid-auto-outbid");
    public static final Key BID_FAIL_ALREADY_CURRENT_BIDDER = new Key("bid-fail-already-current-bidder");
    public static final Key BID_FAIL_ARENA = new Key("bid-fail-arena");
    public static final Key BID_FAIL_AUTO_OUTBID = new Key("bid-fail-auto-outbid");
    public static final Key BID_FAIL_BLOCKED_BY_OTHER_PLUGIN = new Key("bid-fail-blocked-by-other-plugin");
    public static final Key BID_FAIL_BID_REQUIRED = new Key("bid-fail-bid-required");
    public static final Key BID_FAIL_BUYNOW_EXPIRED = new Key("bid-fail-buynow-expired");
    public static final Key BID_FAIL_CANNOT_ALLOCATE_FUNDS = new Key("bid-fail-cant-allocate-funds");
    public static final Key BID_FAIL_IS_AUCTION_OWNER = new Key("bid-fail-is-auction-owner");
    public static final Key BID_FAIL_NO_AUCTION = new Key("bid-fail-no-auction");
    public static final Key BID_FAIL_NO_BIDDER = new Key("bid-fail-no-bidder");
    public static final Key BID_FAIL_CONSOLE = new Key("bid-fail-console");
    public static final Key BID_FAIL_GAMEMODE_CREATIVE = new Key("bid-fail-gamemode-creative");
    public static final Key BID_FAIL_TOO_LOW = new Key("bid-fail-too-low");
    public static final Key BID_FAIL_UNDER_STARTING_BID = new Key("bid-fail-under-starting-bid");
    public static final Key BID_FAIL_OUTSIDE_AUCTIONHOUSE = new Key("bid-fail-outside-auctionhouse");
    public static final Key BID_FAIL_PERMISSIONS = new Key("bid-fail-permissions");
    public static final Key BID_SUCCESS_NO_CHALLENGER = new Key("bid-success-no-challenger");
    public static final Key BID_SUCCESS_OUTBID = new Key("bid-success-outbid");
    public static final Key BID_SUCCESS_SEALED = new Key("bid-success-sealed");
    public static final Key BID_SUCCESS_UPDATE_OWN_BID = new Key("bid-success-update-own-bid");
    public static final Key BID_SUCCESS_UPDATE_OWN_MAXBID = new Key("bid-success-update-own-maxbid");
    public static final Key BROADCAST_BID_UPDATES = new Key("broadcast-bid-updates");
    public static final Key CANCEL_PREVENTION_SECONDS = new Key("cancel-prevention-seconds");
    public static final Key CANCEL_PREVENTION_PERCENT = new Key("cancel-prevention-percent");
    public static final Key CHAT_PREFIX = new Key("chat-prefix");
    public static final Key CONFISCATE_FAIL_SELF = new Key("confiscate-fail-self");
    public static final Key CONFISCATE_FAIL_CONSOLE = new Key("confiscate-fail-console");
    public static final Key CONFISCATE_FAIL_PERMISSIONS = new Key("confiscate-fail-permissions");
    public static final Key CONFISCATE_SUCCESS = new Key("confiscate-success");
    public static final Key DEPOSIT_TAX_TO_USER = new Key("deposit-tax-to-user");
    public static final Key DEFAULT_AUCTION_TIME = new Key("default-auction-time");
    public static final Key DEFAULT_BID_INCREMENT = new Key("default-bid-increment");
    public static final Key DEFAULT_STARTING_BID = new Key("default-starting-bid");
    public static final Key DISABLED_COMMAND_INSCOPE_MESSAGE = new Key("disabled-command-inscope");
    public static final Key DISABLED_COMMANDS_INSCOPE = new Key("disabled-commands-inscope");
    public static final Key DISABLED_COMMAND_PARTICIPATING_MESSAGE = new Key("disabled-command-participating");
    public static final Key DISABLED_COMMANDS_PARTICIPATING = new Key("disabled-commands-participating");
    public static final Key ENABLE_ACTION_BAR_MESSAGES = new Key("enable-actionbar-messages");
    public static final Key ENABLE_CHAT_MESSAGES = new Key("enable-chat-messages");
    public static final Key EXPIRE_BUYNOW_AT_FIRST_BID = new Key("expire-buynow-at-first-bid");
    public static final Key FIREWORK_TRAIL = new Key("firework-trail");
    public static final Key FIREWORK_TWINKLE = new Key("firework-twinkle");
    public static final Key GAMEMODE_CHANGE_FAIL_PARTICIPATING = new Key("gamemodechange-fail-participating");
    public static final Key LOG_AUCTIONS = new Key("log-auctions");
    public static final Key LOT_DROP = new Key("lot-drop");
    public static final Key LOT_GIVE = new Key("lot-give");
    public static final Key MAX_AUCTION_QUEUE_LENGTH = new Key("max-auction-queue-length");
    public static final Key MAX_AUCTION_TIME = new Key("max-auction-time");
    public static final Key MAX_BUYNOW = new Key("max-buynow");
    public static final Key MAX_STARTING_BID = new Key("max-starting-bid");
    public static final Key MAX_BID_INCREMENT = new Key("max-bid-increment");
    public static final Key MIN_AUCTION_INTERVAL_SECS = new Key("min-auction-interval-secs");
    public static final Key MIN_AUCTION_TIME = new Key("min-auction-time");
    public static final Key MIN_BID_INCREMENT = new Key("min-bid-increment");
    public static final Key NAME_BLACKLIST = new Key("name-blacklist");
    public static final Key NAME_BLACKLIST_ENABLED = new Key("name-blacklist-enabled");
    public static final Key REMOTE_PLUGIN_PROHIBITION_DISABLED = new Key("remote-plugin-prohibition-disabled");
    public static final Key REMOTE_PLUGIN_PROHIBITION_ENABLED = new Key("remote-plugin-prohibition-enabled");
    public static final Key REMOTE_PLUGIN_PROHIBITION_REMINDER = new Key("remote-plugin-prohibition-reminder");
    public static final Key PARSE_ERROR_INVALID_BID = new Key("parse-error-invalid-bid");
    public static final Key PARSE_ERROR_INVALID_BID_INCREMENT = new Key("parse-error-invalid-bid-increment");
    public static final Key PARSE_ERROR_INVALID_BUYNOW = new Key("parse-error-invalid-buynow");
    public static final Key PARSE_ERROR_INVALID_MAX_BID = new Key("parse-error-invalid-max-bid");
    public static final Key PARSE_ERROR_INVALID_QUANTITY = new Key("parse-error-invalid-quantity");
    public static final Key PARSE_ERROR_INVALID_STARTING_BID = new Key("parse-error-invalid-starting-bid");
    public static final Key PARSE_ERROR_INVALID_TIME = new Key("parse-error-invalid-time");
    public static final Key PLUGIN_DISABLED_NO_ECONOMY = new Key("plugin-disabled-no-economy");
    public static final Key PLUGIN_RELOAD_FAIL_AUCTIONS_RUNNING = new Key("plugin-reload-fail-auctions-running");
    public static final Key PLUGIN_RELOAD_FAIL_PERMISSIONS = new Key("plugin-reload-fail-permissions");
    public static final Key PLUGIN_RELOADED = new Key("plugin-reloaded");
    public static final Key QUEUE_GUI_ITEM_AUCTIONED_BY = new Key("queue-gui-item-auctioned-by");
    public static final Key QUEUE_GUI_TITLE = new Key("queue-gui-title");
    public static final Key SUSPENSION_FAIL_PERMISSIONS = new Key("suspension-fail-permissions");
    public static final Key SUSPENSION_USER_FAIL_IS_OFFLINE = new Key("suspension-user-fail-is-offline");
    public static final Key SUSPENSION_USER_IS_ADMIN = new Key("suspension-user-fail-is-admin");
    public static final Key SUSPENSION_USER_FAIL_ALREADY_SUSPENDED = new Key("suspension-user-fail-already-suspended");
    public static final Key SUSPENSION_USER = new Key("suspension-user");
    public static final Key SUSPENSION_USER_SUCCESS = new Key("suspension-user-success");
    public static final Key SUSPENSION_GLOBAL = new Key("suspension-global");
    public static final Key SUPPRESS_COUNTDOWN = new Key("suppress-countdown");
    public static final Key TAXED_ITEMS = new Key("taxed-items");
    public static final Key TIMER_COUNTDOWN_NOTIFICATION = new Key("timer-countdown-notification");
    public static final Key TIME_FORMAT_MINSEC = new Key("time-format-minsec");
    public static final Key TIME_FORMAT_SECONLY = new Key("time-format-seconly");
    public static final Key USE_OLD_BID_LOGIC = new Key("use-old-bid-logic");
    public static final Key UNSUSPENSION_USER_FAIL_IS_OFFLINE = new Key("unsuspension-user-fail-is-offline");
    public static final Key UNSUSPENSION_FAIL_PERMISSIONS = new Key("unsuspension-fail-permissions");
    public static final Key UNSUSPENSION_USER_FAIL_NOT_SUSPENDED = new Key("unsuspension-user-fail-not-suspended");
    public static final Key UNSUSPENSION_USER = new Key("unsuspension-user");
    public static final Key UNSUSPENSION_USER_SUCCESS = new Key("unsuspension-user-success");
    public static final Key UNSUSPENSION_GLOBAL = new Key("unsuspension-global");

    public static Key create(String configKey) {
        return new Key(configKey);
    }

    private final String configKey;

    private Key(String configKey) {
        this.configKey = configKey;
    }

    @Override
    public String toString() {
        return this.configKey;
    }
}