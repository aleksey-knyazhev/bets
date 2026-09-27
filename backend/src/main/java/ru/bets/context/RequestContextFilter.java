package ru.bets.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component("scopedRequestContextFilter")
public class RequestContextFilter extends HttpFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        var context = new RequestContext(requestId);

        try {
            ScopedValue.where(RequestContextHolder.scopedValue(), context).call(() -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        var requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
