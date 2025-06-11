package service.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dao.UserDAO;
import io.jsonwebtoken.Claims;
import model.Role;
import model.User;
import org.apache.http.ParseException;
import service.AuthService;
import util.AuthUtil;
import util.JwtUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginServlet extends HttpServlet {
    private AuthService authService;
    private UserDAO userDAO;

    @Override
    public void init() {
        this.authService = new AuthService();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();

        if ("/auth/login".equals(uri)) {
            handleLogin(request, response);
        } else if ("/auth/refresh".equals(uri)) {
            handleTokenRefresh(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Endpoint not found");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Read the request body as a String
        String requestBody = AuthUtil.readRequestBody(request);

        String username = null;
        String password = null;

        try {
            Gson gson = new Gson();

            Map<String, String> jsonMap = gson.fromJson(requestBody, Map.class);

            username = jsonMap.get("username");
            password = jsonMap.get("password");
        } catch (JsonSyntaxException e) {
            // Handle exception if the JSON is invalid
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid JSON format in request body");
        }

        String token = null;

        try {
            if (authService.verifyCredentialsWithApi(username, password)) {
                if (!userDAO.usernameExist(username)) {
                    userDAO.saveUser(new User(username, Role.USER));
                }

                token = authService.authenticate(username, password);
            }

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


        if (token != null) {
            // Create a Map to hold response data
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("token", token);
            responseMap.put("username", username);
            responseMap.put("role", JwtUtil.getRole(token));


            String jsonResponse = new Gson().toJson(responseMap);

            response.setContentType("application/json");

            response.getWriter().write(jsonResponse);
            response.setStatus(HttpServletResponse.SC_OK);


        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid username or password");
        }
    }

    private void handleTokenRefresh(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Read the request body as a String (optional)
        // String requestBody = AuthUtil.readRequestBody(request);

        String refreshToken = null;

        try {
            // Extract the refresh token from the request header (Authorization: Bearer <refresh_token>)
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                refreshToken = authorizationHeader.substring(7);
            } else {
                throw new RuntimeException("Missing refresh token in Authorization header");
            }

            // Validate the refresh token (check if it's valid and not expired on the server-side)
            Claims claims = JwtUtil.validateToken(refreshToken); // throws exception if invalid

            // Extract username and role from the refresh token claims
            String username = claims.getSubject();
            String role = JwtUtil.getRole(refreshToken);

            // Generate a new access token using username and role
            String newAccessToken = JwtUtil.generateToken(username, role);

            response.setStatus(HttpServletResponse.SC_OK);


            // Create a Map to hold response data
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("token", newAccessToken);
            responseMap.put("username", username);
            responseMap.put("role", JwtUtil.getRole(newAccessToken));


            String jsonResponse = new Gson().toJson(responseMap);

            response.setContentType("application/json");

            response.getWriter().write(jsonResponse);


        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid refresh token");
        }
    }


}
// eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJ1c2VybmFtZSI6IkRhbWkiLCJzdWIiOiJEYW1pIiwiaWF0IjoxNzE4NjE1NzAyLCJleHAiOjE3MTg3MDIxMDJ9.I984xwlY0v1c8Ek8OfrNh3gK99T5fepNPXXvgekMO3Y
