package com.minyaryung.livelab.status;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

// 라벨·Unit 진행 정보는 UnitProgress 의 Java 상수에서 읽는다 — application.properties 의
// Korean 인코딩 사고(ISO-8859-1 ↔ UTF-8 mojibake)를 회피하기 위해.
// produces 에 charset=UTF-8 을 명시해서 일부 브라우저의 Latin-1 폴백도 차단.
@RestController
@RequestMapping(
        value = "/api/status",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
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
