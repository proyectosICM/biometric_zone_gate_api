package com.icm.biometric_zone_gate_api.security;

import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserModel userModel = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario " + username + " no existe"));

        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(userModel.getRole().name())
        );


        return new User(
                userModel.getUsername(),
                userModel.getPassword(),
                true,
                true,
                true,
                true,
                authorities
        );
    }
}