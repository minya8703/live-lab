package com.minyaryung.livelab.presentation.ops;

import com.minyaryung.livelab.application.ops.OpsLoader;
import com.minyaryung.livelab.domain.ops.OpsEntry;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class OpsController {

    private final OpsLoader loader;
    public OpsController(OpsLoader loader) { this.loader = loader; }

    @GetMapping
    public List<OpsEntry> entries() { return loader.loadAll(); }
}
