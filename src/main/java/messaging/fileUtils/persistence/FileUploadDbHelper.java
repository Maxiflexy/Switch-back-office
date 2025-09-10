package messaging.fileUtils.persistence;

import constants.FileUploadModules;
import exceptions.CustomException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ConnectionUtil;
import persistence.ContraAccountDbHelper;
import persistence.FeeConfigDbHelper;
import persistence.FileUploadOps;
import persistence.InstitutionListDbHelper;
import persistence.InstitutionRouteDbHelper;
import persistence.ResponseCodeNextActionDbHelper;
import util.BaseBean;
import util.JsonUtil;

import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static constants.AppConstants.DbTables.FILE_REQUEST_TABLE;

public class FileUploadDbHelper {
    final static Logger LOG = LogManager.getLogger(FileUploadDbHelper.class);

    public static boolean fetchUploadedFiles(BaseBean requestBean, JsonObject request) {

        String fetchByStatusQuery = "SELECT * FROM "
                .concat(FILE_REQUEST_TABLE)
                .concat(" f where f.status ");
        if (requestBean.getString("action").equals("unapproved")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" is null");
        } else if (requestBean.getString("action").equals("cancelled")) {
            fetchByStatusQuery = fetchByStatusQuery.concat("= 'N'");
        } else {
            fetchByStatusQuery = fetchByStatusQuery.concat(" is null");
        }
        if (requestBean.containsKey("module_name")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.module_name = ?");
        }
        if (requestBean.containsKey("document_id")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.document_id = ?");
        }
        fetchByStatusQuery = fetchByStatusQuery.concat(" order by f.upload_date");

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching uploaded files {}", fetchByStatusQuery);

