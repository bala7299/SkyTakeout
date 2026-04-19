package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * AI 经营诊断报表数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDiagnosisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 差评总数（score 小于等于 2） */
    private Integer totalBadReviews;

    /** AI 诊断建议文案 */
    private String aiAdvice;

    /** ECharts 坐标轴：菜品名称 */
    private List<String> dishNames;

    /** 与 dishNames 一一对应的差评关联统计数量 */
    private List<Integer> badCounts;
}
