package persistence;

import constants.FileUploadModules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.*;

public class InstitutionListDbHelper implements FileUploadOps {

    final static Logger LOG = LogManager.getLogger(InstitutionListDbHelper.class);


    @Override
    public boolean getUnapprovedOrCanceledRequest(BaseBean requestBean, JsonObject request) {

        String approvedQuery = "SELECT * FROM "
                .concat(INST_LIST_TABLE)
                .concat(" f WHERE f.DEL_FLG = 'N'");


        String query = "SELECT * FROM "
                .concat(INST_LIST_TABLE_MC)
                .concat(" c WHERE c.document_id = ?");

        if (request.getString("action").equals("approved")) {
            query = approvedQuery;
        }
        if (request.containsKey("id")) {
            query = query.concat(" and sno = ?");
        }

        boolean success = false;
        LOG.info("Fetching institution routes list by ID {} {}", requestBean.getString("document-id"), query);
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
                List<BaseBean> institutions = new ArrayList<>();
                while (rs.next()) {
                    BaseBean institutionBean = new BaseBean();
                    institutionBean.setString("service_provider_id", rs.getString("fsp_id"));
                    institutionBean.setString("institution_id", rs.getString("finst_id"));
                    institutionBean.setString("institution_name", rs.getString("finst_name"));
                    institutionBean.setString("its_institution_id", rs.getString("its_inst_id"));
                    institutionBean.setString("id", rs.getString("sno"));
                    if (!request.getString("action").equals("approved")) {
                        institutionBean.setString("document_id", rs.getString("document_id"));
                    }
                    if (request.getString("action").equals("approved")) {
                        institutionBean.setString("creation_date", rs.getString("creation_date"));
                        institutionBean.setString("created_by", rs.getString("created_by"));
                    }
                    institutions.add(institutionBean);
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(institutions));
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

        String query = "INSERT INTO ".concat(INST_LIST_TABLE).concat(" (fsp_id, FINST_ID, FINST_NAME, ITS_INST_ID, CREATION_DATE, CREATED_BY, sno) SELECT fsp_id, FINST_ID, finst_name, its_inst_id, sysdate, f.uploaded_by, esbuser.ibt_finst_list_seq.nextval FROM ").concat(INST_LIST_TABLE_MC).concat("  c inner join ").concat(FILE_REQUEST_TABLE).concat(" f ON f.document_id = c.document_id WHERE c.document_id = ?");

        LOG.info("Updating institution route config: {}", query);
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
                    LOG.info("writing to institution route table : ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to institution route");
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
            if (fetchInstitutionListEntity(deleteBean, requestBean, cnn)) {
                status = deleteInstitutionList(deleteBean, requestBean, cnn);
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

    private boolean deleteInstitutionList(BaseBean deleteBean, BaseBean requestBean, Connection cnn) {
        String deleteQuery = "DELETE FROM "
                .concat(INST_LIST_TABLE)
                .concat(" f WHERE ")
                .concat(" f.sno = (select sno from ")
                .concat(INST_LIST_TABLE_MC)
                .concat(" d where d.document_id = ?)");

        boolean success = false;

        LOG.info("Deleting Institution route request: {}", deleteQuery);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(deleteQuery);
            ps.setString(++kk, requestBean.getString("document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = DBHelper.createDeleteArchive(deleteBean, cnn);
                    LOG.info("deleting from institution route list: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to delete from institution route code list dets");
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

    private boolean fetchInstitutionListEntity(BaseBean deleteBean, BaseBean requestBean, Connection cnn) {
        String fetchQuery = "SELECT * FROM "
                .concat(INST_LIST_TABLE)
                .concat(" f WHERE ")
                .concat(" f.sno = (select sno from ")
                .concat(INST_LIST_TABLE_MC)
                .concat(" d where d.document_id = ?)");
        boolean success = false;
        LOG.info("Fetching institution routes list by ID {} {}", requestBean.getString("document-id"), fetchQuery);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            ps = cnn.prepareStatement(fetchQuery);
            ps.setString(++kk, requestBean.getString("document_id"));

            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> institutions = new ArrayList<>();
                while (rs.next()) {
                    BaseBean institutionBean = new BaseBean();
                    institutionBean.setString("service_provider_id", rs.getString("fsp_id"));
                    institutionBean.setString("institution_id", rs.getString("finst_id"));
                    institutionBean.setString("institution_name", rs.getString("finst_name"));
                    institutionBean.setString("its_institution_id", rs.getString("its_inst_id"));
                    institutionBean.setString("id", rs.getString("sno"));
                    institutionBean.setString("creation_date", rs.getString("creation_date"));
                    institutionBean.setString("created_by", rs.getString("created_by"));
                    institutions.add(institutionBean);
                    success = true;
                    deleteBean.setString("module_name", FileUploadModules.INST_ROUTE.name());
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
        String query = "INSERT INTO ".concat(INST_LIST_TABLE_MC).concat(" ( fsp_id, finst_id, finst_name, its_inst_id, DOCUMENT_ID, sno) ").concat("VALUES (?, ?, ?, ?, ?, ?)");

        boolean success = false;

        LOG.info("Creating Institution route request: {}", query);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, row.getString("service_provider_id"));
            ps.setString(++kk, row.getString("institution_id"));
            ps.setString(++kk, row.getString("institution_name"));
            ps.setString(++kk, row.getString("its_institution_id"));
            ps.setString(++kk, requestBean.getString("document-id"));
            ps.setString(++kk, row.getString("sno"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to institution route list table dets: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to institution route code list dets");
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
                .concat(INST_LIST_TABLE)
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
                    row.setString("service_provider_id", rs.getString("fsp_id"));
                    row.setString("institution_id", rs.getString("finst_id"));
                    row.setString("institution_name", rs.getString("finst_name"));
                    row.setString("its_institution_id", rs.getString("its_inst_id"));
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
