package messaging.fileUtils.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenInvalidationFilter implements Filter {
    private final Map<String, Long> invalidatedTokens = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String tokenId = (String) httpRequest.getAttribute("tokenId"); // Get token from request
        long expirationTime = (long) httpRequest.getAttribute("expiration-time");
        long currentTime = System.currentTimeMillis();

        System.out.println(httpRequest.getServletPath());
        if (tokenId != null && invalidatedTokens.containsKey(tokenId)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("unauthorized");
            return;
        }

        if ("/switch/auth/logout".equalsIgnoreCase(httpRequest.getServletPath()) && tokenId != null) {
            cleanUpExpiredTokens(currentTime);
            invalidatedTokens.put(tokenId, expirationTime);
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.getWriter().write("logout successful");
            return;
        }

        chain.doFilter(request, response);
    }

    private void cleanUpExpiredTokens(long currentTime) {
        invalidatedTokens.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }


}
