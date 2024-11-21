package com.huaixv06.fileCenter.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 
 * @TableName type
 */
@Data
@TableName(value ="type")
public class Type implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父类
     */
    private Long pType;

    /**
     * 分类名称
     */
    private String typeName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}