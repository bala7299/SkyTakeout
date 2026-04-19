package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.AiDiagnosisVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@Slf4j
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
public class ReportController {
    @Autowired
    private ReportService reportService;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("营业额统计")
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("{} 到 {} 营业额统计", begin, end);
        return Result.success(reportService.turnoverReport(begin, end));
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("用户统计")
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("{} 到 {} 用户统计", begin, end);
        return Result.success(reportService.userReport(begin, end));
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("订单统计")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("{} 到 {} 订单统计", begin, end);
        return Result.success(reportService.ordersReport(begin, end));
    }

    /**
     * 销售前十统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("销售前十统计")
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> salesTop10Report(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("{} 到 {} 销售前十统计", begin, end);
        return Result.success(reportService.salesTop10Report(begin, end));
    }

    /**
     * AI 经营诊断
     *
     * @param begin 开始日期，可为空；与 end 任一为 null 时默认最近 7 天（含今天共 7 个自然日）
     * @param end   结束日期，可为空
     */
    @ApiOperation("AI 经营诊断")
    @GetMapping("/aiDiagnosis")
    public Result<AiDiagnosisVO> aiDiagnosis(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        if (begin == null || end == null) {
            end = LocalDate.now();
            begin = end.minusDays(6);
        }
        log.info("{} 到 {} AI 经营诊断", begin, end);
        return Result.success(reportService.getAiDiagnosis(begin, end));
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @ApiOperation("导出运营数据报表")
    @GetMapping("/export")
    public void export(HttpServletResponse response){
        reportService.export(response);
    }
}
