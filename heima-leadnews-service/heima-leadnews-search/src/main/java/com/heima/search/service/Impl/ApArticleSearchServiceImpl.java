package com.heima.search.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.vos.SearchArticleVo;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.common.AppThreadLocalUtil;
import com.heima.search.service.ApArticleSearchService;
import com.heima.search.service.ApUserSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ApArticleSearchServiceImpl implements ApArticleSearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * es 分页查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) {
        String searchWords = dto.getSearchWords();
        //1.检查参数
        if (dto == null || StringUtils.isBlank(searchWords)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //浏览器发送请求到达Tomcat，会自动创建一个新的线程来执行代码
        //主线程：拦截器-》controller-》service
        ApUser user = AppThreadLocalUtil.getUser();
        log.error("取出的userid是{}",user);
//2.设置查询条件
        if(user != null && user.getId() != 0 && dto.getFromIndex() == 0) {
            //@Async就会创建子线程，因此在这个方法内部无法获取到用户ID
            apUserSearchService.insert(dto.getSearchWords(), user.getId());
        }
        SearchRequest searchRequest = new SearchRequest("app_info_article");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //关键字的分词之后查询
        //执行条件过滤
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //关键字查询
        QueryStringQueryBuilder defaultField = QueryBuilders.queryStringQuery(searchWords).field("title").field("content").defaultOperator(Operator.OR);
        boolQueryBuilder.must(defaultField);

        //查询小于mindate的数据
        RangeQueryBuilder publishTime = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime());
        boolQueryBuilder.filter(publishTime);
        //分页查询
        // 分页
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(dto.getPageSize());

        // 按照发布时间倒序
        searchSourceBuilder.sort("publishTime", SortOrder.DESC);
        // 高亮 三要素
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
        highlightBuilder.postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        searchSourceBuilder.query(boolQueryBuilder);

        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            //3 解析结果 封装结果
            SearchHits searchHits = searchResponse.getHits();
            List<Map> resultList = new ArrayList<>();
            // 总记录数
            long total = searchHits.getTotalHits().value;
            log.info("search result total:{}", total);
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                String jsonString = hit.getSourceAsString();
                Map apArticle = JSON.parseObject(jsonString, Map.class);

                if (hit.getHighlightFields() != null && hit.getHighlightFields().size() > 0) {
                    // 获取高亮结果集
                    Text[] titles = hit.getHighlightFields().get("title").getFragments();
                    // 保留原始标题
                    // apArticle.put("title", apArticle.get("title"));
                    //["123", "456"] -> 123456
                    String title = StringUtils.join(titles);

                    //高亮标题
                    apArticle.put("h_title", title);
                } else {
                    //原始标题
                    apArticle.put("h_title", apArticle.get("title"));
                }

                resultList.add(apArticle);
            }

            return ResponseResult.okResult(resultList);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("search result error:{}", e);
        }
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);

    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void createArticleESIndex(ApArticle apArticle, String content, String path) {
        //获取vo对象
        SearchArticleVo vo = new SearchArticleVo();
        //属性拷贝
        BeanUtils.copyProperties(apArticle, vo);
        //设置文章内容
        vo.setContent(content);
        //静态页面路径
        vo.setStaticUrl(path);

    }
}
