package com.minyaryung.livelab.domain.kafkademo;

public record OrderEvent(String runId, Long orderId, String item, int quantity) {}
