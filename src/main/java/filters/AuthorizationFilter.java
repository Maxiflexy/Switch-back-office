package filters;


import constants.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import util.BaseBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AuthorizationFilter implements Filter {

    final static Logger LOG = LogManager.getLogger(AuthorizationFilter.class);

//    private static final List<String> ROLES_USER = Arrays.asList("USER", "INITIATOR", "ADMIN");
//    private static final List<String> ROLES_REQUEST = Arrays.asList("INITIATOR", "ADMIN");
//    private static final List<String> ROLES_ADMIN = Collections.singletonList("ADMIN");


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String svciD = httpRequest.getHttpServletMapping().getServletName().toLowerCase().replace("servlet", "");
        String requestPath = httpRequest.getServletPath();
        System.out.println("path:: " + httpRequest.getContextPath());
        LOG.info("SVCID:  {}", svciD);

        BaseBean requestBean = new BaseBean();
        requestBean.setString("email", (String) request.getAttribute("username"));
        requestBean.setString("role_id", (String) request.getAttribute("role_id"));
        requestBean.setString("url", requestPath);
        requestBean.setString("method", ((HttpServletRequest) request).getMethod());

        boolean hasRightPermission = DBHelper.hasRightPermission(requestBean);
        LOG.info("Has right permission: {}", hasRightPermission);

        if (hasRightPermission) {
            chain.doFilter(request, response);
        } else {
            request.setAttribute("javax.servlet.error.status_code", 403);
            response.setContentType("application/json");
            RequestDispatcher rd = request.getRequestDispatcher("/error");
            rd.include(request, response);
        }
    }

    private Boolean checkPermission(Permission permission, List<String> permissions) {
        if (permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(permission.name());
    }

    private boolean isAuthorized(String uri, String role) {

//        if (uri.contains("get_all") || uri.contains("get")) {
//            return ROLES_USER.contains(role);
//        } else if (uri.startsWith("/apiRegistry/") && !uri.contains("get")) {
//            return ROLES_REQUEST.contains(role);
//        } else if (uri.startsWith("/switch/") && !uri.contains("get")) {
//            return ROLES_REQUEST.contains(role);
//        }  else if (uri.startsWith("/thirdPartyAddr/") && !uri.contains("get")) {
//            return ROLES_REQUEST.contains(role);
//        } else if (uri.startsWith("/crypto_config/") && !uri.contains("get")) {
//            return ROLES_REQUEST.contains(role);
//        } else if (uri.startsWith("/apiConfig/") && !uri.contains("get")) {
//            return ROLES_REQUEST.contains(role);
//        } else if (uri.startsWith("/user")) {
//            return ROLES_REQUEST.contains(role);
//        } else if (uri.equals("/user/changeUserRole") || uri.contains("/enable_disable")) {
//            return ROLES_ADMIN.contains(role);
//        }
        return false;
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
