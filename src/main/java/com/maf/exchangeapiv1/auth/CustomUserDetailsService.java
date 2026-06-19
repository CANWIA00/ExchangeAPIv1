package com.maf.exchangeapiv1.auth;

import com.maf.exchangeapiv1.model.User;
import com.maf.exchangeapiv1.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User can not be found: " + userId));

        return new org.springframework.security.core.userdetails.User(
                user.getId(),
                "",
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
