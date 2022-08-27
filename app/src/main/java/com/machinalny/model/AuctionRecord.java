package com.machinalny.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionRecord {
    private String bidder;
    private int price;
    private int increment;
    private int bid;
    private String auction;
    private AuctionMessageType messageType;
}
