package com.minyaryung.livelab.domain.status;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UnitProgress {

    public static final int CURRENT_UNIT = 10;
    public static final int TOTAL_UNITS = 11;
    public static final String CURRENT_LABEL = buildCurrentLabel();

    private UnitProgress() {}

    private static String buildCurrentLabel() {
        int[] cp = {
                'U', '1', '0', ' ', 'A', 'W', 'S', ' ',
                0xC6B4, 0xC601, ' ', 0xD398, 0xC774, 0xC9C0, ' ',
                0xAD6C, 0xCD95, ' ', 0xC911, ' ', 0x00B7, ' ',
                'A', 'I', '-', 'D', 'L', 'C', ' ',
                'C', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'i', 'o', 'n'
        };
        return new String(cp, 0, cp.length);
    }

    public static Map<String, String> snapshot() {
        Map<String, String> units = new LinkedHashMap<>();
        units.put("3", "live");
        units.put("4", "live");
        units.put("5", "live");
        units.put("7", "live");
        units.put("8", "live");
        units.put("11", "live");
        units.put("10", "in-progress");
        return units;
    }
}
