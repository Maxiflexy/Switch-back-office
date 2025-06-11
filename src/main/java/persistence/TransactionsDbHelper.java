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

import static constants.AppConstants.DbTables.*;

public class TransactionsDbHelper {

    final static Logger LOG = LogManager.getLogger(TransactionsDbHelper.class);

    public static boolean getTransactionStatistics(BaseBean requestBean) {
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
        Connection cnn = ConnectionUtil.getConnection();
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
                List<BaseBean> transactions = new ArrayList<>();
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
                requestBean.setString("transaction_response", JsonUtil.convertBaseBeanListToJsonString(transactions));

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
        return success;
    }

    private static boolean fetchTotalCount(String countQuery, BaseBean requestBean) {

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
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

            ConnectionUtil.closeConnection(cnn);

        }
        return success;
    }

    private static void addParametersTo(BaseBean requestBean, PreparedStatement ps, int kk) throws SQLException {
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

    private static void fetchUpInflowData(ResultSet rs, List<BaseBean> transactions) throws SQLException {
        BaseBean inflowTransactions = new BaseBean();
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


    private static void fetchInflowData(ResultSet rs, List<BaseBean> transactions) throws SQLException {
        BaseBean inflowTransactions = new BaseBean();
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

    private static void fetchUPSLOutflowData(ResultSet rs, List<BaseBean> transactions) throws SQLException {
        BaseBean outflowTransactions = new BaseBean();
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

    private static void fetchNIPOutflowTransactions(ResultSet rs, List<BaseBean> transactions) throws SQLException {
        BaseBean outflowTransactions = new BaseBean();

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
}
