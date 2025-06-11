package dao;

import util.BaseBean;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CustomDAO {


    public void approveOrReject(BaseBean baseBean, String tableName) throws SQLException {
        String APPROVAL_SQL = "UPDATE " + tableName +
                " SET approval = ?, approve_by = ?, disabled = ? WHERE id = ?";

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

    public void disableRow(BaseBean baseBean, String tableName) throws SQLException {
        String sql = "UPDATE " + tableName +
                " SET disabled = ? WHERE id = ?";

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, baseBean.get("disabled"));
            ps.setInt(2, Integer.parseInt(baseBean.get("id")));

            int updateCount = ps.executeUpdate();
            if (updateCount > 0) {
                conn.commit();
            } else {
                conn.rollback();
            }
        }
    }

}
