package com.huaixv06.fileCenter.model.vo;

import com.google.gson.Gson;
import com.huaixv06.fileCenter.model.entity.File;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件视图
 */
@Data
public class FileVO implements Serializable {

    private final static Gson GSON = new Gson();

    /**
     * id
     */
    private Long id;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 文件类型
     */
    private String fileIlk;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否已收藏
     */
    private Boolean hasFavour;

    /**
     * 包装类转对象
     *
     * @param fileVO
     * @return
     */
    public static File voToObj(FileVO fileVO) {
        if (fileVO == null) {
            return null;
        }
        File file = new File();
        BeanUtils.copyProperties(fileVO, file);
        return file;
    }

    /**
     * 对象转包装类
     *
     * @param file
     * @return
     */
    public static FileVO objToVo(File file) {
        if (file == null) {
            return null;
        }
        FileVO fileVO = new FileVO();
        BeanUtils.copyProperties(file, fileVO);
        return fileVO;
    }
}
