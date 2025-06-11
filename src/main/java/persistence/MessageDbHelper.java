package persistence;

import constants.AppModules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.MESSAGE_TABLE;

public class MessageDbHelper {

    final static Logger LOG = LogManager.getLogger(MessageDbHelper.class);

    public static boolean createMessage(BaseBean requestBean) {
        String query = "INSERT INTO "
                .concat(MESSAGE_TABLE)
                .concat(" (title, message, entry_date, user_id, app_module, created_by) VALUES (?, ?, sysdate, ?, ?, ?)");

        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        LOG.info("Inserting Message:: {}", query);
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("title"));
            ps.setString(++kk, requestBean.getString("message"));
            ps.setString(++kk, requestBean.getString("user-id"));
            ps.setString(++kk, requestBean.getString("app-module"));
            ps.setString(++kk, requestBean.getString("created-by"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("writing to message table ");

                    success = true;
                    cnn.commit();

                } else {
                    //check if app has been verified
                    LOG.info("unable to write to channel maker checker");
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

    public static boolean updateReadStatus(BaseBean requestBean) {
        String query = "UPDATE "
                .concat(MESSAGE_TABLE)
                .concat(" m SET m.read_status = ? where m.user_id = ? and m.read_status = ?");

        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        LOG.info("Updating read Messages:: {}", query);
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, "Y");
            ps.setString(++kk, requestBean.getString("user-id"));
            ps.setString(++kk, "N");

            try {

                if (ps.executeUpdate() >= 0) {
                    LOG.info("writing to message table : ");

                    success = true;
                    cnn.commit();

                } else {
                    //check if app has been verified
                    LOG.info("unable to write to message table");
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


    public static boolean fetchMessages(BaseBean requestBean) {
        String query = "SELECT * FROM "
                .concat(MESSAGE_TABLE)
                .concat(" m where m.user_id = ? ORDER BY m.entry_date DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        String offset = "0";
        String limit = "10";

        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        LOG.info("fetching Messages :: {}", query);
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("user-id"));
            ps.setString(++kk, offset);
            ps.setString(++kk, limit);

            try {

                ResultSet result = ps.executeQuery();
                List<BaseBean> messages = new ArrayList<>();
                while (result.next()) {
                    BaseBean message = new BaseBean();

                    message.setString("id", result.getString("id"));
                    message.setString("read-status", result.getString("read_status"));
                    message.setString("entry-date", result.getString("entry_date"));
                    message.setString("user-id", result.getString("user_id"));
                    message.setString("app-module", result.getString("app_module"));
                    message.setString("created-by", result.getString("created_by"));
                    message.setString("message", result.getString("message"));

                    messages.add(message);
                }

                createJsonResponse(messages, requestBean);
                success = true;
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

    private static void createJsonResponse(List<BaseBean> messages, BaseBean requestBean) {
        JsonArrayBuilder jArr = Json.createArrayBuilder();

        for (BaseBean message : messages) {
            jArr.add(Json.createObjectBuilder()
                    .add("id", message.getString("id"))
                    .add("message", message.getString("message"))
                    .add("read-status", message.getString("read-status").equals("Y"))
                    .add("entry-date", message.getString("entry-date"))
                    .add("user-id", message.getString("user-id"))
                    .add("app-module", message.getString("app-module"))
                    .add("created-by", message.getString("created-by"))
            );
        }
        requestBean.setString("messages", JsonUtil.toStr(Json
                .createObjectBuilder()
                .add("data", jArr.build())
                .build()));
    }

    public static void createMessageHelper(String title, String message, String messageFor, AppModules moduleName, String createdBy) {

        BaseBean messageBean = new BaseBean();
        messageBean.setString("title", title);
        messageBean.setString("message", message);
        messageBean.setString("user-id", messageFor);
        messageBean.setString("app-module", moduleName.name());
        messageBean.setString("created-by", createdBy);

        MessageDbHelper.createMessage(messageBean);
    }
}