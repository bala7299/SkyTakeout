package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO turnoverReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        while (!begin.isAfter(end)) {
            dates.add(begin);
            begin = begin.plusDays(1);
        }
        List<BigDecimal> turnoverList = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime begintime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endtime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begintime", begintime);
            map.put("endtime", endtime);
            map.put("status", Orders.COMPLETED);
            BigDecimal turnover = orderMapper.getSumByDate(map);
            turnover = turnover == null ? BigDecimal.ZERO : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder().
                dateList(StringUtils.join(dates, ",")).turnoverList(StringUtils.join(turnoverList, ",")).build();
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO userReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        List<Integer> allUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        LocalDateTime endtime = LocalDateTime.of(begin, LocalTime.MIN);
        Map firstMap = new HashMap();
        firstMap.put("endtime", endtime);
        Integer allUser = userMapper.countUserByDate(firstMap);
        allUser = allUser == null ? 0 : allUser;
        for (LocalDate date : dates) {
            LocalDateTime begintime = LocalDateTime.of(date, LocalTime.MIN);
            endtime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begintime", begintime);
            map.put("endtime", endtime);
            Integer newUser = userMapper.countUserByDate(map);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
            allUser += newUser;
            allUserList.add(allUser);
        }
        return UserReportVO.builder().dateList(StringUtils.join(dates, ",")).
                newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(allUserList, ",")).build();
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO ordersReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dateList.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        List<Integer> orderCountList = new ArrayList<>(); // 每天的总订单
        List<Integer> validOrderCountList = new ArrayList<>(); // 每天的有效订单
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begintime", beginTime);
            map.put("endtime", endTime);

            Integer orderCount = orderMapper.countByMap(map);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);

            map.put("status", Orders.COMPLETED);
            Integer validCount = orderMapper.countByMap(map);
            validCount = validCount == null ? 0 : validCount;
            validOrderCountList.add(validCount);
            totalOrderCount += orderCount;
            validOrderCount += validCount;
        }
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销售排行榜
     *
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO salesTop10Report(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dateList.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        Map map = new HashMap();
        map.put("begintime", beginTime);
        map.put("endtime", endTime);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.goodsSalesTop10(map);
        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> countList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String name = StringUtils.join(nameList, ",");
        String number = StringUtils.join(countList, ",");
        return new SalesTop10ReportVO(name, number);
    }

    /**
     * 导出运营数据报表
     *
     * @param response
     */
    public void export(HttpServletResponse response) {
        //查询数据库，获得数据
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(beginDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX));
        //通过POI设置excel表格并填充数据
        //在文件里面找到resource中的报表模板。并将其变成一个输入流接过来
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于模板文件创建excel 将刚刚得到的输入流接到这个excel里面
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //填充数据
            XSSFSheet sheet = excel.getSheet("sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间：" + beginDate + "至" + endDate);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            //POI中，单元格的值默认是字符串类型，所以需要将BigDecimal类型转换成字符串类型
            row.getCell(2).setCellValue(businessData.getTurnover().doubleValue());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = beginDate.plusDays(i);
                BusinessDataVO perDateBusiness =
                        workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(perDateBusiness.getTurnover().doubleValue());
                row.getCell(3).setCellValue(perDateBusiness.getValidOrderCount());
                row.getCell(4).setCellValue(perDateBusiness.getOrderCompletionRate());
                row.getCell(5).setCellValue(perDateBusiness.getUnitPrice());
                row.getCell(6).setCellValue(perDateBusiness.getNewUsers());
            }

            //通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);
            //关闭流
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
