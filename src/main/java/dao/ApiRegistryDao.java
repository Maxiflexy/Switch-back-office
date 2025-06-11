package dao;

import util.BaseBean;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.RegAppConstants.API_REGISTRY;

public class ApiRegistryDao {

    private static final String INSERT_SQL = "INSERT INTO " + API_REGISTRY +
            " (app_code, app_name, creation_date, created_by, approval, approve_by, last_modify_by," +
            " last_modify_date, disabled) " +
            "VALUES (?, ?, sysdate, ?, ?, ?, ?, ?, ?)";

    private static final String updateQuery = "UPDATE " + API_REGISTRY +
            " SET app_code = ?, app_name = ?, " +
            "last_modify_by = ?, last_modify_date = sysdate, disabled = ? WHERE id = ?";
    private static final String APPROVAL_SQL = "UPDATE " + API_REGISTRY +
            " SET approval = ?, approve_by = ?, disabled = ? WHERE id = ?";

    public List<BaseBean> getAllApiRegistries() throws SQLException {
        List<BaseBean> records = new ArrayList<>();
        String query = "SELECT * FROM " + API_REGISTRY;

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                BaseBean bean = new BaseBean();

                bean.setString("id", String.valueOf(rs.getInt("id")));
                bean.setString("app_code", rs.getString("app_code"));
                bean.setString("app_name", rs.getString("app_name"));
                bean.setString("creation_date", rs.getString("creation_date"));
                bean.setString("created_by", rs.getString("created_by"));
                bean.setString("approval", rs.getString("approval"));
                bean.setString("approve_by", rs.getString("approve_by"));
                bean.setString("last_modify_by", rs.getString("last_modify_by"));
                bean.setString("last_modify_date", rs.getString("last_modify_date"));
                bean.setString("disabled", rs.getString("disabled"));

                records.add(bean);
            }
        }

        return records;
    }

    public BaseBean getApiRegistryByID(int id) throws SQLException {
        String query = "SELECT * FROM " + API_REGISTRY + " WHERE id = ?";
        BaseBean bean = null;

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bean = new BaseBean();

                    bean.setString("id", String.valueOf(rs.getInt("id")));
                    bean.setString("app_code", rs.getString("app_code"));
                    bean.setString("app_name", rs.getString("app_name"));
                    bean.setString("creation_date", rs.getString("creation_date"));
                    bean.setString("created_by", rs.getString("created_by"));
                    bean.setString("approval", rs.getString("approval"));
                    bean.setString("approve_by", rs.getString("approve_by"));
                    bean.setString("last_modify_by", rs.getString("last_modify_by"));
                    bean.setString("last_modify_date", rs.getString("last_modify_date"));
                    bean.setString("disabled", rs.getString("disabled"));
                }
            }
        }

        return bean;
    }


    public List<BaseBean> getApiRegistryByApproval(String request) throws SQLException {
        String query = "SELECT * FROM " + API_REGISTRY + " WHERE approval = ?";
        List<BaseBean> records = new ArrayList<>();

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, request);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BaseBean bean = new BaseBean();

                    bean.setString("id", String.valueOf(rs.getInt("id")));
                    bean.setString("app_code", rs.getString("app_code"));
                    bean.setString("app_name", rs.getString("app_name"));
                    bean.setString("creation_date", rs.getString("creation_date"));
                    bean.setString("created_by", rs.getString("created_by"));
                    bean.setString("approval", rs.getString("approval"));
                    bean.setString("approve_by", rs.getString("approve_by"));
                    bean.setString("last_modify_by", rs.getString("last_modify_by"));
                    bean.setString("last_modify_date", rs.getString("last_modify_date"));
                    bean.setString("disabled", rs.getString("disabled"));

                    records.add(bean);
                }
            }
        }

        return records;
    }


    public boolean insertApiRegistry(BaseBean bean) {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

            ps.setString(1, bean.getString("app_code"));
            ps.setString(2, bean.getString("app_name"));
            ps.setString(3, bean.getString("created_by"));
            ps.setString(4, bean.getString("approval"));
            ps.setString(5, null);
            ps.setString(6, null);
            ps.setString(7, null);
            ps.setString(8, bean.getString("disabled"));

            int updateCount = ps.executeUpdate();
            if (updateCount > 0) {
                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean updateApiRegistry(BaseBean bean) throws SQLException {

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {

            ps.setString(1, bean.getString("app_code"));
            ps.setString(2, bean.getString("app_name"));
            ps.setString(3, bean.getString("last_modify_by"));
            ps.setString(4, bean.getString("disable"));
            ps.setInt(5, Integer.parseInt(bean.getString("id")));

            int rowCount = ps.executeUpdate();

            if (rowCount > 0) {
                conn.commit();
                return true;
            }
        }
        return false;
    }

    public void deleteApiRegistry(int id) throws SQLException {
        String query = "DELETE FROM " + API_REGISTRY + " WHERE id = ?";

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void approveOrReject(BaseBean baseBean) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(APPROVAL_SQL)) {

            ps.setString(1, baseBean.get("approval"));
            ps.setString(2, baseBean.get("approve_by"));
            ps.setString(3, baseBean.get("disabled"));
            ps.setInt(4, Integer.parseInt(baseBean.get("id")));

            int updateCount = ps.executeUpdate();
            if (updateCount > 0) {
                conn.commit();
            } else {
                conn.rollback();
            }
        }
    }




}


// ps.setString(1, bean.getString("app_code"));
//         ps.setString(2, bean.getString("app_name"));
//         ps.setString(3, bean.getString("created_by"));
//         ps.setString(4, bean.getString("approval"));
//         ps.setString(5, bean.getString("approve_by"));
//         ps.setString(6, bean.getString("last_modify_by"));
//         ps.setString(7, bean.getString("last_modify_date"));
//         ps.setString(8, bean.getString("disabled"));