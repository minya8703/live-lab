package com.minyaryung.livelab.ops;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class OpsController {

    private final OpsLoader loader;

    public OpsController(OpsLoader loader) {
        this.loader = loader;
    }

    @GetMapping
    public List<OpsEntry> entries() {
        return loader.loadAll();
    }
}
