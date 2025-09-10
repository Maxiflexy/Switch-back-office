package messaging.fileUtils.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ConnectionUtil;
import util.BaseBean;
import util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.*;

public class TransactionsDbHelper {

    final static Logger LOG = LogManager.getLogger(TransactionsDbHelper.class);

    public static boolean getTransactionStatistics(messaging.fileUtils.util.BaseBean requestBean) {
//        if (requestBean.getString("size").equals("")) {
//            requestBean.setString("size", "10");
//        }
//        if (requestBean.getString("page").equals("")) {
//            requestBean.setString("page", "1");
//        }
//
//        int limit = Integer.parseInt(requestBean.get("size").equals("") ? "10" : requestBean.get("size"));
//        int offset = (Integer.parseInt(requestBean.get("page").equals("") ? "1" : requestBean.get("page")) - 1) * limit;

        String switchType = requestBean.getString("switch-type");
        StringBuilder sb = new StringBuilder("SELECT * FROM ")
                .append(getTableName(switchType))
                .append(" where");
        StringBuilder sbCount = new StringBuilder("SELECT count(*) as transaction_count FROM ")
                .append(getTableName(switchType))
                .append(" where");
        boolean isAndNeeded = false;

        if (requestBean.containsKey("start-date") && requestBean.containsKey("end-date")) {
            sb.append(" REQUESTDATE BETWEEN TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')");
            sbCount.append(" REQUESTDATE BETWEEN TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')");
            isAndNeeded = true;
        } else if (requestBean.containsKey("start-date")) {
            sb.append(" REQUESTDATE >= TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')");
            sbCount.append(" REQUESTDATE >= TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')");
            isAndNeeded = true;
        } else if (requestBean.containsKey("end-date")) {
            sb.append(" REQUESTDATE <= TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') ");
            sbCount.append(" REQUESTDATE <= TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') ");
            isAndNeeded = true;
        }
        if (requestBean.containsKey("session-id")) {
            if (isAndNeeded) {
                sb.append(" and");
                sbCount.append(" and");
            }
            sb.append(" sessionid like ?");
            sbCount.append(" sessionid like ?");
            isAndNeeded = true;
        }
        if (requestBean.containsKey("account-number")) {
            if (isAndNeeded) {
                sb.append(" and");
                sbCount.append(" and");
            }
            sb.append(" beneficiaryaccountnumber like ? || originatoraccountnumber like ? ");
            sbCount.append(" beneficiaryaccountnumber like ? || originatoraccountnumber like ? ");
            isAndNeeded = true;
        }

        if (requestBean.containsKey("isSuccessful")) {
            if (Boolean.parseBoolean(requestBean.getString("isSuccessful"))) {
                if (isAndNeeded) {
                    sb.append(" and");
                    sbCount.append(" and");
                }
                switch (switchType) {
                    case "up_inflow":
                        sb.append(" (RESPONSECODE = '00' or TSQ_2_RSP_CODE = '00') and C24_RSP_CODE = '000'");
                        sbCount.append(" (RESPONSECODE = '00' or TSQ_2_RSP_CODE = '00') and C24_RSP_CODE = '000'");
                        break;
                    case "up_outflow":
                        sbCount.append(" DEBIT_RSP_CODE in ('000', '913') and (its_rsp_code = '00' or its_tsq_rsp_code = '00')");
                        break;
                    case "nip_inflow":
                        sb.append("");
                        break;
                    case "nip_outflow":
                        sb.append(" responsecode = '00' and C24_RSP_CODE = '000'");
                        sbCount.append(" responsecode = '00' and C24_RSP_CODE = '000'");
                        break;
                }
            }

        }
//        sb.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit).append(" ROWS ONLY");
        String query = sb.toString();
        String countQuery = sbCount.toString();

        boolean success = false;
        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        LOG.info("Fetching transactions: {}", query);
        PreparedStatement ps = null;
        try {
            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(query);
            addParametersTo(requestBean, ps, kk);
            fetchTotalCount(countQuery, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                List<messaging.fileUtils.util.BaseBean> transactions = new ArrayList<>();
                switch (switchType) {
                    case "up_inflow":
                        while (rs.next()) {
                            fetchUpInflowData(rs, transactions);
                        }
                        break;
                    case "nip_inflow":
                        while (rs.next()) {
                            fetchInflowData(rs, transactions);
                        }
                        break;

                    case "up_outflow":
                        while (rs.next()) {
                            fetchUPSLOutflowData(rs, transactions);
                        }
                        break;

                    case "nip_outflow":
                        while (rs.next()) {
                            fetchNIPOutflowTransactions(rs, transactions);
                        }
                        break;

                }
                success = true;
                requestBean.setString("transaction_response", messaging.fileUtils.util.JsonUtil.convertBaseBeanListToJsonString(transactions));

            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

            }


        } catch (SQLException e) {
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

            messaging.fileUtils.persistence.ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }

    private static boolean fetchTotalCount(String countQuery, messaging.fileUtils.util.BaseBean requestBean) {

        boolean success = false;
        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        LOG.info("Counting response: {}", countQuery);

        PreparedStatement ps = null;
        try {

            int kk = 0;
            cnn.setAutoCommit(false);

            ps = cnn.prepareStatement(countQuery);
            addParametersTo(requestBean, ps, 0);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String count = rs.getString("transaction_count");
                    success = true;
                    requestBean.setString("transaction_count", count);
                }
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

            }


        } catch (SQLException e) {
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

            messaging.fileUtils.persistence.ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }

