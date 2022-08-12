package com.machinalny.service;

import com.machinalny.kafka.AuctionKafkaConsumer;
import com.machinalny.kafka.AuctionKafkaProducer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AuctionService {

    private final AuctionKafkaProducer auctionKafkaProducer;

    private final AuctionKafkaConsumer auctionKafkaConsumer;

    private final List<String> currentAuctions;

    public AuctionService(AuctionKafkaProducer auctionKafkaProducer, AuctionKafkaConsumer auctionKafkaConsumer) {
        this.auctionKafkaProducer = auctionKafkaProducer;
        this.auctionKafkaConsumer = auctionKafkaConsumer;
        this.currentAuctions = new ArrayList<>();
    }

    public void startBiddingIn(String topic) {
        auctionKafkaProducer.send(topic, "JOIN");
        currentAuctions.add(topic);
    }

    public String getAuctionStatusBy(String itemIdentificator) throws InterruptedException {
        if (currentAuctions.contains(itemIdentificator)) {
            boolean message = auctionKafkaConsumer.getLatch().await(10, TimeUnit.SECONDS);
            if (message) {
                String payload = auctionKafkaConsumer.getPayload();
                return payload.contains("LOST") && currentAuctions.stream().anyMatch(payload::contains) ? "LOST": "";
            }
            return "";
        } else {
            return "NOT_BIDDING";
        }
    }
}
