package filters;

import io.jsonwebtoken.Claims;
import util.BaseBean;
import util.JsonUtil;
import util.JwtUtil;

import javax.json.JsonObject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecureFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String authHeader = httpRequest.getHeader("Authorization");


        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = JwtUtil.validateToken(token);
                httpRequest.setAttribute("claims", claims);

                chain.doFilter(request, response);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        httpResponse.setContentType("application/json");
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        BaseBean jsonResponse = new BaseBean();
        jsonResponse.put("message", "Unauthorized");

        JsonObject jsonObject = JsonUtil.convertBeanToJsonObject(jsonResponse);

        httpResponse.getWriter().write(JsonUtil.toStr(jsonObject));
    }


    @Override
    public void destroy() {
        // Cleanup if needed
    }
}

