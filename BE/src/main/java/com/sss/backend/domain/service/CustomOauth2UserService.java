package com.sss.backend.domain.service;


import com.sss.backend.api.dto.OAuth.*;
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOauth2UserService extends DefaultOAuth2UserService {
    /**
     * 사용하지 않는 Service.
     * Spring Security 기반 로그인에서 사용.
     */

    private final UserRepository userRepository;

    public CustomOauth2UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println(oAuth2User); // 로깅

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response;

        // Naver Google 분기.
        if (registrationId.equals("naver")){
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }
        // 추후 작성
        //리소스 서버에서 발급 받은 정보로 사용자를 특정할 아이디값을 만듬
        String username = oAuth2Response.getProvider() + " " + oAuth2Response.getProviderId();
        Optional<Users> existData = userRepository.findByUserName(username);

        Users userEntity;

        if (existData.isEmpty()) {
            // 신규 유저
            userEntity = new Users();
            userEntity.setUserName(username);
            userEntity.setEmail(oAuth2Response.getEmail());
            userEntity.setNickname(oAuth2Response.getName());
            userEntity.setRole("ROLE_USER");

            userRepository.save(userEntity);

        } else {
            // 기존 회원 -> 정보 업데이트
            userEntity = existData.get();
            userEntity.setEmail(oAuth2Response.getEmail());
            userEntity.setNickname(oAuth2Response.getName());

            userRepository.save(userEntity);
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(userEntity.getUserName());
        userDTO.setName(userEntity.getNickname());
        userDTO.setRole(userEntity.getRole());


        return new CustomOAuth2User(userDTO);
    }
}
