package com.huaixv06.fileCenter.model.dto.file;

import com.huaixv06.fileCenter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class FileQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件分类
     */
    private String filetype;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 文件内容
     */
    private byte[] content;

    private static final long serialVersionUID = 1L;
}
