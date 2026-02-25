package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler 
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        //Duplicate entry 'bala' for key 'employee.idx_username';
        String message = ex.getMessage();
        if (ex.getErrorCode() == 1062) {
            Pattern pattern = Pattern.compile("Duplicate entry '(.*?)'");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String username = matcher.group(1);
                String msg = username + MessageConstant.ACCOUNT_EXISTS;
                return Result.error(msg);
            }
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}