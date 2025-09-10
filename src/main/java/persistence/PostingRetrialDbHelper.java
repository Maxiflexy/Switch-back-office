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

public class PostingRetrialDbHelper {

    final static Logger LOG = LogManager.getLogger(PostingRetrialDbHelper.class);

    public static boolean getFailedRetrialRequests(BaseBean requestBean) {
        // Check if batch_id is provided to determine query mode
        boolean hasBatchId = requestBean.containsKey("batch_id") && !requestBean.getString("batch_id").trim().isEmpty();

        if (hasBatchId) {
            // Single record mode - return all 20 columns (added module)
            return getFailedRetrialRequestSingle(requestBean);
        } else {
            // Paginated mode - return 16 columns with pagination (added module)
            return getFailedRetrialRequestsPaginated(requestBean);
        }
    }

    /**
     * Get single failed retrial request by batch_id with all 20 columns (added module)
     */
    private static boolean getFailedRetrialRequestSingle(BaseBean requestBean) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT sno, service_type, ");
        queryBuilder.append("TO_CHAR(retrial_start_date, 'YYYY-MM-DD HH24:MI:SS') as retrial_start_date, ");
        queryBuilder.append("TO_CHAR(retrial_end_date, 'YYYY-MM-DD HH24:MI:SS') as retrial_end_date, ");
        queryBuilder.append("created_by, ");
        queryBuilder.append("TO_CHAR(creation_date, 'YYYY-MM-DD HH24:MI:SS') as creation_date, ");
        queryBuilder.append("batch_id, batch_count, status, approved_by, ");
        queryBuilder.append("TO_CHAR(approval_date, 'YYYY-MM-DD HH24:MI:SS') as approval_date, ");
        queryBuilder.append("TO_CHAR(posting_date, 'YYYY-MM-DD HH24:MI:SS') as posting_date, ");
        queryBuilder.append("posting_resp_flg, posting_resp_code, posting_retrial_count, ");
        // Add the additional 5 columns for complete record (20 columns total - including module)
        queryBuilder.append("updated_by, TO_CHAR(updated_date, 'YYYY-MM-DD HH24:MI:SS') as updated_date, approval_message, switch_type, ");
        queryBuilder.append("module ");  // Added module field
        queryBuilder.append("FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        List<Object> parameters = new ArrayList<>();

        // Add service_type filter (required)
        if (requestBean.containsKey("service_type") && !requestBean.getString("service_type").trim().isEmpty()) {
            queryBuilder.append(" AND UPPER(service_type) = UPPER(?)");
            parameters.add(requestBean.getString("service_type").trim());
            LOG.info("Adding service_type filter: {}", requestBean.getString("service_type"));
        }

        // Add status filter (required)
        if (requestBean.containsKey("request_status") && !requestBean.getString("request_status").trim().isEmpty()) {
            queryBuilder.append(" AND UPPER(status) = UPPER(?)");
            parameters.add(requestBean.getString("request_status").trim());
            LOG.info("Adding request_status filter: {}", requestBean.getString("request_status"));
        }

        // Add batch_id filter (required for single mode)
        queryBuilder.append(" AND batch_id = ?");
        parameters.add(requestBean.getString("batch_id").trim());
        LOG.info("Adding batch_id filter: {}", requestBean.getString("batch_id"));

        // Add module filter (optional) - applies to both single and paginated modes
        if (requestBean.containsKey("module") && !requestBean.getString("module").trim().isEmpty()) {
            queryBuilder.append(" AND UPPER(module) = UPPER(?)");
            parameters.add(requestBean.getString("module").trim());
            LOG.info("Adding module filter: {}", requestBean.getString("module"));
        }

        String query = queryBuilder.toString();

        boolean success = false;
        Connection cnn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        LOG.info("Executing single record query: {}", query);
        LOG.info("Parameters: {}", parameters);

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            ps = cnn.prepareStatement(query);
            int paramIndex = 1;
            for (Object param : parameters) {
                ps.setObject(paramIndex++, param);
                LOG.debug("Query parameter {}: {}", paramIndex-1, param);
            }

            rs = ps.executeQuery();
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            int rowCount = 0;

            LOG.info("DEBUG: Starting to process single record result set...");
            while (rs.next()) {
                rowCount++;
                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

                // Handle all 20 columns with null safety (added module)
                jsonBuilder.add("sno", rs.getString("sno") != null ? rs.getString("sno") : "");
                jsonBuilder.add("service_type", rs.getString("service_type") != null ? rs.getString("service_type") : "");
                jsonBuilder.add("retrial_start_date", rs.getString("retrial_start_date") != null ? rs.getString("retrial_start_date") : "");
                jsonBuilder.add("retrial_end_date", rs.getString("retrial_end_date") != null ? rs.getString("retrial_end_date") : "");
                jsonBuilder.add("created_by", rs.getString("created_by") != null ? rs.getString("created_by") : "");
                jsonBuilder.add("creation_date", rs.getString("creation_date") != null ? rs.getString("creation_date") : "");
                jsonBuilder.add("batch_id", rs.getString("batch_id") != null ? rs.getString("batch_id") : "");
                jsonBuilder.add("batch_count", rs.getString("batch_count") != null ? rs.getString("batch_count") : "");
                jsonBuilder.add("status", rs.getString("status") != null ? rs.getString("status") : "");
                jsonBuilder.add("approved_by", rs.getString("approved_by") != null ? rs.getString("approved_by") : "");
                jsonBuilder.add("approval_date", rs.getString("approval_date") != null ? rs.getString("approval_date") : "");
                jsonBuilder.add("posting_date", rs.getString("posting_date") != null ? rs.getString("posting_date") : "");
                jsonBuilder.add("posting_resp_flg", rs.getString("posting_resp_flg") != null ? rs.getString("posting_resp_flg") : "");
                jsonBuilder.add("posting_resp_code", rs.getString("posting_resp_code") != null ? rs.getString("posting_resp_code") : "");
                jsonBuilder.add("posting_retrial_count", rs.getString("posting_retrial_count") != null ? rs.getString("posting_retrial_count") : "");
                // Add the additional 5 columns for complete record (including module)
                jsonBuilder.add("updated_by", rs.getString("updated_by") != null ? rs.getString("updated_by") : "");
                jsonBuilder.add("updated_date", rs.getString("updated_date") != null ? rs.getString("updated_date") : "");
                jsonBuilder.add("approval_message", rs.getString("approval_message") != null ? rs.getString("approval_message") : "");
                jsonBuilder.add("switch_type", rs.getString("switch_type") != null ? rs.getString("switch_type") : "");
                jsonBuilder.add("module", rs.getString("module") != null ? rs.getString("module") : "");  // Added module field

                jsonArrayBuilder.add(jsonBuilder.build());
            }
            LOG.info("DEBUG: Finished processing single record result set. Total records processed: {}", rowCount);

            success = true;
            requestBean.setString("retrial_requests", JsonUtil.toStr(jsonArrayBuilder.build()));
            // For single record mode, set these values appropriately
            requestBean.setString("total_rows", String.valueOf(rowCount));
            requestBean.setString("total_pages", rowCount > 0 ? "1" : "0");
            requestBean.setString("current_page", "1");
            requestBean.setString("page_size", String.valueOf(rowCount));

            LOG.info("Successfully fetched {} retrial request(s) for batch_id: {}", rowCount, requestBean.getString("batch_id"));

        } catch (SQLException e) {
            LOG.error("SQL error in getFailedRetrialRequestSingle: {}", e.getMessage(), e);
            requestBean.setString("message", "Database error: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error in getFailedRetrialRequestSingle: {}", e.getMessage(), e);
            requestBean.setString("message", "Error processing request: " + e.getMessage());
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (cnn != null) ConnectionUtil.closeConnection(cnn);
            } catch (SQLException e) {
                LOG.error("Error closing database resources", e);
            }
        }

        return success;
    }

    /**
     * Get paginated failed retrial requests with 16 columns (added module)
     */
    private static boolean getFailedRetrialRequestsPaginated(BaseBean requestBean) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT sno, service_type, ");
        queryBuilder.append("TO_CHAR(retrial_start_date, 'YYYY-MM-DD HH24:MI:SS') as retrial_start_date, ");
        queryBuilder.append("TO_CHAR(retrial_end_date, 'YYYY-MM-DD HH24:MI:SS') as retrial_end_date, ");
        queryBuilder.append("created_by, ");
        queryBuilder.append("TO_CHAR(creation_date, 'YYYY-MM-DD HH24:MI:SS') as creation_date, ");
        queryBuilder.append("batch_id, batch_count, status, approved_by, ");
        queryBuilder.append("TO_CHAR(approval_date, 'YYYY-MM-DD HH24:MI:SS') as approval_date, ");
        queryBuilder.append("TO_CHAR(posting_date, 'YYYY-MM-DD HH24:MI:SS') as posting_date, ");
        queryBuilder.append("posting_resp_flg, posting_resp_code, posting_retrial_count, ");
        queryBuilder.append("module ");  // Added module field
        queryBuilder.append("FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(*) as total_count FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        List<Object> parameters = new ArrayList<>();

        // Add service_type filter (required) - can be POSTING or TSQ,
        if (requestBean.containsKey("service_type") && !requestBean.getString("service_type").trim().isEmpty()) {
            queryBuilder.append(" AND UPPER(service_type) = UPPER(?)");
            countQueryBuilder.append(" AND UPPER(service_type) = UPPER(?)");
            parameters.add(requestBean.getString("service_type").trim());
            LOG.info("Adding service_type filter: {}", requestBean.getString("service_type"));
        }

        // Add status filter (required) - status values are PENDING, APPROVED, REJECTED
        if (requestBean.containsKey("request_status") && !requestBean.getString("request_status").trim().isEmpty()) {
            queryBuilder.append(" AND UPPER(status) = UPPER(?)");
            countQueryBuilder.append(" AND UPPER(status) = UPPER(?)");
            parameters.add(requestBean.getString("request_status").trim());
            LOG.info("Adding request_status filter: {}", requestBean.getString("request_status"));
        }

        // Add batch_id filter (optional) - filter on batch_id column
        if (requestBean.containsKey("batch_id") && !requestBean.getString("batch_id").trim().isEmpty()) {
            queryBuilder.append(" AND batch_id = ?");
            countQueryBuilder.append(" AND batch_id = ?");
            parameters.add(requestBean.getString("batch_id").trim());
            LOG.info("Adding batch_id filter: {}", requestBean.getString("batch_id"));
        }

        // Add module filter (optional) - applies to both single and paginated modes
        if (requestBean.containsKey("module") && !requestBean.getString("module").trim().isEmpty()) {
            queryBuilder.append(" AND UPPER(module) = UPPER(?)");
            countQueryBuilder.append(" AND UPPER(module) = UPPER(?)");
            parameters.add(requestBean.getString("module").trim());
            LOG.info("Adding module filter: {}", requestBean.getString("module"));
        }

        // Handle date range filters - filter on CREATION_DATE column instead of retrial date columns
        // Only add date filters if dates are provided and not empty
        boolean hasStartDate = requestBean.containsKey("retrial_start_date") &&
                !requestBean.getString("retrial_start_date").trim().isEmpty();
        boolean hasEndDate = requestBean.containsKey("retrial_end_date") &&
                !requestBean.getString("retrial_end_date").trim().isEmpty();

        if (hasStartDate) {
            // Filter on CREATION_DATE using retrial_start_date parameter
            queryBuilder.append(" AND creation_date >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND creation_date >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("retrial_start_date").trim());
            LOG.info("Adding retrial_start_date filter on creation_date: {}", requestBean.getString("retrial_start_date"));
        }

        if (hasEndDate) {
            // Filter on CREATION_DATE using retrial_end_date parameter
            queryBuilder.append(" AND creation_date <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND creation_date <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("retrial_end_date").trim());
            LOG.info("Adding retrial_end_date filter on creation_date: {}", requestBean.getString("retrial_end_date"));
        }

        // Add sorting by creation_date in descending order
        queryBuilder.append(" ORDER BY creation_date DESC");

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

        // Debug: Log the exact SQL with parameters for troubleshooting
        if (requestBean.containsKey("batch_id") && !requestBean.getString("batch_id").trim().isEmpty()) {
            LOG.info("DEBUG: Filtering by batch_id: {}", requestBean.getString("batch_id"));
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
                countPs.setObject(paramIndex++, param);
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
                ps.setObject(paramIndex++, param);
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
                String currentBatchId = rs.getString("batch_id");
                String currentSno = rs.getString("sno");
                LOG.debug("DEBUG: Processing record {}: sno={}, batch_id={}", rowCount, currentSno, currentBatchId);

                // Handle all 16 columns with null safety (added module)
                jsonBuilder.add("sno", rs.getString("sno") != null ? rs.getString("sno") : "");
                jsonBuilder.add("service_type", rs.getString("service_type") != null ? rs.getString("service_type") : "");
                jsonBuilder.add("retrial_start_date", rs.getString("retrial_start_date") != null ? rs.getString("retrial_start_date") : "");
                jsonBuilder.add("retrial_end_date", rs.getString("retrial_end_date") != null ? rs.getString("retrial_end_date") : "");
                jsonBuilder.add("created_by", rs.getString("created_by") != null ? rs.getString("created_by") : "");
                jsonBuilder.add("creation_date", rs.getString("creation_date") != null ? rs.getString("creation_date") : "");
                jsonBuilder.add("batch_id", rs.getString("batch_id") != null ? rs.getString("batch_id") : "");
                jsonBuilder.add("batch_count", rs.getString("batch_count") != null ? rs.getString("batch_count") : "");
                jsonBuilder.add("status", rs.getString("status") != null ? rs.getString("status") : "");
                jsonBuilder.add("approved_by", rs.getString("approved_by") != null ? rs.getString("approved_by") : "");
                jsonBuilder.add("approval_date", rs.getString("approval_date") != null ? rs.getString("approval_date") : "");
                jsonBuilder.add("posting_date", rs.getString("posting_date") != null ? rs.getString("posting_date") : "");
                jsonBuilder.add("posting_resp_flg", rs.getString("posting_resp_flg") != null ? rs.getString("posting_resp_flg") : "");
                jsonBuilder.add("posting_resp_code", rs.getString("posting_resp_code") != null ? rs.getString("posting_resp_code") : "");
                jsonBuilder.add("posting_retrial_count", rs.getString("posting_retrial_count") != null ? rs.getString("posting_retrial_count") : "");
                jsonBuilder.add("module", rs.getString("module") != null ? rs.getString("module") : "");  // Added module field

                jsonArrayBuilder.add(jsonBuilder.build());
            }
            LOG.info("DEBUG: Finished processing result set. Total records processed: {}", rowCount);

            success = true;
            requestBean.setString("retrial_requests", JsonUtil.toStr(jsonArrayBuilder.build()));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));

            LOG.info("Successfully fetched {} retrial requests out of {} total rows", rowCount, totalRows);

        } catch (SQLException e) {
            LOG.error("SQL error in getFailedRetrialRequests: {}", e.getMessage(), e);
            requestBean.setString("message", "Database error: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error in getFailedRetrialRequests: {}", e.getMessage(), e);
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