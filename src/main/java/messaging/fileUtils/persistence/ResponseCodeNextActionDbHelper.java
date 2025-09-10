package messaging.fileUtils.persistence;

import messaging.fileUtils.constants.FileUploadModules;
import messaging.fileUtils.util.BaseBean;
import messaging.fileUtils.util.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.RESPONSE_CODE;
import static constants.AppConstants.DbTables.RESPONSE_CODE_MC;

public class ResponseCodeNextActionDbHelper implements FileUploadOps, FetchRequest {

    final static Logger LOG = LogManager.getLogger(ResponseCodeNextActionDbHelper.class);

    @Override
    public boolean getUnapprovedOrCanceledRequest(BaseBean requestBean, JsonObject request) {
        String approvedQuery = "SELECT * FROM "
                .concat(RESPONSE_CODE)
                .concat(" f WHERE f.DEL_FLG = 'N'");

        String query = "SELECT * FROM "
                .concat(RESPONSE_CODE_MC)
                .concat(" f WHERE f.document_id = ?");

        if (request.getString("action").equals("approved")) {
            query = approvedQuery;
        }

        if (request.containsKey("id")) {
            query = query.concat(" and sno = ?");
        }
        boolean success = false;
        LOG.info("Fetching response code list by ID {} {}", requestBean.getString("document-id"), query);
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
                    feeBean.setString("resp_code", rs.getString("resp_code"));
                    feeBean.setString("next_actn", rs.getString("next_actn"));
                    feeBean.setString("serviceid", rs.getString("serviceid"));
                    feeBean.setString("id", rs.getString("sno"));
                    if (!request.getString("action").equals("approved")) {
                        feeBean.setString("document_id", rs.getString("document_id"));
                    }
                    if (request.getString("action").equals("approved")) {
                        feeBean.setString("del_flg", rs.getString("del_flg"));
                        feeBean.setString("del_date", rs.getString("del_date"));
                        feeBean.setString("del_by", rs.getString("del_by"));
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

    @Override
    public boolean updateApprovedRecord(BaseBean requestBean, JsonObject request, Connection cnn) {
        String query = "INSERT INTO "
                .concat(RESPONSE_CODE)
                .concat(" (RESP_CODE, NEXT_ACTN, SERVICEID, sno) SELECT resp_code, next_actn, serviceid, esbuser.next_action_seq.nextval FROM ")
                .concat(RESPONSE_CODE_MC)
                .concat(" f WHERE f.document_id = ?");
        LOG.info("Updating response code and next action: {}", query);
        PreparedStatement ps = null;
        boolean success = false;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, request.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to response code  table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to response code");
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

        BaseBean deleteBean = new BaseBean();
        requestBean.setString("document_id", request.getString("document_id"));
        boolean status = false;
        try {
            if (fetchResponseCodeAndNextActionEntity(deleteBean, requestBean, cnn)) {
                status = deleteResponseCodeAndNextAction(deleteBean, requestBean, cnn);
                if (status) {
                    cnn.commit();
                } else {
                    cnn.rollback();
                }
            }

        } catch (Exception ex) {
            LOG.error("", ex);
            requestBean.setString("message", ex.getMessage());
        }
        return status;
    }

    private boolean deleteResponseCodeAndNextAction(BaseBean deleteBean, BaseBean requestBean, Connection cnn) {
        String deleteQuery = "DELETE FROM "
                .concat(RESPONSE_CODE)
                .concat(" f WHERE ")
                .concat(" f.sno = (select sno from ")
                .concat(RESPONSE_CODE_MC)
                .concat(" d where d.document_id = ?)");

        boolean success = false;

        LOG.info("Deleting Response code request: {}", deleteQuery);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(deleteQuery);
            ps.setString(++kk, requestBean.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = DBHelper.createDeleteArchive(deleteBean, cnn);
                    LOG.info("deleting from response code next action list: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to delete from response code next action  list dets");
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

    private boolean fetchResponseCodeAndNextActionEntity(BaseBean deleteBean, BaseBean requestBean, Connection cnn) {
        String fetchQuery = "SELECT * FROM "
                .concat(RESPONSE_CODE)
                .concat(" f WHERE ")
                .concat(" f.sno = (select sno from ")
                .concat(RESPONSE_CODE_MC)
                .concat(" d where d.document_id = ?)");
        boolean success = false;
        LOG.info("Fetching institution list by ID {} {}", requestBean.getString("document-id"), fetchQuery);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            ps = cnn.prepareStatement(fetchQuery);
            ps.setString(++kk, requestBean.getString("document_id"));

            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> institutions = new ArrayList<>();
                while (rs.next()) {
                    BaseBean feeBean = new BaseBean();
                    feeBean.setString("resp_code", rs.getString("resp_code"));
                    feeBean.setString("next_actn", rs.getString("next_actn"));
                    feeBean.setString("serviceid", rs.getString("serviceid"));
                    feeBean.setString("id", rs.getString("sno"));
                    feeBean.setString("del_flg", rs.getString("del_flg"));
                    feeBean.setString("del_date", rs.getString("del_date"));
                    feeBean.setString("del_by", rs.getString("del_by"));
                    institutions.add(feeBean);
                    success = true;
                    deleteBean.setString("module_name", FileUploadModules.RSP_CODE_NEXT_ACTN.name());
                    deleteBean.setString("del_id", rs.getString("sno"));
                    deleteBean.setString("del_by", requestBean.getString("email"));
                }
                deleteBean.setString("del_data", JsonUtil.convertBaseBeanListToJsonString(institutions));

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
        }
        return success;
    }

    @Override
    public boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn) {
        String query = "INSERT INTO "
                .concat(RESPONSE_CODE_MC)
                .concat(" ( resp_code, next_actn, serviceid, DOCUMENT_ID, sno) ")
                .concat("VALUES (?,?, ?, ?, ?)");

        boolean success = false;

        LOG.info("Creating RESPONSE CODE request: {}", query);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, row.getString("resp_code"));
            ps.setString(++kk, row.getString("next_actn"));
            ps.setString(++kk, row.getString("serviceid"));
            ps.setString(++kk, requestBean.getString("document-id"));
            ps.setString(++kk, row.getString("sno"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to response code list table dets: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to response code list dets");
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
    public boolean createDeleteRequest(BaseBean requestBean, Connection cnn) {
        String query = "select * from "
                .concat(RESPONSE_CODE)
                .concat(" F WHERE F.sno = ? and f.del_flg = 'N'");
        PreparedStatement ps = null;
        boolean success = false;

        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("id"));
            try {
                ResultSet rs = ps.executeQuery();
                BaseBean row = new BaseBean();
                if (rs.next()) {
                    row.setString("resp_code", rs.getString("resp_code"));
                    row.setString("next_actn", rs.getString("next_actn"));
                    row.setString("serviceid", rs.getString("serviceid"));
                    row.setString("sno", rs.getString("sno"));
                    success = createModuleRequest(requestBean, row, cnn);
                } else {
                    requestBean.setString("message", "Invalid ID");
                    requestBean.setString("statusCode", "400");
                }

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

        }
        return success;
    }
}
