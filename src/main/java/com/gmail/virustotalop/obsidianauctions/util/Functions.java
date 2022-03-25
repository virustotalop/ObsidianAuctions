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

package com.gmail.virustotalop.obsidianauctions.util;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;

import java.text.DecimalFormat;
import java.util.UUID;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class Functions {

    public static String formatTime(int seconds, AuctionScope auctionScope) {
        String returnTime;
        if (seconds >= 60) {
            returnTime = AuctionConfig.getLanguageString("time-format-minsec", auctionScope);
            returnTime = returnTime.replace("%s", Integer.toString(seconds % 60));
            returnTime = returnTime.replace("%m", Integer.toString((seconds - (seconds % 60)) / 60));
        } else {
            returnTime = AuctionConfig.getLanguageString("time-format-seconly", auctionScope);
            returnTime = returnTime.replace("%s", Integer.toString(seconds));
        }
        return returnTime;
    }

    public static String removeUselessDecimal(String number) {
        if (number.endsWith(".0")) {
            number = number.replace(".0", "");
        }
        return number;
    }

    // Merges player's preset with the current specifications and system defaults.
    public static String[] mergeInputArgs(UUID playerUUID, String[] inputArgs, boolean validateArgs) {
        // Get existing defaults (if present)
        String[] resultArgs = new String[]{"this",
                removeUselessDecimal(Double.toString(AuctionConfig.getDouble("default-starting-bid", null))),
                removeUselessDecimal(Double.toString(AuctionConfig.getDouble("default-bid-increment", null))),
                Integer.toString(AuctionConfig.getInt("default-auction-time", null)), "0"};

        // Size increased in 2.10.0
        if (resultArgs.length < 5) {
            String[] tmp = resultArgs.clone();
            resultArgs = new String[]{tmp[0], tmp[1], tmp[2], tmp[3], "0"};
        }

        // Remove the "start" and "prep" args:
        String[] processArgs = inputArgs;
        if (processArgs.length > 0) {
            if (processArgs[0].equalsIgnoreCase("start") || processArgs[0].equalsIgnoreCase("s") || processArgs[0].equalsIgnoreCase("prep") || processArgs[0].equalsIgnoreCase("p")) {
                processArgs = new String[inputArgs.length - 1];
                System.arraycopy(inputArgs, 1, processArgs, 0, inputArgs.length - 1);
            }
        }

        // Quantity:
        if (processArgs.length > 0) {
            if (!processArgs[0].equalsIgnoreCase("-")) {
                resultArgs[0] = processArgs[0];
            }
            if (validateArgs) {
                // This is similar to the validation in Auction.java but without verifying availability.
                if (!resultArgs[0].equalsIgnoreCase("this") && !resultArgs[0].equalsIgnoreCase("hand") && !resultArgs[0].equalsIgnoreCase("all") && !resultArgs[0].matches("[0-9]{1,7}")) {
                    ObsidianAuctions.get().getMessageManager().sendPlayerMessage("parse-error-invalid-quantity", playerUUID, (AuctionScope) null);
                    return null;
                }
            }

            // Starting Price:
            if (processArgs.length > 1) {
                if (!processArgs[1].equalsIgnoreCase("-")) {
                    resultArgs[1] = processArgs[1];
                }
                if (validateArgs) {
                    if (resultArgs[1].isEmpty() || !resultArgs[1].matches(ObsidianAuctions.decimalRegex)) {
                        ObsidianAuctions.get().getMessageManager().sendPlayerMessage("parse-error-invalid-starting-bid", playerUUID, (AuctionScope) null);
                        return null;
                    }
                }

                // Increment
                if (processArgs.length > 2) {
                    if (!processArgs[2].equalsIgnoreCase("-")) {
                        resultArgs[2] = processArgs[2];
                    }
                    if (validateArgs) {
                        if (resultArgs[2].isEmpty() || !resultArgs[2].matches(ObsidianAuctions.decimalRegex)) {
                            ObsidianAuctions.get().getMessageManager().sendPlayerMessage("parse-error-invalid-max-bid", playerUUID, (AuctionScope) null);
                            return null;
                        }
                    }

                    // Time
                    if (processArgs.length > 3) {
                        if (!processArgs[3].equalsIgnoreCase("-")) {
                            resultArgs[3] = processArgs[3];
                        }
                        if (validateArgs) {
                            if (!resultArgs[3].matches("[0-9]{1,7}")) {
                                ObsidianAuctions.get().getMessageManager().sendPlayerMessage("parse-error-invalid-time", playerUUID, (AuctionScope) null);
                                return null;
                            }
                        }

                        // BuyNow
                        if (processArgs.length > 4) {
                            if (!processArgs[4].equalsIgnoreCase("-")) {
                                resultArgs[4] = processArgs[4];
                            }
                            if (validateArgs) {
                                if (resultArgs[4].isEmpty() || !resultArgs[4].matches(ObsidianAuctions.decimalRegex)) {
                                    ObsidianAuctions.get().getMessageManager().sendPlayerMessage("parse-error-invalid-buynow", playerUUID, (AuctionScope) null);
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        }

        return resultArgs;

    }

    // Money functions.
    public static String formatAmount(long safeMoney) {
        return formatAmount(getUnsafeMoney(safeMoney));
    }

    public static String formatAmount(double unsafeMoney) {
        String vaultFormat = ObsidianAuctions.get().getEconomy().format(unsafeMoney);
        return vaultFormat;
    }

    public static boolean hasBalance(UUID uuid, double preAuctionTax) {
        OfflinePlayer player = getOfflinePlayer(uuid);
        if (player == null) {
            return false;
        }
        return ObsidianAuctions.get().getEconomy().has(player, preAuctionTax);
    }

    public static boolean withdrawPlayer(UUID uuid, long safeMoney) {
        return withdrawPlayer(uuid, getUnsafeMoney(safeMoney));
    }

    public static boolean withdrawPlayer(UUID uuid, double unsafeMoney) {
        OfflinePlayer player = getOfflinePlayer(uuid);
        if (player == null) {
            return false;
        }
        EconomyResponse receipt = ObsidianAuctions.get().getEconomy().withdrawPlayer(player, unsafeMoney);
        return receipt.transactionSuccess();
    }

    public static boolean depositPlayer(UUID uuid, double unsafeMoney) {
        OfflinePlayer player = getOfflinePlayer(uuid);
        if (player == null) {
            return false;
        }
        EconomyResponse receipt = ObsidianAuctions.get().getEconomy().depositPlayer(player, unsafeMoney);
        return receipt.transactionSuccess();
    }

    public static long getSafeMoney(Double money) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        return Long.valueOf(twoDForm.format(money * Math.pow(10, ObsidianAuctions.decimalPlaces)));
    }

    public static double getUnsafeMoney(long money) {
        return (double) money / Math.pow(10, ObsidianAuctions.decimalPlaces);
    }

    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        OfflinePlayer player = Bukkit.getServer().getPlayer(uuid);
        return player != null ? player : Bukkit.getServer().getOfflinePlayer(uuid);
    }

    private Functions() {
    }
}