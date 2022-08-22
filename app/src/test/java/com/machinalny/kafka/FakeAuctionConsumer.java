package com.machinalny.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.model.AuctionRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Component
@Slf4j
public class FakeAuctionConsumer {

    private CountDownLatch latch = new CountDownLatch(1);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${test.auction1}")
    private String auction;

    @Value("${auction-sniper.auction-topic}")
    private String auctionTopic;

    private String payload;

    private AuctionRecord lastRecordReceived;


    @KafkaListener(topics = {"${auction-sniper.auction-topic}"}, groupId = "auctionService")
    public void receive(ConsumerRecord<String, String> consumerRecord) throws JsonProcessingException {
        payload = consumerRecord.toString();
        lastRecordReceived = objectMapper.readValue(consumerRecord.value(), AuctionRecord.class);
        log.info(payload);
        latch.countDown();
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }


    public void startSellingItem() {
        this.resetLatch();
    }

    private void receivesAMessageMatching(Matcher<? super AuctionRecord> auctionRecordMatcher) throws InterruptedException {
        boolean messageConsumed = this.latch.await(20, TimeUnit.SECONDS);
        if (!messageConsumed) {
            throw new RuntimeException("Didn't got any message");
        }
        assertThat(lastRecordReceived, auctionRecordMatcher);
    }

    public void hasReceivedJoinRequestFrom(String auction, String bidder) throws InterruptedException {
        receivesAMessageMatching(equalTo(AuctionRecord.builder().auction(auction).bidder(bidder).messageType("JOIN").build()));
    }

    public void announceClosed() throws JsonProcessingException {
        kafkaTemplate.send(auctionTopic, objectMapper.writeValueAsString(AuctionRecord.builder()
                .messageType("LOST")
                .auction(auction)
                .build()));
    }

    public void reportPrice(int price, int increment, String bidder) throws JsonProcessingException {
        kafkaTemplate.send(auctionTopic, objectMapper.writeValueAsString(AuctionRecord.builder()
                .messageType("BID")
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .auction(auction)
                .build()));
    }

    public void hasReceivedBid(String auction, int bid, String fromBidder) throws InterruptedException {
        receivesAMessageMatching(equalTo(AuctionRecord.builder().auction(auction).bidder(fromBidder).bid(bid).messageType("BID").build()));
    }
}
