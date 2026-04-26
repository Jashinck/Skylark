package org.skylark.bluewhale.domain.model.memory;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class MemoryContent {
    private final String text;
    private final Map<String, Object> structuredData;

    public static MemoryContent ofText(String text) {
        return MemoryContent.builder().text(text).build();
    }

    public static MemoryContent ofStructured(String text, Map<String, Object> data) {
        return MemoryContent.builder().text(text).structuredData(data).build();
    }

    public boolean hasStructuredData() {
        return structuredData != null && !structuredData.isEmpty();
    }
}
