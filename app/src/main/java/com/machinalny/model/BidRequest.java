package com.machinalny.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BidRequest {
    private String bidder;
    private String auction;
    @Builder.Default
    private Integer stopPrice = Integer.MAX_VALUE;
    private List<String> auctions;
}
