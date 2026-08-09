package com.minyaryung.livelab.domain.status;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UnitProgress {

    public static final int CURRENT_UNIT = 10;
    public static final int TOTAL_UNITS = 11;
    public static final String CURRENT_LABEL = "U10 AWS 운영 페이지 구축 중 · AI-DLC Construction";

    private UnitProgress() {}

    public static Map<String, String> snapshot() {
        Map<String, String> units = new LinkedHashMap<>();
        units.put("3", "live");
        units.put("4", "live");
        units.put("5", "live");
        units.put("7", "live");
        units.put("8", "live");
        units.put("11", "planned");
        units.put("10", "in-progress");
        return units;
    }
}
