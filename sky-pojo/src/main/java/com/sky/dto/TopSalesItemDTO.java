package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSalesItemDTO implements Serializable {

    private Long dishId;

    private Long setmealId;

    private String name;

    private String image;

    private BigDecimal price;

    private Integer totalSales;
}