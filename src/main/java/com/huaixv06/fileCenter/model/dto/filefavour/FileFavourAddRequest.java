package com.huaixv06.fileCenter.model.dto.filefavour;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子收藏 / 取消收藏请求
 *
 */
@Data
public class FileFavourAddRequest implements Serializable {

    /**
     * 帖子 id
     */
    private Long fileId;

    private static final long serialVersionUID = 1L;
}