package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.*;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuctionSniper {

    private final AuctionKafkaProducer auctionKafkaProducer;
    private final Map<String, AuctionReport> currentAuctions;

    public AuctionSniper(AuctionKafkaProducer auctionKafkaProducer) {
        this.auctionKafkaProducer = auctionKafkaProducer;
        this.currentAuctions = new HashMap<>();
    }

    public void startBiddingIn(BidRequest bidRequest) throws JsonProcessingException {
        auctionKafkaProducer.send(AuctionRecord.builder()
                .bidder(bidRequest.getBidder())
                .messageType(AuctionMessageType.JOIN)
                .auction(bidRequest.getAuction())
                .build());
        currentAuctions.put(bidRequest.getAuction(), AuctionReport.builder().bidder(bidRequest.getBidder()).state(AuctionState.WAITING_TO_JOIN).build());
    }

    public void updateAuction(AuctionRecord auctionRecord) {
        AuctionReport auctionReport = currentAuctions.getOrDefault(auctionRecord.getAuction(), AuctionReport.NO_REPORT);
        if (!auctionReport.equals(AuctionReport.NO_REPORT))
            switch (auctionRecord.getMessageType()) {
                case PRICE -> this.decideOnPriceMessage(auctionRecord, auctionReport);
                case CLOSED -> this.closeAuction(auctionReport);
            }

    }

    @SneakyThrows
    private void decideOnPriceMessage(AuctionRecord auctionRecord, AuctionReport auctionReport) {
        if (!auctionReport.getBidder().equals(auctionRecord.getBidder())) {
            this.bidOnAuction(currentAuctions.get(auctionRecord.getAuction()), auctionRecord);
            auctionReport.setState(AuctionState.BIDDING);
            auctionReport.setPrice(auctionRecord.getPrice());
        } else {
            auctionReport.setState(AuctionState.WINNING);
            auctionReport.setPrice(auctionRecord.getPrice());
        }

    }

    private void closeAuction(AuctionReport auctionReport){
        if (auctionReport.getState().equals(AuctionState.WINNING)){
            auctionReport.setState(AuctionState.WON);
        } else {
            auctionReport.setState(AuctionState.LOST);
        }

    }

    private void bidOnAuction(AuctionReport auctionReport, AuctionRecord auctionRecord) throws JsonProcessingException {
        auctionKafkaProducer.send(AuctionRecord.builder()
                .auction(auctionRecord.getAuction())
                .bidder(auctionReport.getBidder())
                .messageType(AuctionMessageType.BID)
                .bid(auctionRecord.getPrice() + auctionRecord.getIncrement())
                .build());
    }

    public AuctionReport getAuctionStatusBy(String itemIdentification) {
        return currentAuctions.get(itemIdentification);
    }
}
