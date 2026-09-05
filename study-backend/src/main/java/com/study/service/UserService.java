package com.study.service;

import com.study.dto.UserLoginDTO;
import com.study.entity.User;
import com.study.vo.LoginVO;
import com.study.vo.UserVO;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取所有用户列表
     */
    List<UserVO> listUsers();

    /**
     * 用户登录
     */
    LoginVO login(UserLoginDTO loginDTO);

    /**
     * 根据ID获取用户
     */
    User getUserById(Long userId);

    /**
     * 更新用户积分
     */
    void updateUserPoints(Long userId, Integer points);
}
