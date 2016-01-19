%A1 ➔ %auction-owner-name%

%A2 ➔ %auction-owner-display-name%

%A3 ➔ %auction-quantity%

%A4 ➔ %auction-bid-starting% //If the starting bid is 0

%A5 ➔ %auction-bid-increment%

%A6 ➔ %auction-buy-now%

%A7 ➔ %auction-remaining-time%

%A8 ➔ %auction-pre-tax%

%A9 ➔ %auction-post-tax%


%B1 ➔ %current-bid-name%

%B2 ➔ %current-bid-display-name%

%B3 ➔ %current-bid-amount%

%B4 ➔ %auction-bid-starting% //Same as %A4


%L1 ➔ %item-material-name%

%L2 ➔ %item-display-name%

%L3 ➔ %item-firework-power%

%L4 ➔ %item-book-author%

%L5 ➔ %item-book-title%

%L6 ➔ %item-durability-left%

%L7 ➔ %item-enchantments%


%P1 ➔ %auction-prep-amount-other%

%P2 ➔ %auction-prep-amount-other%

%P3 ➔ %auction-prep-price-formatted%

%P4 ➔ %auction-prep-price%

%P5 ➔ %auction-prep-increment-formatted%

%P6 ➔ %auction-prep-increment%

%P7 ➔ %auction-prep-time-formatted%

%P8 ➔ %auction-prep-time%

%P9 ➔ %auction-prep-buynow-formatted%

%P0 ➔ %auction-prep-buynow%


%S1 ➔ %player-auction-queue-position%

%S2 ➔ %auction-queue-length%

%S3 ➔ %auction-scope-name%

%S4 ➔ %auction-scope-id%


%R1 ➔ %repeatable-enchantment%

%R2 ➔ %repeatable-firework-payload%

%R3 ➔ %repeatable-lore%


%C1 ➔ %conditional-true%

%N ➔ %conditional-false%

```java
/*
Conditionals are a bit odd and I haven't used them.
If you use them tell me and if you can give me a description what you use them for.
*/

Map<String, Boolean> conditionals = new HashMap<String, Boolean>();
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && (message.contains("%C") || message.contains("%N"))) {
    	    	conditionals.put("1", player != null && floAuction.perms.has(player, "auction.admin"));
    	    	conditionals.put("2", player != null && floAuction.perms.has(player, "auction.start"));
    	    	conditionals.put("3", player != null && floAuction.perms.has(player, "auction.bid"));
    	    	conditionals.put("4", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0);
    	    	conditionals.put("5", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0);
    	    	conditionals.put("6", auction != null && auction.sealed);
    	    	conditionals.put("7", auction != null && !auction.sealed && auction.getCurrentBid() != null);
    	    	conditionals.put("8", isBroadcast);
    	    	conditionals.put("9", lot != null && Items.getBookTitle(lot) != null && !Items.getBookTitle(lot).isEmpty());
    	    	conditionals.put("0", lot != null && Items.getBookAuthor(lot) != null && !Items.getBookAuthor(lot).isEmpty());
    	    	conditionals.put("A", lot != null && Items.getLore(lot) != null && Items.getLore(lot).length > 0);
    	    	conditionals.put("B", lot != null && lot.getType().getMaxDurability() > 0 && lot.getDurability() > 0);
    	    	conditionals.put("C", lot != null && (lot.getType() == Material.FIREWORK || lot.getType() == Material.FIREWORK_CHARGE));
    	    	conditionals.put("D", auction != null && auction.getBuyNow() != 0);
    	    	conditionals.put("E", lot != null && ((lot.getEnchantments() != null && lot.getEnchantments().size() > 0) || (Items.getStoredEnchantments(lot) != null && Items.getStoredEnchantments(lot).size() > 0)));
    	    	conditionals.put("F", AuctionConfig.getBoolean("allow-max-bids", auctionScope));
    	    	conditionals.put("G", AuctionConfig.getBoolean("allow-buynow", auctionScope));
    	    	conditionals.put("H", AuctionConfig.getBoolean("allow-auto-bid", auctionScope));
    	    	conditionals.put("I", AuctionConfig.getBoolean("allow-early-end", auctionScope));
    	    	conditionals.put("J", AuctionConfig.getInt("cancel-prevention-percent", auctionScope) < 100);
    	    	conditionals.put("K", AuctionConfig.getBoolean("allow-unsealed-auctions", auctionScope));
    	    	conditionals.put("L", AuctionConfig.getBoolean("allow-sealed-auctions", auctionScope));
    	    	conditionals.put("M", conditionals.get("K") || conditionals.get("L"));
    	    	conditionals.put("N", auctionScope != null && auctionScope.getActiveAuction() != null);
    	    	conditionals.put("O", auctionScope != null && auctionScope.getAuctionQueueLength() > 0);
    			break;
    		}

```




