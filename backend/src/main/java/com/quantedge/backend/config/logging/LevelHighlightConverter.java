package com.quantedge.backend.config.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

/**
 * Logback conversion word ({@code %levelClr(...)}, wired up in logback-spring.xml) that colors
 * the log level by severity: ERROR=red, WARN=yellow, INFO=green, DEBUG/TRACE=faint/dim. Spring
 * Boot's built-in %clr(%level) highlight only colors ERROR/WARN/INFO and leaves DEBUG/TRACE
 * uncolored, so this fills that gap rather than pulling in a separate logging library.
 */
public final class LevelHighlightConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        return switch (event.getLevel().toInt()) {
            case Level.ERROR_INT -> "31"; // red
            case Level.WARN_INT -> "33"; // yellow
            case Level.INFO_INT -> "32"; // green
            default -> "2"; // faint/dim - DEBUG, TRACE
        };
    }
}
