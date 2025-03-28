package com.sss.backend.api.controller;

import com.sss.backend.api.dto.OAuth.OAuthLoginRequest;
import com.sss.backend.domain.service.OAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService){
        this.oAuthService = oAuthService;
    }

    @PostMapping("/oauth")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        log.info("post 요청, {}",request);
        String jwt = oAuthService.processOAuthLogin(request.getProvider(), request.getCode());
        log.info("jwt return {}",jwt);
        return ResponseEntity.ok(Map.of("accessToken", jwt));
    }


}
