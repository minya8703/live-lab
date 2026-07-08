package com.minyaryung.livelab.presentation.devlog;

import com.minyaryung.livelab.application.devlog.DevLogLoader;
import com.minyaryung.livelab.domain.devlog.DevLogEntry;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devlog")
public class DevLogController {

    private final DevLogLoader loader;
    public DevLogController(DevLogLoader loader) { this.loader = loader; }

    @GetMapping
    public List<DevLogEntry> entries() { return loader.loadAll(); }
}
