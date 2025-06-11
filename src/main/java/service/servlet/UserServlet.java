package service.servlet;

import dao.UserDAO;
import model.Role;
import model.User;
import services.servlets.BaseServlet;
import util.BaseBean;
import util.JsonUtil;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


public class UserServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws  IOException {
        String idParam = request.getParameter("id");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String uri = request.getRequestURI();

        try {
            if (idParam != null) {
                long id = Long.parseLong(idParam);
                User user = userDAO.getUserById(id);
                if (user != null) {
                    String userJson = convertToBean(user);
                    out.println(userJson);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.println("{\"error\": \"User not found\"}");
                }
            } else {
                if (uri.equals("/user")) {
                    List<User> users = userDAO.getAllUsers();
                    String usersJson = convertToBean(users);
                    out.println(usersJson);
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("{\"error\": \"Invalid user ID format\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String requestBody = getBody(request);

        JsonObject jsonObject = JsonUtil.toJsonObject(requestBody);

        String username = JsonUtil.getJsonObjValue(jsonObject, "username");
        String newRoleStr = JsonUtil.getJsonObjValue(jsonObject, "role");

        // Validate input parameters
        if (username == null || username.isEmpty() || newRoleStr == null || newRoleStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Username and role are required.\"}");
            return;
        }

        try {
            // Convert the new role to the Role enum
            Role newRole = Role.valueOf(newRoleStr.toUpperCase());

            // Update the user's role using UserDAO
            userDAO.updateUserRoleByUsername(username, newRole);

            response.getWriter().write("{\"message\": \"Role successfully changed\"}");
            } catch (IllegalArgumentException e) {
                // Handle invalid role
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid role specified.");
            } catch (Exception e) {
                // Handle other exceptions
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while updating the role.");
            }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    private String convertToBean(User user) {
        BaseBean bean = new BaseBean();

        bean.put("id", String.valueOf(user.getId()));
        bean.put("username", user.getUsername());
        bean.put("role", user.getRole().toString());

        JsonObject jsonObject = JsonUtil.convertBeanToJsonObject(bean);

        return JsonUtil.toStr(jsonObject);
    }

    private String convertToBean(List<User> users) {
        StringBuilder sb = new StringBuilder();

        for (User user : users) {
            String userStr = convertToBean(user);
            sb.append(userStr);
        }

        return sb.toString();
    }
}

