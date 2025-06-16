package persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.ConnectionUtil;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PendingTransactionDbHelper {

    final static Logger LOG = LogManager.getLogger(PendingTransactionDbHelper.class);

    public static boolean getPendingTransactions(BaseBean requestBean) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT n.PAYMENTREFERENCE, ");
        queryBuilder.append("TO_CHAR(n.REQUESTDATE, 'YYYY-MM-DD HH24:MI:SS') as REQUESTDATE, ");
        queryBuilder.append("n.ACCOUNTNUMBER, n.AMOUNT, n.BATCH_ID, n.NARRATION, ");
        queryBuilder.append("n.C24_RSP_CODE, COALESCE(b.ERR_DESC, '') as ERR_DESC ");
        queryBuilder.append("FROM ESBUSER.NIP_IN_FLW_V2 n ");
        queryBuilder.append("LEFT JOIN ESBUSER.BANCS_CONNECT_RESPONSE b ON n.C24_RSP_CODE = b.ERR_CODE ");
        queryBuilder.append("WHERE n.BATCH_ID = ?");

        StringBuilder countQueryBuilder = new StringBuilder();
        countQueryBuilder.append("SELECT COUNT(*) as total_count FROM ESBUSER.NIP_IN_FLW_V2 n ");
        countQueryBuilder.append("WHERE n.BATCH_ID = ?");

        List<Object> parameters = new ArrayList<>();

        // BATCH_ID is NUMBER(28,0) in database, so convert string to Long for proper parameter binding
        try {
            parameters.add(Long.parseLong(requestBean.getString("batch_id")));
        } catch (NumberFormatException e) {
            LOG.error("Invalid batch_id format: {}", requestBean.getString("batch_id"));
            requestBean.setString("message", "Invalid batch_id format. Must be a valid number.");
            return false;
        }

        // Handle TIMESTAMP date range filters with ISO format
        if (requestBean.containsKey("start_date") && !requestBean.getString("start_date").isEmpty()) {
            queryBuilder.append(" AND n.REQUESTDATE >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND n.REQUESTDATE >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("start_date"));
            LOG.info("Adding start date filter: {}", requestBean.getString("start_date"));
        }

        if (requestBean.containsKey("end_date") && !requestBean.getString("end_date").isEmpty()) {
            queryBuilder.append(" AND n.REQUESTDATE <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND n.REQUESTDATE <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("end_date"));
            LOG.info("Adding end date filter: {}", requestBean.getString("end_date"));
        }

        // Add sorting
        queryBuilder.append(" ORDER BY n.REQUESTDATE DESC");

        // Add pagination
        int page = 1;
        int size = 10;

        try {
            if (requestBean.containsKey("page") && !requestBean.getString("page").isEmpty()) {
                page = Integer.parseInt(requestBean.getString("page"));
            }
            if (requestBean.containsKey("size") && !requestBean.getString("size").isEmpty()) {
                size = Integer.parseInt(requestBean.getString("size"));
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid page or size parameter, using defaults");
        }

        int offset = (page - 1) * size;
        queryBuilder.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        String query = queryBuilder.toString();
        String countQuery = countQueryBuilder.toString();

        boolean success = false;
        Connection cnn = null;
        PreparedStatement ps = null;
        PreparedStatement countPs = null;
        ResultSet rs = null;
        ResultSet countRs = null;

        LOG.info("Executing query: {}", query);
        LOG.info("Parameters: {}", parameters);

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            // First get the total count
            int totalRows = 0;
            countPs = cnn.prepareStatement(countQuery);
            int paramIndex = 1;
            for (Object param : parameters) {
                if (param instanceof Long) {
                    countPs.setLong(paramIndex++, (Long) param);
                } else {
                    countPs.setObject(paramIndex++, param);
                }
            }

            countRs = countPs.executeQuery();
            if (countRs.next()) {
                totalRows = countRs.getInt("total_count");
            }
            LOG.info("Total rows found: {}", totalRows);

            // Calculate total pages
            int totalPages = (int) Math.ceil((double) totalRows / size);

            // Now get the actual data
            ps = cnn.prepareStatement(query);
            paramIndex = 1;
            for (Object param : parameters) {
                if (param instanceof Long) {
                    ps.setLong(paramIndex++, (Long) param);
                } else {
                    ps.setObject(paramIndex++, param);
                }
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, size);

            rs = ps.executeQuery();
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

            while (rs.next()) {
                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
                jsonBuilder.add("tran_ref", rs.getString("PAYMENTREFERENCE") != null ? rs.getString("PAYMENTREFERENCE") : "");
                jsonBuilder.add("tran_date", rs.getString("REQUESTDATE") != null ? rs.getString("REQUESTDATE") : "");
                jsonBuilder.add("acct_no", rs.getString("ACCOUNTNUMBER") != null ? rs.getString("ACCOUNTNUMBER") : "");
                jsonBuilder.add("tran_amt", rs.getString("AMOUNT") != null ? rs.getString("AMOUNT") : "");
                jsonBuilder.add("batch_id", rs.getString("BATCH_ID") != null ? rs.getString("BATCH_ID") : "");
                jsonBuilder.add("tran_narration", rs.getString("NARRATION") != null ? rs.getString("NARRATION") : "");
                jsonBuilder.add("response_code", rs.getString("C24_RSP_CODE") != null ? rs.getString("C24_RSP_CODE") : "");
                jsonBuilder.add("response_desc", rs.getString("ERR_DESC") != null ? rs.getString("ERR_DESC") : "");

                jsonArrayBuilder.add(jsonBuilder.build());
            }

            success = true;
            requestBean.setString("pending_transactions", JsonUtil.toStr(jsonArrayBuilder.build()));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));

            LOG.info("Successfully fetched {} pending transactions", totalRows);

        } catch (SQLException e) {
            LOG.error("SQL error in getPendingTransactions: {}", e.getMessage(), e);
            requestBean.setString("message", "Database error: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error in getPendingTransactions: {}", e.getMessage(), e);
            requestBean.setString("message", "Error processing request: " + e.getMessage());
        } finally {
            // Close resources in reverse order
            try {
                if (rs != null) rs.close();
                if (countRs != null) countRs.close();
                if (ps != null) ps.close();
                if (countPs != null) countPs.close();
                if (cnn != null) ConnectionUtil.closeConnection(cnn);
            } catch (SQLException e) {
                LOG.error("Error closing database resources", e);
            }
        }

        return success;
    }
}