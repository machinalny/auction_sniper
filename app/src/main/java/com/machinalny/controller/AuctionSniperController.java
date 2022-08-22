package com.machinalny.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.model.AuctionState;
import com.machinalny.model.BidRequest;
import com.machinalny.service.AuctionSniper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/auction/sniper")
@CrossOrigin(origins = "http://auction-sniper:8080")
public class AuctionSniperController {

    private final AuctionSniper auctionSniper;

    public AuctionSniperController(AuctionSniper auctionSniper) {
        this.auctionSniper = auctionSniper;
    }

    @GetMapping("/{itemIdentificator}")
    public ResponseEntity<AuctionState> getAuctionStatusByItemIdentification(@PathVariable("itemIdentificator") String itemIdentificator)  {

        return ResponseEntity.ok(auctionSniper.getAuctionStatusBy(itemIdentificator));

    }

    @PostMapping("/")
    public void startAuctionOn(@RequestBody BidRequest bidRequest) throws JsonProcessingException {
        auctionSniper.startBiddingIn(bidRequest);
    }
}
