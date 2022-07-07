package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/channel/")
@RestController
public class WmChannelController {


    @Autowired
    private WmChannelService wmChannelMapper;

    /**
     * 文章频道列表查询
     * @return
     */
    @GetMapping("/channels")
    public ResponseResult channels(){
        return ResponseResult.okResult(wmChannelMapper.list()) ;

    }


}
