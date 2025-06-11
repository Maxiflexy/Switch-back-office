package persistence;

import constants.FileUploadModules;
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

public class InstitutionRouteDbHelper implements FileUploadOps, FetchRequest {
    final static Logger LOG = LogManager.getLogger(InstitutionRouteDbHelper.class);

    @Override
    public boolean getUnapprovedOrCanceledRequest(BaseBean requestBean, JsonObject request) {
        String approvedQuery = "SELECT * FROM "
                .concat(INST_ROUTE_TABLE)
                .concat(" f WHERE f.DEL_FLG = 'N'");
        String query = "SELECT * FROM "
                .concat(INST_ROUTE_TABLE_MC)
                .concat(" c WHERE c.document_id = ?");

        if (request.getString("action").equals("approved")) {
            query = approvedQuery;
        }

        if (request.containsKey("id")) {
            query = query.concat(" and sno = ?");
        }
        boolean success = false;
        LOG.info("Fetching financial institution routes list by ID {} {}", requestBean.getString("document-id"), query);
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
                    feeBean.setString("finst_id", rs.getString("finst_id"));
                    feeBean.setString("fsp_id", rs.getString("fsp_id"));
                    feeBean.setString("channel_id", rs.getString("channel_id"));
                    feeBean.setString("id", rs.getString("sno"));
                    if (!request.getString("action").equals("approved")) {
                        feeBean.setString("document_id", rs.getString("document_id"));
                    }
                    if (request.getString("action").equals("approved")) {
                        feeBean.setString("del_flg", rs.getString("del_flg"));
                        feeBean.setString("del_date", rs.getString("del_date"));
                        feeBean.setString("deleted_by", rs.getString("del_by"));
                        feeBean.setString("entrydate", rs.getString("creation_date"));
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
                .concat(INST_ROUTE_TABLE)
                .concat(" (finst_id, fsp_id, channel_id, creation_date, created_by, DEL_FLG, sno)")
                .concat(" SELECT finst_id, fsp_id, channel_id, sysdate, f.uploaded_by, 'N', esbuser.seq_finst_map.nextval FROM ")
                .concat(INST_ROUTE_TABLE_MC)
                .concat("  c inner join ")
                .concat(FILE_REQUEST_TABLE)
                .concat(" f ON f.document_id = c.document_id WHERE c.document_id = ?");

        LOG.info("Updating Financial institution config: {}", query);
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
                    LOG.info("writing to financial institution table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to financial institution");
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
            if (fetchInstitutionRoute(deleteBean, requestBean, cnn)) {
                status = deleteInstitutionRoute(deleteBean, requestBean, cnn);
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

    private boolean deleteInstitutionRoute(BaseBean deleteBean, BaseBean requestBean, Connection cnn) {
        String deleteQuery = "DELETE FROM "
                .concat(INST_ROUTE_TABLE)
                .concat(" f WHERE ")
                .concat(" f.sno = (select sno from ")
                .concat(INST_ROUTE_TABLE_MC)
                .concat(" d where d.document_id = ?)");

        boolean success = false;

        LOG.info("Deleting Institution list request: {}", deleteQuery);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(deleteQuery);
            ps.setString(++kk, requestBean.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = DBHelper.createDeleteArchive(deleteBean, cnn);
                    LOG.info("deleting from institution list: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to delete from institution list code list dets");
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

    private boolean fetchInstitutionRoute(BaseBean deleteBean, BaseBean requestBean, Connection cnn) {
        String fetchQuery = "SELECT * FROM "
                .concat(INST_ROUTE_TABLE)
                .concat(" f WHERE ")
                .concat(" f.sno = (select sno from ")
                .concat(INST_ROUTE_TABLE_MC)
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
                    BaseBean institutionRouteBean = new BaseBean();
                    institutionRouteBean.setString("finst_id", rs.getString("finst_id"));
                    institutionRouteBean.setString("fsp_id", rs.getString("fsp_id"));
                    institutionRouteBean.setString("channel_id", rs.getString("channel_id"));
                    institutionRouteBean.setString("id", rs.getString("sno"));
                    institutionRouteBean.setString("del_flg", rs.getString("del_flg"));
                    institutionRouteBean.setString("del_date", rs.getString("del_date"));
                    institutionRouteBean.setString("deleted_by", rs.getString("del_by"));
                    institutionRouteBean.setString("entrydate", rs.getString("creation_date"));
                    institutions.add(institutionRouteBean);
                    success = true;
                    deleteBean.setString("module_name", FileUploadModules.FIN_INST.name());
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
                .concat(INST_ROUTE_TABLE_MC)
                .concat(" ( finst_id, fsp_id, channel_id,DOCUMENT_ID, sno) ")
                .concat("VALUES (?,?, ?, ?, ?)");

        boolean success = false;

        LOG.info("Creating Financial institution  request: {}", query);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, row.getString("finst_id"));
            ps.setString(++kk, row.getString("fsp_id"));
            ps.setString(++kk, row.getString("channel_id"));
            ps.setString(++kk, requestBean.getString("document-id"));
            ps.setString(++kk, row.getString("sno"));


            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to financial institution list table dets: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to financial institution list dets");
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
                .concat(INST_ROUTE_TABLE)
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
                    row.setString("fsp_id", rs.getString("fsp_id"));
                    row.setString("finst_id", rs.getString("finst_id"));
                    row.setString("channel_id", rs.getString("channel_id"));
                    row.setString("creation_date", rs.getString("creation_date"));
                    row.setString("sno", rs.getString("sno"));
                    success = createModuleRequest(requestBean, row, cnn);
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
}
