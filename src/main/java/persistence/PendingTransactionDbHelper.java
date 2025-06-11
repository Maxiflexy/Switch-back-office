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

public class PendingTransactionDbHelper {

    final static Logger LOG = LogManager.getLogger(PendingTransactionDbHelper.class);

    public static boolean getPendingTransactions(BaseBean requestBean) {
        StringBuilder queryBuilder = new StringBuilder("SELECT n.PAYMENTREFERENCE, n.REQUESTDATE, n.ACCOUNTNUMBER, n.AMOUNT, n.BATCH_ID, n.NARRATION, n.C24_RSP_CODE, COALESCE(b.ERR_DESC, '') as ERR_DESC FROM ESBUSER.NIP_IN_FLW_V2 n LEFT JOIN ESBUSER.BANCS_CONNECT_RESPONSE b ON n.C24_RSP_CODE = b.ERR_CODE WHERE n.BATCH_ID = ?");

        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(*) as total_count FROM ESBUSER.NIP_IN_FLW_V2 n WHERE n.BATCH_ID = ?");

        List<Object> parameters = new ArrayList<>();
        // BATCH_ID is NUMBER(28,0) in database, so convert string to Long for proper parameter binding
        try {
            parameters.add(Long.parseLong(requestBean.getString("batch_id")));
        } catch (NumberFormatException e) {
            LOG.error("Invalid batch_id format: {}", requestBean.getString("batch_id"));
            requestBean.setString("message", "Invalid batch_id format. Must be a valid number.");
            return false;
        }

        // Add date range filters with smart formatting
        if (requestBean.containsKey("start_date") && !requestBean.getString("start_date").isEmpty()) {
            queryBuilder.append(" AND n.REQUESTDATE >= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            countQueryBuilder.append(" AND n.REQUESTDATE >= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            String formattedStartDate = formatDateForDatabase(requestBean.getString("start_date"), true);
            parameters.add(formattedStartDate);
        }

        if (requestBean.containsKey("end_date") && !requestBean.getString("end_date").isEmpty()) {
            queryBuilder.append(" AND n.REQUESTDATE <= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            countQueryBuilder.append(" AND n.REQUESTDATE <= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            String formattedEndDate = formatDateForDatabase(requestBean.getString("end_date"), false);
            parameters.add(formattedEndDate);
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
        Connection cnn = ConnectionUtil.getConnection();
        LOG.info("Fetching pending transactions: {}", query);

        PreparedStatement ps = null;
        PreparedStatement countPs = null;

        try {
            cnn.setAutoCommit(false);

            // First get the total count
            int totalRows = 0;
            countPs = cnn.prepareStatement(countQuery);
            int paramIndex = 1;
            for (Object param : parameters) {
                if (param instanceof Long) {
                    countPs.setLong(paramIndex++, (Long) param);
                } else {
                    countPs.setString(paramIndex++, param.toString());
                }
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
            for (Object param : parameters) {
                if (param instanceof Long) {
                    ps.setLong(paramIndex++, (Long) param);
                } else {
                    ps.setString(paramIndex++, param.toString());
                }
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, size);

            ResultSet rs = ps.executeQuery();
            List<BaseBean> transactions = new ArrayList<>();

            while (rs.next()) {
                BaseBean transaction = new BaseBean();
                transaction.setString("tran_ref", rs.getString("PAYMENTREFERENCE"));
                transaction.setString("tran_date", rs.getString("REQUESTDATE"));
                transaction.setString("acct_no", rs.getString("ACCOUNTNUMBER"));
                transaction.setString("tran_amt", rs.getString("AMOUNT"));
                transaction.setString("batch_id", rs.getString("BATCH_ID"));
                transaction.setString("tran_narration", rs.getString("NARRATION"));
                transaction.setString("response_code", rs.getString("C24_RSP_CODE"));
                transaction.setString("response_desc", rs.getString("ERR_DESC"));

                transactions.add(transaction);
            }

            success = true;
            requestBean.setString("pending_transactions", JsonUtil.convertBaseBeanListToJsonString(transactions));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));

        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
            LOG.error("Error fetching pending transactions", e);
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