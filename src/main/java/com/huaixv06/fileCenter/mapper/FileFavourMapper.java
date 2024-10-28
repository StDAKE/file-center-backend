package com.huaixv06.fileCenter.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.entity.FileFavour;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 11517
* @description 针对表【file_favour】的数据库操作Mapper
* @createDate 2024-10-28 18:12:54
* @Entity com.huaixv06.fileCenter.model.entity.FileFavour
*/
public interface FileFavourMapper extends BaseMapper<FileFavour> {

    /**
     * 分页查询收藏文件列表
     *
     * @param page
     * @param queryWrapper
     * @param favourUserId
     * @return
     */
    Page<File> listFavourFileByPage(IPage<File> page, @Param(Constants.WRAPPER) Wrapper<File> queryWrapper,
                                    long favourUserId);
    
}




