package com.minyaryung.livelab.ops;

import java.util.List;

public record OpsEntry(
        String slug,
        String category,
        String title,
        String date,
        List<String> tags,
        String htmlContent
) {}
