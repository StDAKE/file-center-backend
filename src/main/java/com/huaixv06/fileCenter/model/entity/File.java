package com.huaixv06.fileCenter.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 文件
 * @TableName file
 */
@TableName(value ="file")
@Data
public class File implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件分类
     */
    private String fileType;

    /**
     * 文件内容
     */
    private byte[] content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}