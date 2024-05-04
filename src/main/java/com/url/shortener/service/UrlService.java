package com.url.shortener.service;

import com.url.shortener.entity.Url;
import com.url.shortener.repository.UrlRepository;
import com.url.shortener.request.UrlRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class UrlService {

    private final int shortUrlLength = 7;

    private final int urlExpirationDays = 30;

    private final int generationUniqueUrlRetry= 3;

    private final String HTTP_PROTOCOL = "http://";

    private final String HTTPS_PROTOCOL = "https://";

    private final RedisTemplate<String, Object> redisTemplate;

    private final UrlRepository urlRepository;
    private final String ALGORITHM_AES = "AES";

    @Autowired
    public UrlService(UrlRepository urlRepository, RedisTemplate<String, Object> redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    protected Url fetchUrl(String shortUrl) {
        return urlRepository.findByShortUrl(shortUrl);
    }

    @Cacheable(value = "longUrl", key = "#shortUrl")
    public String getOriginalUrl(String shortUrl) {
        Optional<Url> url = Optional.ofNullable(fetchUrl(shortUrl));
        return url.isPresent() ? url.get().getLongUrl() : StringUtils.EMPTY;
    }

    protected URL createURL(String urlSpec) throws MalformedURLException {
        if (!urlSpec.startsWith(HTTP_PROTOCOL) && !urlSpec.startsWith(HTTPS_PROTOCOL)) {
            urlSpec = HTTPS_PROTOCOL + urlSpec;
        }
        return new URL(urlSpec);
    }
    public boolean isValidURL(UrlRequest url) {
        try {
            if(url.getLongUrl().length() > 100) {
                return false;
            }
            URL formattedLongUrl = createURL(url.getLongUrl());
            formattedLongUrl.toURI();
            url.setLongUrl(formattedLongUrl.toString());
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    private byte[] generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM_AES);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            log.error("Could not generate a key", e);
            return null;
        }
    }

    public String getHost(HttpServletRequest request) {
        String host = request.getServerName();
        int port = request.getServerPort();

        return port != 80 ? host + ":" + port : host;

    }

    public String shortenURL(String longURL, String remoteIp) throws Exception {
        try {
            int count = 0;
            boolean existsShortUrl;
            String shortURL;
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            do {
                SecretKeySpec secretKeySpec = new SecretKeySpec(Objects.requireNonNull(generateKey()), ALGORITHM_AES);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
                byte[] encryptedBytes = cipher.doFinal((Instant.now().toString() + longURL).getBytes());
                String encryptedURL = this.convertToAlphanumeric(Base64.getEncoder().encodeToString(encryptedBytes));

                shortURL = encryptedURL.substring(0, shortUrlLength);
                String url = getOriginalUrl(shortURL);
                existsShortUrl = StringUtils.isNotEmpty(url);
                count++;
            } while (count < generationUniqueUrlRetry && existsShortUrl);

            if (!existsShortUrl) {
                saveUrl(shortURL, longURL, remoteIp);
                redisTemplate.opsForList().leftPush("#shortUrl", longURL);
                return shortURL;
            }

            return null;
        } catch (Exception e) {
            log.error("Could not generate a short url", e);
            throw new Exception(e);
        }
    }

    private void saveUrl(String shortURL, String longURL, String remoteIp) {
        Url url = new Url();
        url.setLongUrl(longURL);
        url.setShortUrl(shortURL);
        url.setExpirationDate(LocalDate.now().plusDays(urlExpirationDays));
        url.setSourceIp(remoteIp);
        urlRepository.save(url);
    }

    public String convertToAlphanumeric(String base64String) {
        return base64String.replaceAll("[^a-zA-Z0-9]", "");
    }
}
