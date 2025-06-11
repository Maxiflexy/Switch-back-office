package util;

import io.jsonwebtoken.Claims;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AuthUtil {
    public static boolean isValidRole(HttpServletRequest request, String validRole) {

        String token = request.getHeader("Authorization");

        if (token != null && JwtUtil.validateToken(token) != null) {
            Claims claims = JwtUtil.validateToken(token);
            String role = (String) claims.get("role");
            System.out.println("role: " + role);

            return validRole.equals(role);
        }
        return false;
    }

public static String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
