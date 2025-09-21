package com.zzjj.depaganalyzer.service.datasource;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public interface MarketDataSource {
    List<Point> series(String symbol, Instant from, Instant to, String Interval);

    record Point(Instant t, double price) {}
}
