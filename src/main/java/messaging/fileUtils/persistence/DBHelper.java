package messaging.fileUtils.persistence;


import messaging.fileUtils.constants.AppConstants;
import messaging.fileUtils.util.BaseBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.Constants.APP_CODE;
import static constants.AppConstants.DbTables.*;


public class DBHelper {

    final static Logger LOG = LogManager.getLogger(DBHelper.class);


    public static boolean insertUserIntoDB(BaseBean requestBean) {

        Connection cnn = ConnectionUtil.getConnection();

        String query = "INSERT INTO ".concat(USERS_TABLE).concat("(firstname, lastname, username, email, role_id, date_created, date_modified, last_login_date, refresh_token, app_code) " +
                "SELECT  ?, ?, ?, ?,  r.id , sysdate, sysdate, sysdate, ?, ? FROM esbuser.BCK_ROLES r where r.role_name = 'AUDITOR' AND r.app_code = 'SWITCH'");
        LOG.info("Query: " + query);
        PreparedStatement ps = null;

        boolean success = false;
        LOG.info("inserting user into user table");
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            String email = requestBean.getString("username");
            int index = email.lastIndexOf("@") == -1 ? email.length() : email.lastIndexOf("@");
            String[] names = email.substring(0, index).split("\\.");
            requestBean.setString("user", names[0].concat(".").concat(names[1]));
            ps.setString(++kk, names[0]);
            ps.setString(++kk, names[1]);
            ps.setString(++kk, email);
            ps.setString(++kk, email);
//            ps.setInt(++kk, 1);
            ps.setString(++kk, requestBean.getString("refresh-token"));
            ps.setString(++kk, APP_CODE);
            try {

                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    getRole(requestBean, cnn);
                    LOG.info("writing to users  succeeded");
                    success = true;
                } else {

                    LOG.info("unable to write to users");
                    cnn.rollback();
                    LOG.info("done with rollback");

                }
            } catch (SQLIntegrityConstraintViolationException ex) {
                LOG.error("User already present: " + ex.getMessage());
                LOG.error("Updating user login time: " + ex.getMessage());
                success = updateUserTime(requestBean);

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

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

    private static boolean updateUserTime(BaseBean requestBean) {

        String query = "UPDATE ".concat(USERS_TABLE).concat(" SET  last_login_date = sysdate, refresh_token = ? WHERE username = ? and app_code = ?");
        LOG.info("QUERY: " + query);
        PreparedStatement ps = null;
        Connection cnn = ConnectionUtil.getConnection();

        boolean success = false;
        LOG.info("Updating user: " + requestBean.getString("username"));

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("refresh-token"));
            ps.setString(++kk, requestBean.getString("username"));
            ps.setString(++kk, APP_CODE);
            try {
                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    getRole(requestBean, cnn);
                    LOG.info("updated logged in  user");
                    success = true;
                } else {

                    LOG.info("unable to write to users");
                    cnn.rollback();
                    LOG.info("done with rollback");

                }

            } catch (Exception e) {

                LOG.error("", e);

            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage());

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


    public static boolean getRole(BaseBean requestBean, Connection cnn) {

        String query = "SELECT r.role_name FROM "
                .concat(USERS_TABLE)
                .concat(" u,").concat(AppConstants.DbTables.ROLES)
                .concat(" r where username = ? and u.role_id = r.id and u.app_code = ? and r.app_code = ?");

        LOG.info("Query: " + query);

        PreparedStatement ps = null;

        boolean success = false;
        LOG.info("updating DB with status");
        try {

            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("username"));
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, APP_CODE);
            try {
                int count = 0;
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    count++;
                    requestBean.setString("role_name", rs.getString("role_name"));
                }
                success = true;

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

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


