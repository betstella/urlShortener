package com.url.shortener.service;

import com.url.shortener.entity.Url;
import com.url.shortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;

@Service
public class UrlService {
    private static final String SECRET_KEY = "secretKey1234567";
    private static final int SHORT_URL_LENGTH = 6;
    private final UrlRepository urlRepository;

    @Autowired
    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public String getOriginalUrl(String shortUrl) {
        Optional<Url> url = Optional.ofNullable(urlRepository.findByShortUrl(shortUrl));
        return url.isPresent() ? url.get().getLongUrl() : "";
    }

    public String shortenURL(String longURL) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(longURL.getBytes());

            String encryptedURL = Base64.getEncoder().encodeToString(encryptedBytes);

            String shortURL = encryptedURL.substring(0, SHORT_URL_LENGTH);

            Url url = new Url();
            url.setLongUrl(longURL);
            url.setShortUrl(shortURL);
            urlRepository.save(url);

            return shortURL;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
