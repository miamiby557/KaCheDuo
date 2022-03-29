package com.szcinda.service.user;


import com.szcinda.repository.User;
import com.szcinda.service.PageResult;

public interface UserService {
    User findUserById(String id);

    void create(UserCreateDto createDto);

    void update(UserUpdateDto updateDto);

    void delete(String id);

    void updatePwd(UpdatePWDDto pwdDto);

    UserIdentity findIdentity(String username, String password);

    String getToken(String userId, String password);

    void initAdmin();

    void resetPwd(UpdatePWDDto updateDto);

    void logout(String userId);

    PageResult<UserDto> query(UserQueryDto params);
}
