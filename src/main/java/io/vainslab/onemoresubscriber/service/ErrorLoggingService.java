package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.entity.ErrorLog;
import io.vainslab.onemoresubscriber.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLoggingService {

    private final ErrorLogRepository errorLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(BotUser user, String action, Exception ex) {
        try {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setUser(user);
            errorLog.setAction(action);
            errorLog.setExceptionType(ex.getClass().getSimpleName());
            errorLog.setMessage(ex.getMessage());
            errorLog.setStackTrace(getStackTrace(ex));
            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("Failed to persist error log for action={}", action, e);
        }
    }

    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
