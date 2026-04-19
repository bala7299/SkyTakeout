package com.sky.vo;

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
public class RecommendItemVO implements Serializable {

    private Long id;
    
    private String name;
    
    private BigDecimal price;
    
    private String image;
    
    private String flavorTag;
    
    private Integer type;
}