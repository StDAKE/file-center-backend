package com.huaixv06.fileCenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaixv06.fileCenter.mapper.TypeMapper;
import com.huaixv06.fileCenter.model.entity.Type;
import com.huaixv06.fileCenter.service.TypeService;
import org.springframework.stereotype.Service;

/**
* @author 11517
* @description 针对表【type】的数据库操作Service实现
* @createDate 2024-11-12 18:09:15
*/
@Service
public class TypeServiceImpl extends ServiceImpl<TypeMapper, Type>
    implements TypeService{

}




