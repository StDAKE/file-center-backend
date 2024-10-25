package com.huaixv06.fileCenter.model.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FileUpdateRequest implements Serializable {

    /**
     * id
     */
    private long id;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件分类
     */
    private String fileType;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
