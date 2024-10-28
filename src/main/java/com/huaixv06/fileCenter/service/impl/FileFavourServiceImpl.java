package com.huaixv06.fileCenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.entity.FileFavour;
import com.huaixv06.fileCenter.model.entity.User;
import com.huaixv06.fileCenter.service.FileFavourService;
import com.huaixv06.fileCenter.mapper.FileFavourMapper;
import com.huaixv06.fileCenter.service.FileService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
* @author 11517
* @description 针对表【file_favour】的数据库操作Service实现
* @createDate 2024-10-28 18:12:54
*/
@Service
public class FileFavourServiceImpl extends ServiceImpl<FileFavourMapper, FileFavour>
    implements FileFavourService{

    @Resource
    private FileService fileService;

    /**
     * 帖子收藏
     *
     * @param fileId
     * @param loginUser
     * @return
     */
    @Override
    public int doFileFavour(long fileId, User loginUser) {
        // 判断是否存在
        File file = fileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已帖子收藏
        long userId = loginUser.getId();
        // 每个用户串行帖子收藏
        // 锁必须要包裹住事务方法
        FileFavourService fileFavourService = (FileFavourService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return fileFavourService.doFileFavourInner(userId, fileId);
        }
    }

    @Override
    public Page<File> listFavourFileByPage(IPage<File> page, Wrapper<File> queryWrapper, long favourUserId) {
        if (favourUserId <= 0) {
            return new Page<>();
        }
        return baseMapper.listFavourFileByPage(page, queryWrapper, favourUserId);
    }

    /**
     * 封装了事务的方法
     *
     * @param userId
     * @param fileId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doFileFavourInner(long userId, long fileId) {
        FileFavour fileFavour = new FileFavour();
        fileFavour.setUserId(userId);
        fileFavour.setFileId(fileId);
        QueryWrapper<FileFavour> fileFavourQueryWrapper = new QueryWrapper<>(fileFavour);
        FileFavour oldFileFavour = this.getOne(fileFavourQueryWrapper);
        boolean result;
        // 已收藏
        if (oldFileFavour != null) {
            result = this.remove(fileFavourQueryWrapper);
            if (result) {
                return -1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 未帖子收藏
            result = this.save(fileFavour);
            if (result) {
                return 1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

}




