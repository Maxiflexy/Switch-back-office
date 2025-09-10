package persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SwitchAlgorithmDbHelper implements OutflowSwitchRequest {

    final static Logger LOG = LogManager.getLogger(SwitchAlgorithmDbHelper.class);

    @Override
    public boolean createModuleRequest(BaseBean requestBean, Connection cnn) {
        String query = "INSERT INTO "
                .concat(SWITCH_ALG_MC)
                .concat(" (name, code, type, failure_count, failure_time, measurement_period, request_id) " +
                        "values (?, ?, ?, ?, ?, ?, esbuser.outflow_switch_seq.currval)");
        PreparedStatement ps = null;
        boolean success = false;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("name"));
            ps.setString(++kk, requestBean.getString("code"));
            ps.setString(++kk, requestBean.getString("type"));
            ps.setString(++kk, requestBean.getString("failure_count"));
            ps.setString(++kk, requestBean.getString("failure_time"));
            ps.setString(++kk, requestBean.getString("measurement_period"));
            try {
                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    success = true;
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

    @Override
    public boolean updateModuleRequest(BaseBean request, Connection connection) {

        return false;
    }

    @Override
    public boolean deactivateModuleRequest(BaseBean request, Connection connection) {

        return false;
    }

    @Override
    public boolean approveModuleRequest(BaseBean requestBean, Connection connection) {
        BaseBean unapprovedAlgorithm = findUnapprovedById(Long.parseLong(requestBean.getString("id")));
        if (unapprovedAlgorithm.isEmpty()) {
            throw new IllegalArgumentException("Unapproved request not found");
        }
        Connection cnn = ConnectionUtil.getConnection();
        String query = "";
        if (unapprovedAlgorithm.getString("action").equalsIgnoreCase("create")) {
            query = "UPDATE "
                    .concat(SWITCH_ALG_MC)
                    .concat(" SET approval_date = sysdate, approved_by = ?, approval_message = ?, status = ? where id = ?");

        } else {
            throw new IllegalArgumentException("Invalid action");
        }
        PreparedStatement ps = null;
        boolean success = false;
        boolean isApproved = requestBean.getString("status").equalsIgnoreCase("approved");
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("user"));
            ps.setString(++kk, requestBean.getString("message"));
            ps.setString(++kk, unapprovedAlgorithm.getString("id"));
            ps.setString(++kk, isApproved ? "APPROVED" : "REJECTED");

            try {
                if (ps.executeUpdate() > 0) {
                    if (isApproved) {
                        success = createApprovedAlgorithm(requestBean, unapprovedAlgorithm, cnn);
                    } else {
                        success = true;
                        cnn.commit();
                    }
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to  switch request table");
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

    private boolean createApprovedAlgorithm(BaseBean requestBean, BaseBean unapprovedAlgorithm, Connection cnn) {

        String query = "INSERT INTO "
                .concat(SWITCH_ALG)
                .concat(" (name, code, type, failure_time, failure_count, measurement_period, del_status, created_by, created_at, approved_by, approval_date )")
                .concat(" SELECT sa.name, sa.code, sa.type, sa.failure_time, sa.failure_count, sa.measurement_period, 'N' so.created_by, so.created_at, so.approved_by, so.approval_date FROM ")
                .concat(SWITCH_OUTFLOW_REQUEST)
                .concat(" so INNER JOIN ")
                .concat(SWITCH_ALG_MC)
                .concat(" sa ON so.id = sa.request_id where so.id = ?");

        PreparedStatement ps = null;
        boolean success = false;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, unapprovedAlgorithm.getString("id"));

            try {
                if (ps.executeUpdate() > 0) {
                    success = true;
                    cnn.commit();
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to switch algorithm table");
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


    public BaseBean findApprovedById(long id) {
        String query = "SELECT * FROM "
                .concat(SWITCH_ALG)
                .concat(" WHERE id = ?");
        BaseBean algorithmBean = new BaseBean();
        PreparedStatement ps = null;


        LOG.info("Fetching outflow switch {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        try {
            ps = cnn.prepareStatement(query);
            int kk = 0;
            ps.setLong(++kk, id);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    algorithmBean.put("id", rs.getString("id"));
                    algorithmBean.put("name", rs.getString("name"));
                    algorithmBean.put("code", rs.getString("code"));
                    algorithmBean.put("created_by", rs.getString("created_by"));
                    algorithmBean.put("created_at", rs.getString("created_at"));
                    algorithmBean.put("approved_by", rs.getString("approved_by"));
                    algorithmBean.put("del_status", rs.getString("del_status"));
                    algorithmBean.put("del_by", rs.getString("del_by"));
                    algorithmBean.put("del_date", rs.getString("del_date"));
                }
            } catch (SQLException e) {
                LOG.error("", e);
            }

        } catch (Exception e) {
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
        return algorithmBean;

    }

    public BaseBean findUnapprovedById(long id) {
        String query = "SELECT * FROM "
                .concat(SWITCH_ALG_MC)
                .concat(" WHERE id = ?");
        BaseBean algorithmBean = new BaseBean();
        PreparedStatement ps = null;


        LOG.info("Fetching outflow unapproved switch {}", query);
        Connection cnn = ConnectionUtil.getConnection();
        try {
            ps = cnn.prepareStatement(query);
            int kk = 0;
            ps.setLong(++kk, id);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    algorithmBean.put("id", rs.getString("id"));
                    algorithmBean.put("name", rs.getString("name"));
                    algorithmBean.put("code", rs.getString("code"));
                    algorithmBean.put("created_by", rs.getString("created_by"));
                    algorithmBean.put("created_at", rs.getString("created_at"));
                    algorithmBean.put("approved_by", rs.getString("approved_by"));
                    algorithmBean.put("approval_date", rs.getString("approval_date"));
                    algorithmBean.put("action", rs.getString("action"));
                    algorithmBean.put("status", rs.getString("status"));
                }
            } catch (SQLException e) {
                LOG.error("", e);
            }

        } catch (Exception e) {
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
        return algorithmBean;

    }

    public static boolean fetchApprovedRequests(BaseBean requestBean, Connection cnn) {
        String query = "SELECT * FROM "
                .concat(SWITCH_ALG)
                .concat(" where id is not null");

        query = query.concat(createApprovedFetchQueryString(requestBean));

        if (requestBean.getString("size").isEmpty()) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").isEmpty()) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));
        query = query.concat(" ORDER BY created_at DESC");
        query = query.concat(" OFFSET ")
                .concat(offset).concat(" ROWS FETCH NEXT ")
                .concat(limit).concat(" ROWS ONLY");
        boolean success = false;
        PreparedStatement ps = null;


        LOG.info("Fetching outflow switch {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createApprovedStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> transactions = new ArrayList<>();
                while (rs.next()) {
                    BaseBean documentBean = new BaseBean();
                    try {
                        documentBean.put("id", rs.getString("id"));
                        documentBean.put("name", rs.getString("name"));
                        documentBean.put("code", rs.getString("code"));
                        documentBean.put("created_by", rs.getString("created_by"));
                        documentBean.put("created_at", rs.getString("created_at"));
                        documentBean.put("approved_by", rs.getString("approved_by"));
                        documentBean.put("del_status", rs.getString("del_status"));
                        documentBean.put("del_by", rs.getString("del_by"));
                        documentBean.put("del_date", rs.getString("del_date"));
                        transactions.add(documentBean);
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(transactions));
                fetchTotalApprovedRecordCount(requestBean);
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

    private static void createApprovedStatementVariables(PreparedStatement ps, BaseBean requestBean) throws SQLException {
        int kk = 0;
        if (requestBean.containsKey("id")) {
            ps.setString(++kk, requestBean.getString("id"));
        }

        if (requestBean.containsKey("start_date") && requestBean.containsKey("end_date")) {
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
        }

    }

    private static String createApprovedFetchQueryString(BaseBean requestBean) {
        String query = "";
        if (requestBean.containsKey("id")) {
            query = query.concat(" AND sa.id = ? ");
        }
        if (requestBean.containsKey("start_date") && requestBean.containsKey("end_date")) {
            query = query.concat(" AND created_at between  ? and ? ");
        }
        return query;
    }

    private static void fetchTotalApprovedRecordCount(BaseBean requestBean) {
        String query = "SELECT COUNT(*) as count FROM "
                .concat(SWITCH_ALG);

        query = query.concat(createApprovedFetchQueryString(requestBean));
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        try {
            ps = cnn.prepareStatement(query);
            createApprovedStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_count", rs.getString("count"));
                }
                success = true;
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

    }

    public static boolean fetchSwitchAlgorithmRequest(BaseBean requestBean) {

        Connection cnn = ConnectionUtil.getConnection();
        if (requestBean.containsKey("status") && requestBean.getString("status").equals("APPROVED")) {
            return fetchApprovedRequests(requestBean, cnn);
        }

        String query = "SELECT sa.id, sa.name, sa.code, so.created_by, so.created_at, so.approved_by, so.approval_date, so.action, so.status FROM "
                .concat(SWITCH_OUTFLOW_REQUEST)
                .concat(" so INNER JOIN ")
                .concat(SWITCH_ALG_MC)
                .concat(" sa ON so.id = sa.request_id where so.module = ?");

        query = query.concat(createFetchQueryString(requestBean));

        if (requestBean.getString("size").isEmpty()) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").isEmpty()) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));
        query = query.concat(" ORDER BY created_at DESC");
        query = query.concat(" OFFSET ")
                .concat(offset).concat(" ROWS FETCH NEXT ")
                .concat(limit).concat(" ROWS ONLY");
        boolean success = false;
        PreparedStatement ps = null;


        LOG.info("Fetching outflow switch {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> transactions = new ArrayList<>();
                while (rs.next()) {
                    BaseBean documentBean = new BaseBean();
                    try {
                        documentBean.put("id", rs.getString("id"));
                        documentBean.put("name", rs.getString("name"));
                        documentBean.put("code", rs.getString("code"));
                        documentBean.put("created_by", rs.getString("created_by"));
                        documentBean.put("created_at", rs.getString("created_at"));
                        documentBean.put("approved_by", rs.getString("approved_by"));
                        documentBean.put("approval_date", rs.getString("approval_date"));
                        documentBean.put("action", rs.getString("action"));
                        documentBean.put("status", rs.getString("status"));
                        transactions.add(documentBean);
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(transactions));
                fetchTotalRecordCount(requestBean);
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

    private static void createStatementVariables(PreparedStatement ps, BaseBean requestBean) throws SQLException {
        int kk = 0;
        ps.setString(++kk, requestBean.getString("module"));
        if (requestBean.containsKey("id")) {
            ps.setString(++kk, requestBean.getString("id"));
        }

        if (requestBean.containsKey("start_date") && requestBean.containsKey("end_date")) {
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
        }

        if (requestBean.containsKey("status")) {
            ps.setString(++kk, requestBean.getString("status"));
        }

    }

    private static void fetchTotalRecordCount(BaseBean requestBean) throws SQLException {
        String query = "SELECT count(sa.id) as count FROM "
                .concat(SWITCH_OUTFLOW_REQUEST)
                .concat(" so INNER JOIN ")
                .concat(SWITCH_ALG_MC)
                .concat(" sa ON so.id = sa.request_id where so.module = ?");

        query = query.concat(createFetchQueryString(requestBean));
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        boolean success = false;
        try {
            ps = cnn.prepareStatement(query);
            createStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_count", rs.getString("count"));
                }
                success = true;
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
    }

    private static String createFetchQueryString(BaseBean requestBean) {
        String query = "";
        if (requestBean.containsKey("id")) {
            query = query.concat(" AND sa.id = ? ");
        }
        if (requestBean.containsKey("start_date") && requestBean.containsKey("end_date")) {
            query = query.concat(" AND created_at between  ? and ? ");
        }
        if (requestBean.containsKey("status")) {
            query = query.concat(" AND status = ? ");
        }
        return query;
    }

}
