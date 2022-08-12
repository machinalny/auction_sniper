package com.machinalny.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuctionState {
    private String price;
    private String state;
}
