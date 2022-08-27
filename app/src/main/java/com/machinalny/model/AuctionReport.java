package com.machinalny.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuctionReport {
    public static AuctionReport NO_REPORT = AuctionReport.builder().bidder("NO_REPORT").build();
    private String bidder;
    private Integer price;
    private AuctionState state;

}
