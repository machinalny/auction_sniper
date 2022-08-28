package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class AuctionSniper {

    private final AuctionKafkaProducer auctionKafkaProducer;
    private final Map<String, Auction> currentAuctions;

    public AuctionSniper(AuctionKafkaProducer auctionKafkaProducer) {
        this.auctionKafkaProducer = auctionKafkaProducer;
        this.currentAuctions = new HashMap<>();
    }

    public void startBiddingIn(BidRequest bidRequest) throws JsonProcessingException {
        if (isMultipleAuctionBidRequest(bidRequest)) {
            bidRequest.getAuctions().forEach(auction -> {
                try {
                    this.joinAuctionWithBidder(auction, bidRequest.getBidder(), bidRequest.getStopPrice());
                } catch (JsonProcessingException e) {
                    log.warn("Unable to join {} with {}", auction, bidRequest.getBidder());
                }
            });
        } else {
            this.joinAuctionWithBidder(bidRequest.getAuction(), bidRequest.getBidder(), bidRequest.getStopPrice());
        }
    }

    private void joinAuctionWithBidder(String auction, String bidder, Integer stopPrice) throws JsonProcessingException {
        auctionKafkaProducer.send(auction, AuctionRecord.builder()
                .bidder(bidder)
                .messageType(AuctionMessageType.JOIN)
                .build());
        currentAuctions.put(auction,
                Auction.builder()
                        .bidder(bidder)
                        .stopPrice(stopPrice)
                        .state(AuctionState.JOINING)
                        .build());
    }

    private boolean isMultipleAuctionBidRequest(BidRequest bidRequest) {
        return Objects.nonNull(bidRequest.getAuctions());
    }

    public void updateAuction(String auction, AuctionRecord auctionRecord) {
        Auction auctionReport = currentAuctions.getOrDefault(auction, Auction.NO_REPORT);
        if (!auctionReport.equals(Auction.NO_REPORT) && isAuctionRecordProcessable(auctionRecord))
            switch (auctionRecord.getMessageType()) {
                case PRICE -> this.decideOnPriceMessage(auction, auctionRecord, auctionReport);
                case CLOSED -> this.closeAuction(auctionReport);
            }

    }

    private boolean isAuctionRecordProcessable(AuctionRecord auctionRecord) {
        return !auctionRecord.getMessageType().equals(AuctionMessageType.BID)
                && !auctionRecord.getMessageType().equals(AuctionMessageType.JOIN);
    }

    @SneakyThrows
    private void decideOnPriceMessage(String auction, AuctionRecord auctionRecord, Auction auctionReport) {
        if (!auctionReport.getBidder().equals(auctionRecord.getBidder())) {
            int bid = auctionRecord.getPrice() + auctionRecord.getIncrement();
            if (auctionReport.allowsBid(bid)) {
                this.bidOnAuction(auction, auctionReport.getBidder(), bid);
                auctionReport.setState(AuctionState.BIDDING);
                auctionReport.setLastBid(bid);
            } else {
                auctionReport.setState(AuctionState.LOSING);
            }
            auctionReport.setPrice(auctionRecord.getPrice());

        } else {
            auctionReport.setState(AuctionState.WINNING);
            auctionReport.setPrice(auctionRecord.getPrice());
        }

    }

    private void closeAuction(Auction auction) {
        if (auction.getState().equals(AuctionState.WINNING)) {
            auction.setState(AuctionState.WON);
        } else {
            auction.setState(AuctionState.LOST);
        }

    }

    private void bidOnAuction(String auction, String bidder, Integer bid) throws JsonProcessingException {
        auctionKafkaProducer.send(auction, AuctionRecord.builder()
                .bidder(bidder)
                .messageType(AuctionMessageType.BID)
                .bid(bid)
                .build());
    }

    public Auction getAuctionStatusBy(String itemIdentification) {
        return currentAuctions.get(itemIdentification);
    }
}
