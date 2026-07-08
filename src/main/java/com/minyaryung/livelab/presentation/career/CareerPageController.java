package com.minyaryung.livelab.presentation.career;

import com.minyaryung.livelab.application.career.CareerPageLoader;
import com.minyaryung.livelab.domain.career.CareerSection;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/career")
public class CareerPageController {

    private final CareerPageLoader loader;
    public CareerPageController(CareerPageLoader loader) { this.loader = loader; }

    @GetMapping
    public CareerSection.CareerPage career() { return loader.loadPage(); }
}
