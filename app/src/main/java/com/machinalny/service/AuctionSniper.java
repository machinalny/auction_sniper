package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.AuctionRecord;
import com.machinalny.model.AuctionState;
import com.machinalny.model.BidRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuctionSniper {

    private final AuctionKafkaProducer auctionKafkaProducer;
    private final Map<String, AuctionState> currentAuctions;

    public AuctionSniper(AuctionKafkaProducer auctionKafkaProducer) {
        this.auctionKafkaProducer = auctionKafkaProducer;
        this.currentAuctions = new HashMap<>();
    }

    public void startBiddingIn(BidRequest bidRequest) throws JsonProcessingException {
        auctionKafkaProducer.send(AuctionRecord.builder()
                .bidder(bidRequest.getBidder())
                .messageType("JOIN")
                .auction(bidRequest.getAuction())
                .build());
        currentAuctions.put(bidRequest.getAuction(), AuctionState.builder().bidder(bidRequest.getBidder()).state("WAITING_TO_JOIN").build());
    }

    public void updateAuction(AuctionRecord auctionRecord) throws JsonProcessingException {
        if (currentAuctions.containsKey(auctionRecord.getAuction()) && auctionRecord.getMessageType().equals("PRICE")) {
            this.bidOnAuction(currentAuctions.get(auctionRecord.getAuction()), auctionRecord);
            currentAuctions.get(auctionRecord.getAuction()).setState("BIDDING");
        } else {
            currentAuctions.get(auctionRecord.getAuction()).setState(auctionRecord.getMessageType());
        }
    }

    private void bidOnAuction(AuctionState auctionState, AuctionRecord auctionRecord) throws JsonProcessingException {
        auctionKafkaProducer.send(AuctionRecord.builder()
                        .auction(auctionRecord.getAuction())
                        .bidder(auctionState.getBidder())
                        .messageType("BID")
                        .bid(auctionRecord.getPrice() + auctionRecord.getIncrement())
                .build());
    }

    public AuctionState getAuctionStatusBy(String itemIdentification) {
        return currentAuctions.get(itemIdentification);
    }
}
