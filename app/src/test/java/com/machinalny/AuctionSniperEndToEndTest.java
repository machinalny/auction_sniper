/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.machinalny;

import com.machinalny.kafka.FakeAuctionConsumer;
import com.machinalny.service.AuctionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class AuctionSniperEndToEndTest {

    @Autowired
    private FakeAuctionConsumer auctionServer;
    @Autowired
    private AuctionService auctionService;

    @Value("${test.auction1}")
    private String auctionTopic1;

    @Test
    void sniperJoinsAuctionUntilAuctionClose() throws Exception {
        auctionServer.startSellingItem();
        auctionService.startBiddingIn(auctionTopic1);
        auctionServer.hasReceivedJoinRequestFromSniper();
        auctionServer.announceClosed();
        assertTrue(auctionService.sniperHasLostAuction());
    }

}
