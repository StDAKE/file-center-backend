package com.huaixv06.fileCenter.model.dto.file;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileAddRequest implements Serializable {

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件分类
     */
    private String fileType;

    private static final long serialVersionUID = 1L;
}
