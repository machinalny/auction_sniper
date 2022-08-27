package com.machinalny.framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinalny.model.AuctionMessageType;
import com.machinalny.model.AuctionRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
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

    @Value("${auction-sniper.auction-topic}")
    private String auctionTopic;

    private final Map<String, AuctionRecord> lastRecordReceivedForAuction = new HashMap<>();

    private final Set<String> auctions = new HashSet<>();

    private final List<AuctionMessageType> biddersMessageTypes = List.of(AuctionMessageType.BID, AuctionMessageType.JOIN);


    @KafkaListener(topics = {"${auction-sniper.auction-topic}"}, groupId = "auctionService")
    public void receive(ConsumerRecord<String, String> consumerRecord) throws JsonProcessingException {
        if (this.auctions.contains(consumerRecord.key())) {
            String payload = consumerRecord.toString();
            AuctionRecord auctionRecord = objectMapper.readValue(consumerRecord.value(), AuctionRecord.class);
            if (biddersMessageTypes.contains(auctionRecord.getMessageType())) {
                lastRecordReceivedForAuction.put(consumerRecord.key(), objectMapper.readValue(consumerRecord.value(), AuctionRecord.class));
                latch.countDown();
            }
            log.info(payload);
        }
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }


    public void startSellingItem(String auction) {
        this.auctions.add(auction);
        this.resetLatch();
    }

    private void receivesAMessageForAuctionMatching(String auction, Matcher<? super AuctionRecord> auctionRecordMatcher) throws InterruptedException {
        boolean messageConsumed = this.latch.await(20, TimeUnit.SECONDS);
        if (!messageConsumed) {
            throw new RuntimeException("Didn't got any message");
        }
        assertThat(lastRecordReceivedForAuction.getOrDefault(auction, AuctionRecord.builder().build()), auctionRecordMatcher);
        this.resetLatch();
    }

    public void hasReceivedJoinRequestFrom(String auction, String bidder) throws InterruptedException {
        receivesAMessageForAuctionMatching(auction,
                equalTo(AuctionRecord.builder()
                        .bidder(bidder)
                        .messageType(AuctionMessageType.JOIN)
                        .build()));
    }

    public void announceClosed(String auction) throws JsonProcessingException {
        kafkaTemplate.send(auctionTopic, auction, objectMapper.writeValueAsString(AuctionRecord.builder()
                .messageType(AuctionMessageType.CLOSED)
                .build()));
    }

    public void reportPrice(String auction, int price, int increment, String bidder) throws JsonProcessingException {
        kafkaTemplate.send(auctionTopic, auction, objectMapper.writeValueAsString(AuctionRecord.builder()
                .messageType(AuctionMessageType.PRICE)
                .price(price)
                .increment(increment)
                .bidder(bidder)
                .build()));
    }

    public void hasReceivedBid(String auction, int bid, String fromBidder) throws InterruptedException {
        receivesAMessageForAuctionMatching(auction,
                equalTo(AuctionRecord
                        .builder()
                        .bidder(fromBidder)
                        .bid(bid)
                        .messageType(AuctionMessageType.BID)
                        .build()));
    }
}
