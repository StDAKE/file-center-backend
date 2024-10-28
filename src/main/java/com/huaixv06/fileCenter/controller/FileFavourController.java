package com.huaixv06.fileCenter.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huaixv06.fileCenter.common.BaseResponse;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.common.ResultUtils;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.exception.ThrowUtils;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequest;
import com.huaixv06.fileCenter.model.dto.filefavour.FileFavourAddRequest;
import com.huaixv06.fileCenter.model.dto.filefavour.FileFavourQueryRequest;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.entity.User;
import com.huaixv06.fileCenter.model.vo.FileVO;
import com.huaixv06.fileCenter.service.FileFavourService;
import com.huaixv06.fileCenter.service.FileService;
import com.huaixv06.fileCenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子收藏接口
 */
@RestController
@RequestMapping("/file_favour")
@Slf4j
public class FileFavourController {

    @Resource
    private FileFavourService fileFavourService;

    @Resource
    private FileService fileService;

    @Resource
    private UserService userService;

    /**
     * 收藏 / 取消收藏
     *
     * @param fileFavourAddRequest
     * @param request
     * @return resultNum 收藏变化数
     */
    @PostMapping("/")
    public BaseResponse<Integer> doFileFavour(@RequestBody FileFavourAddRequest fileFavourAddRequest,
                                              HttpServletRequest request) {
        if (fileFavourAddRequest == null || fileFavourAddRequest.getFileId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        final User loginUser = userService.getLoginUser(request);
        long fileId = fileFavourAddRequest.getFileId();
        int result = fileFavourService.doFileFavour(fileId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取我收藏的文件列表
     *
     * @param fileQueryRequest
     * @param request
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<FileVO>> listMyFavourFileByPage(@RequestBody FileQueryRequest fileQueryRequest,
                                                             HttpServletRequest request) {
        if (fileQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long current = fileQueryRequest.getCurrent();
        long size = fileQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<File> filePage = fileFavourService.listFavourFileByPage(new Page<>(current, size),
                fileService.getQueryWrapper(fileQueryRequest), loginUser.getId());
        return ResultUtils.success(fileService.getFileVOPage(filePage, request));
    }

    /**
     * 获取用户收藏的文件列表
     *
     * @param fileFavourQueryRequest
     * @param request
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<FileVO>> listFavourFileByPage(@RequestBody FileFavourQueryRequest fileFavourQueryRequest,
            HttpServletRequest request) {
        if (fileFavourQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = fileFavourQueryRequest.getCurrent();
        long size = fileFavourQueryRequest.getPageSize();
        Long userId = fileFavourQueryRequest.getUserId();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20 || userId == null, ErrorCode.PARAMS_ERROR);
        Page<File> filePage = fileFavourService.listFavourFileByPage(new Page<>(current, size),
                fileService.getQueryWrapper(fileFavourQueryRequest.getFileQueryRequest()), userId);
        return ResultUtils.success(fileService.getFileVOPage(filePage, request));
    }
}
