package com.minyaryung.livelab.application.chat;

import java.util.List;

public record ChatAnswer(String answer, List<String> sources, boolean grounded) {
}