    private static void addParametersTo(messaging.fileUtils.util.BaseBean requestBean, PreparedStatement ps, int kk) throws SQLException {
        if (requestBean.containsKey("start-date") && requestBean.containsKey("end-date")) {
            ps.setString(++kk, requestBean.getString("start-date").replace("T", " "));
            ps.setString(++kk, requestBean.getString("end-date").replace("T", " "));
        } else if (requestBean.containsKey("start-date")) {
            ps.setString(++kk, requestBean.getString("start-date").replace("T", " "));
        } else if (requestBean.containsKey("end-date")) {
            ps.setString(++kk, requestBean.getString("end-date").replace("T", " "));
        }
        if (requestBean.containsKey("session-id")) {
            ps.setString(++kk, requestBean.getString("session-id").concat("%"));
        }
        if (requestBean.containsKey("account-number")) {
            ps.setString(++kk, requestBean.getString("account-number").concat("%"));
            ps.setString(++kk, requestBean.getString("account-number").concat("%"));
        }
    }

    private static void fetchUpInflowData(ResultSet rs, List<messaging.fileUtils.util.BaseBean> transactions) throws SQLException {
        messaging.fileUtils.util.BaseBean inflowTransactions = new messaging.fileUtils.util.BaseBean();
        inflowTransactions.setString("responsedate", rs.getString("responsedate"));
        inflowTransactions.setString("sessionid", rs.getString("sessionid"));
        inflowTransactions.setString("destinationinstitutioncode", rs.getString("destinationinstitutioncode"));
        inflowTransactions.setString("channelcode", rs.getString("channelcode"));
        inflowTransactions.setString("accountnumber", rs.getString("accountnumber"));
        inflowTransactions.setString("bankverificationnumber", rs.getString("bankverificationnumber"));
        inflowTransactions.setString("responsecode", rs.getString("responsecode"));
        inflowTransactions.setString("nameenquiryref", rs.getString("nameenquiryref"));
        inflowTransactions.setString("beneficiaryaccountname", rs.getString("beneficiaryaccountname"));
        inflowTransactions.setString("beneficiaryaccountnumber", rs.getString("beneficiaryaccountnumber"));
        inflowTransactions.setString("originatoraccountname", rs.getString("originatoraccountname"));
        inflowTransactions.setString("originatoraccountnumber", rs.getString("originatoraccountnumber"));
        inflowTransactions.setString("paymentreference", rs.getString("paymentreference"));
        inflowTransactions.setString("amount", rs.getString("amount"));
        inflowTransactions.setString("tsq_2_flg", rs.getString("tsq_2_flg"));
        inflowTransactions.setString("tsq_2_date", rs.getString("tsq_2_date"));
        inflowTransactions.setString("tsq_2_rsp_code", rs.getString("tsq_2_rsp_code"));
        inflowTransactions.setString("tsq_2_count", rs.getString("tsq_2_count"));
        inflowTransactions.setString("c24_rsp_code", rs.getString("c24_rsp_code"));
        inflowTransactions.setString("c24_rsp_date", rs.getString("c24_rsp_date"));
        inflowTransactions.setString("c24_rsp_flg", rs.getString("c24_rsp_flg"));
        inflowTransactions.setString("c24_num_trial", rs.getString("c24_num_trial"));
        inflowTransactions.setString("c24_rev_rsp_code", rs.getString("c24_rev_rsp_code"));
        inflowTransactions.setString("c24_rev_rsp_date", rs.getString("c24_rev_rsp_date"));
        inflowTransactions.setString("c24_rev_rsp_flg", rs.getString("c24_rev_rsp_flg"));
        inflowTransactions.setString("c24_rev_num_trial", rs.getString("c24_rev_num_trial"));
        inflowTransactions.setString("reversal_rsp_code", rs.getString("reversal_rsp_code"));
        inflowTransactions.setString("reversal_rsp_date", rs.getString("reversal_rsp_date"));
        inflowTransactions.setString("reversal_rsp_flg", rs.getString("reversal_rsp_flg"));
        inflowTransactions.setString("reversal_num_trial", rs.getString("reversal_num_trial"));
        transactions.add(inflowTransactions);
    }


