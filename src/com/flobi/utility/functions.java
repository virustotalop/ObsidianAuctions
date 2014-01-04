package com.flobi.utility;

import java.text.DecimalFormat;

import net.milkbowl.vault.economy.EconomyResponse;

import com.flobi.floAuction.floAuction;

public class functions {

	public static String formatTime(int seconds) {
		String returnTime = "-";
		if (seconds >= 60) {
			returnTime = floAuction.textConfig.getString("time-format-minsec");
			returnTime = returnTime.replace("%s", Integer.toString(seconds % 60));
			returnTime = returnTime.replace("%m", Integer.toString((seconds - (seconds % 60)) / 60));
		} else {
			returnTime = floAuction.textConfig.getString("time-format-seconly");
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
	public static String[] mergeInputArgs(String playerName, String[] inputArgs, boolean validateArgs) {
		// Get existing defaults (if present)
		String[] resultArgs = null;
		
		// if player has no preset, use the current system defaults:
		if (floAuction.userSavedInputArgs.get(playerName) == null) {
			resultArgs = new String[]{"this", removeUselessDecimal(Double.toString(getUnsafeMoney(floAuction.defaultStartingBid))), removeUselessDecimal(Double.toString(getUnsafeMoney(floAuction.defaultBidIncrement))), Integer.toString(floAuction.defaultAuctionTime), "0"};
		} else {
			resultArgs = floAuction.userSavedInputArgs.get(playerName).clone();
		}
		
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
				if (
					!resultArgs[0].equalsIgnoreCase("this") && 
					!resultArgs[0].equalsIgnoreCase("hand") && 
					!resultArgs[0].equalsIgnoreCase("all") && 
					!resultArgs[0].matches("[0-9]{1,7}")
				) {
					floAuction.sendMessage("parse-error-invalid-quantity", playerName, null);
					return null;
				}
			}
			
			// Starting Price:
			if (processArgs.length > 1) {
				if (!processArgs[1].equalsIgnoreCase("-")) {
					resultArgs[1] = processArgs[1];
				}
				if (validateArgs) {
					if (resultArgs[1].isEmpty() || !resultArgs[1].matches(floAuction.decimalRegex)) {
						floAuction.sendMessage("parse-error-invalid-starting-bid", playerName, null);
						return null;
					}
				}
				
				// Increment
				if (processArgs.length > 2) {
					if (!processArgs[2].equalsIgnoreCase("-")) {
						resultArgs[2] = processArgs[2];
					}
					if (validateArgs) {
						if (resultArgs[2].isEmpty() || !resultArgs[2].matches(floAuction.decimalRegex)) {
							floAuction.sendMessage("parse-error-invalid-max-bid", playerName, null);
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
								floAuction.sendMessage("parse-error-invalid-time", playerName, null);
								return null;
							}
						}

						// BuyNow
						if (processArgs.length > 4) {
							if (!processArgs[4].equalsIgnoreCase("-")) {
								resultArgs[4] = processArgs[4];
							}
							if (validateArgs) {
								if (resultArgs[4].isEmpty() || !resultArgs[4].matches(floAuction.decimalRegex)) {
									floAuction.sendMessage("parse-error-invalid-buynow", playerName, null);
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
		if (floAuction.econ == null) return "-";
		if (!floAuction.econ.isEnabled()) return "-";
		return floAuction.econ.format(unsafeMoney);
	}
	
	public static boolean withdrawPlayer(String playerName, long safeMoney) {
		return withdrawPlayer(playerName, getUnsafeMoney(safeMoney));
	}
	
	public static boolean withdrawPlayer(String playerName, double unsafeMoney) {
		EconomyResponse receipt = floAuction.econ.withdrawPlayer(playerName, unsafeMoney);
		return receipt.transactionSuccess();
	}
	
	public static boolean depositPlayer(String playerName, double unsafeMoney) {
		EconomyResponse receipt = floAuction.econ.depositPlayer(playerName, unsafeMoney);
		return receipt.transactionSuccess();
	}
	
	public static long getSafeMoney(Double money) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        return Long.valueOf(twoDForm.format(money * Math.pow(10, floAuction.decimalPlaces)));
	}
	
	public static double getUnsafeMoney(long money) {
		return (double)money / Math.pow(10, floAuction.decimalPlaces);
	}

}
















