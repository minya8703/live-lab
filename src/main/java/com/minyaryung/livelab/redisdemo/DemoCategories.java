package com.minyaryung.livelab.redisdemo;

import java.util.List;

// 데모용 카테고리 목록. 한국어 문자열은 properties 파일이 아닌 Java 코드에 둔다 —
// 파일 인코딩(UTF-16/CP949 등)으로 인한 글자 깨짐을 원천 차단하기 위해.
public final class DemoCategories {

    public static final List<String> ALL = List.of(
            "전자제품", "도서", "식료품", "의류", "스포츠"
    );

    private DemoCategories() {}
}
