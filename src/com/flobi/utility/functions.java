package com.flobi.utility;

import java.text.DecimalFormat;

import net.milkbowl.vault.economy.EconomyResponse;

import com.flobi.floAuction.floAuction;

public class functions {

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
















