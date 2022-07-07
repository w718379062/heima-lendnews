package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialService extends IService<WmMaterial> {
    /**
     * 自媒体上传素材
     * @param file
     * @return
     */
    public ResponseResult uploadPicture(MultipartFile multipartFile);
    public ResponseResult list(WmMaterialDto dto);
}
