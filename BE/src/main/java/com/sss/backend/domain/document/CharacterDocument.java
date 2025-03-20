package com.sss.backend.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data  // Lombok이 자동으로 getter/setter를 생성해줌
@Document(collection = "characters")
public class CharacterDocument {

    @Id
    private String id;

    private String storyId; // RDBMS에서 생성된 story_id

    private List<Map<String, Object>> characterArr; // JSON 리스트 그대로 저장

}
