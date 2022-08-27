package com.machinalny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.kafka.AuctionKafkaProducer;
import com.machinalny.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
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
        String otherBidder = "OtherBidder";
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        AuctionRecord bidRecord = AuctionRecord.builder()
                .bid(price + increment)
                .auction(auction)
                .bidder(bidder)
                .messageType(AuctionMessageType.BID)
                .build();

        auctionSniper.startBiddingIn(bidRequest);

        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(otherBidder)
                .messageType(AuctionMessageType.PRICE)
                .build();

        auctionSniper.updateAuction(auctionRecord);

        verify(auctionKafkaProducer).send(bidRecord);
    }


    @Test
    void isWinningWhenCurrentPriceComesFromBidder() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.WINNING)
                .bidder(bidder)
                .price(price)
                .build();

        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        auctionSniper.startBiddingIn(bidRequest);

        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .messageType(AuctionMessageType.PRICE).build();

        auctionSniper
                .updateAuction(auctionRecord);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));

    }

    @Test
    void lostIfAuctionClosesImmediately() throws JsonProcessingException {
        String auction = "Auction";
        String bidder = "Bidder";

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.LOST)
                .bidder(bidder)
                .build();

        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        auctionSniper.startBiddingIn(bidRequest);

        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .messageType(AuctionMessageType.CLOSED).build();

        auctionSniper
                .updateAuction(auctionRecord);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));
    }

    @Test
    void lostIfAuctionClosesWhenBidding() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.LOST)
                .price(price)
                .bidder(bidder)
                .build();

        AuctionRecord priceRecord = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(otherBidder)
                .messageType(AuctionMessageType.PRICE)
                .build();

        auctionSniper.startBiddingIn(bidRequest);

        auctionSniper.updateAuction(priceRecord);

        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .messageType(AuctionMessageType.CLOSED)
                .build();

        auctionSniper.updateAuction(auctionRecord);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));
    }

    @Test
    void wonIfAuctionClosesWhenWinning() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.WON)
                .price(price)
                .bidder(bidder)
                .build();

        AuctionRecord priceRecord = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(otherBidder)
                .messageType(AuctionMessageType.PRICE)
                .build();

        auctionSniper.startBiddingIn(bidRequest);

        auctionSniper.updateAuction(priceRecord);

        AuctionRecord priceRecord2 = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .messageType(AuctionMessageType.PRICE)
                .build();

        auctionSniper.updateAuction(priceRecord2);


        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .messageType(AuctionMessageType.CLOSED)
                .build();

        auctionSniper.updateAuction(auctionRecord);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));
    }
}