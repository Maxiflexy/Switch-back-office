package filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CORSFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Handle preflight requests (OPTIONS)
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setHeader("Access-Control-Allow-Origin", "*"); // Allow requests from any origin
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); // Allowed methods
            response.setHeader("Access-Control-Allow-Headers", "*"); // Allowed headers
            response.setStatus(HttpServletResponse.SC_OK); // Set OK status for preflight request
            return;
        }

        // Process other requests and add CORS headers
        response.setHeader("Access-Control-Allow-Origin", "*"); // Allow requests from any origin (consider restricting later)
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); // Allowed methods
        response.setHeader("Access-Control-Allow-Headers", "*"); // Allowed headers
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
