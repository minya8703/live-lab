package com.minyaryung.livelab.domain.devlog;

import java.util.List;

public record DevLogEntry(String slug, Integer unit, String title, String date,
                          List<String> tags, String htmlContent) {}
