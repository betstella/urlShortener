package com.url.shortener.controller;

import com.url.shortener.service.UrlService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;

@Controller
@RequestMapping("/")
public class UrlResolverController {
    @Autowired
    private UrlService urlService;

    @GetMapping("/{shortUrl}")
    public ResponseEntity<Object> getOriginalUrl(@PathVariable String shortUrl) {
        String longUrl = urlService.getOriginalUrl(shortUrl);
        if (StringUtils.isNotEmpty(longUrl)) {
            URI uri = URI.create(longUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setLocation(uri);
            return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
