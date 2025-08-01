package com.huaixv06.fileCenter.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequest;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequestByEs;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.vo.FileVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 11517
* @description 针对表【file(文件)】的数据库操作Service
* @createDate 2024-10-13 16:36:31
*/
public interface FileService extends IService<File> {

    void validFile(File file, boolean b);

    Page<File> searchFromEs(FileQueryRequestByEs fileQueryRequestByEs);

    public FileVO getFileVO(File file, HttpServletRequest request);

    public Page<FileVO> listFileVOByPage(FileQueryRequest fileQueryRequest, HttpServletRequest request);

    Page<FileVO> getFileVOPage(Page<File> filePage, HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param fileQueryRequest
     * @return
     */
    QueryWrapper<File> getQueryWrapper(FileQueryRequest fileQueryRequest);

    void saveInEs(File file);

}
