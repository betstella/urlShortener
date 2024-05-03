package com.url.shortener.controller;

import com.url.shortener.request.UrlRequest;
import com.url.shortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/url")
public class UrlController {
    @Autowired
    private UrlService urlService;
    @PostMapping
    public ResponseEntity<String> generateShortUrl(@RequestBody UrlRequest urlRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(urlService.shortenURL(urlRequest.getLongUrl()));
    }

}
