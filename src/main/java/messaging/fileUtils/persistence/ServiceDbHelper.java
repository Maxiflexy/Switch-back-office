package messaging.fileUtils.persistence;

import constants.AppConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ConnectionUtil;
import util.BaseBean;
import util.JsonUtil;

import javax.json.JsonObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.ApprovalType.CANCELLED;
import static constants.AppConstants.DbTables.UNAPPROVED_SERVICE_PROVIDERS;
import static constants.AppConstants.DbTables.VA_SERVICE_PROVIDERS_TABLE;

public class ServiceDbHelper {
    public static final int DOES_NOT_EXIST = -1;
    final static Logger LOG = LogManager.getLogger(ServiceDbHelper.class);

    public static boolean getApprovedServices(BaseBean requestBean, JsonObject jsonRequest) {


        StringBuilder querySb = new StringBuilder("SELECT * FROM ");
        String approvalStatus = jsonRequest.getString("mc-status");
        switch (approvalStatus) {
            case AppConstants.ApprovalStatus.APPROVED:

                querySb.append(VA_SERVICE_PROVIDERS_TABLE);

                if (jsonRequest.containsKey("service-provider-id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider_id = ?");
                } else if (jsonRequest.containsKey("service-provider-id")) {
                    querySb.append(" AND service_provider_id = ?");
                }

                if (jsonRequest.containsKey("service-provider-name") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider_name = ?");
                } else if (jsonRequest.containsKey("service-provider-name")) {
                    querySb.append(" AND service_provider_name = ?");
                }

                if (jsonRequest.containsKey("serial-num") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE serial_num = ?");
                } else if (jsonRequest.containsKey("serial-num")) {
                    querySb.append(" AND serial_num = ?");
                }

                if (jsonRequest.containsKey("approval-comment") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE approval_comment = ?");
                } else if (jsonRequest.containsKey("approval-comment")) {
                    querySb.append(" AND approval_comment = ?");
                }


                querySb.append(" order by service_provider_name");
                break;

            case AppConstants.ApprovalStatus.UNAPPROVED:
                querySb.append(UNAPPROVED_SERVICE_PROVIDERS)
                        .append(" s where s.mc_status is null");
                if (jsonRequest.containsKey("service-provider-id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider_id = ?");
                } else if (jsonRequest.containsKey("sp_id")) {
                    querySb.append(" AND service_provider_id = ?");
                }
                querySb.append(" order by creatn_date desc");
                break;

            case AppConstants.ApprovalStatus.CANCELLED:
                querySb.append(UNAPPROVED_SERVICE_PROVIDERS)
                        .append(" s where s.mc_status = '")
                        .append(CANCELLED)
                        .append("'");

                if (jsonRequest.containsKey("service-provider-id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider_id = ?");
                } else if (jsonRequest.containsKey("id")) {
                    querySb.append(" AND service_provider_id = ?");
                }
                querySb.append(" order by creatn_date desc");
                break;
            default:
                return false;
        }

        boolean success = false;
        String query = querySb.toString();
        LOG.info("Service Provider list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            if (jsonRequest.containsKey("service-provider-id")) {
                ps.setString(++kk, jsonRequest.getString("service_provider_id"));
            }
            if (approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                if (jsonRequest.containsKey("service-provider-name")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("service_provider_name")));
                }
                if (jsonRequest.containsKey("serial-num")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("serial_num")));
                }
                if (jsonRequest.containsKey("approval-comment")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("approval_comment")));
                }
            }


            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> services = new ArrayList<>();
                while (rs.next()) {
                    BaseBean stat = new BaseBean();
                    stat.setString("service-provider-name", rs.getString("service_provider_name"));
                   stat.setString("service-provider-id", rs.getString("service_provider_id"));
                    stat.setString("serial-num", rs.getString("serial_num"));
                    stat.setString("created-by", rs.getString("created_by"));
                    stat.setString("creatn-date", rs.getString("creatn_date"));
                    stat.setString("approved-by", rs.getString("approved_by"));
                    stat.setString("approval-comment", rs.getString("approval_comment"));
                    if (approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
//                        stat.setString("service-provider-id", rs.getString("service_provider_id"));
                        stat.setString("approval-date", rs.getString("approval_date"));
                        stat.setString("modification-date", rs.getString("modification_date"));
                        stat.setString("modified-by", rs.getString("modified_by"));
                        stat.setString("del-flg", String.valueOf(rs.getString("del_flg").equals("N")));
//                        stat.setString("approval-comment", rs.getString("approval_comment"));
                    }
                    if (!approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
//                        stat.setString("service-provider-id", rs.getString("service_provider_id"));
                        stat.setString("action", rs.getString("action"));
                        stat.setString("update-id", rs.getString("update_id"));
                    }

                    services.add(stat);
                }
                success = true;
                requestBean.setString("db_response", JsonUtil.convertBaseBeanListToJsonString(services));


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

            ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }


    public static boolean createServiceRequest(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(UNAPPROVED_SERVICE_PROVIDERS)
                .concat(" (service_provider_name, serial_num, service_provider_id, action, prev_value, created_by, creatn_date) ")
                .concat("values (?, serial_seq.NEXTVAL, ?, ?, ?, ?,sysdate)");

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Creating request Query: {}", query);

        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("service-provider-name"));
            ps.setString(++kk, requestBean.getString("service-provider-id"));
            ps.setString(++kk, requestBean.getString("action"));
            ps.setString(++kk, requestBean.getString("update-id"));
            ps.setString(++kk, requestBean.getString("user"));


            if (ps.executeUpdate() > 0) {
                LOG.info("Request created successfully");
                success = true;
                cnn.commit();

            } else {
                cnn.rollback();
                LOG.info("Failed to create request");
            }
        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("SQLException: ", e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("Error closing PreparedStatement: ", e);
                }
            }
            ConnectionUtil.closeConnection(cnn);
        }
        return success;
    }


    public static boolean approveServiceRequest(BaseBean requestBean) {
        boolean fetchUnapprovedService = fetchUnapprovedRequest(requestBean);
        if (!fetchUnapprovedService && requestBean.getString("db_response").isEmpty()) {
            return false;
        }
        JsonObject obj = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String action = obj.getString("action");


        boolean approvalStatus = Boolean.parseBoolean(requestBean.getString("mc-status"));
        Connection cnn = ConnectionUtil.getConnection();

        if (!action.equals(AppConstants.AppActions.CREATE)) {
            fetchCurrentApprovedService(requestBean, cnn);
        }
        String query = "UPDATE "
                .concat(UNAPPROVED_SERVICE_PROVIDERS)
                .concat(" c set c.approval_date = sysdate ,c.mc_status=?,  c.approval_comment=? where c.service_provider_id = ? and c.mc_status is null");


        boolean success = false;
        LOG.info("Updating unapproved service provider table: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, Boolean.parseBoolean(requestBean.getString("mc-status")) ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, requestBean.getString("service-provider-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to service provider to unapproved table ");
                    if (!approvalStatus) {
                        success = true;
                    } else {

                        switch (action) {

                            case AppConstants.AppActions.CREATE:
                                success = createService(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.ACTIVATE:
                            case AppConstants.AppActions.DEACTIVATE:
                                success = activateService(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.UPDATE:
                                success = updateService(requestBean, cnn);
                                break;
                        }
                    }

                    if (success) {
                        cnn.commit();
                    } else {
                        cnn.rollback();
                        LOG.info("Unable to complete creation request");
                    }


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to service provider to unapproved checker");
                    cnn.rollback();
                    LOG.info("done with rollback");
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                requestBean.setString("message", "service provider with the same configuration, already exists");
                LOG.error("", e);

            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

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



    private static boolean fetchCurrentApprovedService(BaseBean requestBean, Connection cnn) {
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String id = request.getString("service-provider-id"); // Use a default value to avoid NullPointerException

        String query = "SELECT * FROM " + VA_SERVICE_PROVIDERS_TABLE + " s WHERE s.service_provider_id = ?";
        boolean success = false;
        LOG.info("Fetching current Service Provider value: {}", query);

        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, id);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("service-provider-id", rs.getString("service_provider_id"));
                    switchBean.setString("service-provider-name", rs.getString("service_provider_name"));
                    switchBean.setString("serial-num", rs.getString("serial_num"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("modified-by", rs.getString("modified_by"));
                    switchBean.setString("creatn-date", rs.getString("creatn_date"));
                    switchBean.setString("modification_-date", rs.getString("modification_date"));
                    switchBean.setString("approval-date", rs.getString("approval_date"));
                    switchBean.setString("approval-comment", rs.getString("approval_date"));
                    switchBean.setString("del-flg", rs.getString("del_flg"));
                    requestBean.setString("service_provider-value", JsonUtil.convertBeanToJsonObject(switchBean).toString());
                    success = true;
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

    private static boolean createService(BaseBean requestBean, Connection cnn) {
        String query = "INSERT INTO "
                .concat(VA_SERVICE_PROVIDERS_TABLE)
                .concat(" (service_provider_name, serial_num, created_by, del_flg, approved_by, approval_comment ,creatn_date, approval_date, service_provider_id) ")
                .concat("SELECT service_provider_name, serial_num, created_by,'N', ?, ? , SYSDATE, SYSDATE, service_provider_id FROM ")
                .concat(UNAPPROVED_SERVICE_PROVIDERS)
                .concat(" WHERE service_provider_id = ?");
        boolean success = false;
        LOG.info("Adding to service provider table: {}", query);

        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, "'" +requestBean.getString("user") + "'");
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, requestBean.getString("service-provider-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to service provider list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to service provider to approve list");

                }

            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

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
    private static boolean activateService(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(VA_SERVICE_PROVIDERS_TABLE)
                .concat(" s set s.del_flg = ?"));
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        sb.append(", s.modified_by=?, s.modification_date=sysdate where s.service_provider_id = ?");
        String query = sb.toString();

        LOG.info("Updating service providers details: {}", query);
        boolean success = false;
        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, request.getString("action").equals(AppConstants.AppActions.ACTIVATE) ? "N" : "Y");
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, request.getString("service-provider-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to service provider list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to service provider to approve list");

                }

            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

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

    private static boolean updateService(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(VA_SERVICE_PROVIDERS_TABLE)
                .concat(" s set "));
        boolean addComma = false;
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String name = request.getString("service-provider-id");
        String code = request.getString("service-provider-name");
        if (name != null && !name.isEmpty()) {
            sb.append("s.service_provider_id = ?");
            addComma = true;
        }
        if (code != null && !code.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.service_provider_name = ? ");
        }
        sb.append(", s.modified_by=?, s.modification_date=sysdate, s.approval_comment=? where s.service_provider_id = ?");
        String query = sb.toString();

        LOG.info("Updating service provider details: {}", query);
        boolean success = false;
        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            if (name != null && !name.isEmpty()) {
                ps.setString(++kk, name);
            }
            if (code != null && !code.isEmpty()) {
                ps.setString(++kk, code);

            }
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, request.getString("update-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to service provider list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to service provider to approved list");

                }
            }catch (SQLIntegrityConstraintViolationException ex) {
                requestBean.setString("message", "Service provider configurations already exists");
                LOG.error("", ex);
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

            }


        } catch (SQLException e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("", e);

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

    public static boolean fetchUnapprovedRequest(BaseBean requestBean) {
        String query = "SELECT * FROM "
                .concat(UNAPPROVED_SERVICE_PROVIDERS)
                .concat(" s WHERE s.service_provider_id = ? AND s.mc_status IS NULL ");

        boolean success = false;
        LOG.info("Service Provider list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("service-provider-id"));
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("sp-id", rs.getString("sp_id"));
                    switchBean.setString("serial-num", rs.getString("serial_num"));
                    switchBean.setString("service-provider-id", rs.getString("service_provider_id"));
                    switchBean.setString("service-provider-name", rs.getString("service_provider_name"));
                    switchBean.setString("creatn-date", rs.getString("creatn_date"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("action", rs.getString("action"));
                    switchBean.setString("approval-comment", rs.getString("approval_comment"));
                    switchBean.setString("update-id", rs.getString("prev_value"));
                    requestBean.setString("db_response", JsonUtil.convertBeanToJsonObject(switchBean).toString());
                    success = true;
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

            ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }
}