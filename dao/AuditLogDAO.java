package dao;

import org.json.JSONObject;
import util.BaseBean;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.RegAppConstants.AUDIT_LOG;

public class AuditLogDAO {
    private static final String INSERT_SQL =
            "INSERT INTO " + AUDIT_LOG + " (table_name, operation, operation_date, operated_by, old_value, new_value)" +
                    " VALUES (?, ?, sysdate, ?, ?, ?)";

    private static final String SELECT_SQL =
            "SELECT * FROM " + AUDIT_LOG + " WHERE audit_id = ?";

    public boolean insertIntoAuditLog(BaseBean auditLogBean)  {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            pstmt.setString(1, auditLogBean.get("table_name"));
            pstmt.setString(2, auditLogBean.get("operation"));
            pstmt.setString(3, auditLogBean.get("operated_by"));
            pstmt.setString(4, auditLogBean.get("old_value"));
            pstmt.setString(5, auditLogBean.get("new_value"));

            if (pstmt.executeUpdate() > 0) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    public BaseBean getAuditById(int id) throws SQLException {
//        BaseBean auditLogBean = null;
//        try (Connection conn = ConnectionUtil.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
//            pstmt.setInt(1, id);
//
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                auditLogBean = new BaseBean();
//
//                auditLogBean.put("audit_id", String.valueOf(rs.getInt("audit_id")));
//                auditLogBean.put("table_name", rs.getString("table_name"));
//                auditLogBean.put("operation", rs.getString("operation"));
//                auditLogBean.put("operated_by", rs.getString("operated_by"));
////                auditLogBean.put("old_value", rs.getString("old_value"));
////                auditLogBean.put("new_value", rs.getString("new_value"));
//
//
//                String oldValueJson = rs.getString("old_value");
//                String newValueJson = rs.getString("new_value");
//                auditLogBean.put("old_value", new JSONObject(oldValueJson).toString());
//                auditLogBean.put("new_value", new JSONObject(newValueJson).toString());
//            }
//        }
//        return auditLogBean;
//    }

    public BaseBean getAuditById(int id) throws SQLException {
        BaseBean auditLogBean = null;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                auditLogBean = new BaseBean();
                auditLogBean.put("audit_id", String.valueOf(rs.getInt("audit_id")));
                auditLogBean.put("table_name", rs.getString("table_name"));
                auditLogBean.put("operation", rs.getString("operation"));
                auditLogBean.put("operated_by", rs.getString("operated_by"));

                // Parse old_value and new_value as JSON, if valid
                String oldValueJson = rs.getString("old_value");
                String newValueJson = rs.getString("new_value");
                if (oldValueJson != null && oldValueJson.trim().startsWith("{")) {
                    auditLogBean.put("old_value", new JSONObject(oldValueJson).toString());
                } else {
                    auditLogBean.put("old_value", "{}");
                }
                if (newValueJson != null && newValueJson.trim().startsWith("{")) {
                    auditLogBean.put("new_value", new JSONObject(newValueJson).toString());
                } else {
                    auditLogBean.put("new_value", "{}");
                }
            }
        }
        return auditLogBean;
    }

//    public List<BaseBean> getAuditLogs() throws SQLException {
//        List<BaseBean> auditLogs = new ArrayList<>();
//
//        String query = "SELECT * FROM " + AUDIT_LOG;
//        try (Connection conn = ConnectionUtil.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(query);
//             ResultSet rs = pstmt.executeQuery()) {
//
//            while (rs.next()) {
//                BaseBean auditLogBean = new BaseBean();
//
//                auditLogBean.put("audit_id", String.valueOf(rs.getInt("audit_id")));
//                auditLogBean.put("table_name", rs.getString("table_name"));
//                auditLogBean.put("operation", rs.getString("operation"));
//                auditLogBean.put("operated_by", rs.getString("operated_by"));
//                auditLogBean.put("old_value", rs.getString("old_value"));
//                auditLogBean.put("new_value", rs.getString("new_value"));
//
//                auditLogs.add(auditLogBean);
//            }
//        }
//        return auditLogs;
//    }

    public List<BaseBean> getAuditLogs() throws SQLException {
        List<BaseBean> auditLogs = new ArrayList<>();

        String query = "SELECT * FROM " + AUDIT_LOG;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                BaseBean auditLogBean = new BaseBean();

                auditLogBean.put("audit_id", String.valueOf(rs.getInt("audit_id")));
                auditLogBean.put("table_name", rs.getString("table_name"));
                auditLogBean.put("operation", rs.getString("operation"));
                auditLogBean.put("operated_by", rs.getString("operated_by"));

                // Parse old_value and new_value as JSON, if valid
                String oldValueJson = rs.getString("old_value");
                String newValueJson = rs.getString("new_value");
                if (oldValueJson != null && oldValueJson.trim().startsWith("{")) {
                    auditLogBean.put("old_value", new JSONObject(oldValueJson).toString());
                } else {
                    auditLogBean.put("old_value", "{}");
                }
                if (newValueJson != null && newValueJson.trim().startsWith("{")) {
                    auditLogBean.put("new_value", new JSONObject(newValueJson).toString());
                } else {
                    auditLogBean.put("new_value", "{}");
                }

                auditLogs.add(auditLogBean);
            }
        }
        return auditLogs;
    }
}
