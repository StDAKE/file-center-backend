package com.huaixv06.fileCenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huaixv06.fileCenter.common.BaseResponse;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.common.ResultUtils;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.model.entity.Type;
import com.huaixv06.fileCenter.model.entity.User;
import com.huaixv06.fileCenter.service.TypeService;
import com.huaixv06.fileCenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/type")
@Slf4j
public class TypeController {

    @Resource
    private UserService userService;

    @Resource
    private TypeService typeService;

    /**
     * 获取列表(一级分类)
     *
     * @param typeQueryRequest
     * @return
     */
    @GetMapping("/list/first")
    public BaseResponse<List<Type>> listTypeByFirst(Type typeQueryRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        // 构建查询条件
        QueryWrapper<Type> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pType",0);
        // 执行查询，获取列表
        List<Type> typeList = typeService.list(queryWrapper);
        return ResultUtils.success(typeList);
    }

    /**
     * 获取列表(次级分类)
     *
     * @param typeQueryRequest
     * @return
     */
    @GetMapping("/list/second")
    public BaseResponse<List<Type>> listTypeBySecond(Type typeQueryRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        QueryWrapper<Type> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pType",typeQueryRequest.getPType());
        // 执行查询，获取列表
        List<Type> resultList = typeService.list(queryWrapper);
        // 返回分类列表
        return ResultUtils.success(resultList);
    }
}
