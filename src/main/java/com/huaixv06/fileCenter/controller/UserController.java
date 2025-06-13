package com.huaixv06.fileCenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.huaixv06.fileCenter.annotation.AuthCheck;
import com.huaixv06.fileCenter.common.BaseResponse;
import com.huaixv06.fileCenter.common.DeleteRequest;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.common.ResultUtils;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.model.dto.user.*;
import com.huaixv06.fileCenter.model.entity.User;
import com.huaixv06.fileCenter.model.vo.UserLoginVO;
import com.huaixv06.fileCenter.model.vo.UserVO;
import com.huaixv06.fileCenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.huaixv06.fileCenter.utils.ExcelUtils.parseUsernamesFromExcel;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    private static final String SALT = "huaixv_06";

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserLoginVO userLoginVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        String userAccount = userAddRequest.getUserAccount();
        String userPassword = userAddRequest.getUserPassword();
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
//        // 校验用户账号格式
//        String userAccountRegex = "^[\u4e00-\u9fa5]+1[3-9]\\d{9}$";
//        Pattern pattern = Pattern.compile(userAccountRegex);
//        Matcher matcher = pattern.matcher(userAccount);
//        if (!matcher.matches()) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号格式不正确，必须为姓名加11位手机号");
//        }
        if (userPassword.length() < 6 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(user.getId());
    }

/**
     * 批量添加用户（使用MyBatis-Plus）
     *
     * @param file
     * @param request
     * @return
     * @throws IOException
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/batchAddWithMybatisPlus")
    public BaseResponse<Integer> batchAddUsers(@RequestPart("file") MultipartFile file, HttpServletRequest request) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        // 1. 从Excel中解析用户名
        List<String> usernames = parseUsernamesFromExcel(file);
        // 2. 检查是否存在重复用户
        checkDuplicateUsers(usernames);
        // 3. 创建用户列表（填充默认值）
        List<User> usersToAdd = new ArrayList<>();
        for (String username : usernames) {
            usersToAdd.add(createUserWithDefaults(username));
        }
        // 4. 使用MyBatis-Plus批量插入
        boolean success = userService.saveBatch(usersToAdd);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量创建用户失败");
        }
        return ResultUtils.success(usersToAdd.size());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 加密
        String userPassword = user.getUserPassword();
        if (userPassword != null && !userPassword.isEmpty()) {
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            user.setUserPassword(encryptPassword);
        }
        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取用户
     *
     * @param id
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/get")
    public BaseResponse<UserVO> getUserById(int id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        List<User> userList = userService.list(queryWrapper);
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list/page")
    public BaseResponse<Page<UserVO>> listUserByPage(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        long current = 1;
        long size = 10;
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
            current = userQueryRequest.getCurrent();
            size = userQueryRequest.getPageSize();
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);
        Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    // 检查是否存在重复用户（防止数据冲突）
    private void checkDuplicateUsers(List<String> usernames) {
        List<User> existingUsers = userService.list(
                new LambdaQueryWrapper<User>()
                        .in(User::getUserAccount, usernames)
        );

        if (!existingUsers.isEmpty()) {
            List<String> existingUsernames = existingUsers.stream()
                    .map(User::getUserAccount)
                    .collect(Collectors.toList());

            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "以下用户名已存在: " + String.join(", ", existingUsernames));
        }
    }

    // 创建用户对象并填充默认值
    private User createUserWithDefaults(String username) {

        // 设置默认密码（建议从配置读取）
        String defaultPassword = "123456";

        // 创建用户对象
        User user = new User();
        user.setUserAccount(username);

        // 加密密码
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptedPassword);

        // 填充其他默认值
        user.setUserName(username);          // 默认用户名与账号相同
        user.setUserRole("user");            // 默认角色
        user.setStatus(0);                   // 默认状态（0启用）

        return user;
    }

    // 添加事务注解确保操作原子性
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<User> userList) {
        // 批量插入前可添加额外校验逻辑
        for (User user : userList) {
            // 验证用户信息完整性
            if (user.getUserAccount() == null || user.getUserAccount().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能为空");
            }
            if (user.getUserPassword() == null || user.getUserPassword().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
            }
        }

        // 使用MyBatis-Plus的saveBatch方法执行批量插入
        return userService.saveBatch(userList);
    }
}
