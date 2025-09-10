package messaging.fileUtils.service;


import com.google.gson.Gson;
import dao.UserDAO;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import util.JwtUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final String AUTH_API_URL =
            "https://f1a4ca39-cf2b-4e8f-981c-c8f317fc9d5f.mock.pstmn.io/login";
    private Gson gson;

    private UserDAO userDAO;

    public AuthService() {
        this.gson = new Gson();
        userDAO = new UserDAO();
    }

    public String authenticate(String username, String password) throws ParseException {
        String role;

        if (!userDAO.usernameExist(username)) {
            role = "USER";
        } else {
            role = String.valueOf(userDAO.getUserByUsername(username).getRole());
        }
        return JwtUtil.generateToken(username, role);
    }

    public boolean verifyCredentialsWithApi(String username, String password) throws ParseException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(AUTH_API_URL);
            Map<String, String> credentials = new HashMap<>();
            credentials.put("username", username);
            credentials.put("password", password);

            StringEntity entity = new StringEntity(gson.toJson(credentials), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return Boolean.parseBoolean(jsonResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
