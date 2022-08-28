/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.machinalny;

import com.machinalny.framework.FakeAuctionConsumer;
import com.machinalny.model.AuctionState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static com.machinalny.model.AuctionState.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class AuctionSniperEndToEndTest {

    private final String bidder = "AuctionSniper";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FakeAuctionConsumer auctionServer;

    @Test
    void sniperJoinsAuctionUntilAuctionClose() throws Exception {
        String auction = "joinsAuction";

        auctionServer.startSellingItem(auction);

        this.startBiddingOnAuctionWithBidder(auction, bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction, bidder);

        auctionServer.announceClosed(auction);
        this.verifySateOfAuction(LOST, auction);
    }

    @Test
    void sniperMakesAHigherBitButLoses() throws Exception {
        String auction = "losesAuctionByBiddingHigher";

        auctionServer.startSellingItem(auction);

        this.startBiddingOnAuctionWithBidder(auction, bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction, bidder);

        auctionServer.reportPrice(auction, 1000, 98, "other bidRequest");
        this.verifySateOfAuction(BIDDING, auction);
        auctionServer.hasReceivedBid(auction, 1098, bidder);

        auctionServer.announceClosed(auction);
        this.verifySateOfAuction(LOST, auction);
    }

    @Test
    void sniperWinsAnAuctionByBiddingHigher() throws Exception {
        String auction = "wonAuctionByBiddingHigher";

        auctionServer.startSellingItem(auction);

        this.startBiddingOnAuctionWithBidder(auction, bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction, bidder);

        auctionServer.reportPrice(auction, 1000, 98, "other bidRequest");
        this.verifySateOfAuction(BIDDING, auction);
        auctionServer.hasReceivedBid(auction, 1098, bidder);

        auctionServer.reportPrice(auction, 1098, 97, bidder);
        this.verifySateOfAuction(WINNING, auction);

        auctionServer.announceClosed(auction);
        this.verifySateOfAuction(WON, auction);
    }


    @Test
    void sniperLosesAnAuctionWhenOtherBidsHigher() throws Exception {
        String auction = "lostAuctionWhenOutBiden";
        String otherBidder = "OtherBidder";

        auctionServer.startSellingItem(auction);

        this.startBiddingOnAuctionWithBidder(auction, bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction, bidder);

        auctionServer.reportPrice(auction, 1000, 98, otherBidder);
        auctionServer.hasReceivedBid(auction, 1098, bidder);

        auctionServer.reportPrice(auction, 1098, 97, bidder);
        this.verifySateOfAuction(WINNING, auction);

        auctionServer.reportPrice(auction, 1195, 96, otherBidder);

        auctionServer.announceClosed(auction);
        this.verifySateOfAuction(LOST, auction);
    }

    @Test
    void sniperBidsForMultipleItems() throws Exception {
        String auction = "won1AuctionByBiddingHigher";
        String auction2 = "won2AuctionByBiddingHigher";

        auctionServer.startSellingItem(auction);
        auctionServer.startSellingItem(auction2);

        this.startBiddingOnAuctionsWithBidder(List.of(auction, auction2), bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction, bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction2, bidder);

        auctionServer.reportPrice(auction, 1000, 98, "other bidRequest");
        auctionServer.hasReceivedBid(auction, 1098, bidder);

        auctionServer.reportPrice(auction2, 500, 21, "other bidRequest");
        auctionServer.hasReceivedBid(auction2, 521, bidder);

        auctionServer.reportPrice(auction, 1098, 97, bidder);
        auctionServer.reportPrice(auction2, 521, 22, bidder);
        this.verifySateOfAuction(WINNING, auction);
        this.verifySateOfAuction(WINNING, auction2);

        auctionServer.announceClosed(auction);
        auctionServer.announceClosed(auction2);

        this.verifySateOfAuction(WON, auction);
        this.verifySateOfAuction(WON, auction2);
    }

    @Test
    void sniperLosesAnAuctionWhenThePriceIsTooHigh() throws Exception {
        String auction = "losesWhenPriceIsTooHigh";

        auctionServer.startSellingItem(auction);
        this.startBiddingWithStopPriceOnAuctionWithBidder(auction, 1100, bidder);
        auctionServer.hasReceivedJoinRequestFrom(auction, bidder);
        auctionServer.reportPrice(auction, 1000, 98, "other Bidder");
        this.verifySateOfAuction(BIDDING, auction);

        auctionServer.hasReceivedBid(auction, 1098, bidder);

        auctionServer.reportPrice(auction, 1197, 10, "third party");
        this.verifyIsLosing(auction, 1197, 1098);

        auctionServer.reportPrice(auction, 1207, 10, "fourth party");
        this.verifyIsLosing(auction, 1207, 1098);

        auctionServer.announceClosed(auction);
        this.verifySateOfAuction(LOST, auction);

    }

    public void startBiddingOnAuctionWithBidder(String auction, String bidder) throws Exception {
        this.mockMvc.perform(post("/api/auction/sniper/")
                .contentType("application/json")
                .content("""
                        {
                        "bidder": "%s",
                        "auction": "%s"
                        }
                        """.formatted(bidder, auction)));
    }

    public void startBiddingWithStopPriceOnAuctionWithBidder(String auction, Integer stopPrice, String bidder) throws Exception {
        this.mockMvc.perform(post("/api/auction/sniper/")
                .contentType("application/json")
                .content("""
                        {
                        "bidder": "%s",
                        "stopPrice": %s,
                        "auction": "%s"
                        }
                        """.formatted(bidder, stopPrice, auction)));
    }

    public void startBiddingOnAuctionsWithBidder(List<String> auctions, String bidder) throws Exception {
        List<String> auctionStrings = auctions.stream().map(auction -> "\"" + auction + "\"").toList();
        this.mockMvc.perform(post("/api/auction/sniper/")
                .contentType("application/json")
                .content("""
                        {
                        "bidder": "%s",
                        "auctions": [
                        %s
                        ]
                        }
                        """.formatted(bidder, String.join(",", auctionStrings))));
    }

    public void verifySateOfAuction(AuctionState expectedState, String auction) {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auction))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value(expectedState.toString())));
    }

    public void verifyIsLosing(String auction, Integer price, Integer lastBid) {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auction))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value(LOSING.toString()))
                                .andExpect(jsonPath("$.lastBid").value(lastBid))
                                .andExpect(jsonPath("$.price").value(price))
                );
    }

}
