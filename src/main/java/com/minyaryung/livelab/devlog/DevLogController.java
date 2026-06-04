package com.minyaryung.livelab.devlog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devlog")
public class DevLogController {

    private final DevLogLoader loader;

    public DevLogController(DevLogLoader loader) {
        this.loader = loader;
    }

    @GetMapping
    public List<DevLogEntry> entries() {
        return loader.loadAll();
    }
}
