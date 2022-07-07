package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmSensitiveService;
import org.springframework.stereotype.Service;

/**
 * 自定义敏感词管理
 */
@Service
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive>implements WmSensitiveService {
}
