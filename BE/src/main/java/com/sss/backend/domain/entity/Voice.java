package com.sss.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Voice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String voiceName;

    @Lob
//    @Column(columnDefinition = "CLOB")
    private byte[] embeddingTensor;


    private String voiceSampleUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist // 엔티티 저장 직전에 JPA가 알아서 호출해줌.
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate // 엔티티 업데이트 직전에 JPA가 알아서 호출해줌.
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
// Lob : CLOB(charater larget object) 또는 BLOB(BInary larget Object)로 매핑해줌.
// 일반 VARCHAR, TEXT 필드로는 길이 제한이 있음. -> 대용량 데이터 처리에 좋음
