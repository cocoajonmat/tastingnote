package com.dongjin.tastingnote.common.resolver;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)  // Swagger에서 이 파라미터를 쿼리 파라미터로 노출하지 않음
public @interface CurrentUserId {
}
