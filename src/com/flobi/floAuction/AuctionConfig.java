package com.flobi.floAuction;

import com.flobi.utility.functions;

public class AuctionConfig {
	public static long getSafeMoneyFromDouble(String path, AuctionScope auctionScope) {
		Double configDouble = null; 
		floAuction.server.broadcastMessage("scoped value being checked");
		if (auctionScope != null) {
			floAuction.server.broadcastMessage("scoped detected");
			if (auctionScope.getConfig() != null) {
				floAuction.server.broadcastMessage("scoped config found");
				configDouble = auctionScope.getConfig().getDouble(path);
			}
		}
		if (configDouble == null) configDouble = floAuction.config.getDouble(path);
		return functions.getSafeMoney(configDouble);
	}
}
