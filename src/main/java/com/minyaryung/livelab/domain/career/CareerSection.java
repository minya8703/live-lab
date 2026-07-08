package com.minyaryung.livelab.domain.career;

import java.util.List;

public record CareerSection(String key, String title, String htmlContent) {
    public record CareerPage(
            CareerSection profile, CareerSection techStack, CareerSection summary,
            List<CareerSection> projects, List<CareerSection> experience
    ) {}
}
