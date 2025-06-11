package dao;

import model.Role;
import model.User;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.RegAppConstants.REGISTRY_USER;


public class UserDAO {
    public void saveUser(User user) {
        String sql = "INSERT INTO " + REGISTRY_USER + " (username, role) VALUES (?, ?)";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql, new String[]{"id"})) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getRole().toString());

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUserRoleByUsername(String username, Role newRole) {
        String sql = "UPDATE " + REGISTRY_USER +" SET role = ? WHERE username = ?";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, newRole.name());
            preparedStatement.setString(2, username);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to check if a username exists
    public boolean usernameExist(String username) {
        String sql = "SELECT COUNT(*) FROM " + REGISTRY_USER + " WHERE username = ?";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Method to get a user by ID
    public User getUserByUsername(String usernameTofFind) {
        String sql = "SELECT * FROM " + REGISTRY_USER + " WHERE username = ?";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usernameTofFind);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String username = rs.getString("username");
                    Role role = Role.valueOf(rs.getString("role"));

                    return new User(id, username, role);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserById(long id) {
        String query = "SELECT id, username, role FROM " + REGISTRY_USER + " WHERE id = ?";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    Role role = Role.valueOf(rs.getString("role"));
                    return new User(id, username, role);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT id, username, role FROM " + REGISTRY_USER;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String username = rs.getString("username");
                Role role = Role.valueOf(rs.getString("role"));
                users.add(new User(id, username, role));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
