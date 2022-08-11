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

    private final List<String> currentActions;

    public AuctionService(AuctionKafkaProducer auctionKafkaProducer, AuctionKafkaConsumer auctionKafkaConsumer) {
        this.auctionKafkaProducer = auctionKafkaProducer;
        this.auctionKafkaConsumer = auctionKafkaConsumer;
        this.currentActions = new ArrayList<>();
    }

    public void startBiddingIn(String topic) {
        auctionKafkaProducer.send(topic, "JOIN");
        currentActions.add(topic);
    }

    public boolean sniperHasLostAuction() throws InterruptedException {
        boolean message = auctionKafkaConsumer.getLatch().await(10, TimeUnit.SECONDS);
        if (message){
            String payload = auctionKafkaConsumer.getPayload();
            return payload.contains("LOST") && currentActions.stream().anyMatch(payload::contains);
        }
        return false;
    }
}