        try {
            int kk = 0;
            ps = cnn.prepareStatement(fetchByStatusQuery);
            if (requestBean.containsKey("module_name")) {
                ps.setString(++kk, requestBean.getString("module_name").toLowerCase());
            }
            if (requestBean.containsKey("document_id")) {
                ps.setString(++kk, requestBean.getString("document_id"));
            }
            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> files = new ArrayList<>();
                while (rs.next()) {
                    BaseBean fileBean = new BaseBean();
                    fileBean.setString("document_name", rs.getString("module_name"));
                    fileBean.setString("document_type", rs.getString("document_type"));
                    fileBean.setString("document_id", rs.getString("document_id"));
                    fileBean.setString("module_name", rs.getString("module_name"));
                    fileBean.setString("upload_date", rs.getString("upload_date"));
                    fileBean.setString("uploaded_by", rs.getString("uploaded_by"));
                    fileBean.setString("request_type", rs.getString("action"));
//                    fileBean.setString("key_name", rs.getString("key_name"));
//                    fileBean.setString("key_value", rs.getString("key_value"));
                    if (requestBean.getString("action").equals("cancelled")) {
                        fileBean.setString("approved_by", rs.getString("approved_by"));
                        fileBean.setString("approval_date", rs.getString("approval_date"));
                        fileBean.setString("approval_comment", rs.getString("approval_comment"));
                    }
                    files.add(fileBean);
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(files).toString());

                success = true;
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

    public static boolean approveFileUpload(BaseBean requestBean, JsonObject request, String moduleName, String action) {
//        update esbuser.file_upload_mc c set c.status = 'Y', c.approved_by = 'michael',c.approval_date = sysdate, c.approval_comment = 'Done' where c.document_id='a366918c-cb00-4085-96d7-491f74096258' and c.status is null;
        String query = "update "
                .concat(FILE_REQUEST_TABLE)
                .concat(" c set c.status = ?, c.approved_by = ?, c.approval_date = sysdate, c.approval_comment = ? where c.document_id=? and c.module_name = ? and c.status is null");
        PreparedStatement ps = null;
        Connection cnn = ConnectionUtil.getConnection();
        boolean success = false;
        try {
            cnn.setAutoCommit(false);
            boolean approvalStatus = Boolean.parseBoolean(requestBean.getString("status"));
            LOG.info("Executing file approval query: {}", query);
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, approvalStatus ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("email"));
            ps.setString(++kk, requestBean.getString("message"));
            ps.setString(++kk, requestBean.getString("document_id"));
            ps.setString(++kk, request.getString("module_name"));

            try {

                if (ps.executeUpdate() > 0) {
                    if (approvalStatus) {
                        LOG.info("writing to file upload maker checker: ");
                        FileUploadOps helper = getApprovedUploadedFileHelper(requestBean, moduleName);
                        if (action.equals("create")) {
                            success = helper.updateApprovedRecord(requestBean, request, cnn);
                        } else if (action.equals("delete")) {
                            success = helper.approveDeleteRecord(requestBean, request, cnn);
                        }
                    } else {
                        success = true;
                    }
                    if (success) {
                        cnn.commit();
                    } else {
                        cnn.rollback();
                    }
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to file upload maker checker");
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

            ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }

    public static boolean approveUpload(BaseBean requestBean, JsonObject requestObject) {
        boolean success = false;
        JsonObject request = getFileRequest(requestBean, requestObject);
        String moduleName = request.getString("module_name");
        String action = request.getString("request_type");
        success = approveFileUpload(requestBean, request, moduleName, action);
        return success;

    }

    private static JsonObject getFileRequest(BaseBean requestBean, JsonObject requestObject) {
        BaseBean fileBean = new BaseBean();
        fileBean.setString("action", "unapproved");
        fileBean.setString("document_id", requestBean.getString("document_id"));
        boolean fetchFile = fetchUploadedFiles(fileBean, requestObject);

        if (!fetchFile || fileBean.getString("jsonBean").isEmpty()) {
            requestBean.setString("message", "unable to fetch details with id: " + fileBean.getString("document_id"));
            throw new CustomException(requestBean);
        }

        return (JsonObject) JsonUtil.toJsonArray(fileBean.getString("jsonBean")).get(0);
    }

    private static FileUploadOps getApprovedUploadedFileHelper(BaseBean requestBean, String moduleName) {
        FileUploadOps fileUploadHelper;
        try {
            switch (FileUploadModules.valueOf(moduleName.toUpperCase())) {
                case FEE_CONFIG:
                    fileUploadHelper = new FeeConfigDbHelper();
                    break;
                case CONTRA_ACCOUNT:
                    fileUploadHelper = new ContraAccountDbHelper();
                    break;
                case FIN_INST:
                    fileUploadHelper = new InstitutionListDbHelper();
                    break;
                case RSP_CODE_NEXT_ACTN:
                    fileUploadHelper = new ResponseCodeNextActionDbHelper();
                    break;
                case INST_ROUTE:
                    fileUploadHelper = new InstitutionRouteDbHelper();
                    break;

                default:
                    requestBean.setString("message", "Invalid module name");
                    throw new CustomException(requestBean);
            }
        } catch (IllegalArgumentException e) {
            requestBean.setString("message", e.getMessage());
            throw new CustomException(requestBean);
        }
        return fileUploadHelper;
    }

    public static boolean fetchFileContent(BaseBean requestBean, JsonObject requestObject) {
        String moduleName;
        if (!requestBean.containsKey("module_name")) {
            moduleName = getFileRequest(requestBean, requestObject).getString("module_name");
        } else {
            moduleName = requestBean.getString("module_name");
        }
        FileUploadOps fetchHelper = getApprovedUploadedFileHelper(requestBean, moduleName);
        return fetchHelper.getUnapprovedOrCanceledRequest(requestBean, requestObject);
    }

    public static boolean saveNewRecord(Map<String, Object> tables, BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();
        boolean success = false;
        PreparedStatement ps = null;

        try {
            cnn.setAutoCommit(false);
            if (requestBean.containsKey("action") && requestBean.getString("action").equals("delete")) {
                String query = "INSERT INTO "
                        .concat(FILE_REQUEST_TABLE)
                        .concat(" (document_name, document_type, module_name, upload_date, uploaded_by, document_id, action) values (?, ?, ?, sysdate, ?, ?, ?)");
                LOG.info("Executing Delete OPs: {}", query);
                int kk = 0;
                ps = cnn.prepareStatement(query);
                ps.setString(++kk, "delete_request");
                ps.setString(++kk, "delete_request");
                ps.setString(++kk, requestBean.getString("module_name").toLowerCase());
                ps.setString(++kk, requestBean.getString("email"));
                ps.setString(++kk, requestBean.getString("document-id"));
                ps.setString(++kk, requestBean.getString("action"));
                try {

                    if (ps.executeUpdate() > 0) {
                        LOG.info("writing to file details maker checker: {}", requestBean.getString("module_name"));
                        FileUploadOps ops = getApprovedUploadedFileHelper(requestBean, requestBean.getString("module_name"));
                        success = ops.createDeleteRequest(requestBean, cnn);
                        if (!success) {
                            cnn.rollback();
                        }

                    } else {
                        //check if app has been verified
                        LOG.info("unable to write to fee config maker checker");
                        cnn.rollback();
                        LOG.info("done with rollback");

                    }

                } catch (Exception e) {
                    requestBean.setString("message", e.getMessage());
                    LOG.error("", e);
                    cnn.rollback();
                }

            } else {

                for (String key : tables.keySet()) {
                    String query = "INSERT INTO "
                            .concat(FILE_REQUEST_TABLE)
                            .concat(" (document_name,document_type,document_id,upload_date,uploaded_by,module_name, key_name, key_value, action) VALUES (?, ?,?,sysdate,?,?,?,?,?)");
                    LOG.info("Executing file upload query: {}", query);
                    BaseBean fileBean = (BaseBean) ((Map<String, Object>) tables.get(key)).get("file");
                    List<BaseBean> tableBeans = (List<BaseBean>) ((Map<String, Object>) tables.get(key)).get("table");
                    int kk = 0;
                    ps = cnn.prepareStatement(query);
                    ps.setString(++kk, requestBean.getString("fileName"));
                    ps.setString(++kk, requestBean.getString("extension"));
                    ps.setString(++kk, requestBean.getString("document-id"));
                    ps.setString(++kk, requestBean.getString("email"));
                    ps.setString(++kk, requestBean.getString("module_name").toLowerCase());
                    ps.setString(++kk, requestBean.getString("key_name"));
                    ps.setString(++kk, requestBean.getString("key_value"));
                    ps.setString(++kk, requestBean.getString("action"));
                    requestBean.setString("document-id", fileBean.getString("document-id"));

                    try {

                        if (ps.executeUpdate() > 0) {
                            LOG.info("writing to file details maker checker: {}", requestBean.getString("module_name"));
                            FileUploadOps ops = getApprovedUploadedFileHelper(requestBean, requestBean.getString("module_name"));
                            for (BaseBean row : tableBeans) {
                                success = ops.createModuleRequest(requestBean, row, cnn);
                                if (!success) {
                                    cnn.rollback();
                                    break;
                                }
                            }
                        } else {
                            //check if app has been verified
                            LOG.info("unable to write to fee config maker checker");
                            cnn.rollback();
                            LOG.info("done with rollback");
                            break;

                        }

                    } catch (Exception e) {
                        requestBean.setString("message", e.getMessage());
                        LOG.error("", e);
                        cnn.rollback();

                    }
                }
            }

            LOG.info("Saving transaction in db {}", success);

            if (success) {
                cnn.commit();
            } else {
                cnn.rollback();
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

            ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }
}
