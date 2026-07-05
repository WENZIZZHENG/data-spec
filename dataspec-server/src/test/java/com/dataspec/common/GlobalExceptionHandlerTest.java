package com.dataspec.common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void handleBizExceptionLogsSanitizedMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            handler.handleBizException(new BizException(
                    "连接失败 password=raw-secret Authorization: Basic raw-auth jdbc:postgresql://host/db dsn=postgres://user:pass@host/db"));

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertTrue(logs.contains("[REDACTED]"));
            assertFalse(logs.contains("raw-secret"));
            assertFalse(logs.contains("raw-auth"));
            assertFalse(logs.contains("jdbc:postgresql://host"));
            assertFalse(logs.contains("user:pass@host"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
