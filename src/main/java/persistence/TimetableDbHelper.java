package persistence;

import constants.AppConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.ConnectionUtil;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TimetableDbHelper {

    final static Logger LOG = LogManager.getLogger(TimetableDbHelper.class);

    /**
     * Create timetable request in maker-checker flow
     * @param requestBean Contains channel_code, switch_details, created_by, action_id
     * @return true if request created successfully
     */
    public static boolean createTimetableRequest(BaseBean requestBean) {
        String insertRequestQuery = "INSERT INTO ESBUSER.SWITCH_REQUESTS " +
                "(MODULE, CREATED_BY, CREATED_AT, ACTION, STATUS) " +
                "VALUES (?, ?, SYSDATE, ?, ?)";

        String insertTimetableQuery = "INSERT INTO ESBUSER.SWITCH_TIME_TABLE_MC " +
                "(HOUR, SWITCH_CODE, CHANNEL_CODE, REQUEST_ID) " +
                "VALUES (?, ?, ?, ?)";

        Connection cnn = null;
        PreparedStatement requestPs = null;
        PreparedStatement timetablePs = null;
        ResultSet rs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for timetable creation");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            cnn.setAutoCommit(false); // Start transaction

            // First, insert into SWITCH_REQUESTS table
            requestPs = cnn.prepareStatement(insertRequestQuery, new String[]{"ID"});
            requestPs.setString(1, "SWITCH TIME TABLE");
            requestPs.setString(2, requestBean.getString("created_by"));
            requestPs.setString(3, "CREATE");
            requestPs.setString(4, "PENDING");

            int requestRowsInserted = requestPs.executeUpdate();
            if (requestRowsInserted == 0) {
                LOG.error("Failed to insert request into SWITCH_REQUESTS table");
                requestBean.setString("message", "Failed to create timetable request");
                cnn.rollback();
                return false;
            }

            // Get the generated request ID
            rs = requestPs.getGeneratedKeys();
            long requestId = 0;
            if (rs.next()) {
                // Use getBigDecimal for Oracle NUMBER columns with auto-generation
                java.math.BigDecimal idBigDecimal = rs.getBigDecimal(1);
                if (idBigDecimal != null) {
                    requestId = idBigDecimal.longValue();
                    LOG.info("Generated request ID: {} for channel: {}", requestId, requestBean.getString("channel_code"));
                } else {
                    LOG.error("Generated request ID is null");
                    requestBean.setString("message", "Generated request ID is null");
                    cnn.rollback();
                    return false;
                }
            } else {
                LOG.error("Failed to retrieve generated request ID");
                requestBean.setString("message", "Failed to retrieve request ID");
                cnn.rollback();
                return false;
            }

            // Parse switch details and insert into SWITCH_TIME_TABLE_MC
            JsonArray switchDetailsArray = JsonUtil.toJsonArray(requestBean.getString("switch_details"));
            timetablePs = cnn.prepareStatement(insertTimetableQuery);

            int totalInserted = 0;
            for (int i = 0; i < switchDetailsArray.size(); i++) {
                JsonObject switchDetail = switchDetailsArray.getJsonObject(i);

                String switchCode = JsonUtil.getJsonObjValue2(switchDetail, "switchCode").trim().toUpperCase();
                String timeStr = JsonUtil.getJsonObjValue2(switchDetail, "time").trim();
                int hour = Integer.parseInt(timeStr);

                timetablePs.setInt(1, hour);
                timetablePs.setString(2, switchCode);
                timetablePs.setString(3, requestBean.getString("channel_code"));
                timetablePs.setLong(4, requestId);

                timetablePs.addBatch();
                totalInserted++;
            }

            // Execute batch insert
            int[] batchResults = timetablePs.executeBatch();

            // Verify all inserts were successful
            for (int result : batchResults) {
                if (result <= 0) {
                    LOG.error("Batch insert failed for one or more timetable entries");
                    requestBean.setString("message", "Failed to insert all timetable entries");
                    cnn.rollback();
                    return false;
                }
            }

            cnn.commit(); // Commit transaction
            LOG.info("Successfully created timetable request with ID: {} for channel: {} with {} entries",
                    requestId, requestBean.getString("channel_code"), totalInserted);

            requestBean.setString("request_id", String.valueOf(requestId));
            return true;

        } catch (SQLException e) {
            LOG.error("SQL error in createTimetableRequest for channel: {}",
                    requestBean.getString("channel_code"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Database error during timetable creation: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in createTimetableRequest for channel: {}",
                    requestBean.getString("channel_code"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Error processing timetable creation: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (timetablePs != null) timetablePs.close();
                if (requestPs != null) requestPs.close();
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
     * Fetch timetable data with pagination and filtering support
     * @param requestBean Contains page, size, id, channel_code parameters and will contain the formatted timetable data
     * @return true if data fetched successfully
     */
    public static boolean fetchTimetableData(BaseBean requestBean) {
        Connection cnn = null;
        PreparedStatement ps = null;
        PreparedStatement countPs = null;
        ResultSet rs = null;
        ResultSet countRs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for timetable data fetch");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            // Extract parameters
            int page = Integer.parseInt(requestBean.getString("page"));
            int size = Integer.parseInt(requestBean.getString("size"));
            String id = requestBean.getString("id");
            String channelCode = requestBean.getString("channel_code");

            StringBuilder queryBuilder = new StringBuilder();
            StringBuilder countQueryBuilder = new StringBuilder();
            int parameterIndex = 1;

            // Base queries
            queryBuilder.append("SELECT ID, CHANNEL_CODE, SWITCH_CODE, HOUR, REQUEST_ID FROM ESBUSER.SWITCH_TIME_TABLE_MC");
            countQueryBuilder.append("SELECT COUNT(*) FROM ESBUSER.SWITCH_TIME_TABLE_MC");

            // Add WHERE conditions based on parameters
            if (channelCode != null && !channelCode.trim().isEmpty()) {
                // Filter by channel code (overrides ID)
                queryBuilder.append(" WHERE CHANNEL_CODE = ?");
                countQueryBuilder.append(" WHERE CHANNEL_CODE = ?");
                LOG.info("Filtering by channelCode: {}", channelCode);
            } else if (id != null && !id.trim().isEmpty()) {
                // Filter by ID
                queryBuilder.append(" WHERE ID = ?");
                countQueryBuilder.append(" WHERE ID = ?");
                LOG.info("Filtering by ID: {}", id);
            }

            // Add ORDER BY for consistent pagination
            queryBuilder.append(" ORDER BY CHANNEL_CODE, HOUR");

            // Add pagination using Oracle OFFSET...FETCH syntax
            int offset = (page - 1) * size;
            queryBuilder.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

            // Get total count first
            String countQuery = countQueryBuilder.toString();
            LOG.info("Count query: {}", countQuery);

            countPs = cnn.prepareStatement(countQuery);
            if (channelCode != null && !channelCode.trim().isEmpty()) {
                countPs.setString(1, channelCode);
            } else if (id != null && !id.trim().isEmpty()) {
                countPs.setString(1, id);
            }

            countRs = countPs.executeQuery();
            int totalRows = 0;
            if (countRs.next()) {
                totalRows = countRs.getInt(1);
            }

            // Calculate total pages
            int totalPages = (int) Math.ceil((double) totalRows / size);

            // Store pagination metadata
            requestBean.setString("current_page", String.valueOf(page));
            requestBean.setString("page_size", String.valueOf(size));
            requestBean.setString("total_rows", String.valueOf(totalRows));
            requestBean.setString("total_pages", String.valueOf(totalPages));

            // If no records found, return empty result
            if (totalRows == 0) {
                JsonArray emptyArray = Json.createArrayBuilder().build();
                requestBean.setString("timetable_data", JsonUtil.toStr(emptyArray));
                LOG.info("No records found for the given criteria");
                return true;
            }

            // Execute main query with pagination
            String mainQuery = queryBuilder.toString();
            LOG.info("Main query: {}", mainQuery);

            ps = cnn.prepareStatement(mainQuery);

            // Set parameters for main query
            if (channelCode != null && !channelCode.trim().isEmpty()) {
                ps.setString(parameterIndex++, channelCode);
            } else if (id != null && !id.trim().isEmpty()) {
                ps.setString(parameterIndex++, id);
            }

            // Set pagination parameters
            ps.setInt(parameterIndex++, offset);
            ps.setInt(parameterIndex, size);

            rs = ps.executeQuery();

            // Handle different response formats based on query type
            if (id != null && !id.trim().isEmpty()) {
                // Single record response (but still in array format)
                return processSingleRecord(rs, requestBean);
            } else {
                // Multiple records response (grouped by channel or all records)
                return processMultipleRecords(rs, requestBean, channelCode != null);
            }

        } catch (SQLException e) {
            LOG.error("SQL error in fetchTimetableData", e);
            requestBean.setString("message", "Database error during timetable data fetch: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in fetchTimetableData", e);
            requestBean.setString("message", "Error fetching timetable data: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (countRs != null) countRs.close();
                if (rs != null) rs.close();
                if (countPs != null) countPs.close();
                if (ps != null) ps.close();
                if (cnn != null) ConnectionUtil.closeConnection(cnn);
            } catch (SQLException e) {
                LOG.error("Error closing database resources", e);
            }
        }
    }

    /**
     * Process single record result (when filtering by ID)
     */
    private static boolean processSingleRecord(ResultSet rs, BaseBean requestBean) throws SQLException {
        JsonArrayBuilder responseDataBuilder = Json.createArrayBuilder();

        if (rs.next()) {
            String channelCode = rs.getString("CHANNEL_CODE");
            String id = String.valueOf(rs.getLong("ID"));
            String switchCode = rs.getString("SWITCH_CODE");
            String hour = String.valueOf(rs.getInt("HOUR"));
            String requestId = String.valueOf(rs.getLong("REQUEST_ID"));

            // Create switch detail object
            JsonObject switchDetail = Json.createObjectBuilder()
                    .add("id", id)
                    .add("switchCode", switchCode != null ? switchCode.trim() : "")
                    .add("time", hour)
                    .build();

            // Create single channel object with one switch detail
            JsonObject channelObject = Json.createObjectBuilder()
                    .add("channelCode", channelCode != null ? channelCode.trim() : "")
                    .add("request_id", requestId)
                    .add("switchDetails", Json.createArrayBuilder().add(switchDetail).build())
                    .build();

            responseDataBuilder.add(channelObject);
        }

        JsonArray responseData = responseDataBuilder.build();
        requestBean.setString("timetable_data", JsonUtil.toStr(responseData));

        LOG.info("Successfully processed single record");
        return true;
    }

    /**
     * Process multiple records result (when filtering by channel or all records)
     */
    private static boolean processMultipleRecords(ResultSet rs, BaseBean requestBean, boolean isChannelFiltered) throws SQLException {
        // Group data by channel code and store request_id per channel
        Map<String, List<JsonObject>> channelGroups = new LinkedHashMap<>();
        Map<String, String> channelRequestIds = new LinkedHashMap<>();

        while (rs.next()) {
            String channelCode = rs.getString("CHANNEL_CODE");
            String id = String.valueOf(rs.getLong("ID"));
            String switchCode = rs.getString("SWITCH_CODE");
            String hour = String.valueOf(rs.getInt("HOUR"));
            String requestId = String.valueOf(rs.getLong("REQUEST_ID"));

            // Store the request_id for this channel (should be same for all records of same channel)
            channelRequestIds.put(channelCode, requestId);

            // Create switch detail object
            JsonObject switchDetail = Json.createObjectBuilder()
                    .add("id", id)
                    .add("switchCode", switchCode != null ? switchCode.trim() : "")
                    .add("time", hour)
                    .build();

            // Group by channel code
            channelGroups.computeIfAbsent(channelCode, k -> new ArrayList<>()).add(switchDetail);
        }

        // Convert grouped data to required format
        JsonArrayBuilder responseDataBuilder = Json.createArrayBuilder();

        for (Map.Entry<String, List<JsonObject>> entry : channelGroups.entrySet()) {
            String channelCode = entry.getKey();
            List<JsonObject> switchDetails = entry.getValue();
            String requestId = channelRequestIds.get(channelCode); // Get request_id for this channel

            // Create switch details array
            JsonArrayBuilder switchDetailsBuilder = Json.createArrayBuilder();
            for (JsonObject switchDetail : switchDetails) {
                switchDetailsBuilder.add(switchDetail);
            }

            // Create channel object with its switch details and request_id
            JsonObject channelObject = Json.createObjectBuilder()
                    .add("channelCode", channelCode != null ? channelCode.trim() : "")
                    .add("request_id", requestId != null ? requestId : "0")
                    .add("switchDetails", switchDetailsBuilder.build())
                    .build();

            responseDataBuilder.add(channelObject);
        }

        JsonArray responseData = responseDataBuilder.build();
        requestBean.setString("timetable_data", JsonUtil.toStr(responseData));

        LOG.info("Successfully processed {} channel groups", channelGroups.size());
        return true;
    }

    /**
     * Check if channel already has existing timetable entries
     * @param requestBean Contains channel_code
     * @return true if channel has existing entries
     */
    public static boolean checkExistingTimetable(BaseBean requestBean) {
        String query = "SELECT COUNT(*) FROM ESBUSER.SWITCH_TIME_TABLE " +
                "WHERE CHANNEL_CODE = ? AND (DEL_STATUS IS NULL OR DEL_STATUS != 'Y')";

        Connection cnn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for existing timetable check");
                return false;
            }

            ps = cnn.prepareStatement(query);
            ps.setString(1, requestBean.getString("channel_code"));

            rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                LOG.info("Found {} existing timetable entries for channel: {}",
                        count, requestBean.getString("channel_code"));
                return count > 0;
            }

            return false;

        } catch (SQLException e) {
            LOG.error("SQL error in checkExistingTimetable for channel: {}",
                    requestBean.getString("channel_code"), e);
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in checkExistingTimetable for channel: {}",
                    requestBean.getString("channel_code"), e);
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

    /**
     * Check for conflicting hours in existing approved timetables for a channel
     * @param requestBean Contains channel_code and switch_details
     * @return true if there are conflicting hours
     */
    public static boolean checkHourConflicts(BaseBean requestBean) {
        String query = "SELECT HOUR FROM ESBUSER.SWITCH_TIME_TABLE " +
                "WHERE CHANNEL_CODE = ? AND HOUR = ? AND (DEL_STATUS IS NULL OR DEL_STATUS != 'Y')";

        Connection cnn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for hour conflict check");
                return false;
            }

            ps = cnn.prepareStatement(query);
            JsonArray switchDetailsArray = JsonUtil.toJsonArray(requestBean.getString("switch_details"));

            for (int i = 0; i < switchDetailsArray.size(); i++) {
                JsonObject switchDetail = switchDetailsArray.getJsonObject(i);
                String timeStr = JsonUtil.getJsonObjValue2(switchDetail, "time").trim();
                int hour = Integer.parseInt(timeStr);

                ps.setString(1, requestBean.getString("channel_code"));
                ps.setInt(2, hour);

                rs = ps.executeQuery();
                if (rs.next()) {
                    LOG.warn("Hour conflict found: Channel {} already has entry for hour {}",
                            requestBean.getString("channel_code"), hour);
                    requestBean.setString("message", "Hour " + hour + " is already assigned for this channel");
                    return true;
                }
                rs.close();
            }

            return false;

        } catch (SQLException e) {
            LOG.error("SQL error in checkHourConflicts for channel: {}",
                    requestBean.getString("channel_code"), e);
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in checkHourConflicts for channel: {}",
                    requestBean.getString("channel_code"), e);
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


    /**
     * Update timetable records in SWITCH_TIME_TABLE_MC
     * @param requestBean Contains channel_code, switch_details, updated_by, action_id
     * @return true if update successful
     */
    public static boolean updateTimetableRecords(BaseBean requestBean) {
        String updateQuery = "UPDATE " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_TIME_TABLE_MC " +
                "SET HOUR = ?, SWITCH_CODE = ?, CHANNEL_CODE = ? " +
                "WHERE ID = ?";

        String validateQuery = "SELECT ID, CHANNEL_CODE FROM " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_TIME_TABLE_MC " +
                "WHERE ID = ?";

        Connection cnn = null;
        PreparedStatement updatePs = null;
        PreparedStatement validatePs = null;
        ResultSet rs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for timetable update");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            cnn.setAutoCommit(false); // Start transaction

            // Parse switch details and validate/update records
            JsonArray switchDetailsArray = JsonUtil.toJsonArray(requestBean.getString("switch_details"));
            updatePs = cnn.prepareStatement(updateQuery);
            validatePs = cnn.prepareStatement(validateQuery);

            int totalUpdated = 0;
            for (int i = 0; i < switchDetailsArray.size(); i++) {
                JsonObject switchDetail = switchDetailsArray.getJsonObject(i);

                String idStr = JsonUtil.getJsonObjValue2(switchDetail, "id").trim();
                String switchCode = JsonUtil.getJsonObjValue2(switchDetail, "switchCode").trim().toUpperCase();
                String timeStr = JsonUtil.getJsonObjValue2(switchDetail, "time").trim();

                long id = Long.parseLong(idStr);
                int hour = Integer.parseInt(timeStr);

                // First validate that the record exists
                validatePs.setLong(1, id);
                rs = validatePs.executeQuery();

                if (!rs.next()) {
                    LOG.error("Record with ID {} not found", id);
                    requestBean.setString("message", "Record with ID " + id + " not found");
                    cnn.rollback();
                    return false;
                }

                String existingChannelCode = rs.getString("CHANNEL_CODE");

                // Verify the record belongs to the specified channel
                if (!requestBean.getString("channel_code").equals(existingChannelCode)) {
                    LOG.error("Record with ID {} belongs to channel {} but request is for channel {}",
                            id, existingChannelCode, requestBean.getString("channel_code"));
                    requestBean.setString("message", "Record with ID " + id + " does not belong to channel " + requestBean.getString("channel_code"));
                    cnn.rollback();
                    return false;
                }

                rs.close();

                // Update the record
                updatePs.setInt(1, hour);
                updatePs.setString(2, switchCode);
                updatePs.setString(3, requestBean.getString("channel_code"));
                updatePs.setLong(4, id);

                updatePs.addBatch();
                totalUpdated++;
            }

            // Execute batch update
            int[] batchResults = updatePs.executeBatch();

            // Verify all updates were successful
            for (int i = 0; i < batchResults.length; i++) {
                if (batchResults[i] <= 0) {
                    LOG.error("Batch update failed for one or more timetable entries at index {}", i);
                    requestBean.setString("message", "Failed to update all timetable entries");
                    cnn.rollback();
                    return false;
                }
            }

            cnn.commit(); // Commit transaction
            LOG.info("Successfully updated {} timetable records for channel: {}",
                    totalUpdated, requestBean.getString("channel_code"));

            return true;

        } catch (SQLException e) {
            LOG.error("SQL error in updateTimetableRecords for channel: {}",
                    requestBean.getString("channel_code"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Database error during timetable update: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in updateTimetableRecords for channel: {}",
                    requestBean.getString("channel_code"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Error processing timetable update: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (validatePs != null) validatePs.close();
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
     * Approve timetable request - update SWITCH_REQUESTS and move data to main table
     * @param requestBean Contains request_id, approved_by, approval_message
     * @return true if approval successful
     */
    public static boolean approveTimetableRequest(BaseBean requestBean) {
        String updateRequestQuery = "UPDATE " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_REQUESTS " +
                "SET APPROVED_BY = ?, APPROVAL_DATE = TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'), " +
                "MESSAGE = ?, STATUS = 'APPROVED' " +
                "WHERE ID = ? AND STATUS = 'PENDING'";

        String getRequestDataQuery = "SELECT CREATED_BY, CREATED_AT, APPROVED_BY, APPROVAL_DATE " +
                "FROM " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_REQUESTS " +
                "WHERE ID = ?";

        String getMcDataQuery = "SELECT HOUR, SWITCH_CODE, CHANNEL_CODE " +
                "FROM " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_TIME_TABLE_MC " +
                "WHERE REQUEST_ID = ?";

        String insertMainTableQuery = "INSERT INTO " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_TIME_TABLE " +
                "(HOUR, SWITCH_CODE, CHANNEL_CODE, CREATED_BY, CREATED_AT, APPROVED_BY, APPROVAL_DATE) " +
                "VALUES (?, ?, ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'))";

        Connection cnn = null;
        PreparedStatement updatePs = null;
        PreparedStatement getRequestPs = null;
        PreparedStatement getMcPs = null;
        PreparedStatement insertPs = null;
        ResultSet requestRs = null;
        ResultSet mcRs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for timetable approval");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            cnn.setAutoCommit(false); // Start transaction

            // Step 1: Update SWITCH_REQUESTS table
            updatePs = cnn.prepareStatement(updateRequestQuery);
            updatePs.setString(1, requestBean.getString("approved_by"));
            updatePs.setString(2, requestBean.getString("approval_message"));
            updatePs.setLong(3, Long.parseLong(requestBean.getString("request_id")));

            int requestRowsUpdated = updatePs.executeUpdate();
            if (requestRowsUpdated == 0) {
                LOG.error("No rows updated in SWITCH_REQUESTS table. Request may not exist or already processed.");
                requestBean.setString("message", "Request not found or already processed");
                cnn.rollback();
                return false;
            }

            // Step 2: Get request data for main table insertion
            getRequestPs = cnn.prepareStatement(getRequestDataQuery);
            getRequestPs.setLong(1, Long.parseLong(requestBean.getString("request_id")));
            requestRs = getRequestPs.executeQuery();

            if (!requestRs.next()) {
                LOG.error("Failed to retrieve request data for request_id: {}", requestBean.getString("request_id"));
                requestBean.setString("message", "Failed to retrieve request data");
                cnn.rollback();
                return false;
            }

            String createdBy = requestRs.getString("CREATED_BY");
            String createdAt = requestRs.getString("CREATED_AT");
            String approvedBy = requestRs.getString("APPROVED_BY");
            String approvalDate = requestRs.getString("APPROVAL_DATE");

            // Step 3: Get MC table data and insert into main table
            getMcPs = cnn.prepareStatement(getMcDataQuery);
            getMcPs.setLong(1, Long.parseLong(requestBean.getString("request_id")));
            mcRs = getMcPs.executeQuery();

            insertPs = cnn.prepareStatement(insertMainTableQuery);
            int totalInserted = 0;

            while (mcRs.next()) {
                int hour = mcRs.getInt("HOUR");
                String switchCode = mcRs.getString("SWITCH_CODE");
                String channelCode = mcRs.getString("CHANNEL_CODE");

                insertPs.setInt(1, hour);
                insertPs.setString(2, switchCode);
                insertPs.setString(3, channelCode);
                insertPs.setString(4, createdBy);
                insertPs.setString(5, createdAt);
                insertPs.setString(6, approvedBy);
                insertPs.setString(7, approvalDate);

                insertPs.addBatch();
                totalInserted++;
            }

            if (totalInserted == 0) {
                LOG.error("No MC records found for request_id: {}", requestBean.getString("request_id"));
                requestBean.setString("message", "No timetable data found for this request");
                cnn.rollback();
                return false;
            }

            // Execute batch insert
            int[] batchResults = insertPs.executeBatch();

            // Verify all inserts were successful
            for (int result : batchResults) {
                if (result <= 0) {
                    LOG.error("Batch insert failed for one or more timetable entries");
                    requestBean.setString("message", "Failed to insert all timetable entries");
                    cnn.rollback();
                    return false;
                }
            }

            cnn.commit(); // Commit transaction
            LOG.info("Successfully approved timetable request {} with {} entries moved to main table",
                    requestBean.getString("request_id"), totalInserted);

            return true;

        } catch (SQLException e) {
            LOG.error("SQL error in approveTimetableRequest for request_id: {}",
                    requestBean.getString("request_id"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Database error during timetable approval: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in approveTimetableRequest for request_id: {}",
                    requestBean.getString("request_id"), e);
            try {
                if (cnn != null) cnn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error rolling back transaction", rollbackEx);
            }
            requestBean.setString("message", "Error processing timetable approval: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (mcRs != null) mcRs.close();
                if (requestRs != null) requestRs.close();
                if (insertPs != null) insertPs.close();
                if (getMcPs != null) getMcPs.close();
                if (getRequestPs != null) getRequestPs.close();
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
     * Reject timetable request - update SWITCH_REQUESTS table only
     * @param requestBean Contains request_id, approved_by, approval_message
     * @return true if rejection successful
     */
    public static boolean rejectTimetableRequest(BaseBean requestBean) {
        String updateRequestQuery = "UPDATE " + AppConstants.DbTables.TABLE_SPACE + ".SWITCH_REQUESTS " +
                "SET APPROVED_BY = ?, APPROVAL_DATE = TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'), " +
                "MESSAGE = ?, STATUS = 'UNAPPROVED' " +
                "WHERE ID = ? AND STATUS = 'PENDING'";

        Connection cnn = null;
        PreparedStatement updatePs = null;

        try {
            cnn = ConnectionUtil.getConnection();
            if (cnn == null) {
                LOG.error("Failed to get database connection for timetable rejection");
                requestBean.setString("message", "Database connection failed");
                return false;
            }

            updatePs = cnn.prepareStatement(updateRequestQuery);
            updatePs.setString(1, requestBean.getString("approved_by"));
            updatePs.setString(2, requestBean.getString("approval_message"));
            updatePs.setLong(3, Long.parseLong(requestBean.getString("request_id")));

            int requestRowsUpdated = updatePs.executeUpdate();
            if (requestRowsUpdated == 0) {
                LOG.error("No rows updated in SWITCH_REQUESTS table. Request may not exist or already processed.");
                requestBean.setString("message", "Request not found or already processed");
                return false;
            }

            LOG.info("Successfully rejected timetable request: {}", requestBean.getString("request_id"));
            return true;

        } catch (SQLException e) {
            LOG.error("SQL error in rejectTimetableRequest for request_id: {}",
                    requestBean.getString("request_id"), e);
            requestBean.setString("message", "Database error during timetable rejection: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Unexpected error in rejectTimetableRequest for request_id: {}",
                    requestBean.getString("request_id"), e);
            requestBean.setString("message", "Error processing timetable rejection: " + e.getMessage());
            return false;
        } finally {
            // Close resources
            try {
                if (updatePs != null) updatePs.close();
                if (cnn != null) ConnectionUtil.closeConnection(cnn);
            } catch (SQLException e) {
                LOG.error("Error closing database resources", e);
            }
        }
    }
}