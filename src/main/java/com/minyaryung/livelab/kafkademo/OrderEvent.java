package com.minyaryung.livelab.kafkademo;

import java.io.Serializable;

public record OrderEvent(Long orderId, String item, int quantity) implements Serializable {}
