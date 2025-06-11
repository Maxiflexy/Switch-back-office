package dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.ConnectionUtil;
import util.XMLUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.RegAppConstants.ITS_3RD_PARTY_ADDR;

public class ThirdPartyAddrDAO {
    static final public Logger LOG = LogManager.getLogger(ThirdPartyAddrDAO.class);

    private static final String INSERT_SQL =
            "INSERT INTO " + ITS_3RD_PARTY_ADDR +
            " (app_code, switch_code, ip_addr, creation_date, created_by, approval, approve_by, last_modify_by, last_modify_date, disabled)" +
            "VALUES (?, ?, ?, sysdate, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SQL =
            "SELECT * FROM " + ITS_3RD_PARTY_ADDR + " WHERE id = ?";
    private static final String UPDATE_SQL =
            "UPDATE " + ITS_3RD_PARTY_ADDR +
                    " SET app_code = ?, switch_code = ?, ip_addr = ?, last_modify_by = ?, last_modify_date = sysdate, disabled = ?" +
                    "WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM " + ITS_3RD_PARTY_ADDR + " WHERE id = ?";

    public boolean insertThirdPartyAddr(BaseBean thirdPartyAddr)  {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            LOG.info("QUERY: {}", INSERT_SQL);
            pstmt.setString(1, thirdPartyAddr.get("app_code"));
            pstmt.setString(2, thirdPartyAddr.get("switch_code"));
            pstmt.setString(3, thirdPartyAddr.get("ip_addr"));
            pstmt.setString(4, thirdPartyAddr.get("created_by"));
            pstmt.setString(5, thirdPartyAddr.getString("approval"));
            pstmt.setString(6, null);
            pstmt.setString(7, null);
            pstmt.setString(8, null);
            pstmt.setString(9,  thirdPartyAddr.getString("disabled"));
            int updateCount = pstmt.executeUpdate();
            if (updateCount > 0) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return updateCount > 0;
        } catch (Exception e) {
            LOG.error(e);
            e.printStackTrace();
            return false;
        }
    }

    public BaseBean getThirdPartyAddrById(int id) throws SQLException {
        BaseBean thirdPartyAddr = null;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                thirdPartyAddr = new BaseBean();

                thirdPartyAddr.setString("id", String.valueOf(rs.getInt("id")));
                thirdPartyAddr.put("app_code", rs.getString("app_code"));
                thirdPartyAddr.put("switch_code", rs.getString("switch_code"));
                thirdPartyAddr.put("ip_addr", rs.getString("ip_addr"));
                thirdPartyAddr.put("creation_date", rs.getDate("creation_date").toString());
                thirdPartyAddr.put("created_by", rs.getString("created_by"));
                thirdPartyAddr.put("approval", rs.getString("approval"));
                thirdPartyAddr.put("approve_by", rs.getString("approve_by"));
                thirdPartyAddr.put("last_modify_by", rs.getString("last_modify_by"));
                thirdPartyAddr.put("last_modify_date", rs.getString("last_modify_date"));
                thirdPartyAddr.put("disabled", rs.getString("disabled"));
            }
        }
        return thirdPartyAddr;
    }

    public boolean updateThirdPartyAddr(BaseBean thirdPartyAddr) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, thirdPartyAddr.get("app_code"));
            pstmt.setString(2, thirdPartyAddr.get("switch_code"));
            pstmt.setString(3, thirdPartyAddr.get("ip_addr"));
            pstmt.setString(4, thirdPartyAddr.getString("last_modify_by"));
            pstmt.setString(5, thirdPartyAddr.getString("disable"));
            pstmt.setInt(6, Integer.parseInt(thirdPartyAddr.get("id")));
            
            if(pstmt.executeUpdate() > 0) {
                conn.commit();
                return true;
            }
        }
        return false;
    }

    public void deleteThirdPartyAddr(int id) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setInt(1, id);

            pstmt.executeUpdate();
            conn.commit();
        }
    }

    public List<BaseBean> getAllThirdPartyAddrs() throws SQLException {
        List<BaseBean> thirdPartyAddrs = new ArrayList<>();
        String query = "SELECT * FROM " + ITS_3RD_PARTY_ADDR;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                BaseBean thirdPartyAddr = new BaseBean(); // switchRegistry

                thirdPartyAddr.setString("id", String.valueOf(rs.getInt("id")));
                thirdPartyAddr.put("app_code", rs.getString("app_code"));
                thirdPartyAddr.put("switch_code", rs.getString("switch_code"));
                thirdPartyAddr.put("ip_addr", rs.getString("ip_addr"));
                thirdPartyAddr.put("creation_date", rs.getDate("creation_date").toString());
                thirdPartyAddr.put("created_by", rs.getString("created_by"));
                thirdPartyAddr.setString("approval", rs.getString("approval"));
                thirdPartyAddr.setString("approve_by", rs.getString("approve_by"));
                thirdPartyAddr.setString("last_modify_by", rs.getString("last_modify_by"));
                thirdPartyAddr.setString("last_modify_date", rs.getString("last_modify_date"));
                thirdPartyAddr.setString("disabled", rs.getString("disabled"));
                
                thirdPartyAddrs.add(thirdPartyAddr);
            }
        }
        return thirdPartyAddrs;
    }

    public List<BaseBean> getThirdPartyAddrByApproval(String request) throws SQLException {
        String query = "SELECT * FROM " + ITS_3RD_PARTY_ADDR + " WHERE approval = ?";
        List<BaseBean> records = new ArrayList<>();

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, request);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BaseBean bean = new BaseBean();

                    bean.setString("id", String.valueOf(rs.getInt("id")));
                    bean.setString("app_code", rs.getString("app_code"));
                    bean.setString("switch_code", rs.getString("switch_code"));
                    bean.setString("ip_addr", rs.getString("ip_addr"));
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
