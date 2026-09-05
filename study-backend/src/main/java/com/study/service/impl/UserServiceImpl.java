package com.study.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.common.BusinessException;
import com.study.dto.UserLoginDTO;
import com.study.entity.User;
import com.study.mapper.UserMapper;
import com.study.service.UserService;
import com.study.util.JwtUtil;
import com.study.vo.LoginVO;
import com.study.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    public List<UserVO> listUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, "active")
               .orderByAsc(User::getId);

        List<User> users = userMapper.selectList(wrapper);

        return users.stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    BeanUtil.copyProperties(user, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public LoginVO login(UserLoginDTO loginDTO) {
        User user = userMapper.selectById(loginDTO.getUserId());

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("用户状态异常");
        }

        // 生成JWT token
        String token = jwtUtil.generateToken(user.getId().toString());

        // 构建返回对象
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        loginVO.setUser(userVO);

        log.info("用户登录成功: userId={}, userName={}", user.getId(), user.getName());

        return loginVO;
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPoints(Long userId, Integer points) {
        User user = getUserById(userId);
        user.setTotalPoints(points);
        userMapper.updateById(user);

        log.info("更新用户积分: userId={}, points={}", userId, points);
    }
}
