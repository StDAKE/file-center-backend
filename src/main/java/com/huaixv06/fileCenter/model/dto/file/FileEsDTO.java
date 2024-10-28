package com.huaixv06.fileCenter.model.dto.file;

import com.huaixv06.fileCenter.model.entity.File;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;


@Document(indexName = "file")
@Data
public class FileEsDTO implements Serializable {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date createTime;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     *
     * @param file
     * @return
     */
    public static FileEsDTO objToDto(File file) {
        if (file == null) {
            return null;
        }
        // TODO 对文件内容进行转换
        FileEsDTO fileEsDTO = new FileEsDTO();
        BeanUtils.copyProperties(file, fileEsDTO);
        return fileEsDTO;
    }

    /**
     * 包装类转对象
     *
     * @param fileEsDTO
     * @return
     */
    public static File dtoToObj(FileEsDTO fileEsDTO) {
        if (fileEsDTO == null) {
            return null;
        }
        File file = new File();
        BeanUtils.copyProperties(fileEsDTO, file);
        return file;
    }
}
