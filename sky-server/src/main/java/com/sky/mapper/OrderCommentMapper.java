package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CommentPageQueryDTO;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderComment;
import com.sky.vo.AdminCommentVO;
import com.sky.vo.CommentVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderCommentMapper {

    /**
     * 插入订单评价
     *
     * @param orderComment 订单评价实体
     */
    @Insert("insert into sky_take_out.order_comment (order_id, user_id, score, content, ai_optimized, create_time, reply_content, status) "
            + "values (#{orderId}, #{userId}, #{score}, #{content}, #{aiOptimized}, #{createTime}, #{replyContent}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderComment orderComment);

    /**
     * 管理端条件分页查询
     *
     * @param commentPageQueryDTO 查询条件
     * @return 分页数据
     */
    Page<AdminCommentVO> pageQuery(CommentPageQueryDTO commentPageQueryDTO);

    /**
     * 用户端：历史评价分页（条件由 DTO 提供，通常需包含 userId）
     *
     * @param commentPageQueryDTO 分页与筛选条件
     * @return 分页数据，映射为 CommentVO
     */
    Page<CommentVO> pageQuery4User(CommentPageQueryDTO commentPageQueryDTO);

    /**
     * 动态更新评价（如商家回复）
     *
     * @param orderComment 待更新字段
     */
    void update(OrderComment orderComment);

    /**
     * 商家回复评价（同步更新 reply_content 和 status=1）
     *
     * @param id 评价主键
     * @param replyContent 回复内容
     */
    void replyComment(@Param("id") Long id, @Param("replyContent") String replyContent);

    /**
     * 统计指定时间范围内差评（score &lt;= 2）数量
     */
    Integer countBadReviewsByDateRange(@Param("beginTime") LocalDateTime beginTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 差评订单关联订单明细，按菜品名称统计差评关联次数，取前 5
     */
    List<GoodsSalesDTO> listTopBadDishesByDateRange(@Param("beginTime") LocalDateTime beginTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 指定时间范围内所有差评的正文（用于 AI 汇总）
     */
    List<String> listBadReviewContentsByDateRange(@Param("beginTime") LocalDateTime beginTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 异步回填 AI 润色文案
     */
    @Update("UPDATE sky_take_out.order_comment SET ai_optimized = #{aiOptimized} WHERE id = #{id}")
    void updateAiOptimized(@Param("id") Long id, @Param("aiOptimized") String aiOptimized);

    @Select("SELECT oc.* FROM sky_take_out.order_comment oc " +
            "LEFT JOIN sky_take_out.order_detail od ON oc.order_id = od.order_id " +
            "WHERE od.name LIKE CONCAT('%', #{dishName}, '%') " +
            "ORDER BY oc.score DESC LIMIT 3")
    List<OrderComment> getTopReviewsByDishName(String dishName);
}

