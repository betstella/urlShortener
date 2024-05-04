package com.url.shortener.controller;
import com.url.shortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UrlResolverControllerTest {

    @Mock
    private UrlService urlService;

    @InjectMocks
    private UrlResolverController urlResolverController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetOriginalUrl_ExistingUrl() {
        String shortUrl = "abc";
        String longUrl = "https://example.com";

        when(urlService.getOriginalUrl(shortUrl)).thenReturn(longUrl);
        ResponseEntity<Object> responseEntity = urlResolverController.getOriginalUrl(shortUrl);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, responseEntity.getStatusCode());
        URI location = responseEntity.getHeaders().getLocation();
        assertEquals(longUrl, location.toString());

        verify(urlService, times(1)).getOriginalUrl(shortUrl);
    }

    @Test
    public void testGetOriginalUrl_NonExistingUrl() {
        String shortUrl = "non_existing_url";
        when(urlService.getOriginalUrl(shortUrl)).thenReturn(null);
        ResponseEntity<Object> responseEntity = urlResolverController.getOriginalUrl(shortUrl);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        verify(urlService, times(1)).getOriginalUrl(shortUrl);
    }
}
