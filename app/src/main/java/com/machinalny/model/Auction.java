package com.machinalny.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Auction {
    public static Auction NO_REPORT = Auction.builder().bidder("NO_REPORT").build();
    private String bidder;
    private Integer price;
    private Integer lastBid;
    private Integer stopPrice;
    private AuctionState state;

    public boolean allowsBid(Integer bid){
        return bid <= stopPrice;
    }

}