    private static void fetchInflowData(ResultSet rs, List<messaging.fileUtils.util.BaseBean> transactions) throws SQLException {
        messaging.fileUtils.util.BaseBean inflowTransactions = new messaging.fileUtils.util.BaseBean();
        inflowTransactions.setString("responsedate", rs.getString("responsedate"));
        inflowTransactions.setString("sessionid", rs.getString("sessionid"));
        inflowTransactions.setString("destinationinstitutioncode", rs.getString("destinationinstitutioncode"));
        inflowTransactions.setString("channelcode", rs.getString("channelcode"));
        inflowTransactions.setString("accountnumber", rs.getString("accountnumber"));
        inflowTransactions.setString("bankverificationnumber", rs.getString("bankverificationnumber"));
        inflowTransactions.setString("responsecode", rs.getString("responsecode"));
        inflowTransactions.setString("nameenquiryref", rs.getString("nameenquiryref"));
        inflowTransactions.setString("beneficiaryaccountname", rs.getString("beneficiaryaccountname"));
        inflowTransactions.setString("beneficiaryaccountnumber", rs.getString("beneficiaryaccountnumber"));
        inflowTransactions.setString("originatoraccountname", rs.getString("originatoraccountname"));
        inflowTransactions.setString("originatoraccountnumber", rs.getString("originatoraccountnumber"));
        inflowTransactions.setString("paymentreference", rs.getString("paymentreference"));
        inflowTransactions.setString("amount", rs.getString("amount"));
        inflowTransactions.setString("tsq_2_flg", rs.getString("tsq_2_flg"));
        inflowTransactions.setString("tsq_2_date", rs.getString("tsq_2_date"));
        inflowTransactions.setString("tsq_2_rsp_code", rs.getString("tsq_2_rsp_code"));
        inflowTransactions.setString("tsq_2_count", rs.getString("tsq_2_count"));
        inflowTransactions.setString("c24_rsp_code", rs.getString("c24_rsp_code"));
        inflowTransactions.setString("c24_rsp_date", rs.getString("c24_rsp_date"));
        inflowTransactions.setString("c24_rsp_flg", rs.getString("c24_rsp_flg"));
        inflowTransactions.setString("c24_num_trial", rs.getString("c24_num_trial"));
        inflowTransactions.setString("c24_rev_rsp_code", rs.getString("c24_rev_rsp_code"));
        inflowTransactions.setString("c24_rev_rsp_date", rs.getString("c24_rev_rsp_date"));
        inflowTransactions.setString("c24_rev_rsp_flg", rs.getString("c24_rev_rsp_flg"));
        inflowTransactions.setString("c24_rev_num_trial", rs.getString("c24_rev_num_trial"));

        transactions.add(inflowTransactions);
    }

