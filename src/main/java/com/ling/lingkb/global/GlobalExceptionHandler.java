package com.ling.lingkb.global;

import com.ling.lingkb.entity.Reply;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
* @author shipotian
* @date 2025/6/19
* @since 1.0.0
*/
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public Reply exception(Exception e) {
        log.error("Request exception occurred", e);
        if (StringUtils.isNotBlank(e.getMessage())) {
            return Reply.failure(e.getMessage());
        }
        return Reply.failure();
    }

}
