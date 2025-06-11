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
import static constants.AppConstants.DbTables.VIRTUAL_ACCOUNT_CONFIG;
import static constants.AppConstants.DbTables.VIRTUAL_ACCOUNT_CONFIG_MC;

public class AccountDbHelper {
    public static final int DOES_NOT_EXIST = -1;
    final static Logger LOG = LogManager.getLogger(ServiceDbHelper.class);


    public static boolean createAccountRequest(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(VIRTUAL_ACCOUNT_CONFIG_MC)
                .concat(" (acct_enq_url, acct_prefix, credt_trf_url, debt_trf_url, service_provider, action, prev_value, created_by, creatn_date) ")
                .concat("values (?, ?, ?, ?, ?, ?, ?, ?, sysdate)");

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Creating request Query: {}", query);

        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("acct-enq-url"));
            ps.setString(++kk, requestBean.getString("acct-prefix"));
            ps.setString(++kk, requestBean.getString("credt-trf-url"));
            ps.setString(++kk, requestBean.getString("debt-trf-url"));
            ps.setString(++kk, requestBean.getString("service-provider"));
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

    public static boolean getApprovedAccounts(BaseBean requestBean, JsonObject jsonRequest) {

        StringBuilder querySb = new StringBuilder("SELECT * FROM ");
        String approvalStatus = jsonRequest.getString("mc-status");
        switch (approvalStatus) {
            case AppConstants.ApprovalStatus.APPROVED:

                querySb.append(VIRTUAL_ACCOUNT_CONFIG);

                if (jsonRequest.containsKey("service-provider") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider = ?");
                } else if (jsonRequest.containsKey("service-provider")) {
                    querySb.append(" AND service_provider = ?");
                }

                if (jsonRequest.containsKey("acct-enq-url") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE acct_enq_url = ?");
                } else if (jsonRequest.containsKey("acct-enq-url")) {
                    querySb.append(" AND acct_enq_url = ?");
                }
                if (jsonRequest.containsKey("acct-prefix") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE acct_prefix = ?");
                } else if (jsonRequest.containsKey("acct-prefix")) {
                    querySb.append(" AND acct_prefix = ?");
                }
                if (jsonRequest.containsKey("credt-trf-url") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE credt_trf_url = ?");
                } else if (jsonRequest.containsKey("credt-trf-url")) {
                    querySb.append(" AND credt_trf_url = ?");
                }

                if (jsonRequest.containsKey("debt-trf-url") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE debt_trf_url = ?");
                } else if (jsonRequest.containsKey("debt-trf-url")) {
                    querySb.append(" AND debt_trf_url = ?");
                }

                if (jsonRequest.containsKey("approval-comment") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE approval_comment = ?");
                } else if (jsonRequest.containsKey("approval-comment")) {
                    querySb.append(" AND approval_comment = ?");
                }


                querySb.append(" order by acct_prefix");
                break;

            case AppConstants.ApprovalStatus.UNAPPROVED:
                querySb.append(VIRTUAL_ACCOUNT_CONFIG_MC)
                        .append(" s where s.mc_status is null");
                if (jsonRequest.containsKey("service-provider") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider= ?");
                } else if (jsonRequest.containsKey("va_id")) {
                    querySb.append(" AND service_provider = ?");
                }
                querySb.append(" order by creatn_date desc");
                break;

            case AppConstants.ApprovalStatus.CANCELLED:
                querySb.append(VIRTUAL_ACCOUNT_CONFIG_MC)
                        .append(" s where s.mc_status = '")
                        .append(CANCELLED)
                        .append("'");

                if (jsonRequest.containsKey("service-provider") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE service_provider = ?");
                } else if (jsonRequest.containsKey("va_id")) {
                    querySb.append(" AND service_provider = ?");
                }
                querySb.append(" order by creatn_date desc");
                break;
            default:
                return false;
        }
        boolean success = false;
        String query = querySb.toString();
        LOG.info("Virtual Account list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            if (jsonRequest.containsKey("service-provider")) {
                ps.setString(++kk, jsonRequest.getString("service_provider"));
            }
            if (approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                if (jsonRequest.containsKey("acct-enq-url")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("acct_enq_url")));
                }
                if (jsonRequest.containsKey("acct-prefix")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("acct_prefix")));
                }
                if (jsonRequest.containsKey("credt-trf-url")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("credt_trf_url")));
                }
                if (jsonRequest.containsKey("debt-trf-url")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("debt_trf_url")));
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
                    stat.setString("acct-prefix", rs.getString("acct_prefix"));
                    stat.setString("acct-enq-url", rs.getString("acct_enq_url"));
                    stat.setString("credt-trf-url", rs.getString("credt_trf_url"));
                    stat.setString("debt-trf-url", rs.getString("debt_trf_url"));
                    stat.setString("created-by", rs.getString("created_by"));
                    stat.setString("creatn-date", rs.getString("creatn_date"));
                    stat.setString("service-provider", rs.getString("service_provider"));
                    stat.setString("approval-comment", rs.getString("approval_comment"));
                    stat.setString("approved-by", rs.getString("approved_by"));

                    if (approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                        stat.setString("modification-date", rs.getString("modification_date"));
                        stat.setString("modified-by", rs.getString("modified_by"));
                        stat.setString("del-flg", String.valueOf(rs.getString("del_flg").equals("N")));
                        stat.setString("approval-date", rs.getString("approval_date"));

                    }
                    if (!approvalStatus.equals(AppConstants.ApprovalStatus.APPROVED)) {
                        stat.setString("update-id", rs.getString("update_id"));
                        stat.setString("action", rs.getString("action"));
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

    public static boolean approveAccountRequest(BaseBean requestBean) {
        boolean fetchUnapprovedAccount = fetchUnapprovedRequest(requestBean);
        if (!fetchUnapprovedAccount && requestBean.getString("db_response").isEmpty()) {
            return false;
        }
        JsonObject obj = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String action = obj.getString("action");

        boolean approvalStatus = Boolean.parseBoolean(requestBean.getString("mc-status"));
        Connection cnn = ConnectionUtil.getConnection();

        if (!action.equals(AppConstants.AppActions.CREATE)) {
            fetchCurrentApprovedAccount(requestBean, cnn);
        }
        String query = "UPDATE "
                .concat(VIRTUAL_ACCOUNT_CONFIG_MC)
                .concat(" c set c.approval_date = sysdate ,c.mc_status=?, c.approval_comment=? where c.service_provider = ? and c.mc_status is null");


        boolean success = false;
        LOG.info("Updating unapproved virtual account  table: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, Boolean.parseBoolean(requestBean.getString("mc-status")) ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, requestBean.getString("service-provider"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to virtual account config to unapproved table ");
                    if (!approvalStatus) {
                        success = true;
                    } else {

                        switch (action) {

                            case AppConstants.AppActions.CREATE:
                                success = createAccount(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.ACTIVATE:
                            case AppConstants.AppActions.DEACTIVATE:
                                success = activateAccount(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.UPDATE:
                                success = updateAccount(requestBean, cnn);
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

    private static boolean updateAccount(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(VIRTUAL_ACCOUNT_CONFIG)
                .concat(" s set "));
        boolean addComma = false;
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String prefix = request.getString("acct-prefix");
        String enq = request.getString("acct-enq-url");
        String credt = request.getString("credt-trf-url");
        String debt = request.getString("debt-trf-url");
        String service = request.getString("service-provider");
        if (prefix != null && !prefix.isEmpty()) {
            sb.append("s.acct_prefix = ? ");
            addComma = true;
        }
        if (enq != null && !enq.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.acct_enq_url = ? ");
        }
        if (credt != null && !credt.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.credt_trf_url = ? ");
        }
        if (debt != null && !debt.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.debt_trf_url = ? ");
        }
        if (service != null && !service.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.service_provider = ? ");
        }
        sb.append(", s.modified_by=?, s.modification_date=sysdate, s.approval_comment=? where s.service_provider = ?");
        String query = sb.toString();

        LOG.info("Updating virtual account config details: {}", query);
        boolean success = false;
        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            if (prefix != null && !prefix.isEmpty()) {
                ps.setString(++kk, prefix);
            }
            if (enq != null && !enq.isEmpty()) {
                ps.setString(++kk, enq);

            }
            if (credt != null && !credt.isEmpty()) {
                ps.setString(++kk, credt);

            }
            if (debt != null && !debt.isEmpty()) {
                ps.setString(++kk, debt);

            }
            if (service != null && !service.isEmpty()) {
                ps.setString(++kk, service);

            }
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, request.getString("update-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to virtual account config list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to virtual account config to approved list");

                }
            }catch (SQLIntegrityConstraintViolationException ex) {
                requestBean.setString("message", "Virtual account configurations already exists");
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

    private static boolean activateAccount(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(VIRTUAL_ACCOUNT_CONFIG)
                .concat(" s set s.del_flg = ?"));
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        sb.append(", s.modified_by=?, s.modification_date=sysdate, s.approval_comment=? where s.service_provider = ?");
        String query = sb.toString();

        LOG.info("Updating virtual account config details: {}", query);
        boolean success = false;
        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, request.getString("action").equals(AppConstants.AppActions.ACTIVATE) ? "N" : "Y");
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, request.getString("service-provider"));

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

    private static boolean createAccount(BaseBean requestBean, Connection cnn) {
        String query = "INSERT INTO "
                .concat(VIRTUAL_ACCOUNT_CONFIG)
                .concat(" (acct_prefix, acct_enq_url, credt_trf_url, debt_trf_url, created_by,del_flg, approved_by, approval_comment ,creatn_date, approval_date, service_provider) ")
                .concat("SELECT acct_prefix, acct_enq_url, credt_trf_url, debt_trf_url, created_by,'N', ?, ? , SYSDATE, SYSDATE, service_provider FROM ")
                .concat(VIRTUAL_ACCOUNT_CONFIG_MC)
                .concat(" WHERE service_provider = ?");
        boolean success = false;
        LOG.info("Adding to virtual account config table: {}", query);

        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("approval-comment"));
            ps.setString(++kk, requestBean.getString("service-provider"));

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

    private static boolean fetchCurrentApprovedAccount(BaseBean requestBean, Connection cnn) {
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String id = request.getString("service-provider"); // Use a default value to avoid NullPointerException

        String query = "SELECT * FROM " + VIRTUAL_ACCOUNT_CONFIG + " s WHERE s.service_provider = ?";
        boolean success = false;
        LOG.info("Fetching current Virtual Account Config value: {}", query);

        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, id);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("service_provider", rs.getString("service_provider"));
                    switchBean.setString("acct_prefix", rs.getString("acct_prefix"));
                    switchBean.setString("acct_enq_url", rs.getString("acct_enq_url"));
                    switchBean.setString("credt_trf_url", rs.getString("credt_trf_url"));
                    switchBean.setString("debt_trf_url", rs.getString("debt_trf_url"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("modified_by", rs.getString("modified_by"));
                    switchBean.setString("creatn_date", rs.getString("creatn_date"));
                    switchBean.setString("modification_date", rs.getString("modification_date"));
                    switchBean.setString("approval_date", rs.getString("approval_date"));
                    switchBean.setString("isactive", rs.getString("isactive"));
                    requestBean.setString("service_provider_value", JsonUtil.convertBeanToJsonObject(switchBean).toString());
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

    public static boolean fetchUnapprovedRequest(BaseBean requestBean) {
        String query = "SELECT * FROM "
                .concat(VIRTUAL_ACCOUNT_CONFIG_MC)
                .concat(" s WHERE s.service_provider = ? AND s.mc_status IS NULL ");

        boolean success = false;
        LOG.info("Virtual Account Config list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("service-provider"));
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("va-id", rs.getString("va_id"));
                    switchBean.setString("acct-enq-url", rs.getString("acct_enq_url"));
                    switchBean.setString("acct-prefix", rs.getString("acct_prefix"));
                    switchBean.setString("credt-trf-url", rs.getString("credt_trf_url"));
                    switchBean.setString("service-provider", rs.getString("service_provider"));
                    switchBean.setString("debt-trf-url", rs.getString("debt_trf_url"));
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