    private static void fetchUPSLOutflowData(ResultSet rs, List<messaging.fileUtils.util.BaseBean> transactions) throws SQLException {
        messaging.fileUtils.util.BaseBean outflowTransactions = new messaging.fileUtils.util.BaseBean();
        outflowTransactions.setString("responsedate", rs.getString("responsedate"));
        outflowTransactions.setString("sessionid", rs.getString("sessionid"));
        outflowTransactions.setString("destinationinstitutioncode", rs.getString("destinationinstitutioncode"));
        outflowTransactions.setString("channelcode", rs.getString("channelcode"));
        outflowTransactions.setString("responsecode", rs.getString("responsecode"));
        outflowTransactions.setString("nameenquiryref", rs.getString("nameenquiryref"));
        outflowTransactions.setString("beneficiaryaccountname", rs.getString("beneficiaryaccountname"));
        outflowTransactions.setString("beneficiaryaccountnumber", rs.getString("beneficiaryaccountnumber"));
        outflowTransactions.setString("originatoraccountname", rs.getString("originatoraccountname"));
        outflowTransactions.setString("originatoraccountnumber", rs.getString("originatoraccountnumber"));
        outflowTransactions.setString("paymentreference", rs.getString("paymentreference"));
        outflowTransactions.setString("amount", rs.getString("amount"));

        outflowTransactions.setString("tsq_2_flg", rs.getString("tsq_2_flg"));
        outflowTransactions.setString("tsq_2_date", rs.getString("tsq_2_date"));
        outflowTransactions.setString("tsq_2_rsp_code", rs.getString("tsq_2_rsp_code"));
        outflowTransactions.setString("tsq_2_count", rs.getString("tsq_2_count"));

        outflowTransactions.setString("its_rsp_code", rs.getString("its_rsp_code"));
        outflowTransactions.setString("its_rsp_date", rs.getString("its_rsp_date"));
        outflowTransactions.setString("its_rsp_flg", rs.getString("its_rsp_flg"));

        outflowTransactions.setString("debit_rsp_code", rs.getString("debit_rsp_code"));
        outflowTransactions.setString("debit_rsp_date", rs.getString("debit_rsp_date"));
        outflowTransactions.setString("debit_rsp_flg", rs.getString("debit_rsp_flg"));

        outflowTransactions.setString("reversal_rsp_code", rs.getString("reversal_rsp_code"));
        outflowTransactions.setString("reversal_rsp_date", rs.getString("reversal_rsp_date"));

        outflowTransactions.setString("its_tsq_flg", rs.getString("its_tsq_flg"));
        outflowTransactions.setString("its_tsq_date", rs.getString("its_tsq_date"));
        outflowTransactions.setString("its_tsq_rsp_code", rs.getString("its_tsq_rsp_code"));
        outflowTransactions.setString("its_tsq_count", rs.getString("its_tsq_count"));

        transactions.add(outflowTransactions);
    }

    private static void fetchNIPOutflowTransactions(ResultSet rs, List<messaging.fileUtils.util.BaseBean> transactions) throws SQLException {
        messaging.fileUtils.util.BaseBean outflowTransactions = new messaging.fileUtils.util.BaseBean();

        outflowTransactions.setString("responsedate", rs.getString("responsedate"));
        outflowTransactions.setString("sessionid", rs.getString("sessionid"));
        outflowTransactions.setString("destinationinstitutioncode", rs.getString("destinationinstitutioncode"));
        outflowTransactions.setString("channelcode", rs.getString("channelcode"));
        outflowTransactions.setString("responsecode", rs.getString("responsecode"));
        outflowTransactions.setString("nameenquiryref", rs.getString("nameenquiryref"));
        outflowTransactions.setString("beneficiaryaccountname", rs.getString("beneficiaryaccountname"));
        outflowTransactions.setString("beneficiaryaccountnumber", rs.getString("beneficiaryaccountnumber"));
        outflowTransactions.setString("originatoraccountname", rs.getString("originatoraccountname"));
        outflowTransactions.setString("originatoraccountnumber", rs.getString("originatoraccountnumber"));
        outflowTransactions.setString("paymentreference", rs.getString("paymentreference"));
        outflowTransactions.setString("amount", rs.getString("amount"));

        outflowTransactions.setString("tsq_2_flg", rs.getString("tsq_2_flg"));
        outflowTransactions.setString("tsq_2_date", rs.getString("tsq_2_date"));
        outflowTransactions.setString("tsq_2_rsp_code", rs.getString("tsq_2_rsp_code"));
        outflowTransactions.setString("tsq_2_count", rs.getString("tsq_2_count"));

        outflowTransactions.setString("nip_rsp_code", rs.getString("nip_rsp_code"));
        outflowTransactions.setString("nip_rsp_date", rs.getString("nip_rsp_date"));
        outflowTransactions.setString("nip_rsp_flg", rs.getString("nip_rsp_flg"));

        outflowTransactions.setString("debit_rsp_code", rs.getString("debit_rsp_code"));
        outflowTransactions.setString("debit_rsp_date", rs.getString("debit_rsp_date"));
        outflowTransactions.setString("debit_rsp_flg", rs.getString("debit_rsp_flg"));

        outflowTransactions.setString("reversal_rsp_code", rs.getString("reversal_rsp_code"));
        outflowTransactions.setString("reversal_rsp_date", rs.getString("reversal_rsp_date"));

        outflowTransactions.setString("nip_tsq_flg", rs.getString("nip_tsq_flg"));
        outflowTransactions.setString("nip_tsq_date", rs.getString("nip_tsq_date"));
        outflowTransactions.setString("nip_tsq_rsp_code", rs.getString("nip_tsq_rsp_code"));
        outflowTransactions.setString("nip_tsq_count", rs.getString("nip_tsq_count"));
        transactions.add(outflowTransactions);
    }

