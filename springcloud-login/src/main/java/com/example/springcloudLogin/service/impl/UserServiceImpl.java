package com.example.springcloudLogin.service.impl;

import com.example.springcloudLogin.dto.UserDTO;
import com.example.springcloudLogin.vo.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.security.auth.login.AccountExpiredException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserDetailsService {
    private List<UserDTO> userList;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initData() {
        String password = passwordEncoder.encode("123456");
        userList = new ArrayList<>();
        userList.add(new UserDTO(1L,"macro", password,1, Collections.singletonList("ADMIN")));
        userList.add(new UserDTO(2L,"andy", password,1, Collections.singletonList("TEST")));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<UserDTO> findUserList = userList.stream().filter(item -> item.getName().equals(username)).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(findUserList)) {
            throw new UsernameNotFoundException("user name or passwd not find ");
        }
        SecurityUser securityUser = new SecurityUser(findUserList.get(0));
        if (!securityUser.isEnabled()) {
            throw new DisabledException("ACCOUNT_DISABLED");
        } else if (!securityUser.isAccountNonLocked()) {
            throw new LockedException("ACCOUNT_LOCKED");
        }  else if (!securityUser.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("CREDENTIALS_EXPIRED");
        }
        return securityUser;
    }
}
