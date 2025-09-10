package messaging.fileUtils.persistence;

import constants.AppConstants;
import constants.AppConstants.ApprovalStatus;
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
import static constants.AppConstants.DbTables.SWITCH_list_TABLE;
import static constants.AppConstants.DbTables.UNAPPROVED_SWITCH_LIST;

public class SwitchDbHelper {
    public static final int DOES_NOT_EXIST = -1;
    final static Logger LOG = LogManager.getLogger(SwitchDbHelper.class);



    public static boolean getApprovedSwitches(BaseBean requestBean, JsonObject jsonRequest) {


        StringBuilder querySb = new StringBuilder("SELECT * FROM ");
        String approvalStatus = jsonRequest.getString("approval-status");
        switch (approvalStatus) {
            case ApprovalStatus.APPROVED:

                querySb.append(SWITCH_list_TABLE);
                if (jsonRequest.containsKey("id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE id = ?");
                } else if (jsonRequest.containsKey("id")) {
                    querySb.append(" AND id = ?");
                }

                if (jsonRequest.containsKey("name") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE name = ?");
                } else if (jsonRequest.containsKey("name")) {
                    querySb.append(" AND name = ?");
                }

                if (jsonRequest.containsKey("switch-code") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE code = ?");
                } else if (jsonRequest.containsKey("name")) {
                    querySb.append(" AND code = ?");
                }
                querySb.append(" order by name");
                break;

            case ApprovalStatus.UNAPPROVED:
                querySb.append(UNAPPROVED_SWITCH_LIST)
                        .append(" s where s.approval_status is null");
                if (jsonRequest.containsKey("id") && querySb.lastIndexOf("where") == DOES_NOT_EXIST) {
                    querySb.append(" WHERE mc_id = ?");
                } else if (jsonRequest.containsKey("id")) {
                    querySb.append(" AND mc_id = ?");
                }
                querySb.append(" order by creation_date desc");
                break;

            case ApprovalStatus.CANCELLED:
                querySb.append(UNAPPROVED_SWITCH_LIST)
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
            if (approvalStatus.equals(ApprovalStatus.APPROVED)) {
                if (jsonRequest.containsKey("name")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("name")));
                }
                if (jsonRequest.containsKey("switch-code")) {
                    ps.setString(++kk, String.valueOf(jsonRequest.get("name")));
                }
            }


            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> switches = new ArrayList<>();
                while (rs.next()) {
                    BaseBean stat = new BaseBean();
                    stat.setString("name", rs.getString("name"));
                    stat.setString("code", rs.getString("code"));
                    stat.setString("created-by", rs.getString("created_by"));
                    stat.setString("approved-by", rs.getString("approved_by"));
                    if (approvalStatus.equals(ApprovalStatus.APPROVED)) {
                        stat.setString("id", rs.getString("id"));
                        stat.setString("isActive", String.valueOf(rs.getString("isActive").equals("Y")));
                    }
                    if (!approvalStatus.equals(ApprovalStatus.APPROVED)) {
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

    public static boolean createSwitchRequest(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(UNAPPROVED_SWITCH_LIST)
                .concat(" (name, code, switch_id, created_by, mc_id, mc_action, creation_date) values(?,?,?,?,?,?,sysdate)");

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Creating request Query: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("switch-name"));
            ps.setString(++kk, requestBean.getString("switch-code"));
            ps.setString(++kk, requestBean.getString("switch-id"));
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


    public static boolean approveSwitchRequest(BaseBean requestBean) {
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
                .concat(UNAPPROVED_SWITCH_LIST)
                .concat(" c set c.approval_date = sysdate, c.approval_status = ?, c.current_value = ?, c.approval_message=? where c.mc_id = ? and c.approval_status is null");


        boolean success = false;
        LOG.info("Updating unapproved swich table: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, Boolean.parseBoolean(requestBean.getString("approval-status")) ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("switch_value"));
            ps.setString(++kk, requestBean.getString("message"));
            ps.setString(++kk, requestBean.getString("switch-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to switch to unapproved table ");
                    if (!approvalStatus) {
                        success = true;
                    } else {

                        switch (action) {

                            case AppConstants.AppActions.CREATE:
                                success = createSwitch(requestBean, cnn);
                                break;

                            case AppConstants.AppActions.ACTIVATE:
                            case AppConstants.AppActions.DEACTIVATE:
                                success = activateSwitch(requestBean, cnn);
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
        String id = request.getString("switch-id");
        String query = "SELECT * FROM "
                .concat(SWITCH_list_TABLE)
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
                    switchBean.setString("name", rs.getString("name"));
                    switchBean.setString("code", rs.getString("code"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("modified_by", rs.getString("modified_by"));
                    switchBean.setString("creation_date", rs.getString("creation_date"));
                    switchBean.setString("modification_date", rs.getString("modification_date"));
                    switchBean.setString("approval_date", rs.getString("approval_date"));
                    switchBean.setString("isactive", rs.getString("isactive"));
                    requestBean.setString("switch_value", JsonUtil.convertBeanToJsonObject(switchBean).toString());
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

//    private static boolean createSwitch(BaseBean requestBean) {
//        Connection cnn = ConnectionUtil.getConnection();
////                update esbuser.switch_list_mc c set c.approval_date = sysdate, c.approval_status = 'Y', c.current_value = 'current value in json format'  c.approval_message='testing' where c.mc_id = '351f06e9-bfe3-47b4-9011-7657c80ba323' and c.approval_status is null;
//
//        String query = "UPDATE "
//                .concat(UNAPPROVED_SWITCH_LIST)
//                .concat(" c set c.approval_date = sysdate, c.approval_status = ?, c.current_value = ?  c.approval_message=? where c.mc_id = ? and c.approval_status is null");
//
//
//        boolean success = false;
//        LOG.info("Updating unapproved swich table: {}", query);
//
//        PreparedStatement ps = null;
//        try {
//
//            int kk = 0;
//            cnn.setAutoCommit(false);
//
//            ps = cnn.prepareStatement(query);
//            ps.setString(++kk, Boolean.parseBoolean(requestBean.getString("approval-status")) ? "Y" : "N");
//            ps.setString(++kk, requestBean.getString("db_response"));
//            ps.setString(++kk, requestBean.getString("message"));
//            ps.setString(++kk, requestBean.getString("id"));
//
//            try {
//
//                if (ps.executeUpdate() > 0) {
//                    LOG.info("Updating to switch to unapproved table ");
//
//                    success = addSwitchToApprovedTable(requestBean, cnn);
//                    if (success) {
//                        cnn.commit();
//                    } else {
//                        cnn.rollback();
//                        LOG.info("Unable to complete creation request");
//                    }
//
//
//                } else {
//                    //check if app has been verified
//                    LOG.info("unable to write to switch to unapproved checker");
//                    cnn.rollback();
//                    LOG.info("done with rollback");
//                }
//
//            } catch (Exception e) {
//                requestBean.setString("message", e.getMessage());
//                LOG.error("", e);
//
//            }
//
//
//        } catch (SQLException e) {
//            requestBean.setString("message", e.getMessage());
//            LOG.error("", e);
//
//        } finally {
//
//            if (ps != null) {
//                try {
//                    ps.close();
//                } catch (SQLException e) {
//                    LOG.error("", e);
//                }
//                ps = null;
//            }
//
//            ConnectionUtil.closeConnection(cnn);
//
//        }
//        return success;
//    }

    private static boolean createSwitch(BaseBean requestBean, Connection cnn) {

        String query = "INSERT INTO "
                .concat(SWITCH_list_TABLE)
                .concat("(name, code, created_by, approved_by, creation_date, approval_date, modification_date, isactive, mc_id) ")
                .concat("SELECT name, code, created_by, ?, sysdate, sysdate, sysdate, 'Y', mc_id from ")
                .concat(UNAPPROVED_SWITCH_LIST)
                .concat(" c where c.mc_id = ?");
        boolean success = false;
        LOG.info("adding to switch table: {}", query);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, "'" + requestBean.getString("user") + "'");
            ps.setString(++kk, requestBean.getString("switch-id"));

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

    private static boolean activateSwitch(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(SWITCH_list_TABLE)
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
            ps.setString(++kk, request.getString("switch-id"));

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

    private static boolean updateSwitch(BaseBean requestBean, Connection cnn) {
        StringBuilder sb = new StringBuilder("UPDATE "
                .concat(SWITCH_list_TABLE)
                .concat(" s set "));
        boolean addComma = false;
        JsonObject request = JsonUtil.toJsonObject(requestBean.getString("db_response"));
        String name = request.getString("name");
        String code = request.getString("code");
        if (name != null && !name.isEmpty()) {
            sb.append("s.name = ?");
            addComma = true;
        }
        if (code != null && !code.isEmpty()) {
            if (addComma) {
                sb.append(",");
            }
            sb.append("s.code = ? ");
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
            ps.setString(++kk, request.getString("switch-id"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("Updating to switch list ");

                    success = true;


                } else {
                    //check if app has been verified
                    LOG.info("unable to write to switch to approved list");

                }
            }catch (SQLIntegrityConstraintViolationException ex) {
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
                .concat(UNAPPROVED_SWITCH_LIST)
                .concat(" s WHERE s.mc_id = ? AND s.approval_status IS NULL ");

        boolean success = false;
        LOG.info("Switch list: {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("switch-id"));
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BaseBean switchBean = new BaseBean();
                    switchBean.setString("id", rs.getString("id"));
                    switchBean.setString("mc_id", rs.getString("mc_id"));
                    switchBean.setString("name", rs.getString("name"));
                    switchBean.setString("code", rs.getString("code"));
                    switchBean.setString("created-by", rs.getString("created_by"));
                    switchBean.setString("mc_action", rs.getString("mc_action"));
                    switchBean.setString("creation_date", rs.getString("creation_date"));
                    switchBean.setString("switch-id", rs.getString("switch_id"));
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
