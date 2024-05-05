package com.url.shortener.service;

import com.url.shortener.config.ZooKeeperClient;
import com.url.shortener.encoder.Base10Encoder;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
public class UrlService {

    private final int urlExpirationDays = 30;

    private final String HTTP_PROTOCOL = "http://";

    private final String HTTPS_PROTOCOL = "https://";

    private final Base10Encoder encoder = Base10Encoder.BASE_62;

    private final ZooKeeperClient connectionWatcher = new ZooKeeperClient();

    private final RedisTemplate<String, Object> redisTemplate;

    private final UrlRepository urlRepository;

    @Autowired
    public UrlService(UrlRepository urlRepository, RedisTemplate<String, Object> redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    protected Url fetchUrl(String shortUrl) {
        long id = encoder.decode(shortUrl);
        return urlRepository.findById(id);
    }

    @Cacheable(value = "longUrl", key = "#shortUrl")
    public String getOriginalUrl(String shortUrl) {
        Optional<Url> url = Optional.ofNullable(fetchUrl(shortUrl));
        boolean expired = url.isPresent() && url.get().getExpirationDate().isBefore(LocalDate.now());
        if ( url.isPresent() && expired) {
            deleteUrl(url.get());
        }
        return url.isPresent() && !expired ? url.get().getLongUrl() : StringUtils.EMPTY;
    }

    protected void deleteUrl(Url url) {
        urlRepository.delete(url);
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

    private long generateKey() throws Exception {
        if(!connectionWatcher.existsNode(ZooKeeperClient.COUNTER_NODE)) {
            try {
                connectionWatcher.createNode(ZooKeeperClient.COUNTER_NODE, ZooKeeperClient.INITIAL_COUNTER);
                return Long.parseLong(ZooKeeperClient.INITIAL_COUNTER);
            } catch (Exception e) {
                log.error("Could not generate the id");
                throw new Exception(e);
            }
        }
        return connectionWatcher.getNextId();
    }

    public String getHost(HttpServletRequest request) {
        String host = request.getServerName();
        int port = request.getServerPort();

        return port != 80 ? host + ":" + port : host;
    }

    private void zooKeeperConnect() {
        try {
            connectionWatcher.connect();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error connecting to zookeeper: " + e.getMessage());
        } finally {
            try {
                connectionWatcher.close();
            } catch (InterruptedException e) {
                log.error("Error closing the connection with zookeeper: {}", e.getMessage());
            }
        }
    }

    public String shortenURL(String longURL, String remoteIp) throws Exception {
        try {
            if(!connectionWatcher.isConnected()) {
                zooKeeperConnect();
            }
            long id = generateKey();

            String shortURL = encoder.encode(id);

            saveUrl(id, shortURL, longURL, remoteIp);
            redisTemplate.opsForList().leftPush("#shortUrl", longURL);

            return shortURL;
        } catch (Exception e) {
            log.error("Could not generate a short url", e);
            throw new Exception(e);
        }
    }

    private void saveUrl(long id, String shortURL, String longURL, String remoteIp) {
        Url url = new Url();
        url.setId(id);
        url.setLongUrl(longURL);
        url.setShortUrl(shortURL);
        url.setExpirationDate(LocalDate.now().plusDays(urlExpirationDays));
        url.setSourceIp(remoteIp);
        urlRepository.save(url);
    }
}
