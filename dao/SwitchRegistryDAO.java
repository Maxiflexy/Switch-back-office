package dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.RegAppConstants.ITS_SWITCH_REGISTRY;

public class SwitchRegistryDAO {

    final static Logger LOG = LogManager.getLogger(SwitchRegistryDAO.class);

    private static final String INSERT_SQL =
            "INSERT INTO " + ITS_SWITCH_REGISTRY +
                    " (switch_code, switch_name, creation_date, created_by, approval, approve_by, last_modify_by, last_modify_date, disabled)" +
                    "VALUES (?, ?, sysdate, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SQL =
            "SELECT * FROM " + ITS_SWITCH_REGISTRY + " WHERE id = ?";
    private static final String UPDATE_SQL =
            "UPDATE " + ITS_SWITCH_REGISTRY +
                    " SET switch_code = ?, switch_name = ?, last_modify_by = ?, last_modify_date = sysdate, disabled = ?" +
                    " WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM " + ITS_SWITCH_REGISTRY + " WHERE id = ?";

    public boolean insertSwitchRegistry(BaseBean switchRegistry) throws SQLException {

        PreparedStatement pstmt = null;
        try (Connection conn = ConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);

            pstmt = conn.prepareStatement(INSERT_SQL);

            pstmt.setString(1, switchRegistry.get("switch_code"));
            pstmt.setString(2, switchRegistry.get("switch_name"));
            pstmt.setString(3, switchRegistry.get("created_by"));
            pstmt.setString(4, switchRegistry.getString("approval"));
            pstmt.setString(5, null);
            pstmt.setString(6, null);
            pstmt.setString(7, null);
            pstmt.setString(8,  switchRegistry.getString("disabled"));

            int rowAffected = pstmt.executeUpdate();

            if (rowAffected > 0) {
                LOG.info("writing to ITS_SWITCH_REGISTRY table ");
                conn.commit();
                return true;
            } else {
                //check if app has been verified
                LOG.info("unable to write to ITS_SWITCH_REGISTRY");
                conn.rollback();
                LOG.info("done with rollback");
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("", e);
        }  finally {

            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    LOG.error("", e);
                }
                pstmt = null;
            }
        }
        return false;
    }

    public BaseBean getSwitchRegistryById(int id) throws SQLException {
        BaseBean switchRegistry = null;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                switchRegistry = new BaseBean();

                switchRegistry.setString("id", String.valueOf(rs.getInt("id")));
                switchRegistry.put("switch_code", rs.getString("switch_code"));
                switchRegistry.put("switch_name", rs.getString("switch_name"));
                switchRegistry.put("creation_date", rs.getDate("creation_date").toString());
                switchRegistry.put("created_by", rs.getString("created_by"));
                switchRegistry.setString("approval", rs.getString("approval"));
                switchRegistry.setString("approve_by", rs.getString("approve_by"));
                switchRegistry.setString("last_modify_by", rs.getString("last_modify_by"));
                switchRegistry.setString("last_modify_date", rs.getString("last_modify_date"));
                switchRegistry.setString("disabled", rs.getString("disabled"));
            }
        }
        return switchRegistry;
    }

    public boolean updateSwitchRegistry(BaseBean switchRegistry) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, switchRegistry.get("switch_code"));
            pstmt.setString(2, switchRegistry.get("switch_name"));
            pstmt.setString(3, switchRegistry.getString("last_modify_by"));
            pstmt.setString(4, switchRegistry.getString("disable"));
            pstmt.setInt(5, Integer.parseInt(switchRegistry.get("id")));

            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public void deleteSwitchRegistry(int id) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<BaseBean> getAllSwitchRegistries() throws SQLException {
        List<BaseBean> switchRegistries = new ArrayList<>();
        String query = "SELECT * FROM " + ITS_SWITCH_REGISTRY;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                BaseBean switchRegistry = new BaseBean();

                switchRegistry.setString("id", String.valueOf(rs.getInt("id")));
                switchRegistry.put("switch_code", rs.getString("switch_code"));
                switchRegistry.put("switch_name", rs.getString("switch_name"));
                switchRegistry.put("creation_date", rs.getDate("creation_date").toString());
                switchRegistry.put("created_by", rs.getString("created_by"));
                switchRegistry.setString("approval", rs.getString("approval"));
                switchRegistry.setString("approve_by", rs.getString("approve_by"));
                switchRegistry.setString("last_modify_by", rs.getString("last_modify_by"));
                switchRegistry.setString("last_modify_date", rs.getString("last_modify_date"));
                switchRegistry.setString("disabled", rs.getString("disabled"));

                switchRegistries.add(switchRegistry);
            }
        }
        return switchRegistries;
    }



    public List<BaseBean> getSwitchByApproval(String request) throws SQLException {
        String query = "SELECT * FROM " + ITS_SWITCH_REGISTRY + " WHERE approval = ?";
        List<BaseBean> records = new ArrayList<>();

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, request);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BaseBean bean = new BaseBean();

                    bean.setString("id", String.valueOf(rs.getInt("id")));
                    bean.setString("switch_code", rs.getString("switch_code"));
                    bean.setString("switch_name", rs.getString("switch_name"));
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
}

