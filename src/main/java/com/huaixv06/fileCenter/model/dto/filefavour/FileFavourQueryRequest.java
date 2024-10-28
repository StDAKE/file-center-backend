package com.huaixv06.fileCenter.model.dto.filefavour;


import com.huaixv06.fileCenter.common.PageRequest;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 帖子收藏查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileFavourQueryRequest extends PageRequest implements Serializable {

    /**
     * 帖子查询请求
     */
    private FileQueryRequest fileQueryRequest;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}