    private static String getTableName(String switchType) {
        String response;
        switch (switchType) {
            case "up_inflow":
                response = UP_INFLOW_TRANSACTION;
                break;
            case "up_outflow":
                response = UP_OUTFLOW_TRANSACTION;
                break;
            case "nip_outflow":
                response = NIP_OUTFLOW_TRANSACTION;
                break;
            case "nip_inflow":
                response = NIP_INFLOW_TRANSACTION;
                break;
            default:
                throw new RuntimeException("Switch type unknown");
        }
        return response;
    }


    public static boolean fetchPendingInflowTransactions(messaging.fileUtils.util.BaseBean requestBean) {
        if (requestBean.getString("size").equals("")) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").equals("")) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));
        String query = "SELECT requestdate, sessionid,  amount".concat(createPendingInflowTransactionQuery(requestBean));
        query = query.concat(" OFFSET ").concat(offset).concat(" ROWS FETCH NEXT ").concat(limit).concat(" ROWS ONLY");

        boolean success = false;
        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching uploaded files {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createPendingInflowStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                List<messaging.fileUtils.util.BaseBean> transactions = new ArrayList<>();
                while (rs.next()) {
                    messaging.fileUtils.util.BaseBean documentBean = new messaging.fileUtils.util.BaseBean();
                    try {
                        documentBean.put("tran_ref", rs.getString("sessionid"));
                        documentBean.put("tran_date", rs.getString("requestdate"));
                        documentBean.put("tran_amt", rs.getString("amount"));
                        transactions.add(documentBean);
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
                requestBean.setString("jsonBean", messaging.fileUtils.util.JsonUtil.convertBaseBeanListToJsonString(transactions));
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
            messaging.fileUtils.persistence.ConnectionUtil.closeConnection(cnn);
        }
        return success;


    }

    private static void createPendingInflowStatementVariables(PreparedStatement ps, messaging.fileUtils.util.BaseBean requestBean) throws SQLException {
        int kk = 0;
        if (requestBean.getString("operation_type").equalsIgnoreCase("posting")) {
            ps.setString(++kk, "FTSingleCreditRequest");
            ps.setString(++kk, "00");
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
            ps.setString(++kk, "0");
        } else if (requestBean.getString("operation_type").equalsIgnoreCase("tsq")) {
            ps.setString(++kk, "FTSingleCreditRequest");
            ps.setString(++kk, "00");
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
            ps.setString(++kk, "0");
        }
    }


    private static void fetchTotalRecordCount(messaging.fileUtils.util.BaseBean requestBean) {
        String query = "SELECT COUNT(*) as count".concat(createPendingInflowTransactionQuery(requestBean));

        boolean success = false;
        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching  transaction count {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createPendingInflowStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_count", rs.getString("count"));
                }
                success = true;
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
                e.printStackTrace();

            }


        } catch (SQLException e) {
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
            messaging.fileUtils.persistence.ConnectionUtil.closeConnection(cnn);
        }

    }

