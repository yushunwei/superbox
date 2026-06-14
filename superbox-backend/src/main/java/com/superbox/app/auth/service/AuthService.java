package com.superbox.app.auth.service;

import com.superbox.app.auth.entity.User;
import com.superbox.app.auth.mapper.UserMapper;
import com.superbox.common.BusinessException;
import com.superbox.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return jwtTokenProvider.generateToken(user.getId(), user.getUsername());
    }

    public String wxLogin(String code) {
        // In production, call WeChat API: code2Session to get openid
        // For now, use code directly as openid placeholder
        String openid = code;
        User user = userMapper.findByWxOpenid(openid);
        if (user == null) {
            throw new BusinessException(401, "用户未绑定微信，请先在Web端绑定");
        }
        return jwtTokenProvider.generateToken(user.getId(), user.getUsername());
    }

    public User register(String username, String password) {
        User exist = userMapper.findByUsername(username);
        if (exist != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(username);
        userMapper.insert(user);
        return user;
    }

    public User getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
