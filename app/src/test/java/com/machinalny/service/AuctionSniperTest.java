package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.AuctionRecord;
import com.machinalny.model.BidRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class AuctionSniperTest {

    @MockBean
    private AuctionKafkaProducer auctionKafkaProducer;

    private AuctionSniper auctionSniper;

    @BeforeEach
    void setUp() {
        auctionSniper = new AuctionSniper(auctionKafkaProducer);
    }

    @Test
    void bidsHigherWhenNewPriceArrives() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        AuctionRecord bidRecord = AuctionRecord.builder()
                .bid(price + increment)
                .auction(auction)
                .bidder(bidder)
                .messageType("BID")
                .build();

        auctionSniper.startBiddingIn(bidRequest);

        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .messageType("PRICE")
                .build();

        auctionSniper.updateAuction(auctionRecord);

        verify(auctionKafkaProducer).send(bidRecord);
    }
}