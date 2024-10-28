package com.huaixv06.fileCenter.model.dto.file;

import com.huaixv06.fileCenter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class FileQueryRequestByEs extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件内容
     */
    private String content;

    private static final long serialVersionUID = 1L;
}
