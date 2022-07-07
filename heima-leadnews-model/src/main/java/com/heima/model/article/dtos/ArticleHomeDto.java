package com.heima.model.article.dtos;

import lombok.Data;

import java.util.Date;
@Data
public class ArticleHomeDto {
    /**
     * 最大时间：上一页
     */
    Date maxBehotTime;
    /**
     *  // 最小时间: 下一页
     */
    Date minBehotTime;
    /**
     *    // 分页size
     */
    Integer size;
    /**
     *  // 频道ID: 所有(--all--)
     */
    String tag;
}