    private static String createPendingInflowTransactionQuery(messaging.fileUtils.util.BaseBean requestBean) {
        StringBuilder query = new StringBuilder();
        if (requestBean.getString("operation_type").equalsIgnoreCase("posting")) {
            query.append(" from ")
                    .append(NIP_INFLOW_TRANSACTION)
                    .append(" where TRANTYPE=? and tsq_2_rsp_code =? and tsq_2_date between TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') and (c24_rsp_flg='N' OR c24_rsp_code NOT IN ('000','913')) and txn_posting_fallback_flg='N' and tranid > ? ");
        } else if (requestBean.getString("operation_type").equalsIgnoreCase("tsq")) {
            query.append(" from ")
                    .append(NIP_INFLOW_TRANSACTION)
                    .append(" where trantype= ? and responsecode = ? and responsedate between TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') and (tsq_2_flg='N' OR tsq_2_rsp_code in ('97', '99','25')) and tsq_fallback_flg='N' and TranID > ? ");
        }
        if (!requestBean.getString("switch").isEmpty()) {
            query.append(" and clientname = ").append(requestBean.getString("switch").equalsIgnoreCase("nip") ? "NIBSS" : "ETZ");
        }

        query.append(" order by TranID asc");
        return query.toString();
    }

    public static boolean fetchPendingOutflowTransactions(messaging.fileUtils.util.BaseBean requestBean) {
        if (requestBean.getString("size").isEmpty()) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").isEmpty()) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));
        String query = "SELECT requestdate, sessionid,  amount".concat(createPendingOutflowTransactionQuery(requestBean));
        query = query.concat(" OFFSET ").concat(offset).concat(" ROWS FETCH NEXT ").concat(limit).concat(" ROWS ONLY");

        boolean success = false;
        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching uploaded files {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createPendingOutflowStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                List<messaging.fileUtils.util.BaseBean> transactions = new ArrayList<>();
                while (rs.next()) {
                    messaging.fileUtils.util.BaseBean documentBean = new messaging.fileUtils.util.BaseBean();
                    try {
                        documentBean.put("tran_ref", rs.getString("sessionid"));
                        documentBean.put("tran_date", rs.getString("requestdate"));
                        documentBean.put("tran_amt", rs.getString("amount"));
                        transactions.add(documentBean);
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
                requestBean.setString("jsonBean", messaging.fileUtils.util.JsonUtil.convertBaseBeanListToJsonString(transactions));
                fetchTotalOutflowRecordCount(requestBean);
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
            messaging.fileUtils.persistence.ConnectionUtil.closeConnection(cnn);
        }
        return success;
    }

    public static void fetchTotalOutflowRecordCount(messaging.fileUtils.util.BaseBean requestBean) {
        String query = "SELECT COUNT(*) as count".concat(createPendingOutflowTransactionQuery(requestBean));

        boolean success = false;
        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching Outflow transaction count {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createPendingOutflowStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_count", rs.getString("count"));
                }
                success = true;
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
            }


        } catch (SQLException e) {
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
            messaging.fileUtils.persistence.ConnectionUtil.closeConnection(cnn);
        }
    }

    private static void createPendingOutflowStatementVariables(PreparedStatement ps, messaging.fileUtils.util.BaseBean requestBean) throws SQLException {
        int kk = 0;
        if (requestBean.getString("operation_type").equalsIgnoreCase("reversal")) {
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
        } else if (requestBean.getString("operation_type").equalsIgnoreCase("tsq")) {
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
        }
    }

