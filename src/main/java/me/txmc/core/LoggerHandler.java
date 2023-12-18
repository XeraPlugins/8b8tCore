package me.txmc.core;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * @author 254n_m
 * @since 2023/12/17 9:56 PM
 * This file was created as a part of 8b8tCore
 */
public class LoggerHandler extends Handler {
    @Override
    public void publish(LogRecord logRecord) {
        String ogMessage = logRecord.getMessage();
        if (ogMessage.indexOf(Character.MIN_VALUE) != -1) {
            String[] raw = ogMessage.split(String.valueOf(Character.MIN_VALUE));
            String message = raw[0];
            String[] rawCallerName = raw[1].split("\\.");
            String callerName = rawCallerName[rawCallerName.length - 1];
            logRecord.setMessage(message);
            logRecord.setLoggerName(String.format("%s/%s", logRecord.getLoggerName(), callerName));
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
