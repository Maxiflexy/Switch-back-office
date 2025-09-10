package messaging.fileUtils.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ConnectionUtil;
import persistence.FetchRequest;
import persistence.FileUploadOps;
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

public class ContraAccountDbHelper implements FileUploadOps, FetchRequest {

    final static Logger LOG = LogManager.getLogger(ContraAccountDbHelper.class);

    @Override
    public boolean updateApprovedRecord(BaseBean requestBean, JsonObject request, Connection cnn) {

        String query = "INSERT INTO "
                .concat(CONTRA_ACCOUNT_TABLE)
                .concat(" (COUNTRY_CODE, SERVICE_ID, APPLCODE, USE_MSG_DR_ACC_FLG, USE_MSG_CR_ACC_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG,DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACCT_NUM,DR_ACCT_NUM, SERIAL_NUM, CR_ACC_PREFIX, CR_ACC_SUFFIX, TRXCRNCY, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, DEL_FLG, AUTH_BY, AUTH_DATE, AUTH_FLG, sno )")
                .concat(" SELECT COUNTRY_CODE, SERVICE_ID, APPLCODE, USE_MSG_DR_ACC_FLG, USE_MSG_CR_ACC_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG,DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACCT_NUM,DR_ACCT_NUM, esbuser.contra_acct_seq.nextval, CR_ACC_PREFIX, CR_ACC_SUFFIX, TRXCRNCY, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, 'N', ?, sysdate, 'Y', esbuser.contra_acct_seq.nextval FROM ")
                .concat(CONTRA_ACCOUNT_MC_TABLE)
                .concat(" c where c.document_id = ?");

        LOG.info("Updating contra account config: {}", query);
        PreparedStatement ps = null;
        boolean success = false;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("email"));
            ps.setString(++kk, request.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to CONTRA ACCOUNT table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to CONTRA ACCOUNT");
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
//        UPDATE ESBUSER.NIP_INST_LIST F SET F.DEL_FLG = 'Y', F.DELETION_DATE = sysdate, F.Deleted_By = (SELECT c.uploaded_by from esbuser.file_upload_mc c where c.document_id = 'a7e6a7c0-42ad-474e-b8ab-13556a3d98bb') where f.sno=4
        String query = "update "
                .concat(CONTRA_ACCOUNT_TABLE)
                .concat(" f SET f.del_flg = 'Y', f.del_date = sysdate, f.del_by = (SELECT c.uploaded_by from ")
                .concat(FILE_REQUEST_TABLE)
                .concat(" c where c.document_id = ?) where f.sno = (select sno from ")
                .concat(CONTRA_ACCOUNT_MC_TABLE)
                .concat(" d where d.document_id = ?)");
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
                    LOG.info("writing to CONTRA ACCOUNT table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to CONTRA ACCOUNT");
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
    public boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn) {

        String query = "INSERT INTO "
                .concat(CONTRA_ACCOUNT_MC_TABLE)
                .concat(" (serial_num, country_code, SERVICE_ID, USE_MSG_DR_ACC_FLG, USE_MSG_CR_ACC_FLG, APPLCODE, DR_ACC_DERIVATN_FLG,DR_ACCT_NUM, DR_ACC_PREFIX, DR_ACC_SUFFIX, TRXCRNCY, STATUS, DOCUMENT_ID, sno) ")
                .concat("VALUES (esbuser.contra_acct_seq.nextval, 'NGN', ?,?, ?, ?, ?, ?, ?,?,'NGN', ?, ?,?)");

        boolean success = false;

        LOG.info("Creating Contra Account request: {}", query);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, row.getString("service_id"));
            ps.setString(++kk, row.getString("use_msg_dr_acc_flg").equals("true") ? "Y" : "N");
            ps.setString(++kk, row.getString("use_msg_cr_acc_flg").equals("true") ? "Y" : "N");
            ps.setString(++kk, row.getString("APPLCODE"));
            ps.setString(++kk, row.getString("dr_acc_derivatn_flg").equals("true") ? "Y" : "N");
            ps.setString(++kk, row.getString("dr_acct_num"));
            ps.setString(++kk, row.getString("dr_acc_prefix"));
            ps.setString(++kk, row.getString("dr_acc_suffix"));
            ps.setString(++kk, row.getString("status"));
            ps.setString(++kk, requestBean.getString("document-id"));
            ps.setString(++kk, row.getString("sno"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to contra account table dets: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to contra account dets");
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
                .concat(CONTRA_ACCOUNT_MC_TABLE)
                .concat(" (COUNTRY_CODE, SERVICE_ID, APPLCODE, USE_MSG_DR_ACC_FLG, USE_MSG_CR_ACC_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG,DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACCT_NUM,DR_ACCT_NUM, SERIAL_NUM, CR_ACC_PREFIX, CR_ACC_SUFFIX, TRXCRNCY, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, sno, document_id )")
                .concat(" SELECT COUNTRY_CODE, SERVICE_ID, APPLCODE, USE_MSG_DR_ACC_FLG, USE_MSG_CR_ACC_FLG, CR_ACC_DERIVATN_FLG, DR_ACC_DERIVATN_FLG,DR_ACC_PREFIX, DR_ACC_SUFFIX, CR_ACCT_NUM,DR_ACCT_NUM, SERIAL_NUM, CR_ACC_PREFIX, CR_ACC_SUFFIX, TRXCRNCY, SOL_DERIV_SIDE_IND, SOL_DERIV_NUM_XTERS, SOL_DERIV_FLAG, sno, ? FROM ")
                .concat(CONTRA_ACCOUNT_TABLE)
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
                    requestBean.setString("statusCode", "400");
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
    public boolean getUnapprovedOrCanceledRequest(BaseBean requestBean, JsonObject request) {
        String query = "SELECT * FROM "
                .concat(CONTRA_ACCOUNT_MC_TABLE)
                .concat(" c WHERE c.document_id = ?");

        String approvedQuery = "SELECT * FROM "
                .concat(CONTRA_ACCOUNT_TABLE)
                .concat(" c WHERE c.DEL_FLG = 'N'");

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
                    feeBean.setString("use_msg_dr_acc_flg", rs.getString("use_msg_dr_acc_flg"));
                    feeBean.setString("use_msg_cr_acc_flg", rs.getString("use_msg_cr_acc_flg"));
                    feeBean.setString("cr_acc_derivatn_flg", rs.getString("cr_acc_derivatn_flg"));
                    feeBean.setString("dr_acc_derivatn_flg", rs.getString("dr_acc_derivatn_flg"));
                    feeBean.setString("dr_acc_prefix", rs.getString("dr_acc_prefix"));
                    feeBean.setString("dr_acc_suffix", rs.getString("dr_acc_suffix"));
                    feeBean.setString("cr_acc_prefix", rs.getString("cr_acc_prefix"));
                    feeBean.setString("cr_acc_suffix", rs.getString("cr_acc_suffix"));
                    feeBean.setString("cr_acct_num", rs.getString("cr_acct_num"));
                    feeBean.setString("dr_acct_num", rs.getString("dr_acct_num"));
                    feeBean.setString("trxcrncy", rs.getString("trxcrncy"));
                    feeBean.setString("applcode", rs.getString("applcode"));
                    feeBean.setString("sol_deriv_side_ind", rs.getString("sol_deriv_side_ind"));
                    feeBean.setString("sol_deriv_num_xters", rs.getString("sol_deriv_num_xters"));
                    feeBean.setString("sol_deriv_flag", rs.getString("sol_deriv_flag"));
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

