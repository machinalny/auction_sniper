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

        AuctionRecord bidRecord = AuctionRecord.builder()
                .bid(price + increment)
                .auction(auction)
                .bidder(bidder)
                .messageType(AuctionMessageType.BID)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

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

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, bidder, AuctionMessageType.PRICE);

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

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));
    }

    @Test
    void lostIfAuctionClosesWhenBidding() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.LOST)
                .price(price)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));
    }

    @Test
    void wonIfAuctionClosesWhenWinning() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.WON)
                .price(price)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, price, increment, bidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));
    }

    @Test
    void biddingIfWasWinningWhenHigherBidComes() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        AuctionReport expectedAuctionReport = AuctionReport.builder()
                .state(AuctionState.BIDDING)
                .price(price)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, bidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuctionReport));

    }

    private void joinAuction(String auction, String bidder) throws JsonProcessingException {
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        auctionSniper.startBiddingIn(bidRequest);

    }

    private void updateAuctionWith(String auction, AuctionMessageType messageType){
        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .messageType(messageType)
                .build();

        auctionSniper.updateAuction(auctionRecord);
    }


    private void updateAuctionWith(String auction, int price, int increment, String bidder, AuctionMessageType messageType){
        AuctionRecord auctionRecord = AuctionRecord.builder()
                .auction(auction)
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .messageType(messageType)
                .build();

        auctionSniper.updateAuction(auctionRecord);
    }
}