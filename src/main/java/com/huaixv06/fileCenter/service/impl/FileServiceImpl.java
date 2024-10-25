package com.huaixv06.fileCenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.mapper.FileMapper;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.service.FileService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author 11517
* @description 针对表【file(文件)】的数据库操作Service实现
* @createDate 2024-10-13 16:36:31
*/
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
    implements FileService{

    @Override
    public void validFile(File file, boolean add) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = file.getName();
        String fileType = file.getFileType();
        byte[] content = file.getContent();
        // 创建时，所有参数必须非空
        if(add) {
            if (StringUtils.isAnyBlank(name, fileType) || ObjectUtils.isEmpty(content)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (StringUtils.isNotBlank(name) && name.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名过长");
        }
        if (StringUtils.isNotBlank(fileType) && fileType.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件分类过长");
        }
        if (ObjectUtils.isEmpty(content)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不能为空");
        }
    }
}




