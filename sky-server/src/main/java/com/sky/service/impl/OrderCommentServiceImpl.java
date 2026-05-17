package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.CommentPageQueryDTO;
import com.sky.dto.CommentReplyDTO;
import com.sky.dto.CommentSubmitDTO;
import com.sky.entity.OrderComment;
import com.sky.mapper.OrderCommentMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderCommentService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.AdminCommentVO;
import com.sky.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderCommentServiceImpl implements OrderCommentService {

    @Autowired
    private OrderCommentMapper orderCommentMapper;

    @Value("${sky.comment.ai-optimize-url:http://localhost:8000/ai-optimize}")
    private String aiOptimizeUrl;

    @Override
    public void submitComment(CommentSubmitDTO commentSubmitDTO) {
        log.info("处理用户评价提交：{}", commentSubmitDTO);
        Long userId = BaseContext.getCurrentId();
        String content = commentSubmitDTO.getContent();

        // 先入库，AI 润色留空，秒级响应用户
        OrderComment orderComment = OrderComment.builder()
                .orderId(commentSubmitDTO.getOrderId())
                .userId(userId)
                .score(commentSubmitDTO.getScore() != null ? commentSubmitDTO.getScore() : 5)
                .content(content)
                .aiOptimized(null)
                .createTime(LocalDateTime.now())
                .replyContent(null)
                .status(0)
                .build();

        orderCommentMapper.insert(orderComment);
        Long commentId = orderComment.getId();

        // 异步调 AI 润色，不阻塞用户
        CompletableFuture.runAsync(() -> {
            String aiOptimized = fetchAiOptimizedContent(content);
            if (aiOptimized != null) {
                orderCommentMapper.updateAiOptimized(commentId, aiOptimized);
                log.info("AI 润色回填成功，commentId={}", commentId);
            }
        });
    }

    /**
     * 调用本地 Python 服务获取润色文案；失败或异常时返回 null，不影响原评论入库。
     */
    private String fetchAiOptimizedContent(String rawContent) {
        try {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("content", rawContent != null ? rawContent : "");
            String response = HttpClientUtil.doGet(aiOptimizeUrl, paramMap);
            if (!StringUtils.hasText(response)) {
                log.warn("AI 润色接口返回为空，aiOptimized 将留空");
                return null;
            }
            JSONObject jsonObject = JSON.parseObject(response);
            if (!"success".equals(jsonObject.getString("status"))) {
                log.warn("AI 润色接口非成功状态，响应：{}", response);
                return null;
            }
            String optimized = jsonObject.getString("optimized_content");
            return StringUtils.hasText(optimized) ? optimized : null;
        } catch (Exception e) {
            log.warn("调用 AI 润色接口失败，将继续保存原评论，aiOptimized 留空", e);
            return null;
        }
    }

    @Override
    public PageResult pageQuery(CommentPageQueryDTO commentPageQueryDTO) {
        PageHelper.startPage(commentPageQueryDTO.getPage(), commentPageQueryDTO.getPageSize());
        Page<AdminCommentVO> page = orderCommentMapper.pageQuery(commentPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public PageResult pageQuery4User(CommentPageQueryDTO commentPageQueryDTO) {
        PageHelper.startPage(commentPageQueryDTO.getPage(), commentPageQueryDTO.getPageSize());
        Page<CommentVO> commentPage = orderCommentMapper.pageQuery4User(commentPageQueryDTO);
        return new PageResult(commentPage.getTotal(), commentPage.getResult());
    }

    @Override
    public void replyComment(CommentReplyDTO commentReplyDTO) {
        log.info("商家回复评价：{}", commentReplyDTO);
        orderCommentMapper.replyComment(commentReplyDTO.getId(), commentReplyDTO.getReplyContent());
    }
}
