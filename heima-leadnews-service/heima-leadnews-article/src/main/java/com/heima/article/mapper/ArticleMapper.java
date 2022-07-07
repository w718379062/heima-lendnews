package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.dtos.ArticleHomeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<ApArticle> {
   public List<ApArticle> loadArticleList(@Param("dto") ArticleHomeDto dto, @Param("type") Short type);


   //2023/07/05 14:21 -> 2023/06/30 14:21
   //where publish_time >= '2023/06/30 14:21'
   public List<ApArticle> findArticleListByLast5days(@Param("dayParam") Date dayParam);




}

