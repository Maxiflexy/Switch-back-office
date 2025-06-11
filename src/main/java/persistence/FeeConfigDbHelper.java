package persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;

import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.*;

public class FeeConfigDbHelper implements FileUploadOps {
    final static Logger LOG = LogManager.getLogger(FeeConfigDbHelper.class);
    @Override
    public boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn) {

//        INSERT INTO ESBUSER.FEES_CONFIG_MC (country_code, service_id, fee_id, fee_crncy, fee_amt, use_dr_acc_flg, cr_acc_derivatn_flg, cr_acc_prefix,
//                cr_acc_suffix, cr_acct_num, client_id, feature_id, applcode, fix_amt, add_to_amt, rsvd_fld_1, DOCUMENT_ID) VALUES ('NGN', '123', esbuser.fee_config_seq. nextval, 'NGN', 120, 'Y', 'N','','','123456',54, esbuser.fee_config_seq.currval, 'channel_id','Y','N','', '1223456');

        String query = "INSERT INTO "
                .concat(FEE_CONFIG_MC)
                .concat("(country_code, service_id, fee_id, fee_crncy, fee_amt, use_dr_acc_flg, cr_acc_derivatn_flg, cr_acc_prefix, cr_acc_suffix, cr_acct_num, client_id, feature_id, applcode, fix_amt, add_to_amt, DOCUMENT_ID, status, sno) ")
                .concat("  VALUES ('NGN', ?, esbuser.fee_config_seq.nextval, 'NGN', ?, 'Y', ?,?,?,?,esbuser.fee_config_seq.currval, esbuser.fee_config_seq.currval,?,?,?, ?, ?, ?)");
        boolean success = false;

        LOG.info("Creating Fee request: {}", query);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, row.getString("service_id"));
            ps.setString(++kk, row.getString("fee_amt"));
            ps.setString(++kk, row.getString("cr_acc_derivatn_flg").equals("true") ? "Y" : "N");
            ps.setString(++kk, row.getString("cr_acct_prefix"));
            ps.setString(++kk, row.getString("cr_acct_suffix"));
            ps.setString(++kk, row.getString("cr_acct_num"));
//            ps.setString(++kk, row.getString("channel_id")); CLIENT_ID
            ps.setString(++kk, row.getString("channel_id"));
            ps.setString(++kk, row.getString("fix_amt").equals("true") ? "Y" : "N");
            ps.setString(++kk, row.getString("add_to_amt").equals("true") ? "Y" : "N");