    public static boolean getUsers(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();

        String idQuery = "and u.id = ? ";
        String query = "SELECT u.id, u.firstname, u.lastname, u.email, u.role_id, r.role_name, account_status FROM "
                .concat(USERS_TABLE)
                .concat(" u, ")
                .concat(AppConstants.DbTables.ROLES)
                .concat(" r where r.id = u.role_id and r.app_code = ? and u.app_code = ? ")
                .concat(requestBean.containsKey("id") ? idQuery : "")
                .concat(" ORDER BY u.date_created ");

        LOG.info("Query: " + query);
        PreparedStatement ps = null;

        boolean success = false;
        LOG.info("getting users from DB");
        try {

            int kk = 0;
            ps = cnn.prepareStatement(query);

            try {
                int usercount = 0;
                ps.setString(++kk, APP_CODE);
                ps.setString(++kk, APP_CODE);

                if (requestBean.containsKey("id"))
                    ps.setString(++kk, requestBean.getString("id"));

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    usercount++;
                    requestBean.setString("id_" + usercount, rs.getString("id"));
                    requestBean.setString("firstname_" + usercount, rs.getString("firstname"));
                    requestBean.setString("lastname_" + usercount, rs.getString("lastname"));
                    requestBean.setString("email_" + usercount, rs.getString("email"));
                    requestBean.setString("role_id_" + usercount, rs.getString("role_id"));
                    requestBean.setString("role_name_" + usercount, rs.getString("role_name"));
                    requestBean.setString("status_" + usercount, rs.getString("account_status"));

                }
                requestBean.setString("user_count", String.valueOf(usercount));
                success = true;
                getAllUsersCount(requestBean, cnn);

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

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

    private static void getAllUsersCount(BaseBean requestBean, Connection cnn) {
        String query = "SELECT COUNT(id) as count FROM "
                .concat(USERS_TABLE)
                .concat(" u where u.app_code = ?");
        PreparedStatement ps = null;
        LOG.info("Query: " + query);

        boolean success = false;
        LOG.info("counting users from DB");
        try {

            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, APP_CODE);

            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_users", rs.getString("count"));
                }
                success = true;

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

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

    }

    public static boolean getRoles(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();

        String query = "SELECT * FROM "
                .concat(AppConstants.DbTables.ROLES)
                .concat(" r WHERE r.app_code = ? ");

        PreparedStatement ps = null;

        boolean success = false;
        LOG.info("getting roles from DB");
        try {

            int kk = 0;

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, APP_CODE);

            try {
                int rolesCount = 0;
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    rolesCount++;
                    requestBean.setString("id_" + rolesCount, rs.getString("id"));
                    requestBean.setString("role_name_" + rolesCount, rs.getString("role_name"));

                }
                requestBean.setString("role_count", String.valueOf(rolesCount));
                success = true;

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

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

    public static boolean fetchUserDetailsWithUsername(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();

        String query = "SELECT * FROM "
                .concat(USERS_TABLE)
                .concat(" r WHERE r.app_code = ? AND r.username = ?");

        PreparedStatement ps = null;

        boolean success = false;
        LOG.info("getting user from DB");
        try {

            int kk = 0;

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, requestBean.getString("username"));

            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("id" , rs.getString("id"));
                    requestBean.setString("firstname" , rs.getString("firstname"));
                    requestBean.setString("lastname" , rs.getString("lastname"));
                    requestBean.setString("email" , rs.getString("email"));
                    requestBean.setString("role_id" , rs.getString("role_id"));
                }
                success = true;

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

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

    public static List<String> fetchAllUsersWithRole(String roleName, BaseBean requestBean) {
        String query = "SELECT email FROM "
                .concat(USERS_TABLE)
                .concat(" u inner join ")
                .concat(ROLES)
                .concat(" r on r.role_name = ? AND r.id = u.role_id and u.app_code = ?")
                .concat(" AND r.app_code = ?");


        List<String> emails = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, roleName);
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, APP_CODE);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    emails.add(rs.getString("email"));
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
            ConnectionUtil.closeConnection(cnn);
        }
        return emails;
    }

    public static Boolean updateUserRole(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();
        String query = "UPDATE ".concat(USERS_TABLE)
                .concat(" set  date_modified = sysdate, modified_by = ? ");
        if (requestBean.containsKey("new_role_id") && !requestBean.getString("new_role_id").isEmpty()) {
            query = query.concat(", role_id = ?");
        }
        if (requestBean.containsKey("email") && !requestBean.getString("email").isEmpty()) {
            query = query.concat(", email = ?");
        }
        if (requestBean.containsKey("firstname") && !requestBean.getString("firstname").isEmpty()) {
            query = query.concat(", firstname = ?");
        }
        if (requestBean.containsKey("lastname") && !requestBean.getString("lastname").isEmpty()) {
            query = query.concat(", lastname = ?");
        }
        if (requestBean.containsKey("username") && !requestBean.getString("username").isEmpty()) {
            query = query.concat(", username = ?");
        }

        query = query.concat(" WHERE id = ? and app_code = ?");

        LOG.info("Query: " + query);

        PreparedStatement ps = null;

        boolean success = false;
        LOG.info("updating user in user table");
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("modified_by"));
            if (requestBean.containsKey("new_role_id") && !requestBean.getString("new_role_id").isEmpty()) {
                ps.setString(++kk, requestBean.getString("new_role_id"));
            }
            if (requestBean.containsKey("email") && !requestBean.getString("email").isEmpty()) {
                ps.setString(++kk, requestBean.getString("email"));
            }
            if (requestBean.containsKey("firstname") && !requestBean.getString("firstname").isEmpty()) {
                ps.setString(++kk, requestBean.getString("firstname"));
            }
            if (requestBean.containsKey("lastname") && !requestBean.getString("lastname").isEmpty()) {
                ps.setString(++kk, requestBean.getString("lastname"));
            }
            if (requestBean.containsKey("username") && !requestBean.getString("username").isEmpty()) {
                ps.setString(++kk, requestBean.getString("username"));
            }
            ps.setString(++kk, requestBean.getString("user_id"));
            ps.setString(++kk, APP_CODE);

            try {

                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    LOG.info("writing to users  succeeded");
                    success = true;
                } else {
                    //check if app exists
                    if (!isUserIdPresent(requestBean.getString("user_id"))) {
                        requestBean.setString("message", "User ID not found");
                    }
                    LOG.info("unable to write to users");
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

    private static boolean isUserIdPresent(String appl_id) {
        return false;
    }

    public static boolean verifyUserCredentials(BaseBean requestBean) {
        boolean isUserCredentialsVerified = false;
        String query = "SELECT email FROM "
                .concat(USERS_TABLE)
                .concat(" u WHERE u.username = ? AND u.refresh_token = ? AND u.app_code = ?");
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("username"));
            ps.setString(++kk, requestBean.getString("refresh-token"));
            ps.setString(++kk, APP_CODE);


            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    isUserCredentialsVerified = true;
                } else {
                    requestBean.setString("message", "unable to verify refresh token");
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
        return isUserCredentialsVerified;
    }


    public static List<String> fetchServiceId(BaseBean requestBean) {
        String query = "select service_id from "
                .concat(SERVICEID_TABLE)
                .concat(" s where s.del_flg = 'N'");
        List<String> serviceIds = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            ps = cnn.prepareStatement(query);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    serviceIds.add(rs.getString("service_id"));
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
            ConnectionUtil.closeConnection(cnn);
        }

        return serviceIds;
    }
    public static List<String> fetchChannelIds(BaseBean requestBean) {
        String query = "select appl_code from "
                .concat(CHANNEL_TABLE)
                .concat(" a where a.auth_flg = 'Y' and (a.del_flg is null or a.del_flg = 'N')");
        List<String> channels = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            ps = cnn.prepareStatement(query);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    channels.add(rs.getString("appl_code"));
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
            ConnectionUtil.closeConnection(cnn);
        }
        return channels;
    }
    public static List<String> fetchInstitutionCodes(BaseBean requestBean) {
        String query = "  select institutioncode from "
                .concat(INST_ROUTE_TABLE)
                .concat(" where del_flg = 'N'");
        List<String> institutions = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;

        try {
            ps = cnn.prepareStatement(query);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    institutions.add(rs.getString("institutioncode"));
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
            ConnectionUtil.closeConnection(cnn);
        }
        return institutions;
    }
    public static List<String> fetchSwitchCodes (BaseBean requestBean) {
        String query = " select code from "
                .concat(SWITCH_list_TABLE)
                .concat(" where isactive = 'Y'");
        List<String> switchCodes = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;

        try {
            ps = cnn.prepareStatement(query);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    switchCodes.add(rs.getString("code"));
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
            ConnectionUtil.closeConnection(cnn);
        }
        return switchCodes;
    }


    public static boolean hasRightPermission(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();
        String query = "select sr.method, sr.url from "
                .concat(USERS_TABLE).concat(" u inner join ")
                .concat(ROLE_PERMISSION)
                .concat(" p on u.role_id = p.role_id AND  p.app_code = ? AND u.app_code = ? inner join ")
                .concat(PERMISSION_RESOURCE)
                .concat(" pr on pr.permission_id = p.permission_id and pr.app_code = ? inner join ")
                .concat(SWITCH_RESOURCE)
                .concat(" sr on sr.id = pr.resources_id and sr.app_code = ? where sr.method = ? and sr.url= ? and u.username=?");

        PreparedStatement ps = null;
        boolean response = false;
        LOG.info("Query: {}", query);
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, requestBean.getString("method"));
            ps.setString(++kk, requestBean.getString("url"));
            ps.setString(++kk, requestBean.getString("email"));

            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response = true;
                } else {
                    requestBean.setString("message", "Not authorized");
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
        return response;
    }


    public static List<String> fetchUserNameIds(BaseBean requestBean) {
        String query = "select USR_ID from "
                .concat(SERVICE_ID_TABLE)
                .concat(" a where a.DEL_FLG = 'N'");
        List<String> channels = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            ps = cnn.prepareStatement(query);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    channels.add(rs.getString("USR_ID"));
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
            ConnectionUtil.closeConnection(cnn);
        }
        return channels;
    }


    public static List<String> fetchNextActionCodes(BaseBean requestBean) {
        String query = "select action_code from "
                .concat(NXT_ACTION_TABLE)
                .concat(" a where a.DEL_FLG = 'N'");
        List<String> channels = new ArrayList<>();
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {
            ps = cnn.prepareStatement(query);

            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    channels.add(rs.getString("action_code"));
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
            ConnectionUtil.closeConnection(cnn);
        }
        return channels;
    }

    public static boolean validateToken(BaseBean requestBean, String request) {

        String query = "SELECT token FROM "
                .concat(TOKEN)
                .concat(" where username = ? and current_date > token_iss_date and current_date < token_exp_date and token = ?");

        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean response = false;
        LOG.info("Query: " + query);
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("username"));
            ps.setString(++kk, requestBean.getString("token"));

            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response = true;
                } else {
                    requestBean.setString("message", "Invalid Token");
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
        return true;
    }

    public static boolean createDeleteArchive(BaseBean deleteBean, Connection cnn) {
        String query = "INSERT INTO ".concat(DEL_ARCHIVE)
                .concat(" ( MODULE, DEL_ID, DEL_DATE, DEL_BY, DATA) VALUES( ?, ?, sysdate, ?, ?)");
        boolean success = false;

        LOG.info("Creating Deletion Archive: {}", query);
        PreparedStatement ps = null;

        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, deleteBean.getString("module_name"));
            ps.setString(++kk, deleteBean.getString("del_id"));
            ps.setString(++kk, deleteBean.getString("del_by"));
            ps.setString(++kk, deleteBean.getString("del_data"));

            try {

                if (ps.executeUpdate() > 0) {
                    success = true;
                    LOG.info("writing to deletion archive table: ");
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to deletion archive table:");
                    cnn.rollback();
                    LOG.info("done with rollback");
                }

            } catch (SQLException e) {
                deleteBean.setString("message", e.getMessage());
                LOG.error("", e);
            }

        } catch (Exception e) {
            deleteBean.setString("message", e.getMessage());
            LOG.error("", e);
        }
        return success;
    }

    public static boolean createUser(BaseBean requestBean, String request) {
        Connection cnn = ConnectionUtil.getConnection();
        String query = "INSERT INTO ".concat(USERS_TABLE)
                .concat("( FIRSTNAME, LASTNAME, EMAIL, ROLE_ID, DATE_CREATED, DATE_MODIFIED, MODIFIED_BY, APP_CODE, USERNAME, ACCOUNT_STATUS) ")
                .concat("VALUES( ?, ?, ?, ?, sysdate, sysdate, ?, ?, ?, 'Y')");
        PreparedStatement ps = null;
        LOG.info("Query: " + query);

        boolean success = false;
        LOG.info("Creating user in user table");
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("firstName"));
            ps.setString(++kk, requestBean.getString("lastName"));
            ps.setString(++kk, requestBean.getString("email").toLowerCase());
            ps.setString(++kk, requestBean.getString("role"));
            ps.setString(++kk, requestBean.getString("logged_user"));
            ps.setString(++kk, APP_CODE);
            ps.setString(++kk, requestBean.getString("username"));

            try {
                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    LOG.info("writing to users  succeeded");
                    success = true;
                } else {
                    //check if app exists
                    if (!isUserIdPresent(requestBean.getString("user_id"))) {
                        requestBean.setString("message", "User ID not found");
                    }
                    LOG.info("unable to write to users");
                    cnn.rollback();
                    LOG.info("done with rollback");

                }
            }catch (SQLIntegrityConstraintViolationException ex) {
                requestBean.setString("message", "user already exists with email " + requestBean.getString("email"));
                LOG.error("", ex);

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

    public static boolean updateUser(BaseBean requestBean, String request) {
        Connection cnn = ConnectionUtil.getConnection();
        StringBuilder sb = new StringBuilder("UPDATE ")
                .append(USERS_TABLE)
                .append(" SET");

        boolean setStatus = false;
        if (requestBean.containsKey("firstname")) {
            sb.append("  firstname = ? ");
            setStatus = true;
        }
        if (requestBean.containsKey("lastname")) {
            if (setStatus) {
                sb.append(", ");
            }
            sb.append("  lastname = ? ");
        }
        if (requestBean.containsKey("email")) {
            if (setStatus) {
                sb.append(", ");
            }
            sb.append("  email = ? ");
        }
        if (requestBean.containsKey("role")) {
            if (setStatus) {
                sb.append(", ");
            }
            sb.append("  role_id = ? ");
        }
        if (requestBean.containsKey("status")) {
            if (setStatus) {
                sb.append(", ");
            }
            sb.append("  account_status = ? ");
        }
        sb.append(" WHERE id = ? and APP_CODE = ?");
        String query = sb.toString();
        PreparedStatement ps = null;
        LOG.info("Query: " + query);

        boolean success = false;
        LOG.info("Updating user in user table");
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            if (requestBean.containsKey("firstname")) {
                ps.setString(++kk, requestBean.getString("firstName"));
            }

            if (requestBean.containsKey("lastName")) {
                ps.setString(++kk, requestBean.getString("lastName"));
            }

            if (requestBean.containsKey("email")) {
                ps.setString(++kk, requestBean.getString("email"));
            }

            if (requestBean.containsKey("role")) {
                ps.setString(++kk, requestBean.getString("role"));
            }

            if (requestBean.containsKey("status")) {
                ps.setString(++kk, requestBean.getString("status"));
            }
            ps.setString(++kk, requestBean.getString("id"));
            ps.setString(++kk, APP_CODE);

            try {

                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    LOG.info("writing to users  succeeded");
                    success = true;
                } else {
                    //check if app exists
                    requestBean.setString("message", "User ID not found");
                    LOG.info("unable to write to users");
                    cnn.rollback();
                    LOG.info("done with rollback");
                }
            } catch (SQLIntegrityConstraintViolationException ex) {
                requestBean.setString("message", "User already exists with email " + requestBean.getString("email"));
                LOG.error("", ex);
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

    public static boolean checkUserDetails(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();
        String query = "SELECT * FROM "
                .concat(USERS_TABLE)
                .concat(" WHERE ( email = ? or username = ? ) and APP_CODE = ? AND account_status = 'Y'");

        PreparedStatement ps = null;
        boolean response = false;
        LOG.info("Query: {}", query);
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("username"));
            ps.setString(++kk, requestBean.getString("username"));
            ps.setString(++kk, APP_CODE);

            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response = true;
                } else {
                    requestBean.setString("message", "User details not found, please contact admin");
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
        return response;
    }


    public static boolean writeInflowToRetrialTable(BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();

        String countSubQuery = getServiceType_Query(requestBean.get("service_type"));
        String query = "INSERT INTO ESBUSER.POSTING_RETRIAL " +
                "(batch_count, service_type, retrial_start_date, retrial_end_date, batch_id, created_by, creation_date, status, switch_type, module) " +
                "SELECT (" +
                "SELECT COUNT(TranID) " + countSubQuery +
                ") AS batch_count, " +
                "? AS service_type, TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AS retrial_start_date, " +
                "TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AS retrial_end_date, " +
                "? AS batch_id, ? AS created_by, SYSDATE AS creation_date, " +
                "? AS status, ? AS switch_type, ? AS module FROM dual";

        PreparedStatement ps = null;
        boolean success = false;
        LOG.info("Query: {}", query);
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, "FTSingleCreditRequest");
            ps.setString(++kk, "00");
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
            ps.setString(++kk, requestBean.getString("service_type"));
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
            ps.setString(++kk, requestBean.getString("batch_id"));
            ps.setString(++kk, requestBean.getString("created_by"));
            ps.setString(++kk, "PENDING");
            ps.setString(++kk, requestBean.getString("switch_type"));
            ps.setString(++kk, requestBean.getString("module"));

            try {
                if (ps.executeUpdate() > 0) {
                    LOG.info("writing to posting retrial inflow succeeded");
                    success = writeToInflw_V2Table(requestBean, cnn);
                }

            } catch (Exception e) {
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



    public static boolean writeToInflw_V2Table(BaseBean requestBean, Connection cnn) {
        PreparedStatement ps = null;
        boolean response = false;

        try {
            String serviceType = requestBean.getString("service_type");
            String query = "UPDATE ESBUSER.NIP_IN_FLW_V2 " +
                    "SET BATCH_ID = ? " +
                    "WHERE TRANID IN (SELECT TranID FROM ESBUSER.NIP_IN_FLW_V2 ";

            if ("POSTING".equals(serviceType)) {
                query += "WHERE TRANTYPE = ? " +
                        "AND TSQ_2_RSP_CODE = ? " +
                        "AND TSQ_2_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') " +
                        "AND (C24_RSP_FLG = 'N' OR C24_RSP_CODE NOT IN ('000','913')) " +
                        "AND TXN_POSTING_FALLBACK_FLG = 'N' " +
                        "AND CLIENTNAME = ? " +
                        "AND TRANID > 0)";
            } else if ("TSQ".equals(serviceType)) {
                query += "WHERE TRANTYPE = ? " +
                        "AND RESPONSECODE = ? " +
                        "AND RESPONSEDATE BETWEEN TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') " +
                        "AND (TSQ_2_FLG = 'N' OR TSQ_2_RSP_CODE IN ('97', '99','25')) " +
                        "AND TSQ_FALLBACK_FLG = 'N' " +
                        "AND CLIENTNAME = ? " +
                        "AND TRANID > 0)";
            } else {
                requestBean.setString("message", "Invalid service type provided.");
                return false;
            }

            LOG.info("Query: {}", query);
            ps = cnn.prepareStatement(query);

            int paramIndex = 1;

            ps.setString(paramIndex++, requestBean.getString("batch_id"));
            ps.setString(paramIndex++, "FTSingleCreditRequest");
            ps.setString(paramIndex++, "00");
            ps.setString(paramIndex++, requestBean.getString("start_date"));
            ps.setString(paramIndex++, requestBean.getString("end_date"));
            ps.setString(paramIndex++, requestBean.getString("switch_type").toUpperCase());

            ps.executeUpdate();
            response = true;

            requestBean.setString("message", "Batch Created.");
            cnn.commit();

        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("Error updating NIP_INFLW_V2 table", e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Rollback failed", rollbackEx);
            }

        }

        return response;
    }



    public static String getServiceType_Query(String serviceType) {
        String query = "";
        if (serviceType.equalsIgnoreCase("POSTING")) {
            query = "from esbuser.nip_in_flw_v2 where  TRANTYPE=? and tsq_2_rsp_code =? " +
                    "and tsq_2_date between TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') and (c24_rsp_flg='N' OR c24_rsp_code NOT IN ('000','913')) " +
                    "and txn_posting_fallback_flg='N' and tranid > 0";
        }

        if (serviceType.equalsIgnoreCase("TSQ")) {
            query = "from esbuser.nip_in_flw_v2 where  trantype=? and responsecode =? " +
                    "and responsedate between TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') and (tsq_2_flg='N' OR tsq_2_rsp_code in ('97', '99','25')) " +
                    "and tsq_fallback_flg='N' and TranID > 0";
        }
        return query;
    }


    public static boolean writeToRetrialTableOutflow(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(POSTING_RETRIAL)
                .concat("(batch_count, service_type, retrial_start_date, retrial_end_date, batch_id, created_by, creation_date, status, switch_type, module) VALUES ")
                .concat(" (?, ?, TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS'), TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS'), ?, ?, sysdate, ?, ?, ? )");
        requestBean.setString("operation_type", requestBean.getString("service_type"));
        TransactionsDbHelper.fetchTotalOutflowRecordCount(requestBean);
        String count = requestBean.getString("total_count");
        LOG.info("Outflow count: {}", count);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        LOG.info("Writitng to Outflow retrial Table: {}", query);
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, count);
            ps.setString(++kk, requestBean.getString("service_type"));
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
            ps.setString(++kk, requestBean.getString("batch_id"));
            ps.setString(++kk, requestBean.getString("created_by"));
            ps.setString(++kk, "PENDING");
            ps.setString(++kk, requestBean.getString("switch_type"));
            ps.setString(++kk, requestBean.getString("module"));

            try {
                if (ps.executeUpdate() > 0) {
                    LOG.info("writing to posting retrial  outflow succeeded");
                    success = updateOutflowServiceTable(requestBean, cnn);
                }

            } catch (Exception e) {
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

    private static boolean updateOutflowServiceTable(BaseBean requestBean, Connection cnn) {
        PreparedStatement ps = null;
        boolean response = false;

        try {
            String query = "UPDATE "
                    .concat(UP_OUTFLOW_TRANSACTION)
                    .concat(" SET BATCH_ID = ?  WHERE sessionid IN (SELECT sessionid  ")
                    .concat(TransactionsDbHelper.createPendingOutflowTransactionQuery(requestBean))
                    .concat(" )");

            LOG.info("Query: {}", query);
            ps = cnn.prepareStatement(query);
            int paramIndex = 1;
            ps.setString(paramIndex++, requestBean.getString("batch_id"));
            ps.setString(paramIndex++, requestBean.getString("start_date"));
            ps.setString(paramIndex++, requestBean.getString("end_date"));
            ps.executeUpdate();
            response = true;
            requestBean.setString("message", "Batch Created.");
            cnn.commit();
        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("Error updating Outflow table", e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Rollback failed", rollbackEx);
            }

        }

        return response;
    }

    public static boolean writeToRetrialTableAirtime(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(POSTING_RETRIAL)
                .concat("(batch_count, service_type, retrial_start_date, retrial_end_date, batch_id, created_by, creation_date, status, switch_type, module) VALUES ")
                .concat(" (?, ?, TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS'), TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS'), ?, ?, sysdate, ?, ?, ? )");
        requestBean.setString("operation_type", requestBean.getString("service_type"));
        TransactionsDbHelper.fetchAirtimeTotalRecordCount(requestBean);
        String count = requestBean.getString("total_count");

        LOG.info("Airtime count: {}", count);
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        LOG.info("Writitng to Airitme retrial Table: {}", query);
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, count);
            ps.setString(++kk, requestBean.getString("service_type"));
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
            ps.setString(++kk, requestBean.getString("batch_id"));
            ps.setString(++kk, requestBean.getString("created_by"));
            ps.setString(++kk, "PENDING");
            ps.setString(++kk, requestBean.getString("switch_type"));
            ps.setString(++kk, requestBean.getString("module"));

            try {
                if (ps.executeUpdate() > 0) {
                    LOG.info("writing to posting airtime  outflow succeeded");
                    success = updateAirtimeServiceTable(requestBean, cnn);
                }

            } catch (Exception e) {
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

    private static boolean updateAirtimeServiceTable(BaseBean requestBean, Connection cnn) {
        PreparedStatement ps = null;
        boolean response = false;

        try {
            String query = "UPDATE "
                    .concat(AIRTIME_TABLE)
                    .concat(" SET BATCH_ID = ? WHERE TOPUP_REF_ID IN (SELECT TOPUP_REF_ID  ")
                    .concat(TransactionsDbHelper.createPendingAirtimeTransactionQuery(requestBean))
                    .concat(" )");

            LOG.info("Query: {}", query);
            ps = cnn.prepareStatement(query);
            int paramIndex = 1;
            ps.setString(paramIndex++, requestBean.getString("batch_id"));
            ps.setString(paramIndex++, requestBean.getString("start_date"));
            ps.setString(paramIndex++, requestBean.getString("end_date"));
            ps.executeUpdate();
            response = true;
            requestBean.setString("message", "Batch Created.");
            cnn.commit();
        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("Error updating Airtime table", e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Rollback failed", rollbackEx);
            }

        }

        return response;
    }
}
