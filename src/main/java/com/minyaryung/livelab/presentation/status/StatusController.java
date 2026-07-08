package com.minyaryung.livelab.presentation.status;

import com.minyaryung.livelab.domain.status.UnitProgress;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class StatusController {

    @GetMapping
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currentUnit", UnitProgress.CURRENT_UNIT);
        body.put("totalUnits", UnitProgress.TOTAL_UNITS);
        body.put("currentLabel", UnitProgress.CURRENT_LABEL);
        body.put("units", UnitProgress.snapshot());
        return body;
    }
}
