package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {
    /**
     * 查询所有文章的接口
     * @param dto
     * @return
     */
     ResponseResult list(WmNewsPageReqDto dto);

    /**
     * 文章发布接口
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto);
    /**
     * 文章的上下架
     * @param dto
     * @return
     */
    public ResponseResult downOrUp(WmNewsDto dto);

}
