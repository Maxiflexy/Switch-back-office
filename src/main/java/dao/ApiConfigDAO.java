package dao;

import util.BaseBean;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.RegAppConstants.ITS_3PARTY_API_CONFIG;

public class ApiConfigDAO {

    private static final String INSERT_SQL =
            "INSERT INTO " + ITS_3PARTY_API_CONFIG + " (app_code, switch_code, tsq_url, trf_url, creation_date, created_by, approval, approve_by, last_modify_by, last_modify_date, disabled)" +
                    " VALUES (?, ?, ?, ?, sysdate, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SQL = "SELECT * FROM " + ITS_3PARTY_API_CONFIG +
        " WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM " + ITS_3PARTY_API_CONFIG;
    private static final String UPDATE_SQL =
            "UPDATE " + ITS_3PARTY_API_CONFIG +
                    " SET app_code = ?, switch_code = ?, tsq_url = ?, trf_url = ?," +
                    "last_modify_by = ?, last_modify_date = sysdate, disabled = ?" +
                    " WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM " +
            ITS_3PARTY_API_CONFIG + " WHERE id = ?";

    public boolean addApiConfig(BaseBean apiConfig)  {
        try (Connection connection = ConnectionUtil.getConnection();
            ) {

            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL);

            ps.setString(1, apiConfig.getString("app_code"));
            ps.setString(2, apiConfig.getString("switch_code"));
            ps.setString(3, apiConfig.getString("tsq_url"));
            ps.setString(4, apiConfig.getString("trf_url"));
            ps.setString(5, apiConfig.getString("created_by"));
            ps.setString(6, apiConfig.getString("approval"));
            ps.setString(7, null);
            ps.setString(8, null);
            ps.setString(9, null);
            ps.setString(10,  apiConfig.getString("disabled"));


            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                connection.commit();
                return true;
            }


        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }

    public BaseBean getApiConfigById(int id) throws SQLException {
        try (Connection connection = ConnectionUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_SQL)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BaseBean apiConfig = new BaseBean();
                    apiConfig.setString("id", String.valueOf(rs.getInt("id")));
                    apiConfig.setString("app_code", rs.getString("app_code"));
                    apiConfig.setString("switch_code", rs.getString("switch_code"));
                    apiConfig.setString("tsq_url", rs.getString("tsq_url"));
                    apiConfig.setString("trf_url", rs.getString("trf_url"));
                    apiConfig.setString("creation_date", rs.getDate("creation_date").toString());
                    apiConfig.setString("created_by", rs.getString("created_by"));
                    apiConfig.setString("approval", rs.getString("approval"));
                    apiConfig.setString("approve_by", rs.getString("approve_by"));
                    apiConfig.setString("last_modify_by", rs.getString("last_modify_by"));
                    apiConfig.setString("last_modify_date", rs.getString("last_modify_date"));
                    apiConfig.setString("disabled", rs.getString("disabled"));

                    return apiConfig;

                }
            }
        }
        return null;
    }

    public List<BaseBean> getAllApiConfigs() throws SQLException {
        List<BaseBean> apiConfigs = new ArrayList<>();
        try (Connection connection = ConnectionUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BaseBean apiConfig = new BaseBean();
                apiConfig.setString("id", String.valueOf(rs.getInt("id")));
                apiConfig.setString("app_code", rs.getString("app_code"));
                apiConfig.setString("switch_code", rs.getString("switch_code"));
                apiConfig.setString("tsq_url", rs.getString("tsq_url"));
                apiConfig.setString("trf_url", rs.getString("trf_url"));
                apiConfig.setString("creation_date", rs.getDate("creation_date").toString());
                apiConfig.setString("created_by", rs.getString("created_by"));
                apiConfig.setString("approval", rs.getString("approval"));
                apiConfig.setString("approve_by", rs.getString("approve_by"));
                apiConfig.setString("last_modify_by", rs.getString("last_modify_by"));
                apiConfig.setString("last_modify_date", rs.getString("last_modify_date"));
                apiConfig.setString("disabled", rs.getString("disabled"));

                apiConfigs.add(apiConfig);
            }
        }
        return apiConfigs;
    }

    public boolean updateApiConfig(BaseBean apiConfig) throws SQLException {
        try (Connection connection = ConnectionUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, apiConfig.getString("app_code"));
            ps.setString(2, apiConfig.getString("switch_code"));
            ps.setString(3, apiConfig.getString("tsq_url"));
            ps.setString(4, apiConfig.getString("trf_url"));
            ps.setString(5, apiConfig.getString("last_modify_by"));
            ps.setString(6, apiConfig.getString("disable"));
            ps.setInt(7, Integer.parseInt(apiConfig.getString("id")));

            if (ps.executeUpdate() > 0) {
                connection.commit();
                return true;
            }
        }
        return false;
    }

    public void deleteApiConfig(int id) throws SQLException {
        try (Connection connection = ConnectionUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            int updateCount = ps.executeUpdate();
            if (updateCount > 0) {
                connection.commit();
            }
        }
    }

    public List<BaseBean> getApiConfigByApproval(String request) throws SQLException {
        String query = "SELECT * FROM " + ITS_3PARTY_API_CONFIG + " WHERE approval = ?";
        List<BaseBean> records = new ArrayList<>();

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, request);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BaseBean apiConfig = new BaseBean();

                    apiConfig.setString("id", String.valueOf(rs.getInt("id")));
                    apiConfig.setString("app_code", rs.getString("app_code"));
                    apiConfig.setString("switch_code", rs.getString("switch_code"));
                    apiConfig.setString("tsq_url", rs.getString("tsq_url"));
                    apiConfig.setString("trf_url", rs.getString("trf_url"));
                    apiConfig.setString("creation_date", rs.getDate("creation_date").toString());
                    apiConfig.setString("created_by", rs.getString("created_by"));
                    apiConfig.setString("approval", rs.getString("approval"));
                    apiConfig.setString("approve_by", rs.getString("approve_by"));
                    apiConfig.setString("last_modify_by", rs.getString("last_modify_by"));
                    apiConfig.setString("last_modify_date", rs.getString("last_modify_date"));
                    apiConfig.setString("disabled", rs.getString("disabled"));

                    records.add(apiConfig);
                }
            }
        }

        return records;
    }
}
