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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                .bidder(bidder)
                .messageType(AuctionMessageType.BID)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        verify(auctionKafkaProducer).send(auction, bidRecord);
    }


    @Test
    void isWinningWhenCurrentPriceComesFromBidder() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.WINNING)
                .bidder(bidder)
                .stopPrice(Integer.MAX_VALUE)
                .price(price)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, bidder, AuctionMessageType.PRICE);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));

    }

    @Test
    void lostIfAuctionClosesImmediately() throws JsonProcessingException {
        String auction = "Auction";
        String bidder = "Bidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.LOST)
                .bidder(bidder)
                .stopPrice(Integer.MAX_VALUE)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));
    }

    @Test
    void lostIfAuctionClosesWhenBidding() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        final String auction = "Auction";
        final String bidder = "Bidder";
        final String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.LOST)
                .price(price)
                .bidder(bidder)
                .lastBid(increment + price)
                .stopPrice(Integer.MAX_VALUE)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));
    }

    @Test
    void wonIfAuctionClosesWhenWinning() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.WON)
                .price(price)
                .lastBid(price + increment)
                .stopPrice(Integer.MAX_VALUE)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, price, increment, bidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));
    }

    @Test
    void biddingIfWasWinningWhenHigherBidComes() throws JsonProcessingException {
        final int price = 1001;
        final int increment = 25;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.BIDDING)
                .price(price)
                .lastBid(price + increment)
                .stopPrice(Integer.MAX_VALUE)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder);

        this.updateAuctionWith(auction, price, increment, bidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));

    }

    @Test
    void doesNotBidAndLosingIfSubsequentPriceIsAboveStopPrice() throws JsonProcessingException {
        final int price = 123;
        final int endPrice = 2345;
        final int stopPrice = 1000;
        final int increment = 45;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.LOSING)
                .price(endPrice)
                .lastBid(price + increment)
                .stopPrice(stopPrice)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder, stopPrice);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, endPrice, increment, otherBidder, AuctionMessageType.PRICE);

        verify(auctionKafkaProducer, times(2)).send(any(), any());
        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));

    }

    @Test
    void doesNotBidAndLosingIfFirstPriceIsAboveStopPrice() throws JsonProcessingException {
        final int endPrice = 2345;
        final int stopPrice = 1000;
        final int increment = 45;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.LOSING)
                .price(endPrice)
                .lastBid(null)
                .stopPrice(stopPrice)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder, stopPrice);

        this.updateAuctionWith(auction, endPrice, increment, otherBidder, AuctionMessageType.PRICE);

        verify(auctionKafkaProducer, times(1)).send(any(), any());
        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));
    }

    @Test
    void lostIfAuctionClosesWhenLosing() throws JsonProcessingException {
        final int price = 123;
        final int stopPrice = 1000;
        final int endPrice = 2345;
        final String auction = "Auction";
        final String bidder = "Bidder";
        final String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.LOST)
                .price(endPrice)
                .lastBid(null)
                .stopPrice(stopPrice)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder, stopPrice);

        this.updateAuctionWith(auction, 2345, 25, otherBidder, AuctionMessageType.PRICE);
        this.updateAuctionWith(auction, AuctionMessageType.CLOSED);

        verify(auctionKafkaProducer, times(1)).send(any(), any());
        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));
    }

    @Test
    void continuesToBeLosingOnceStopPriceHasBeenReached() throws JsonProcessingException {
        final int price = 123;
        final int stopPrice = 1000;
        final int endPrice = 45656;
        final int increment = 45;
        String auction = "Auction";
        String bidder = "Bidder";
        String otherBidder = "OtherBidder";

        Auction expectedAuction = Auction.builder()
                .state(AuctionState.LOSING)
                .price(endPrice)
                .lastBid(price + increment)
                .stopPrice(stopPrice)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder, stopPrice);

        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);

        reset(auctionKafkaProducer);
        this.updateAuctionWith(auction, 2345, 25, otherBidder, AuctionMessageType.PRICE);

        this.updateAuctionWith(auction, endPrice, 25, otherBidder, AuctionMessageType.PRICE);
        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedAuction));


        verify(auctionKafkaProducer, times(0)).send(any(), any());
    }

    @Test
    void doesNotBidAndLosingIfPriceAfterWinningIsAboveStopPrice() throws JsonProcessingException {
        final int price = 123;
        final int stopPrice = 1000;
        final int endPrice = 45656;
        final int increment = 45;
        final String auction = "Auction";
        final String bidder = "Bidder";
        final String otherBidder = "OtherBidder";

        Auction expectedWinningAuction = Auction.builder()
                .state(AuctionState.WINNING)
                .price(price + increment)
                .lastBid(price + increment)
                .stopPrice(stopPrice)
                .bidder(bidder)
                .build();

        Auction expectedLosingAuction = Auction.builder()
                .state(AuctionState.LOSING)
                .price(endPrice)
                .lastBid(price + increment)
                .stopPrice(stopPrice)
                .bidder(bidder)
                .build();

        this.joinAuction(auction, bidder, stopPrice);
        this.updateAuctionWith(auction, price, increment, otherBidder, AuctionMessageType.PRICE);
        this.updateAuctionWith(auction, price + increment, increment, bidder, AuctionMessageType.PRICE);
        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedWinningAuction));
        reset(auctionKafkaProducer);

        this.updateAuctionWith(auction, 45656, 25, otherBidder, AuctionMessageType.PRICE);
        assertThat(auctionSniper.getAuctionStatusBy(auction), equalTo(expectedLosingAuction));


        verify(auctionKafkaProducer, times(0)).send(any(), any());
    }


    private void joinAuction(String auction, String bidder) throws JsonProcessingException {
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).build();

        auctionSniper.startBiddingIn(bidRequest);

    }

    private void joinAuction(String auction, String bidder, Integer stopPrice) throws JsonProcessingException {
        BidRequest bidRequest = BidRequest.builder()
                .auction(auction).bidder(bidder).stopPrice(stopPrice).build();

        auctionSniper.startBiddingIn(bidRequest);

    }

    private void updateAuctionWith(String auction, AuctionMessageType messageType){
        AuctionRecord auctionRecord = AuctionRecord.builder()
                .messageType(messageType)
                .build();

        auctionSniper.updateAuction(auction, auctionRecord);
    }


    private void updateAuctionWith(String auction, int price, int increment, String bidder, AuctionMessageType messageType){
        AuctionRecord auctionRecord = AuctionRecord.builder()
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .messageType(messageType)
                .build();

        auctionSniper.updateAuction(auction, auctionRecord);
    }
}