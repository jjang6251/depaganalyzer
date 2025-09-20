package com.zzjj.depaganalyzer.infra.persistence;

import jakarta.persistence.*;
import lombok.Cleanup;

import java.time.Instant;

@Entity
@Table(name = "market_price", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "ts"}))
public class MarketPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String symbol;
    @Column(nullable = false) private Instant ts;
    @Column(nullable = false) private Double price;
    private Double volume;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MarketPrice() {}
    public MarketPrice (String symbol, Instant ts, Double price, Double volume) {
        this.symbol = symbol; this.ts = ts; this.price = price; this.volume = volume;
    }
}
