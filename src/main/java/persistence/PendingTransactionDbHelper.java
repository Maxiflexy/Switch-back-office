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
        queryBuilder.append("WHERE 1=1");

        StringBuilder countQueryBuilder = new StringBuilder();
        countQueryBuilder.append("SELECT COUNT(*) as total_count FROM ESBUSER.NIP_IN_FLW_V2 n ");
        countQueryBuilder.append("WHERE 1=1");

        List<Object> parameters = new ArrayList<>();

        // BATCH_ID is required - NUMBER(28,0) in database
        if (requestBean.containsKey("batch_id") && !requestBean.getString("batch_id").trim().isEmpty()) {
            queryBuilder.append(" AND n.BATCH_ID = ?");
            countQueryBuilder.append(" AND n.BATCH_ID = ?");

            try {
                Long batchId = Long.parseLong(requestBean.getString("batch_id").trim());
                parameters.add(batchId);
                LOG.info("Adding batch_id filter: {}", batchId);
            } catch (NumberFormatException e) {
                LOG.error("Invalid batch_id format: {}", requestBean.getString("batch_id"));
                requestBean.setString("message", "Invalid batch_id format. Must be a valid number.");
                return false;
            }
        } else {
            LOG.error("batch_id parameter is required but not provided");
            requestBean.setString("message", "batch_id parameter is required");
            return false;
        }

        // Add tran_ref filter (optional) - filter on PAYMENTREFERENCE column
        if (requestBean.containsKey("tran_ref") && !requestBean.getString("tran_ref").trim().isEmpty()) {
            queryBuilder.append(" AND n.PAYMENTREFERENCE = ?");
            countQueryBuilder.append(" AND n.PAYMENTREFERENCE = ?");
            parameters.add(requestBean.getString("tran_ref").trim());
            LOG.info("Adding tran_ref filter: {}", requestBean.getString("tran_ref"));
        }

        // Handle DATE range filters - REQUESTDATE is DATE type, convert ISO format to DATE
        boolean hasStartDate = requestBean.containsKey("start_date") &&
                !requestBean.getString("start_date").trim().isEmpty();
        boolean hasEndDate = requestBean.containsKey("end_date") &&
                !requestBean.getString("end_date").trim().isEmpty();

        if (hasStartDate) {
            // Convert ISO format (2025-06-16T07:38:25) to DATE for Oracle
            queryBuilder.append(" AND n.REQUESTDATE >= TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND n.REQUESTDATE >= TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("start_date").trim());
            LOG.info("Adding start_date filter: {}", requestBean.getString("start_date"));
        }

        if (hasEndDate) {
            // Convert ISO format (2025-06-16T07:38:25) to DATE for Oracle
            queryBuilder.append(" AND n.REQUESTDATE <= TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND n.REQUESTDATE <= TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("end_date").trim());
            LOG.info("Adding end_date filter: {}", requestBean.getString("end_date"));
        }

        // Add sorting by REQUESTDATE in descending order
        queryBuilder.append(" ORDER BY n.REQUESTDATE DESC");

        // Add pagination
        int page = 1;
        int size = 10;

        try {
            if (requestBean.containsKey("page") && !requestBean.getString("page").trim().isEmpty()) {
                page = Integer.parseInt(requestBean.getString("page").trim());
                if (page < 1) page = 1; // Ensure minimum page is 1
            }
            if (requestBean.containsKey("size") && !requestBean.getString("size").trim().isEmpty()) {
                size = Integer.parseInt(requestBean.getString("size").trim());
                if (size < 1) size = 10; // Ensure minimum size is 1
                if (size > 100) size = 100; // Cap maximum size
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid page or size parameter, using defaults. Page: {}, Size: {}",
                    requestBean.getString("page"), requestBean.getString("size"));
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
        LOG.info("Pagination - Page: {}, Size: {}, Offset: {}", page, size, offset);

        // Debug: Log the tran_ref filter if provided
        if (requestBean.containsKey("tran_ref") && !requestBean.getString("tran_ref").trim().isEmpty()) {
            LOG.info("DEBUG: Filtering by tran_ref (PAYMENTREFERENCE): {}", requestBean.getString("tran_ref"));
        }

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
                LOG.debug("Count query parameter {}: {}", paramIndex-1, param);
            }

            countRs = countPs.executeQuery();
            if (countRs.next()) {
                totalRows = countRs.getInt("total_count");
            }
            LOG.info("Total rows found: {}", totalRows);

            // Calculate total pages
            int totalPages = totalRows > 0 ? (int) Math.ceil((double) totalRows / size) : 0;

            // Now get the actual data
            ps = cnn.prepareStatement(query);
            paramIndex = 1;
            for (Object param : parameters) {
                if (param instanceof Long) {
                    ps.setLong(paramIndex++, (Long) param);
                } else {
                    ps.setObject(paramIndex++, param);
                }
                LOG.debug("Data query parameter {}: {}", paramIndex-1, param);
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, size);

            rs = ps.executeQuery();
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            int rowCount = 0;

            LOG.info("DEBUG: Starting to process result set...");
            while (rs.next()) {
                rowCount++;
                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

                // Log each record for debugging
                String currentPaymentRef = rs.getString("PAYMENTREFERENCE");
                String currentBatchId = rs.getString("BATCH_ID");
                LOG.debug("DEBUG: Processing record {}: PAYMENTREFERENCE={}, BATCH_ID={}", rowCount, currentPaymentRef, currentBatchId);

                // Map database columns to response fields with null safety
                jsonBuilder.add("tran_ref", rs.getString("PAYMENTREFERENCE") != null ? rs.getString("PAYMENTREFERENCE") : "");
                jsonBuilder.add("tran_date", rs.getString("REQUESTDATE") != null ? rs.getString("REQUESTDATE") : "");
                jsonBuilder.add("acct_no", rs.getString("ACCOUNTNUMBER") != null ? rs.getString("ACCOUNTNUMBER") : "");

                // Handle AMOUNT as string to preserve precision
                String amount = rs.getString("AMOUNT");
                jsonBuilder.add("tran_amt", amount != null ? amount : "");

                // Handle BATCH_ID as string
                String batchId = rs.getString("BATCH_ID");
                jsonBuilder.add("batch_id", batchId != null ? batchId : "");

                jsonBuilder.add("tran_narration", rs.getString("NARRATION") != null ? rs.getString("NARRATION") : "");
                jsonBuilder.add("response_code", rs.getString("C24_RSP_CODE") != null ? rs.getString("C24_RSP_CODE") : "");
                jsonBuilder.add("response_desc", rs.getString("ERR_DESC") != null ? rs.getString("ERR_DESC") : "");

                jsonArrayBuilder.add(jsonBuilder.build());
            }
            LOG.info("DEBUG: Finished processing result set. Total records processed: {}", rowCount);

            success = true;
            requestBean.setString("pending_transactions", JsonUtil.toStr(jsonArrayBuilder.build()));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));

            LOG.info("Successfully fetched {} pending transactions out of {} total rows", rowCount, totalRows);

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

    /**
     * Fetch the status for a specific batch_id from ESBUSER.POSTING_RETRIAL table
     * Since batch_id is unique, this will return a single status value
     */
    public static String getBatchStatus(String batchId) {
        String status;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;

        try {
            String query = "SELECT status FROM ESBUSER.POSTING_RETRIAL WHERE batch_id = ?";

            ps = cnn.prepareStatement(query);
            ps.setString(1, batchId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                status = rs.getString("status");
                //LOG.info("Found status '{}' for batch_id: {}", status, batchId);
            } else {
                //LOG.info("No status record found for batch_id: {}", batchId);
                status = "N/A"; // Default value when no record exists
            }

            rs.close();

        } catch (SQLException e) {
            LOG.error("SQL error while fetching batch status for batch_id {}: {}", batchId, e.getMessage(), e);
            status = "ERROR"; // Indicate there was an error fetching status
        } catch (Exception e) {
            LOG.error("Unexpected error while fetching batch status for batch_id {}: {}", batchId, e.getMessage(), e);
            status = "ERROR";
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("Error closing PreparedStatement: {}", e.getMessage());
                }
            }
            ConnectionUtil.closeConnection(cnn);
        }

        return status;
    }
}