package com.huaixv06.fileCenter.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huaixv06.fileCenter.annotation.AuthCheck;
import com.huaixv06.fileCenter.common.BaseResponse;
import com.huaixv06.fileCenter.common.DeleteRequest;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.common.ResultUtils;
import com.huaixv06.fileCenter.constant.CommonConstant;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.exception.ThrowUtils;
import com.huaixv06.fileCenter.model.dto.file.FileAddRequest;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequest;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequestByEs;
import com.huaixv06.fileCenter.model.dto.file.FileUpdateRequest;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.entity.User;
import com.huaixv06.fileCenter.model.vo.FileVO;
import com.huaixv06.fileCenter.service.FileService;
import com.huaixv06.fileCenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建
     *
     * @param fileAddRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @Transactional
    @PostMapping("/add")
    public BaseResponse<Long> addFile(@RequestPart("file") MultipartFile multipartFile, FileAddRequest fileAddRequest, HttpServletRequest request) throws IOException {
        if(multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
//        if (fileAddRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1000 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1000M");
        // 校验文件大小缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("pdf", "txt", "doc", "docx","xlsx","rtf");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        File file = new File();
        file.setContent(multipartFile.getBytes());
        file.setFileIlk(suffix);
        // 如果请求体name值为空 则自动捕获文件名作为name
        if(fileAddRequest.getName() == null || fileAddRequest.getName().equals("")) {
            fileAddRequest.setName(originalFilename);
        }
        BeanUtils.copyProperties(fileAddRequest, file);
        // 校验
        fileService.validFile(file, true);
        file.setUserId(loginUser.getId());
        boolean result = fileService.save(file);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
//        fileService.saveInEs(file);
        long newFileId = file.getId();
        return ResultUtils.success(newFileId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @Transactional
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFile(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        File oldFile = fileService.getById(id);
        if (oldFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldFile.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = fileService.removeById(id);
//        fileEsDao.deleteById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param fileUpdateRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @Transactional
    @PostMapping("/update")
    public BaseResponse<Boolean> updateFile(@RequestPart("file") MultipartFile multipartFile, FileUpdateRequest fileUpdateRequest,
                                            HttpServletRequest request) throws IOException {
        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (fileUpdateRequest == null || fileUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1000 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1000M");
        // 校验文件大小缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("pdf", "txt", "doc", "docx","xlsx","rtf");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        File file = new File();
        file.setContent(multipartFile.getBytes());
        file.setFileIlk(suffix);
        BeanUtils.copyProperties(fileUpdateRequest, file);
        // 参数校验
        fileService.validFile(file, false);
        long id = fileUpdateRequest.getId();
        // 判断是否存在
        File oldFile = fileService.getById(id);
        if (oldFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldFile.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = fileService.updateById(file);
//        fileService.saveInEs(file);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<File> getFileById(long id, HttpServletRequest request) {
//        User user = userService.getLoginUser(request);
//        // 判断用户是否被封禁
//        if (user.getStatus() == 1) {
//            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
//        }
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        File file = fileService.getById(id);
//        return ResultUtils.success(fileService.getFileVO(file, request));
        File file = fileService.getById(id);
        return ResultUtils.success(file);
    }

    /**
     * 获取列表
     *
     * @param fileQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<File>> listFile(FileQueryRequest fileQueryRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        File fileQuery = new File();
        if (fileQueryRequest != null) {
            BeanUtils.copyProperties(fileQueryRequest, fileQuery);
        }
        QueryWrapper<File> queryWrapper = new QueryWrapper<>(fileQuery);
        List<File> fileList = fileService.list(queryWrapper);
        return ResultUtils.success(fileList);
    }

    /**
     * 分页获取列表
     *
     * @param fileQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<File>> listFileByPage(FileQueryRequest fileQueryRequest, HttpServletRequest request) {
        if (fileQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        File fileQuery = new File();
        BeanUtils.copyProperties(fileQueryRequest, fileQuery);
        long current = fileQueryRequest.getCurrent();
        long size = fileQueryRequest.getPageSize();
        String sortField = fileQueryRequest.getSortField();
        String sortOrder = fileQueryRequest.getSortOrder();
        String fileType = fileQuery.getFileType();
        // 限制爬虫
        if (size > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<File> queryWrapper = new QueryWrapper<>(fileQuery);
        if (StringUtils.isNotBlank(fileType)) {
            queryWrapper.like("fileType", fileType);
        }
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<File> filePage = fileService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(filePage);
    }

    /**
     * 分页获取列表
     *
     * @param fileQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/pageVO")
    public BaseResponse<Page<FileVO>> listFileByPageVO(FileQueryRequest fileQueryRequest, HttpServletRequest request) {
        if (fileQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        long size = fileQueryRequest.getPageSize();
        // 限制爬虫
        if (size > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<FileVO> postVOPage = fileService.listFileVOByPage(fileQueryRequest, request);
        return ResultUtils.success(postVOPage);
    }

    /**
     * 分页搜索（从 ES 查询，封装类）
     *
     * @param fileQueryRequestByEs
     * @param request
     * @return
     */
    @PostMapping("/search/page/vo")
    public BaseResponse<Page<FileVO>> searchFileVOByPage(@RequestBody FileQueryRequestByEs fileQueryRequestByEs,
                                                         HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        // 判断用户是否被封禁
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "用户已被封禁");
        }
        long size = fileQueryRequestByEs.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        Page<File> filePage = fileService.searchFromEs(fileQueryRequestByEs);
        return ResultUtils.success(fileService.getFileVOPage(filePage, request));
    }

    // endregion


}

