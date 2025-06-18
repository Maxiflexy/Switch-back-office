package persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.ConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BatchApprovalDbHelper {

    final static Logger LOG = LogManager.getLogger(BatchApprovalDbHelper.class);

    /**
     * Update batch status to APPROVED and get service type for external API call
     * @param requestBean Contains batch_id and approval_message
     * @return true if update successful and service_type retrieved
     */
    public static boolean approveBatch(BaseBean requestBean) {
        String updateQuery = "UPDATE ESBUSER.POSTING_RETRIAL SET STATUS = 'APPROVED', " +
                "APPROVAL_MESSAGE = ?, " +
                "APPROVED_BY = ?, " +
                "APPROVAL_DATE = SYSDATE " +
                "WHERE BATCH_ID = ?";

        String selectQuery = "SELECT SERVICE_TYPE FROM ESBUSER.POSTING_RETRIAL WHERE BATCH_ID = ?";

        Connection cnn = null;
        PreparedStatement updatePs = null;
        PreparedStatement selectPs = null;
        ResultSet rs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for batch approval");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            cnn.setAutoCommit(false); // Start transaction

            // First, update the batch status
            updatePs = cnn.prepareStatement(updateQuery);
            updatePs.setString(1, requestBean.getString("approval_message"));
            updatePs.setString(2, requestBean.getString("approved_by"));
            updatePs.setString(3, requestBean.getString("batch_id"));

            int rowsUpdated = updatePs.executeUpdate();

            if (rowsUpdated == 0) {
                LOG.warn("No rows updated for batch_id: {}. Batch may not exist.", requestBean.getString("batch_id"));
                requestBean.setString("message", "Batch not found or already processed");
                cnn.rollback();
                return false;
            }

            LOG.info("Successfully approved batch: {} with {} rows updated", requestBean.getString("batch_id"), rowsUpdated);

            // Then, get the service type for external API call
            selectPs = cnn.prepareStatement(selectQuery);
            selectPs.setString(1, requestBean.getString("batch_id"));

            rs = selectPs.executeQuery();

            if (rs.next()) {
                String serviceType = rs.getString("SERVICE_TYPE");
                requestBean.setString("service_type", serviceType != null ? serviceType.trim() : "");
                LOG.info("Retrieved service_type: {} for batch: {}", serviceType, requestBean.getString("batch_id"));
            } else {
                LOG.error("Failed to retrieve service_type for batch: {}", requestBean.getString("batch_id"));
                requestBean.setString("message", "Failed to retrieve batch details after approval");
                cnn.rollback();
                return false;
            }

            cnn.commit(); // Commit transaction
            LOG.info("Batch approval transaction committed successfully for batch: {}", requestBean.getString("batch_id"));
            return true;

        } catch (SQLException e) {
            LOG.error("SQL error in approveBatch for batch: {}", requestBean.getString("batch_id"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Database error during batch approval: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in approveBatch for batch: {}", requestBean.getString("batch_id"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Error processing batch approval: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (selectPs != null) selectPs.close();
                if (updatePs != null) updatePs.close();
                if (cnn != null) {
                    cnn.setAutoCommit(true); // Reset auto-commit
                    ConnectionUtil.closeConnection(cnn);
                }
            } catch (SQLException e) {
                LOG.error("Error closing database resources", e);
            }
        }
    }

    /**
     * Update batch status to REJECTED
     * @param requestBean Contains batch_id and approval_message
     * @return true if update successful
     */
    public static boolean rejectBatch(BaseBean requestBean) {
        String updateQuery = "UPDATE ESBUSER.POSTING_RETRIAL SET STATUS = 'REJECTED', " +
                "APPROVAL_MESSAGE = ?, " +
                "APPROVED_BY = ?, " +
                "APPROVAL_DATE = SYSDATE " +
                "WHERE BATCH_ID = ?";

        Connection cnn = null;
        PreparedStatement ps = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for batch rejection");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            ps = cnn.prepareStatement(updateQuery);
            ps.setString(1, requestBean.getString("approval_message"));
            ps.setString(2, requestBean.getString("approved_by"));
            ps.setString(3, requestBean.getString("batch_id"));

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated == 0) {
                LOG.warn("No rows updated for batch_id: {}. Batch may not exist.", requestBean.getString("batch_id"));
                requestBean.setString("message", "Batch not found or already processed");
                return false;
            }

            LOG.info("Successfully rejected batch: {} with {} rows updated", requestBean.getString("batch_id"), rowsUpdated);
            return true;

        } catch (SQLException e) {
            LOG.error("SQL error in rejectBatch for batch: {}", requestBean.getString("batch_id"), e);
            requestBean.setString("message", "Database error during batch rejection: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in rejectBatch for batch: {}", requestBean.getString("batch_id"), e);
            requestBean.setString("message", "Error processing batch rejection: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (ps != null) ps.close();
                if (cnn != null) ConnectionUtil.closeConnection(cnn);
            } catch (SQLException e) {
                LOG.error("Error closing database resources", e);
            }
        }
    }

    /**
     * Check if batch exists and get current status
     * @param requestBean Contains batch_id
     * @return true if batch exists
     */
    public static boolean validateBatchExists(BaseBean requestBean) {
        String query = "SELECT BATCH_ID, STATUS FROM ESBUSER.POSTING_RETRIAL WHERE BATCH_ID = ?";

        Connection cnn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for batch validation");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            ps = cnn.prepareStatement(query);
            ps.setString(1, requestBean.getString("batch_id"));

            rs = ps.executeQuery();

            if (rs.next()) {
                String currentStatus = rs.getString("STATUS");
                requestBean.setString("current_status", currentStatus != null ? currentStatus.trim() : "");
                LOG.info("Batch {} exists with current status: {}", requestBean.getString("batch_id"), currentStatus);

                // Check if batch is already processed
//                if ("APPROVED".equalsIgnoreCase(currentStatus) || "REJECTED".equalsIgnoreCase(currentStatus)) {
//                    requestBean.setString("message", "Batch is already " + currentStatus.toLowerCase());
//                    return false;
//                }

                return true;
            } else {
                LOG.warn("Batch not found: {}", requestBean.getString("batch_id"));
                requestBean.setString("message", "Batch not found");
                return false;
            }

        } catch (SQLException e) {
            LOG.error("SQL error in validateBatchExists for batch: {}", requestBean.getString("batch_id"), e);
            requestBean.setString("message", "Database error during batch validation: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in validateBatchExists for batch: {}", requestBean.getString("batch_id"), e);
            requestBean.setString("message", "Error validating batch: " + e.getMessage());
            return false;
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
    }
}