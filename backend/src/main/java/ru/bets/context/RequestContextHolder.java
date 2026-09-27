package ru.bets.context;

import java.util.Optional;

public final class RequestContextHolder {

    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

    private RequestContextHolder() {
    }

    public static ScopedValue<RequestContext> scopedValue() {
        return REQUEST_CONTEXT;
    }

    public static Optional<RequestContext> current() {
        return REQUEST_CONTEXT.isBound() ? Optional.of(REQUEST_CONTEXT.get()) : Optional.empty();
    }
}
