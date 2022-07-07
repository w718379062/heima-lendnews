package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.common.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
@Service
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
    /**
     * 图文素材上传
     * @param file Spring提供的简单化的上传工具
     * @return
     */
    @Autowired
    private  FileStorageService fileStorageService;
    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        if (multipartFile==null || multipartFile.getSize()==0){
            //file中没有参数或者为0没必要进行下一步
                //给前端返回一个无效的参数
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //上传图片到minio中
           //生成文件名称
        String fileName = UUID.randomUUID().toString().replace("-", "");
        //获取原始文件名
        String originalFilename = multipartFile.getOriginalFilename();

        //切割原始文件名取后缀
        //断言表达式
        assert originalFilename != null;
        //得到文件的后缀名
        String substring = originalFilename.substring(originalFilename.lastIndexOf("."));
        String imgFile = null;
        try {
             imgFile = fileStorageService.uploadImgFile("", fileName + substring, multipartFile.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("上传图片失败");
        }
        //保存图片到数据库中
        WmMaterial wmMaterial = new WmMaterial();
        //设置图片地址
        wmMaterial.setUrl(imgFile);
        //设置创建时间
        wmMaterial.setCreatedTime(new Date());
        //设置类型 0 : 图片  1: 视频
        wmMaterial.setType((short) 0);

        //设置自媒体用户id
        wmMaterial.setUserId(WmThreadLocalUtils.getUser().getId());
        //是否收藏   0 : 没有收藏 1: 收藏
        wmMaterial.setIsCollection((short) 0);
        save(wmMaterial);
        return ResponseResult.okResult(wmMaterial);
    }

    /**
     * 素材列表查询
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(WmMaterialDto dto) {
        //检查参数
        dto.checkParam();
        //分页查询
        //构建分页查询
        Page page=new Page(dto.getPage(), dto.getSize());
           //分页查询条件
        LambdaQueryWrapper<WmMaterial> wrapper =new LambdaQueryWrapper<>();
           //判断是否是查询收藏的
        wrapper.eq(dto.getIsCollection()!=null&&dto.getIsCollection()==1,WmMaterial::getIsCollection,dto.getIsCollection());
        //查询当前登录用户的素材列表
        wrapper.eq(WmMaterial::getUserId,WmThreadLocalUtils.getUser().getId());
           //按照时间倒序排序
        wrapper.orderByDesc(WmMaterial::getCreatedTime);
      this.page(page,wrapper);
        //结果封装
        ResponseResult responseResult=new PageResponseResult(dto.getPage(),dto.getSize(), (int) page.getTotal());

        //把查询出的数据放入到
        responseResult.setData(page.getRecords());
        return responseResult;



    }
}
