package com.minyaryung.livelab.domain.redisdemo;

import java.io.Serializable;
import java.math.BigDecimal;

public record CategoryStats(
        String subCategory, Long count, Double avgPrice,
        BigDecimal minPrice, BigDecimal maxPrice
) implements Serializable {}