//            ps.setString(++kk, row.getString("add_to_amt"));
            ps.setString(++kk, requestBean.getString("document-id"));
            ps.setString(++kk, row.getString("status"));
            ps.setString(++kk, row.getString("sno"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to fee_config table dets: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to fee_config dets");
                    cnn.rollback();
                    LOG.info("done with rollback");

                }

            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

            }


        } catch (SQLException e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("", e);

        }
        return success;
    }
    @Override
    public boolean createDeleteRequest(BaseBean requestBean, Connection cnn) {

        String query = "INSERT INTO "
                .concat(FEE_CONFIG_MC)
                .concat("(SERIAL_NUM, COUNTRY_CODE, SERVICE_ID, FEE_ID, FEE_CRNCY, USE_DR_ACC_FLG, USE_CR_ACC_FLG, OWR_FEEAMT_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG, DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACC_PREFIX, CR_ACC_SUFFIX, CR_ACCT_NUM, DR_ACCT_NUM, CLIENT_ID, FEATURE_ID, FEE_ACCT_SOL, APPLCODE, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, FIX_AMT, ADD_TO_AMT, RSVD_FLD_1, STATUS, sno, document_id)")
                .concat(" SELECT SERIAL_NUM, COUNTRY_CODE, SERVICE_ID, FEE_ID, FEE_CRNCY, USE_DR_ACC_FLG, USE_CR_ACC_FLG, OWR_FEEAMT_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG, DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACC_PREFIX, CR_ACC_SUFFIX, CR_ACCT_NUM, DR_ACCT_NUM, CLIENT_ID, FEATURE_ID, FEE_ACCT_SOL, APPLCODE, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, FIX_AMT, ADD_TO_AMT, RSVD_FLD_1, STATUS, sno, ? FROM ")
                .concat(FEE_CONFIG)
                .concat(" c where c.sno = ? and c.del_flg = 'N'");

        PreparedStatement ps = null;
        boolean success = false;

        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("document-id"));
            ps.setString(++kk, requestBean.getString("id"));
            try {
                if (ps.executeUpdate() > 0) {
                    success = true;
                } else {
                    requestBean.setString("message", "Invalid ID");
                }
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
                e.printStackTrace();
            }

        } catch (SQLException e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("", e);

        } finally {

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("", e);
                }
                ps = null;
            }

        }
        return success;
    }
    @Override
    public boolean updateApprovedRecord(BaseBean requestBean, JsonObject request, Connection cnn) {

        String sql = "INSERT INTO "
                .concat(FEE_CONFIG)
                .concat("(SERIAL_NUM, COUNTRY_CODE, SERVICE_ID, FEE_ID, FEE_CRNCY, USE_DR_ACC_FLG, USE_CR_ACC_FLG, OWR_FEEAMT_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG, DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACC_PREFIX, CR_ACC_SUFFIX, CR_ACCT_NUM, DR_ACCT_NUM, CLIENT_ID, FEATURE_ID, FEE_ACCT_SOL, APPLCODE, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, FIX_AMT, ADD_TO_AMT, RSVD_FLD_1, STATUS, AUTH_FLG, AUTH_DATE, AUTH_BY, sno, DEL_FLG) SELECT esbuser.fee_config_seq.currval, COUNTRY_CODE, SERVICE_ID, FEE_ID, FEE_CRNCY, USE_DR_ACC_FLG, USE_CR_ACC_FLG, OWR_FEEAMT_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG, DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACC_PREFIX, CR_ACC_SUFFIX, CR_ACCT_NUM, DR_ACCT_NUM, CLIENT_ID, FEATURE_ID, FEE_ACCT_SOL, APPLCODE, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, FIX_AMT, ADD_TO_AMT, RSVD_FLD_1, STATUS, 'Y', sysdate, ?, esbuser.fee_config_seq.nextval, 'N' from ")
                .concat(FEE_CONFIG_MC)
                .concat(" c where c.document_id = ?");

        LOG.info("Updating fee config: {}", sql);
        PreparedStatement ps = null;
        boolean success = false;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(sql);
            ps.setString(++kk, requestBean.getString("email"));
            ps.setString(++kk, request.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to fee_config table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to fee_config");
                    cnn.rollback();
                    LOG.info("done with rollback");

                }

            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

            }


        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("", e);

        }
        return success;
    }
    @Override
    public boolean approveDeleteRecord(BaseBean requestBean, JsonObject request, Connection cnn) {
        String query = "update "
                .concat(FEE_CONFIG)
                .concat(" f SET f.del_flg = 'Y', f.del_date = sysdate, f.del_by = (SELECT c.uploaded_by from ")
                .concat(FILE_REQUEST_TABLE)
                .concat(" c where c.document_id = ?) where f.sno = (select sno from ")
                .concat(FEE_CONFIG_MC)
                .concat(" d where d.document_id = ?)");
        LOG.info("Updating fee config: {}", query);
        PreparedStatement ps = null;
        boolean success = false;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, request.getString("document_id"));
            ps.setString(++kk, request.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to fee_config table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to fee_config");
                    cnn.rollback();
                    LOG.info("done with rollback");

                }

            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

            }


        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("", e);

        }
        return success;
    }
    @Override
    public boolean getUnapprovedOrCanceledRequest(BaseBean requestBean, JsonObject request) {

        String approvedQuery = "SELECT * FROM "
                .concat(FEE_CONFIG)
                .concat(" c WHERE c.del_flg = 'N'");

        String query = "SELECT * FROM "
                .concat(FEE_CONFIG_MC)
                .concat(" c WHERE c.document_id = ?");

        if (request.getString("action").equals("approved")) {
            query = approvedQuery;
        }

        if (request.containsKey("id")) {
            query = query.concat(" and sno = ?");
        }


        boolean success = false;
        LOG.info("Fetching fee config by ID {} {}", requestBean.getString("document-id"), query);
        PreparedStatement ps = null;
        Connection cnn = ConnectionUtil.getConnection();

        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            if (request.containsKey("document_id")) {
                ps.setString(++kk, request.getString("document_id"));
            }
            if (request.containsKey("id")) {
                ps.setString(++kk, request.getString("id"));
            }
            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> fees = new ArrayList<>();
                while (rs.next()) {
                    BaseBean feeBean = new BaseBean();
                    feeBean.setString("serial_num", rs.getString("serial_num"));
                    feeBean.setString("country_code", rs.getString("country_code"));
                    feeBean.setString("service_id", rs.getString("service_id"));
                    feeBean.setString("fee_id", rs.getString("fee_id"));
                    feeBean.setString("fee_crncy", rs.getString("fee_crncy"));
                    feeBean.setString("fee_amt", rs.getString("fee_amt"));
                    feeBean.setString("use_dr_acc_flg", rs.getString("use_dr_acc_flg"));
                    feeBean.setString("use_cr_acc_flg", rs.getString("use_cr_acc_flg"));
                    feeBean.setString("owr_feeamt_flg", rs.getString("owr_feeamt_flg"));
                    feeBean.setString("cr_acc_derivatn_flg", rs.getString("cr_acc_derivatn_flg"));
                    feeBean.setString("dr_acc_derivatn_flg", rs.getString("dr_acc_derivatn_flg"));
                    feeBean.setString("dr_acc_prefix", rs.getString("dr_acc_prefix"));
                    feeBean.setString("dr_acc_suffix", rs.getString("dr_acc_suffix"));
                    feeBean.setString("cr_acc_prefix", rs.getString("cr_acc_prefix"));
                    feeBean.setString("cr_acc_suffix", rs.getString("cr_acc_suffix"));
                    feeBean.setString("cr_acct_num", rs.getString("cr_acct_num"));
                    feeBean.setString("dr_acct_num", rs.getString("dr_acct_num"));
                    feeBean.setString("client_id", rs.getString("client_id"));
                    feeBean.setString("feature_id", rs.getString("feature_id"));
                    feeBean.setString("fee_acct_sol", rs.getString("fee_acct_sol"));
                    feeBean.setString("applcode", rs.getString("applcode"));
                    feeBean.setString("sol_deriv_side_ind", rs.getString("sol_deriv_side_ind"));
                    feeBean.setString("sol_deriv_num_xters", rs.getString("sol_deriv_num_xters"));
                    feeBean.setString("sol_deriv_flag", rs.getString("sol_deriv_flag"));
                    feeBean.setString("fix_amt", rs.getString("fix_amt"));
                    feeBean.setString("add_to_amt", rs.getString("add_to_amt"));
                    feeBean.setString("rsvd_fld_1", rs.getString("rsvd_fld_1"));
                    feeBean.setString("id", rs.getString("sno"));
                    if (!request.getString("action").equals("approved")) {
                        feeBean.setString("document_id", rs.getString("document_id"));
                    }
                    if (request.getString("action").equals("approved")) {
                        feeBean.setString("del_flg", rs.getString("del_flg"));
                        feeBean.setString("del_date", rs.getString("del_date"));
                        feeBean.setString("auth_by", rs.getString("auth_by"));
                        feeBean.setString("auth_date", rs.getString("auth_date"));
                        feeBean.setString("auth_flg", rs.getString("auth_flg"));
                        feeBean.setString("status", rs.getString("status"));
                    }
                    feeBean.setString("rsvd_fld_1", rs.getString("rsvd_fld_1"));
                    fees.add(feeBean);
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(fees));
                success = true;
            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
                e.printStackTrace();

            }


        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("", e);

        } finally {

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("", e);
                }
                ps = null;
            }
            ConnectionUtil.closeConnection(cnn);
        }
        return success;
    }

}
