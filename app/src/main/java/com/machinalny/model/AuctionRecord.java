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
    private String auctioneer;
    private String itemIdentification;
    private String messageType;
    private String value;
}
