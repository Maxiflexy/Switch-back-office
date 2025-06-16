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
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT sno, service_type, ");
        queryBuilder.append("TO_CHAR(retrial_start_date, 'YYYY-MM-DD HH24:MI:SS') as retrial_start_date, ");
        queryBuilder.append("TO_CHAR(retrial_end_date, 'YYYY-MM-DD HH24:MI:SS') as retrial_end_date, ");
        queryBuilder.append("created_by, ");
        queryBuilder.append("TO_CHAR(creation_date, 'YYYY-MM-DD HH24:MI:SS') as creation_date, ");
        queryBuilder.append("batch_id, batch_count, status, approved_by, ");
        queryBuilder.append("TO_CHAR(approval_date, 'YYYY-MM-DD HH24:MI:SS') as approval_date, ");
        queryBuilder.append("TO_CHAR(posting_date, 'YYYY-MM-DD HH24:MI:SS') as posting_date, ");
        queryBuilder.append("posting_resp_flg, posting_resp_code, posting_retrial_count ");
        queryBuilder.append("FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(*) as total_count FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        List<Object> parameters = new ArrayList<>();

        // Add service_type filter
        if (requestBean.containsKey("service_type") && !requestBean.getString("service_type").isEmpty()) {
            queryBuilder.append(" AND service_type = ?");
            countQueryBuilder.append(" AND service_type = ?");
            parameters.add(requestBean.getString("service_type"));
        }

        // Add status filter
        if (requestBean.containsKey("request_status") && !requestBean.getString("request_status").isEmpty()) {
            queryBuilder.append(" AND status = ?");
            countQueryBuilder.append(" AND status = ?");
            parameters.add(requestBean.getString("request_status"));
        }

        // Handle TIMESTAMP date range filters with ISO format
        if (requestBean.containsKey("retrial_start_date") && !requestBean.getString("retrial_start_date").isEmpty()) {
            queryBuilder.append(" AND creation_date >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND creation_date >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("retrial_start_date"));
            LOG.info("Adding start date filter: {}", requestBean.getString("retrial_start_date"));
        }

        if (requestBean.containsKey("retrial_end_date") && !requestBean.getString("retrial_end_date").isEmpty()) {
            queryBuilder.append(" AND creation_date <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            countQueryBuilder.append(" AND creation_date <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
            parameters.add(requestBean.getString("retrial_end_date"));
            LOG.info("Adding end date filter: {}", requestBean.getString("retrial_end_date"));
        }

        // Add sorting
        queryBuilder.append(" ORDER BY creation_date DESC");

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
                countPs.setObject(paramIndex++, param);
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
                ps.setObject(paramIndex++, param);
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, size);

            rs = ps.executeQuery();
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

            while (rs.next()) {
                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
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

                jsonArrayBuilder.add(jsonBuilder.build());
            }

            success = true;
            requestBean.setString("retrial_requests", JsonUtil.toStr(jsonArrayBuilder.build()));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));

            LOG.info("Successfully fetched {} retrial requests", totalRows);

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