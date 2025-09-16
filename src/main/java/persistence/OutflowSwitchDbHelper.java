package persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.OutFlowSwitchFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static constants.AppConstants.DbTables.SWITCH_OUTFLOW_REQUEST;

public class OutflowSwitchDbHelper {

    final static Logger LOG = LogManager.getLogger(OutflowSwitchDbHelper.class);

    public static boolean createOutflowSwitchRequest(BaseBean requestBean) {

        Connection cnn = ConnectionUtil.getConnection();
        String query = "INSERT INTO "
                .concat(SWITCH_OUTFLOW_REQUEST)
                .concat(" ( id, module, created_by, created_at, action, status, old_request_id) ")
                .concat("VALUES (esbuser.outflow_switch_seq.nextval,?, ?, sysdate, ?, ?, ?)");

        boolean success = false;

        LOG.info("Creating Switch Algorithm request: {}", query);
        PreparedStatement ps = null;
        OutflowSwitchRequest operation = OutFlowSwitchFactory.createOutFlowSwitchRequest(requestBean.getString("module"));


        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("module"));
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("action"));
            ps.setString(++kk, "PENDING");
            ps.setString(++kk, requestBean.getString("id"));
            try {

                if (ps.executeUpdate() > 0) {
                    success = operation.createModuleRequest(requestBean, cnn);
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to Outflow switch request table");
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

    public static boolean approveSwitchRequest(BaseBean requestBean) {
        boolean success = false;
        try {
            Connection cnn = ConnectionUtil.getConnection();
            OutflowSwitchRequest operation = OutFlowSwitchFactory.createOutFlowSwitchRequest(requestBean.getString("module"));
            success = operation.approveModuleRequest(requestBean, cnn);
        } catch (Exception e) {
            LOG.error("", e);
            requestBean.setString("message", e.getMessage());
        }
        return success;
    }
}
