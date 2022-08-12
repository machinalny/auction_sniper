package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.AuctionRecord;
import com.machinalny.model.AuctionState;
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

    public void startBiddingIn(String itemIdentification) throws JsonProcessingException {
        auctionKafkaProducer.send(AuctionRecord.builder()
                .auctioneer(this.toString())
                .messageType("JOIN")
                .itemIdentification(itemIdentification)
                .build());
        currentAuctions.put(itemIdentification, AuctionState.builder().state("WAITING_TO_JOIN").build());
    }

    public void updateAuction(AuctionRecord auctionRecord) {
        currentAuctions.get(auctionRecord.getItemIdentification()).setState(auctionRecord.getMessageType());
    }

    public AuctionState getAuctionStatusBy(String itemIdentificator) {
        return currentAuctions.get(itemIdentificator);
    }
}
