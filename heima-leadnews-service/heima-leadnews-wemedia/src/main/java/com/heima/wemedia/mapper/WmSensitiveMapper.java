package com.heima.wemedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.wemedia.pojos.WmSensitive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自定义敏感词管理
 */
@Mapper
public interface WmSensitiveMapper extends BaseMapper<WmSensitive> {
}