package com.sss.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

}
// Lob : CLOB(charater larget object) 또는 BLOB(BInary larget Object)로 매핑해줌.
// 일반 VARCHAR, TEXT 필드로는 길이 제한이 있음. -> 대용량 데이터 처리에 좋음
