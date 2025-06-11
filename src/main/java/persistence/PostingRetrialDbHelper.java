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

public class PostingRetrialDbHelper {

    final static Logger LOG = LogManager.getLogger(PostingRetrialDbHelper.class);

    public static boolean getFailedRetrialRequests(BaseBean requestBean) {
        StringBuilder queryBuilder = new StringBuilder("SELECT sno, service_type, retrial_start_date, retrial_end_date, created_by, creation_date, batch_id, batch_count, status, approved_by, approval_date, posting_date, posting_resp_flg, posting_resp_code, posting_retrial_count FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(*) as total_count FROM ESBUSER.POSTING_RETRIAL WHERE 1=1");

        List<String> parameters = new ArrayList<>();

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

        // Add date range filters with smart formatting
        if (requestBean.containsKey("retrial_start_date") && !requestBean.getString("retrial_start_date").isEmpty()) {
            queryBuilder.append(" AND creation_date >= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            countQueryBuilder.append(" AND creation_date >= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            String formattedStartDate = formatDateForDatabase(requestBean.getString("retrial_start_date"), true);
            parameters.add(formattedStartDate);
        }

        if (requestBean.containsKey("retrial_end_date") && !requestBean.getString("retrial_end_date").isEmpty()) {
            queryBuilder.append(" AND creation_date <= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            countQueryBuilder.append(" AND creation_date <= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            String formattedEndDate = formatDateForDatabase(requestBean.getString("retrial_end_date"), false);
            parameters.add(formattedEndDate);
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
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Fetching failed retrial requests: {}", query);

        PreparedStatement ps = null;
        PreparedStatement countPs = null;

        try {
            cnn.setAutoCommit(false);

            // First get the total count
            int totalRows = 0;
            countPs = cnn.prepareStatement(countQuery);
            int paramIndex = 1;
            for (String param : parameters) {
                countPs.setString(paramIndex++, param);
            }

            ResultSet countRs = countPs.executeQuery();
            if (countRs.next()) {
                totalRows = countRs.getInt("total_count");
            }
            countRs.close();

            // Calculate total pages
            int totalPages = (int) Math.ceil((double) totalRows / size);

            // Now get the actual data
            ps = cnn.prepareStatement(query);
            paramIndex = 1;
            for (String param : parameters) {
                ps.setString(paramIndex++, param);
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, size);

            ResultSet rs = ps.executeQuery();
            List<BaseBean> retrialRequests = new ArrayList<>();

            while (rs.next()) {
                BaseBean retrialRequest = new BaseBean();
                retrialRequest.setString("sno", rs.getString("sno"));
                retrialRequest.setString("service_type", rs.getString("service_type"));
                retrialRequest.setString("retrial_start_date", rs.getString("retrial_start_date"));
                retrialRequest.setString("retrial_end_date", rs.getString("retrial_end_date"));
                retrialRequest.setString("created_by", rs.getString("created_by"));
                retrialRequest.setString("creation_date", rs.getString("creation_date"));
                retrialRequest.setString("batch_id", rs.getString("batch_id"));
                retrialRequest.setString("batch_count", rs.getString("batch_count"));
                retrialRequest.setString("status", rs.getString("status"));
                retrialRequest.setString("approved_by", rs.getString("approved_by"));
                retrialRequest.setString("approval_date", rs.getString("approval_date"));
                retrialRequest.setString("posting_date", rs.getString("posting_date"));
                retrialRequest.setString("posting_resp_flg", rs.getString("posting_resp_flg"));
                retrialRequest.setString("posting_resp_code", rs.getString("posting_resp_code"));
                retrialRequest.setString("posting_retrial_count", rs.getString("posting_retrial_count"));

                retrialRequests.add(retrialRequest);
            }

            success = true;
            requestBean.setString("retrial_requests", JsonUtil.convertBaseBeanListToJsonString(retrialRequests));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));

        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("Error fetching failed retrial requests", e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("Error closing PreparedStatement", e);
                }
            }
            if (countPs != null) {
                try {
                    countPs.close();
                } catch (SQLException e) {
                    LOG.error("Error closing count PreparedStatement", e);
                }
            }
            ConnectionUtil.closeConnection(cnn);
        }

        return success;
    }

    /**
     * Formats date string for database usage with smart defaults
     * @param dateStr The input date string from frontend
     * @param isStartDate true for start date (adds 00:00:00), false for end date (adds 23:59:59)
     * @return Formatted date string for database
     */
    private static String formatDateForDatabase(String dateStr, boolean isStartDate) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return dateStr;
        }

        String trimmedDate = dateStr.trim();

        // Check if date already contains time component (contains space and colon)
        if (trimmedDate.contains(" ") && trimmedDate.contains(":")) {
            // Date already has time component, return as-is
            return trimmedDate;
        }

        // Check if it's just a date in YYYY-MM-DD format (10 characters)
        if (trimmedDate.length() == 10 && trimmedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            if (isStartDate) {
                // For start date, add beginning of day
                return trimmedDate + " 00:00:00";
            } else {
                // For end date, add end of day
                return trimmedDate + " 23:59:59";
            }
        }

        // Check if it's date with just time hours like "2024-01-01 10" (13 characters)
        if (trimmedDate.length() == 13 && trimmedDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}")) {
            if (isStartDate) {
                return trimmedDate + ":00:00";
            } else {
                return trimmedDate + ":59:59";
            }
        }

        // Check if it's date with hours and minutes like "2024-01-01 10:30" (16 characters)
        if (trimmedDate.length() == 16 && trimmedDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
            if (isStartDate) {
                return trimmedDate + ":00";
            } else {
                return trimmedDate + ":59";
            }
        }

        // For any other format, return as-is and let database handle validation
        LOG.warn("Unexpected date format received: {}. Using as-is.", trimmedDate);
        return trimmedDate;
    }
}