package com.url.shortener.controller;

import com.url.shortener.request.UrlRequest;
import com.url.shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
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
    private HttpServletRequest request;
    @Autowired
    private UrlService urlService;
    @PostMapping
    public ResponseEntity<String> generateShortUrl(@RequestBody UrlRequest urlRequest) {
        if (urlService.isValidURL(urlRequest)) {
            try {
                String shortUrl = urlService.shortenURL(urlRequest.getLongUrl(), request.getRemoteAddr());
                String host = urlService.getHost(request);
                if(shortUrl!= null) {
                    return ResponseEntity.status(HttpStatus.CREATED).body(host+"/"+shortUrl);
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("The short url could not be generated please try again later");
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getLocalizedMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Url: "+urlRequest.getLongUrl() + " max length 100 characters");
    }

}
