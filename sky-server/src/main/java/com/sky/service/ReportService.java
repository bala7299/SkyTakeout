package com.sky.service;

import com.sky.vo.AiDiagnosisVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO turnoverReport(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userReport(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO ordersReport(LocalDate begin, LocalDate end);

    /**
     * 销售排行榜
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO salesTop10Report(LocalDate begin, LocalDate end);

    /**
     * AI 经营诊断（差评统计 + 菜品排行 + AI 文案）
     *
     * @param begin 开始日期（含当天 0 点）
     * @param end   结束日期（含当天结束时刻）
     * @return 诊断数据
     */
    AiDiagnosisVO getAiDiagnosis(LocalDate begin, LocalDate end);

    /**
     * 导出运营数据报表
     * @param response
     */
    void export(HttpServletResponse response);
}
