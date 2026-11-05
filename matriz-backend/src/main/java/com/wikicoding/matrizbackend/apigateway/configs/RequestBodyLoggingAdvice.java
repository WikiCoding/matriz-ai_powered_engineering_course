package com.wikicoding.matrizbackend.apigateway.configs;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/** Logs a safe projection after conversion; never reads or buffers the raw request stream. */
@ControllerAdvice(basePackages = "com.wikicoding.matrizbackend")
@Slf4j
public class RequestBodyLoggingAdvice extends RequestBodyAdviceAdapter {
    private final boolean enabled;

    public RequestBodyLoggingAdvice(@Value("${app.logging.request-bodies:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return enabled && log.isDebugEnabled();
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        var fields = new LinkedHashMap<String, Object>();
        if (body.getClass().isRecord()) {
            for (var component : body.getClass().getRecordComponents()) {
                // Default-deny: identifiers, free text, credentials and future fields stay redacted.
                Object value = "[REDACTED]";
                try {
                    if ((component.getName().equals("status") || component.getName().equals("priority"))
                            && component.getType().isEnum()) {
                        var enumeration = (Enum<?>) component.getAccessor().invoke(body);
                        value = enumeration == null ? null : enumeration.name();
                    } else if (component.getName().equals("effort") && component.getType() == Short.class) {
                        value = component.getAccessor().invoke(body);
                    }
                } catch (ReflectiveOperationException ignored) {
                    value = "[UNAVAILABLE]";
                }
                fields.put(component.getName(), value);
            }
        }
        var attributes = RequestContextHolder.getRequestAttributes();
        Object requestId = attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getAttribute(HttpRequestLoggingFilter.REQUEST_ID) : "unavailable";
        log.debug("HTTP body requestId={} type={} fields={}", requestId, body.getClass().getSimpleName(), fields);
        return body;
    }
}
