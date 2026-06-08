package com.minyaryung.livelab.status;

import java.util.LinkedHashMap;
import java.util.Map;

// 각 Unit 진행 상태 + 현재 상태 라벨의 단일 진실원.
// 한국어는 codepoint 배열로 빌드 — 소스 파일이 어떤 인코딩으로 저장되든 javac 가 동일하게 해석.
// 평문(참고용): "U10 AWS 운영 페이지 구축 중 · AI-DLC Construction"
final class UnitProgress {

    static final int CURRENT_UNIT = 10;
    static final int TOTAL_UNITS = 11;
    static final String CURRENT_LABEL = buildCurrentLabel();

    private UnitProgress() {}

    private static String buildCurrentLabel() {
        int[] cp = {
                'U', '1', '0', ' ',
                'A', 'W', 'S', ' ',
                0xC6B4, 0xC601, ' ',                       // 운영
                0xD398, 0xC774, 0xC9C0, ' ',               // 페이지
                0xAD6C, 0xCD95, ' ',                       // 구축
                0xC911, ' ',                               // 중
                0x00B7, ' ',                               // ·
                'A', 'I', '-', 'D', 'L', 'C', ' ',
                'C', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'i', 'o', 'n'
        };
        return new String(cp, 0, cp.length);
    }

    // 각 Unit 진척도. Unit 완료/진입 시 이 한 곳만 갱신한다.
    static Map<String, String> snapshot() {
        Map<String, String> units = new LinkedHashMap<>();
        units.put("3", "live");
        units.put("4", "live");
        units.put("5", "live");
        // U6 (Grafana 임베드) 는 t4g.small 2GB OOM 사고 후 prod 에서 의도적으로 분리.
        // 카드는 trade-off 회고로 이전 — /lab/ops.html 로 link.
        units.put("6", "retro");
        units.put("7", "live");
        units.put("8", "live");
        units.put("11", "live");
        units.put("10", "in-progress");
        return units;
    }
}
