package com.sss.backend.api.controller;

import com.sss.backend.api.dto.VoiceCreateDTO;
import com.sss.backend.domain.service.VoiceService;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/speaker")
public class VoiceController {

    private final JWTUtil jwtUtil;
    private final VoiceService voiceService;


    @PostMapping("/upload")
    public ResponseEntity<?> VoiceCreate(HttpServletRequest request,
                           @Valid @RequestBody VoiceCreateDTO dto){

        log.info("보이스벡터 생성 controller : {} {}",request, dto);

        // 비동기 처리???
        // 일단 동기로 처리해보고 시간 오래걸리면 비동기로 빼자.
        String token = jwtUtil.extractTokenFromRequest(request);
        String email = jwtUtil.getEmail(token);

        return voiceService.VoiceCreate(email, dto);
    }

    @GetMapping("/library")
    public ResponseEntity<?> GetMyVoices(HttpServletRequest request) {

        return voiceService.findMyVoices(request);

    }

    @DeleteMapping("/delete/{voiceId}")
    public ResponseEntity<?> DeleteMyVoice(HttpServletRequest request,
                                           @PathVariable String voiceId){
        log.info("voiceId : {} - 보이스 삭제 APi 호출 controller ",voiceId);
        return voiceService.deleteMyVoice(request, voiceId);
    }
}
