package util;

import io.jsonwebtoken.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhXEvZcwDdEkwprsRDvsf5UfDE0TsESj4JgEG6qmUgRwvBXFEzbeThd7yODMMHSq+M0WM+YGM3CL/LZxRR9GOiwqUrC6Rpxuxdqa6x2XUMN+0uYLNLd/YFTyzxaQ04rzeoz9fGgsW3czGuwdvOGkPCIQcXD3fRr7w1GNw83vlqsyXbJwDPBWdsMYSjphUxgWjT+IOCiMD3W+2OgenoZEAH4qBa/M0Rt8K4ex0Zr9o/+SF13ggpqQPc9+YbpvFu6UHRJn33DTo4VHRoSsuYlpHo8N1SF+zMBBmf/XVMUSSzYskGM+D10QI/6YeWJoQ7ncd9Tkv+ST5WqtJWsiZpzhU6wIDAQAB";
    private static final long EXPIRATION_TIME = 86400000; // 1 day in milliseconds

    public static String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }


    public static Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token has expired", e);
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid token", e);
        }
    }


    public static boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration().before(new Date());
    }

    public static String getUsername(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }
    public static String getUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    public static Claims getAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    public static String getRole(String token) {
        return (String) getAllClaims(token).get("role");
    }

    public static String getToken(HttpServletRequest request) {
        return request.getHeader("Authorization").substring(7);
    }

    // https://d909-129-205-113-176.ngrok-free.app -> http://localhost:9090
}
