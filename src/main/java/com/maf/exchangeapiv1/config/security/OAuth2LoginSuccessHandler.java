package com.maf.exchangeapiv1.config.security;

import com.maf.exchangeapiv1.auth.JwtService;
import com.maf.exchangeapiv1.model.User;
import com.maf.exchangeapiv1.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String id = (String) attributes.get("sub");

        User user = userRepository.findByEmail(email)
                .orElse(User.builder()
                        .id(id)
                        .email(email)
                        .name(name)
                        .picture(picture)
                        .provider("google")
                        .role("ROLE_USER")
                        .build());

        user.setName(name);
        user.setPicture(picture);

        System.out.println("[!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!] Kullanıcı bulundu/oluşturuldu: " + email);
        userRepository.save(user);
        System.out.println("[!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!] Kullanıcı kaydedildi, ID: " + user.getId());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + userRepository.findById(user.getId()).get().getEmail());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());


        String redirectUrl = "http://localhost:3000/auth/callback?access_token=" + accessToken + "&refresh_token=" + refreshToken;
        response.sendRedirect(redirectUrl);
    }
}
