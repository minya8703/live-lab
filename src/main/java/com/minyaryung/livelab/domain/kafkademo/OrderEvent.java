package com.minyaryung.livelab.domain.kafkademo;

public record OrderEvent(Long orderId, String item, int quantity) {}
