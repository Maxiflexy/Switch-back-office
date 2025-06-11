package persistence;

import constants.AppConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;

import javax.json.JsonObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.ApprovalType.CANCELLED;
import static constants.AppConstants.DbTables.*;

public class PerformanceDbHelper {
    final static Logger LOG = LogManager.getLogger(PerformanceDbHelper.class);
    public static final int DOES_NOT_EXIST = -1;


    public static boolean getPerformanceStatistics(BaseBean requestBean) {
        String query = " select * from ".concat(PERF_STAT_TABLE)
                .concat(" where ENTRYDATE between to_date(?, 'yyyy-mm-dd hh24:mi:ss') and to_date(?, 'yyyy-mm-dd hh24:mi:ss') and MSG_TYPE like 'FSC%' and ENDPOINT = ? and TOTAL_TAT > ? order by entrydate desc ");
//        select count(endpoint), endpoint from esbuser.perf_stat where ENTRYDATE between to_date('2019-02-28 00:00:00', 'yyyy-mm-dd hh24:mi:ss') and to_date('2025-02-28 00:00:00', 'yyyy-mm-dd hh24:mi:ss') and MSG_TYPE like 'FSC%' and ENDPOINT IN (SELECT ENDPOINT FROM ESBUSER.ENDPOINT WHERE isactive = 'Y') and TOTAL_TAT > 1 group by endpoint

        String summaryQuery = "select count(endpoint) as count, endpoint from "
                .concat(PERF_STAT_TABLE)
                .concat(" where ENTRYDATE between to_date(?, 'yyyy-mm-dd hh24:mi:ss') and to_date(?, 'yyyy-mm-dd hh24:mi:ss') and MSG_TYPE like 'FSC%' and ENDPOINT IN (SELECT e.ENDPOINT_CODE FROM ESBUSER.ENDPOINT e WHERE e.isactive = 'Y') and TOTAL_TAT > ? group by endpoint");

//      start date: '07-03-2024 08:30:00', end date: 07-03-2024 23:59:59, endpoint = 'NIPINFLWV2',  TOTAL_TAT= 5
        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Getting performance stat: {}", query);
        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            if (requestBean.getString("endpoint").equals("summary")) {
                LOG.info("Getting performance stat: {}", summaryQuery);
                ps = cnn.prepareStatement(summaryQuery);
                ps.setString(++kk, requestBean.getString("start-date").replace("T", " "));
                ps.setString(++kk, requestBean.getString("end-date").replace("T", " "));
                ps.setString(++kk, requestBean.getString("total_tat").isEmpty() ? "5" : requestBean.getString("total_tat"));

            } else {
                LOG.info("Getting performance stat: {}", query);
                ps = cnn.prepareStatement(query);
                ps.setString(++kk, requestBean.getString("start-date").replace("T", " "));
                ps.setString(++kk, requestBean.getString("end-date").replace("T", " "));
                ps.setString(++kk, requestBean.getString("endpoint").isEmpty() ? "NIPINFLWV2" : requestBean.getString("endpoint"));
                ps.setString(++kk, requestBean.getString("total_tat").isEmpty() ? "5" : requestBean.getString("total_tat"));
            }
            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> perfStats = new ArrayList<>();
                if (requestBean.getString("endpoint").equals("summary")) {
                    while (rs.next()) {
                        BaseBean stat = new BaseBean();
                        stat.setString("endpoint", rs.getString("endpoint"));
                        stat.setString("count", rs.getString("count"));
                        perfStats.add(stat);
                    }
                } else {
                    while (rs.next()) {
                        BaseBean stat = new BaseBean();
                        stat.setString("endpoint", rs.getString("endpoint"));
                        stat.setString("msg_type", rs.getString("msg_type"));
                        stat.setString("sessionid", rs.getString("sessionid"));
                        stat.setString("save_tat", rs.getString("save_tat"));
                        stat.setString("val_tat", rs.getString("val_tat"));
                        stat.setString("debit_tat", rs.getString("debit_tat"));
                        stat.setString("credit_tat", rs.getString("credit_tat"));
                        stat.setString("total_tat", rs.getString("total_tat"));
                        stat.setString("entrydate", rs.getString("entrydate"));
                        perfStats.add(stat);
                    }
                }
                success = true;
                requestBean.setString("db_response", JsonUtil.convertBaseBeanListToJsonString(perfStats));


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
    public static boolean getApprovedEndpoints(BaseBean requestBean, JsonObject jsonRequest) {


        StringBuilder querySb = new StringBuilder("SELECT * FROM ");
        String approvalStatus = jsonRequest.getString("approval-status");
        switch (approvalStatus) {
            case AppConstants.ApprovalStatus.APPROVED:

                querySb.append(APPROVED_ENDPOINTS);
                if (jsonRequest.containsKey("id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE id = ?");
                } else if (jsonRequest.containsKey("id")) {
                    querySb.append(" AND id = ?");
                }

                if (jsonRequest.containsKey("endpoint-name") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE endpoint_name = ?");
                } else if (jsonRequest.containsKey("endpoint-name")) {
                    querySb.append(" AND endpoint_name = ?");
                }

                if (jsonRequest.containsKey("endpoint-code") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE endpoint_code = ?");
                } else if (jsonRequest.containsKey("endpoint_code")) {
                    querySb.append(" AND endpoint_code = ?");
                }
                querySb.append(" order by endpoint_name");
                break;

            case AppConstants.ApprovalStatus.UNAPPROVED:
                querySb.append(UNAPPROVED_ENDPOINT)
                        .append(" s where s.approval_status is null");
                if (jsonRequest.containsKey("id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE mc_id = ?");
                } else if (jsonRequest.containsKey("id")) {
                    querySb.append(" AND mc_id = ?");
                }
                querySb.append(" order by creation_date desc");
                break;

            case AppConstants.ApprovalStatus.CANCELLED:
                querySb.append(UNAPPROVED_ENDPOINT)
                        .append(" s where s.approval_status = '")
                        .append(CANCELLED)
                        .append("'");

                if (jsonRequest.containsKey("id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE mc_id = ?");
                } else if (jsonRequest.containsKey("id")) {
                    querySb.append(" AND mc_id = ?");
                }
                querySb.append(" order by creation_date desc");
                break;
            default:
                return false;
        }

        boolean success = false;
        String query = querySb.toString();
        LOG.info("Switch list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            if (jsonRequest.containsKey("id")) {
                ps.setString(++kk, jsonRequest.getString("id"));
            }
            if (approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                if (jsonRequest.containsKey("name")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("endpoint_name")));
                }
                if (jsonRequest.containsKey("switch-code")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("switch-code")));
                }
            }


            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> switches = new ArrayList<>();
                while (rs.next()) {
                    BaseBean stat = new BaseBean();
                    stat.setString("name", rs.getString("endpoint_name"));
                    stat.setString("code", rs.getString("endpoint_code"));
                    stat.setString("created-by", rs.getString("created_by"));
                    stat.setString("approved-by", rs.getString("approved_by"));
                    if (approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                        stat.setString("id", rs.getString("id"));
                        stat.setString("isActive", String.valueOf(rs.getString("isActive").equals("Y")));
                    }
                    if (!approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                        stat.setString("id", rs.getString("mc_id"));
                        stat.setString("message", rs.getString("approval_message"));
                        stat.setString("action", rs.getString("mc_action"));
                    }

                    switches.add(stat);
                }
                success = true;
                requestBean.setString("db_response", JsonUtil.convertBaseBeanListToJsonString(switches));


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

    public static boolean createEndpointRequest(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(UNAPPROVED_ENDPOINT)
                .concat(" (endpoint_name, endpoint_code, endpoint_id, created_by, mc_id, mc_action, creation_date) values(?,?,?,?,?,?,sysdate)");

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Creating request Query: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("endpoint-name"));
            ps.setString(++kk, requestBean.getString("endpoint-code"));
            ps.setString(++kk, requestBean.getString("endpoint-id"));
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("mc-id"));
            ps.setString(++kk, requestBean.getString("mc-action"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("writing to switch to unapproved table ");

                    success = true;
                    cnn.commit();

                } else {
                    //check if app has been verified
                    LOG.info("unable to write to switch to unapproved checker");
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


    public static boolean approveEndpointRequest(BaseBean requestBean) {
        boolean fetchUnapprovedSwitch = fetchUnapprovedRequest(requestBean);
        if (!fetchUnapprovedSwitch && requestBean.getString("db_response").isEmpty()) {
            return false;
        }
        JsonObject obj = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String action = obj.getString("mc_action");


        boolean approvalStatus = Boolean.parseBoolean(requestBean.getString("approval-status"));
        Connection cnn = ConnectionUtil.getConnection();

        if (!action.equals(AppConstants.AppActions.CREATE)) {
            fetchCurrentApprovedSwitch(requestBean, cnn);
        }
        String query = "UPDATE "
                .concat(UNAPPROVED_ENDPOINT)
                .concat(" c set c.approval_date = sysdate, c.approval_status = ?, c.current_value = ?, c.approval_message=? where c.mc_id = ? and c.approval_status is null");


        boolean success = false;
        LOG.info("Updating unapproved endpoint table: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, Boolean.parseBoolean(requestBean.getString("approval-status")) ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("endpoint_value"));
            ps.setString(++kk, requestBean.getString("message"));
            ps.setString(++kk, requestBean.getString("endpoint-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating endpoint to unapproved table ");
                    if (!approvalStatus) {
                        success = true;
                    } else {

                        switch (action) {

                            case AppConstants.AppActions.CREATE:
                                success = createSwitch(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.ACTIVATE:
                            case AppConstants.AppActions.DEACTIVATE:
                                success = activateEndpoint(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.UPDATE:
                                success = updateSwitch(requestBean, cnn);
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
                    LOG.info("unable to write to switch to unapproved checker");
                    cnn.rollback();
                    LOG.info("done with rollback");
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                requestBean.setString("message", "Switch with the same configuration, already exists");
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

    private static boolean fetchCurrentApprovedSwitch(BaseBean requestBean, Connection cnn) {
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String id = request.getString("endpoint-id");
        String query = "SELECT * FROM "
                .concat(APPROVED_ENDPOINTS)
                .concat(" s WHERE s.id = ?");
        boolean success = false;
        LOG.info("Fetching current Switch value: {}", query);
        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, id);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("id", rs.getString("id"));
                    switchBean.setString("mc_id", rs.getString("mc_id"));
                    switchBean.setString("endpoint-name", rs.getString("endpoint_name"));
                    switchBean.setString("endpoint-code", rs.getString("endpoint_code"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("modified_by", rs.getString("modified_by"));
                    switchBean.setString("creation_date", rs.getString("creation_date"));
                    switchBean.setString("modification_date", rs.getString("modification_date"));
                    switchBean.setString("approval_date", rs.getString("approval_date"));
                    switchBean.setString("isactive", rs.getString("isactive"));
                    requestBean.setString("endpoint_value", JsonUtil.convertBeanToJsonObject(switchBean).toString());
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

    private static boolean createSwitch(BaseBean requestBean, Connection cnn) {

        String query = "INSERT INTO "
                .concat(APPROVED_ENDPOINTS)
                .concat("(endpoint_name, endpoint_code, created_by, approved_by, creation_date, approval_date, modification_date, isactive, mc_id) ")
                .concat("SELECT endpoint_name, endpoint_code, created_by, ?, sysdate, sysdate, sysdate, 'Y', mc_id from ")
                .concat(UNAPPROVED_ENDPOINT)
                .concat(" c where c.mc_id = ?");
        boolean success = false;
        LOG.info("adding to switch table: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, "'" + requestBean.getString("user") + "'");
            ps.setString(++kk, requestBean.getString("endpoint-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to switch list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to switch to approved list");

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

    private static boolean activateEndpoint(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(APPROVED_ENDPOINTS)
                .concat(" s set s.isactive = ?"));
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        sb.append(", s.modified_by=?, s.modification_date=sysdate where s.id = ?");
        String query = sb.toString();

        LOG.info("Updating switch details: {}", query);
        boolean success = false;
        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, request.getString("mc_action").equals(AppConstants.AppActions.ACTIVATE) ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, request.getString("endpoint-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to endpoint list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to switch to approved list");

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

    private static boolean updateSwitch(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(APPROVED_ENDPOINTS)
                .concat(" s set "));
        boolean addComma = false;
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String name = request.getString("name");
        String code = request.getString("code");
        if (name != null && !name.isEmpty()) {
            sb.append("s.endpoint_name = ?");
            addComma = true;
        }
        if (code != null && !code.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.endpoint_code = ? ");
        }
        sb.append(", s.modified_by=?, s.modification_date=sysdate where s.id = ?");
        String query = sb.toString();

        LOG.info("Updating switch details: {}", query);
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
            ps.setString(++kk, request.getString("endpoint-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to switch list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to switch to approved list");

                }
            } catch (SQLIntegrityConstraintViolationException ex) {
                requestBean.setString("message", "Switch configurations already exists");
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
                .concat(UNAPPROVED_ENDPOINT)
                .concat(" s WHERE s.mc_id = ? AND s.approval_status IS NULL ");

        boolean success = false;
        LOG.info("Switch list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("endpoint-id"));
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("id", rs.getString("id"));
                    switchBean.setString("mc_id", rs.getString("mc_id"));
                    switchBean.setString("name", rs.getString("endpoint_name"));
                    switchBean.setString("code", rs.getString("endpoint_code"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("mc_action", rs.getString("mc_action"));
                    switchBean.setString("creation_date", rs.getString("creation_date"));
                    switchBean.setString("endpoint-id", rs.getString("endpoint_id"));
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
