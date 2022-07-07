package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.wemedia.service.WmNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

    @Autowired
    private WmNewsService wmNewsService;
    /**
     * 查询文章列表
     * @param dto
     * @return
     */
    @PostMapping("/list")
    public ResponseResult list(@RequestBody WmNewsPageReqDto dto){
        return wmNewsService.list(dto);
    }
    @PostMapping("/submit")
    public ResponseResult submit(@RequestBody WmNewsDto dto){

//        wmNewsService.submitNews(dto);

        return  wmNewsService.submitNews(dto);
    }

    /**
     * 文章的上下架
     * @param wmNewsDto
     * @return
     */
    @PostMapping("/down_or_up")
    public ResponseResult downOrUp(@RequestBody WmNewsDto wmNewsDto){
        return wmNewsService.downOrUp(wmNewsDto);
    }
}
