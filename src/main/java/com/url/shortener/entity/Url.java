package com.url.shortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "url")
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "long-url")
    private String longUrl;

    @Column(name = "short-url", unique = true)
    private String shortUrl;

    @Column(name = "expiration-date")
    private LocalDate expirationDate;

    @Column(name = "source-ip")
    private String sourceIp;

}
