package com.huaixv06.fileCenter.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.entity.FileFavour;
import com.huaixv06.fileCenter.model.entity.User;

/**
* @author 11517
* @description 针对表【file_favour】的数据库操作Service
* @createDate 2024-10-28 18:12:54
*/
public interface FileFavourService extends IService<FileFavour> {

    /**
     * 文件收藏
     *
     * @param fileId
     * @param loginUser
     * @return
     */
    int doFileFavour(long fileId, User loginUser);

    /**
     * 分页获取用户收藏的文件列表
     *
     * @param page
     * @param queryWrapper
     * @param favourUserId
     * @return
     */
    Page<File> listFavourFileByPage(IPage<File> page, Wrapper<File> queryWrapper,
                                    long favourUserId);

    /**
     * 帖子收藏（内部服务）
     *
     * @param userId
     * @param fileId
     * @return
     */
    int doFileFavourInner(long userId, long fileId);
    
}
