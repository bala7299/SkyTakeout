package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 1. 注册拦截器（给保安白名单）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        // 放行列表（Knife4j 所需的所有路径）
        String[] excludePaths = new String[]{
                "/doc.html", "/webjars/**", "/v3/api-docs","/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui.html"
        };

        // 管理端保安
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login")
                .excludePathPatterns(excludePaths); // 强行放行文档

        // 用户端保安
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login", "/user/shop/status")
                .excludePathPatterns(excludePaths); // 强行放行文档
    }

    /**
     * 2. 生成管理端 API (替代旧版的 Docket)
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("苍穹外卖管理端").version("2.0")))
                .build();
    }

    /**
     * 3. 生成用户端 API (替代旧版的 Docket)
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .pathsToMatch("/user/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("苍穹外卖用户端").version("2.0")))
                .build();
    }

    /**
     * 4. 强制静态资源映射 (死守 doc.html 的路线)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("强制映射接口文档静态资源...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 5. 日期转换器（扩展消息转换器以支持自定义 LocalDateTime 序列化格式）
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new JacksonObjectMapper());
        // 插入到默认转换器之后，避免影响 springdoc 的 JSON 序列化
        converters.add(converters.size(), converter);
    }
}