    public static String createPendingOutflowTransactionQuery(messaging.fileUtils.util.BaseBean requestBean) {
        StringBuilder query =  new StringBuilder();
        if (requestBean.getString("operation_type").equalsIgnoreCase("reversal")) {
            query.append(" FROM ")
                    .append(UP_OUTFLOW_TRANSACTION)
//                    SWITCH FAILED AND TSQ FAILED AND (REVERSAL FAILED OR NO REVERSAL)
                    .append(" WHERE ( DEBIT_RSP_CODE = '911' OR (DEBIT_RSP_CODE = '000' AND (its_rsp_code NOT IN ('00','09', '99', '25', '26', '94', '01') OR its_tsq_rsp_code NOT IN ('00', '09', '99', '25', '94', '01')) AND ((REVERSAL_RSP_CODE NOT IN ('000','913') and REVERSAL_FLG='Y') OR REVERSAL_FLG='N')))")
                    .append(" AND DEBIT_RSP_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
        } else if (requestBean.getString("operation_type").equalsIgnoreCase("tsq")) {
            query.append(" FROM ")
                    .append(UP_OUTFLOW_TRANSACTION)
//                    DEBIT SUCCESSFUL AND SWITCH SUCCESSFULL OR NO RESPONSE FROM TSQ)
                    .append(" WHERE  DEBIT_RSP_CODE IN ('000') AND ((its_rsp_code IN ('09', '99', '25', '26', '94', '01')) AND ((its_tsq_flg = 'N') OR (its_tsq_rsp_code IN ('09', '99', '25', '94', '01'))))  ")
                    .append(" AND DEBIT_RSP_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + requestBean.getString("operation_type"));
        }
        return query.toString();
    }

    public static boolean fetchPendingAirtimeTransactions(messaging.fileUtils.util.BaseBean requestBean) {
        if (requestBean.getString("size").isEmpty()) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").isEmpty()) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));
        String query = "SELECT entrydate as requestdate, TOPUP_REF_ID as sessionid,  txnamt as amount".concat(createPendingAirtimeTransactionQuery(requestBean));
        query = query.concat(" OFFSET ").concat(offset).concat(" ROWS FETCH NEXT ").concat(limit).concat(" ROWS ONLY");

        boolean success = false;

        Connection cnn = messaging.fileUtils.persistence.ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching Airtime pending trans- query: {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createPendingAirtimeStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                List<messaging.fileUtils.util.BaseBean> transactions = new ArrayList<>();
                while (rs.next()) {
                    BaseBean documentBean = new BaseBean();
                    try {
                        documentBean.put("tran_ref", rs.getString("sessionid"));
                        documentBean.put("tran_date", rs.getString("requestdate"));
                        documentBean.put("tran_amt", rs.getString("amount"));
                        transactions.add(documentBean);
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(transactions));
                fetchAirtimeTotalRecordCount(requestBean);
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
        return success;
    }

    public static void fetchAirtimeTotalRecordCount(BaseBean requestBean) {
        String query = "SELECT COUNT(*) as count".concat(createPendingAirtimeTransactionQuery(requestBean));

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching Airtime transaction count {}", query);

        try {
            ps = cnn.prepareStatement(query);
            createPendingAirtimeStatementVariables(ps, requestBean);
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_count", rs.getString("count"));
                }
                success = true;
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
            }


        } catch (SQLException e) {
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

    private static void createPendingAirtimeStatementVariables(PreparedStatement ps, BaseBean requestBean) throws SQLException {
        int kk = 0;
        if (requestBean.getString("operation_type").equalsIgnoreCase("reversal")) {
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
        } else if (requestBean.getString("operation_type").equalsIgnoreCase("tsq")) {
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));
        }
    }

    public static String createPendingAirtimeTransactionQuery(BaseBean requestBean) {
        StringBuilder query =  new StringBuilder();
        if (requestBean.getString("operation_type").equalsIgnoreCase("reversal")) {
            query.append(" FROM ")
                    .append(AIRTIME_TABLE)
                    .append(" WHERE (topup_rsp_code <> 'SUC' AND topup_rsp_code_2 <> '00') AND ((DEBIT_REVERSAL_RSP_CODE <> '000' AND DEBIT_REVERSAL_RSP_FLG='Y') OR DEBIT_REVERSAL_RSP_FLG='N')" )
                    .append(" AND DEBIT_RSP_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
        } else if (requestBean.getString("operation_type").equalsIgnoreCase("tsq")) {
            query.append(" FROM ")
                    .append(AIRTIME_TABLE)
                    .append(" WHERE  DEBIT_RSP_CODE IN ('000') AND (topup_rsp_code = 'UNKW' and tsq_rsp_code in ('UNKW', 'QER')) OR ((topup_rsp_code = 'UNKW' or topup_rsp_code is null) and tsq_rsp_code is null) AND tsq_rsp_code <> 'SUC'")
                    .append(" AND DEBIT_RSP_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') AND TO_DATE(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + requestBean.getString("operation_type"));
        }
        return query.toString();
    }
}
