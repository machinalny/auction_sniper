package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.AuctionRecord;
import com.machinalny.model.AuctionState;
import com.machinalny.model.Bidder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuctionService {

    private final AuctionKafkaProducer auctionKafkaProducer;
    private final Map<String, AuctionState> currentAuctions;

    public AuctionService(AuctionKafkaProducer auctionKafkaProducer) {
        this.auctionKafkaProducer = auctionKafkaProducer;
        this.currentAuctions = new HashMap<>();
    }

    public void startBiddingIn(Bidder bidder) throws JsonProcessingException {
        auctionKafkaProducer.send(AuctionRecord.builder()
                .bidder(bidder.getBidder())
                .messageType("JOIN")
                .auction(bidder.getAuction())
                .build());
        currentAuctions.put(bidder.getAuction(), AuctionState.builder().bidder(bidder.getBidder()).state("WAITING_TO_JOIN").build());
    }

    public void updateAuction(AuctionRecord auctionRecord) {
        currentAuctions.get(auctionRecord.getAuction()).setState(auctionRecord.getMessageType());
    }

    public AuctionState getAuctionStatusBy(String itemIdentificator) {
        return currentAuctions.get(itemIdentificator);
    }
}
