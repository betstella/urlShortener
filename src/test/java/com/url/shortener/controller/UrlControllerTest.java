package com.url.shortener.controller;
import com.url.shortener.request.UrlRequest;
import com.url.shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UrlControllerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private UrlService urlService;

    @InjectMocks
    private UrlController urlController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateShortUrl_ValidUrl_Success() throws Exception {
        UrlRequest urlRequest = new UrlRequest("https://example.com");
        String shortUrl = "abc";
        String host = "http://localhost:8080";

        when(urlService.isValidURL(urlRequest)).thenReturn(true);
        when(urlService.shortenURL(urlRequest.getLongUrl(), request.getRemoteAddr())).thenReturn(shortUrl);
        when(urlService.getHost(request)).thenReturn(host);
        ResponseEntity<String> responseEntity = urlController.generateShortUrl(urlRequest);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(host + "/" + shortUrl, responseEntity.getBody());
    }

    @Test
    public void testGenerateShortUrl_InvalidUrl_BadRequest() {
        UrlRequest urlRequest = new UrlRequest("invalid_url");

        // Mocking behavior of urlService.isValidURL()
        when(urlService.isValidURL(urlRequest)).thenReturn(false);

        // Call the method under test
        ResponseEntity<String> responseEntity = urlController.generateShortUrl(urlRequest);

        // Verify that the response status is HttpStatus.BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        // Verify that the response body contains the appropriate message
        assertEquals("Invalid Url: invalid_url max length 100 characters", responseEntity.getBody());
    }

    @Test
    public void testGenerateShortUrl_ShorteningFailure_Conflict() throws Exception {
        UrlRequest urlRequest = new UrlRequest("https://example.com");

        when(urlService.isValidURL(urlRequest)).thenReturn(true);
        when(urlService.shortenURL(urlRequest.getLongUrl(), request.getRemoteAddr())).thenReturn(null);
        ResponseEntity<String> responseEntity = urlController.generateShortUrl(urlRequest);
        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());

        assertEquals("The short url could not be generated please try again later", responseEntity.getBody());
    }

    @Test
    public void testGenerateShortUrl_Exception_InternalServerError() throws Exception {
        UrlRequest urlRequest = new UrlRequest("https://example.com");

        when(urlService.isValidURL(urlRequest)).thenReturn(true);
        when(urlService.shortenURL(urlRequest.getLongUrl(), request.getRemoteAddr())).thenThrow(new RuntimeException("Internal Server Error"));
        ResponseEntity<String> responseEntity = urlController.generateShortUrl(urlRequest);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals("Internal Server Error", responseEntity.getBody());
    }
}
