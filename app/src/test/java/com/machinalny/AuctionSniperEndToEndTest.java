/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.machinalny;

import com.machinalny.framework.FakeAuctionConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

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

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FakeAuctionConsumer auctionServer;

    @Value("${test.auction1}")
    private String auctionTopic1;

    @Value("${test.auction2}")
    private String auctionTopic2;

    @Value("${test.auction2}")
    private String auctionTopic3;

    private final String bidder = "AuctionSniper";

    @Test
    void sniperJoinsAuctionUntilAuctionClose() throws Exception {
        auctionServer.startSellingItem();
        this.mockMvc.perform(post("/api/auction/sniper/")
                .contentType("application/json")
                .content("""
                        {
                        "bidder": "%s",
                        "auction": "%s"
                        }
                        """.formatted(bidder, auctionTopic1)));
        auctionServer.hasReceivedJoinRequestFrom(auctionTopic1, bidder);
        auctionServer.announceClosed(auctionTopic1);
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auctionTopic1))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value("LOST")));
    }

    @Test
    void sniperMakesAHigherBitButLoses() throws Exception {
        auctionServer.startSellingItem();

        this.mockMvc.perform(post("/api/auction/sniper/")
                .contentType("application/json")
                .content("""
                        {
                        "bidder": "%s",
                        "auction": "%s"
                        }
                        """.formatted(bidder, auctionTopic2)));
        auctionServer.hasReceivedJoinRequestFrom(auctionTopic2, bidder);

        auctionServer.reportPrice(auctionTopic2,1000, 98, "other bidRequest");
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auctionTopic2))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value("BIDDING")));

        auctionServer.hasReceivedBid(auctionTopic2, 1098, bidder);
        auctionServer.announceClosed(auctionTopic2);
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auctionTopic2))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value("LOST")));


    }

    @Test
    void sniperWinsAnAuctionByBiddingHigher() throws Exception {
        auctionServer.startSellingItem();

        this.mockMvc.perform(post("/api/auction/sniper/")
                .contentType("application/json")
                .content("""
                        {
                        "bidder": "%s",
                        "auction": "%s"
                        }
                        """.formatted(bidder, auctionTopic3)));
        auctionServer.hasReceivedJoinRequestFrom(auctionTopic3, bidder);

        auctionServer.reportPrice(auctionTopic2,1000, 98, "other bidRequest");
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auctionTopic3))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value("BIDDING")));

        auctionServer.hasReceivedBid(auctionTopic3, 1098, bidder);
        auctionServer.reportPrice(auctionTopic3,1098, 97, bidder);

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auctionTopic3))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value("WINNING")));

        auctionServer.announceClosed(auctionTopic3);
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                        this.mockMvc.perform(get("/api/auction/sniper/" + auctionTopic2))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.state").value("WON")));


    }

}
