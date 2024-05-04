package com.url.shortener.service;

import com.url.shortener.entity.Url;
import com.url.shortener.repository.UrlRepository;
import com.url.shortener.request.UrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(urlService, "shortUrlLength", 7);
        ReflectionTestUtils.setField(urlService, "urlExpirationDays", 30);
        ReflectionTestUtils.setField(urlService, "generationUniqueUrlRetry", 3);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        urlService = new UrlService(urlRepository, redisTemplate);

    }
    @Test
    public void testIsValidURL_ValidUrl() {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setLongUrl("https://example.com");

        assertTrue(urlService.isValidURL(urlRequest));
    }

    @Test
    public void testIsValidURL_InvalidUrl() {
        UrlRequest urlRequest = new UrlRequest();
        // Provide a long URL longer than 100 characters to trigger invalidity
        urlRequest.setLongUrl("https://example.com/this/is/a/very/long/url/that/is/more/than/100/characters/in/length/and/should/fail/validity/check");

        assertFalse(urlService.isValidURL(urlRequest));
    }

    @Test
    public void testGetHost() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);

        assertEquals("localhost:8080", urlService.getHost(request));
    }

    @Test
    public void testShortenURL() throws Exception {
        String longURL = "https://example.com";
        String remoteIp = "127.0.0.1";

        // Mocking repository method
        when(urlRepository.findByShortUrl(anyString())).thenReturn(null);

        String shortenedURL = urlService.shortenURL(longURL, remoteIp);
        assertNotNull(shortenedURL);
    }

    @Test
    public void testShortenURL_Exception() {
        String longURL = "https://example.com";
        String remoteIp = "127.0.0.1";

        // Mocking repository method to throw exception
        when(urlRepository.findByShortUrl(anyString())).thenThrow(new RuntimeException());

        assertThrows(Exception.class, () -> urlService.shortenURL(longURL, remoteIp));
    }

    @Test
    public void testCreateUrl_ValidUrl() {
        String longURL = "https://example.com";

        try {
            Url url = new Url();
            url.setLongUrl(longURL);
            URL createdURL = urlService.createURL(longURL);
            assertNotNull(createdURL);
            assertEquals(longURL, createdURL.toString());
        } catch (MalformedURLException e) {
            fail("MalformedURLException should not be thrown for valid URL");
        }
    }

    @Test
    public void testGetOriginalUrl_NotExpired() {
        String shortUrl = "abc";
        String longUrl = "https://example.com";

        Url url = new Url();
        url.setShortUrl(shortUrl);
        url.setLongUrl(longUrl);
        url.setExpirationDate(LocalDate.now().plusDays(1));
        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(url);
        String originalUrl = urlService.getOriginalUrl(shortUrl);

        assertEquals(longUrl, originalUrl);
    }

    @Test
    public void testGetOriginalUrl_Expired() {
        String shortUrl = "abc";
        String longUrl = "https://example.com";

        Url expiredUrl = new Url();
        expiredUrl.setShortUrl(shortUrl);
        expiredUrl.setLongUrl(longUrl);
        expiredUrl.setExpirationDate(LocalDate.now().minusDays(1));
        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(expiredUrl);

        String originalUrl = urlService.getOriginalUrl(shortUrl);

        assertEquals("", originalUrl);

        verify(urlRepository, times(1)).delete(expiredUrl);
    }

    @Test
    public void testFetchUrl_ExistingUrl() {
        Url mockUrl = new Url();
        mockUrl.setShortUrl("abc");
        mockUrl.setLongUrl("https://example.com");
        when(urlRepository.findByShortUrl("abc")).thenReturn(mockUrl);
        Url fetchedUrl = urlService.fetchUrl("abc");

        assertNotNull(fetchedUrl);
        assertEquals(mockUrl, fetchedUrl);
    }

    @Test
    public void testFetchUrl_NonExistingUrl() {
        when(urlRepository.findByShortUrl("non_existing_url")).thenReturn(null);

        Url fetchedUrl = urlService.fetchUrl("non_existing_url");

        assertNull(fetchedUrl);
    }
}
