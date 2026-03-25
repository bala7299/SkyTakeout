package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解 用于识别某些方法需要的功能字段 并进行自动填充处理
 */
@Target(ElementType.METHOD)  // 规定贴纸的粘贴位置：只能贴在方法上
@Retention(RetentionPolicy.RUNTIME)  // 规定贴纸的保质期：程序跑起来（运行时）贴纸依然有效，不会掉
public @interface AutoFill {
    // 数据库操作类型 UPDATE INSERT
    /**
     * 贴纸上的填空
     * * 返回类型：OperationType (这是一个枚举类，相当于下拉菜单，里面有 UPDATE 和 INSERT)
     * 就相当于类的一个属性，用的时候要给他赋值
     * 怎么用：在方法上贴贴纸时，必须填空，比如 @AutoFill(OperationType.INSERT)
     */
    OperationType value();
}
