package com.szcinda.controller;

import com.szcinda.repository.User;
import com.szcinda.service.PageResult;
import com.szcinda.service.user.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"user"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("create")
    public Result<String> create(@RequestBody UserCreateDto createDto) {
        userService.create(createDto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<String> update(@RequestBody UserUpdateDto updateDto) {
        userService.update(updateDto);
        return Result.success();
    }

    @PostMapping("query")
    public PageResult<UserDto> query(@RequestBody UserQueryDto params) {
        return userService.query(params);
    }

    @PostMapping("updatePwd")
    public Result<String> updatePwd(@RequestBody UpdatePWDDto updateDto) {
        userService.updatePwd(updateDto);
        return Result.success();
    }

    @PostMapping("resetPwd")
    public Result<String> resetPwd(@RequestBody UpdatePWDDto updateDto) {
        userService.resetPwd(updateDto);
        return Result.success();
    }

    @PostMapping("/authenticate")
    public Result<UserIdentity> login(@RequestParam("account") String account, @RequestParam("password") String password) {
        UserIdentity identity = userService.findIdentity(account, password);
        String token = userService.getToken(identity.getId(), identity.getPassword());
        identity.setToken(token);
        return Result.success(identity);
    }

    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        userService.delete(id);
        return Result.success();
    }

    @GetMapping("logout/{userId}")
    public Result<String> logout(@PathVariable String userId) {
        userService.logout(userId);
        return Result.success();
    }

    @GetMapping("{id}")
    public Result<User> getById(@PathVariable String id) {
        return Result.success(userService.findUserById(id));
    }

    @GetMapping("initAdmin")
    public Result<String> initAdmin() {
        userService.initAdmin();
        return Result.success();
    }
}
