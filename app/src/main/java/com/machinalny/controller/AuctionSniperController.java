package com.machinalny.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.machinalny.model.AuctionState;
import com.machinalny.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/auction/sniper")
@CrossOrigin(origins = "http://auction-sniper:8080")
public class AuctionSniperController {

    private final AuctionService auctionService;

    public AuctionSniperController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping("/{itemIdentificator}")
    public ResponseEntity<AuctionState> getAuctionStatusByItemIdentificator(@PathVariable("itemIdentificator") String itemIdentificator) throws InterruptedException {

        return ResponseEntity.ok(auctionService.getAuctionStatusBy(itemIdentificator));

    }

    @PostMapping("/")
    public void startAuctionOn(@RequestBody String itemIdentifiactor) throws JsonProcessingException {
        auctionService.startBiddingIn(itemIdentifiactor);
    }
}
