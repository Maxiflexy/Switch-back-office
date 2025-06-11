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

import static constants.RegAppConstants.ITS_CRYPTO_CONFIG;

public class CryptoConfigDAO {

    final static Logger LOG = LogManager.getLogger(CryptoConfigDAO.class);

    private static final String INSERT_SQL =
            "INSERT INTO " + ITS_CRYPTO_CONFIG +
                    " (app_code, switch_code, its_private_keyfile, its_public_keyfile, switch_public_keyfile, creation_date, created_by, approval, approve_by, last_modify_by, last_modify_date, disabled) " +
                    "VALUES (?, ?, ?, ?, ?, sysdate, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_SQL =
            "SELECT * FROM " + ITS_CRYPTO_CONFIG + " WHERE app_code = ?";

    private static final String SELECT_BY_ID =
            "SELECT * FROM " + ITS_CRYPTO_CONFIG + " WHERE id = ?";
    private static final String UPDATE_SQL =
            "UPDATE " + ITS_CRYPTO_CONFIG +
                    " SET app_code = ?, switch_code = ?, its_private_keyfile = ?, its_public_keyfile = ?, " +
                    "switch_public_keyfile = ?, creation_date = sysdate, last_modify_by = ?," +
                    " last_modify_date = sysdate, disabled = ?" +
                    "WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM " + ITS_CRYPTO_CONFIG + " WHERE id = ?";

    public boolean insertCryptoConfig(BaseBean cryptoConfig) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, cryptoConfig.get("app_code"));
            pstmt.setString(2, cryptoConfig.get("switch_code"));
            pstmt.setString(3, cryptoConfig.get("its_private_keyfile"));
            pstmt.setString(4, cryptoConfig.get("its_public_keyfile"));
            pstmt.setString(5, cryptoConfig.getString("switch_public_keyfile"));
            pstmt.setString(6, cryptoConfig.get("created_by"));
            pstmt.setString(7, cryptoConfig.getString("approval"));
            pstmt.setString(8, null);
            pstmt.setString(9, null);
            pstmt.setString(10, null);
            pstmt.setString(11,  cryptoConfig.getString("disabled"));

            int rowAffected = pstmt.executeUpdate();

            if (rowAffected > 0) {
                LOG.info("writing to ITS_SWITCH_REGISTRY table ");
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
        }
        return false;
    }

    public BaseBean getCryptoConfig(String appCode) throws SQLException {
        BaseBean cryptoConfig = null;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
            pstmt.setString(1, appCode);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                cryptoConfig = new BaseBean();

                cryptoConfig.put("id", String.valueOf(rs.getInt("id")));
                cryptoConfig.put("app_code", rs.getString("app_code"));
                cryptoConfig.put("switch_code", rs.getString("switch_code"));
                cryptoConfig.put("its_private_keyfile", rs.getString("its_private_keyfile"));
                cryptoConfig.put("its_public_keyfile", rs.getString("its_public_keyfile"));
                cryptoConfig.put("switch_public_keyfile", rs.getString("switch_public_keyfile"));
                cryptoConfig.put("creation_date", rs.getDate("creation_date").toString());
                cryptoConfig.put("created_by", rs.getString("created_by"));
            }
        }
        return cryptoConfig;
    }

    public BaseBean getCryptoConfigById(int id) throws SQLException {
        BaseBean cryptoConfig = null;

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID)) {
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                cryptoConfig = new BaseBean();

                cryptoConfig.put("id", String.valueOf(rs.getInt("id")));
                cryptoConfig.put("app_code", rs.getString("app_code"));
                cryptoConfig.put("switch_code", rs.getString("switch_code"));
                cryptoConfig.put("its_private_keyfile", rs.getString("its_private_keyfile"));
                cryptoConfig.put("its_public_keyfile", rs.getString("its_public_keyfile"));
                cryptoConfig.put("switch_public_keyfile", rs.getString("switch_public_keyfile"));
                cryptoConfig.put("creation_date", rs.getDate("creation_date").toString());
                cryptoConfig.put("created_by", rs.getString("created_by"));
                cryptoConfig.put("approval", rs.getString("approval"));
                cryptoConfig.setString("approve_by", rs.getString("approve_by"));
                cryptoConfig.setString("last_modify_by", rs.getString("last_modify_by"));
                cryptoConfig.setString("last_modify_date", rs.getString("last_modify_date"));
                cryptoConfig.setString("disabled", rs.getString("disabled"));
            }
        }
        return cryptoConfig;
    }

    public boolean updateCryptoConfig(BaseBean cryptoConfig) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, cryptoConfig.get("app_code"));
            pstmt.setString(2, cryptoConfig.get("switch_code"));
            pstmt.setString(3, cryptoConfig.get("its_private_keyfile"));
            pstmt.setString(4, cryptoConfig.get("its_public_keyfile"));
            pstmt.setString(5, cryptoConfig.get("switch_public_keyfile"));
            pstmt.setString(6, cryptoConfig.getString("last_modify_by"));
            pstmt.setString(7, cryptoConfig.getString("disable"));
            pstmt.setInt(8, Integer.parseInt(cryptoConfig.getString("id")));


            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public void deleteCryptoConfig(int id) throws SQLException {
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<BaseBean> getAllCryptoConfigs() throws SQLException {
        List<BaseBean> cryptoConfigs = new ArrayList<>();
        String query = "SELECT * FROM " + ITS_CRYPTO_CONFIG;
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                BaseBean cryptoConfig = new BaseBean();

                cryptoConfig.put("id", String.valueOf(rs.getInt("id")));
                cryptoConfig.put("app_code", rs.getString("app_code"));
                cryptoConfig.put("switch_code", rs.getString("switch_code"));
                cryptoConfig.put("its_private_keyfile", rs.getString("its_private_keyfile"));
                cryptoConfig.put("its_public_keyfile", rs.getString("its_public_keyfile"));
                cryptoConfig.put("switch_public_keyfile", rs.getString("switch_public_keyfile"));
                cryptoConfig.put("creation_date", rs.getDate("creation_date").toString());
                cryptoConfig.put("created_by", rs.getString("created_by"));
                cryptoConfig.put("approval", rs.getString("approval"));
                cryptoConfig.put("approve_by", rs.getString("approve_by"));
                cryptoConfig.put("last_modify_by", rs.getString("last_modify_by"));
                cryptoConfig.put("last_modify_date", rs.getString("last_modify_date"));
                cryptoConfig.put("disabled", rs.getString("disabled"));

                cryptoConfigs.add(cryptoConfig);
            }
        }
        return cryptoConfigs;
    }
    public List<BaseBean> getCryptoConfigByApproval(String request) throws SQLException {
        String query = "SELECT * FROM " + ITS_CRYPTO_CONFIG + " WHERE approval = ?";
        List<BaseBean> records = new ArrayList<>();

        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, request);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BaseBean bean = new BaseBean();

                    bean.setString("id", String.valueOf(rs.getInt("id")));
                    bean.put("app_code", rs.getString("app_code"));
                    bean.put("switch_code", rs.getString("switch_code"));
                    bean.put("its_private_keyfile", rs.getString("its_private_keyfile"));
                    bean.put("its_public_keyfile", rs.getString("its_public_keyfile"));
                    bean.put("switch_public_keyfile", rs.getString("switch_public_keyfile"));
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

