package messaging.fileUtils.constants;

public class AppConstants {

    public static class DbTables {
        public static final String TABLE_SPACE = "esbuser";
        public static final String TOKEN = TABLE_SPACE.concat(".token");
        public static final String PERMISSION_TABLE = TABLE_SPACE.concat(".bck_permissions");
        public static final String USERS_TABLE = TABLE_SPACE.concat(".bck_users");
        public static final String ROLE_PERMISSION = TABLE_SPACE.concat(".bck_role_permission");
        public static final String PERMISSION_RESOURCE = TABLE_SPACE.concat(".switch_permission_resources");
        public static final String SWITCH_RESOURCE = TABLE_SPACE.concat(".switch_resources");
        public static final String ROLES = TABLE_SPACE.concat(".bck_roles");
        public static final String SUBSCRIPTIONS = TABLE_SPACE.concat(".bck_subscriptions");

        public static final String PERF_STAT_TABLE = TABLE_SPACE.concat(".perf_stat");
        public static final String SWITCH_list_TABLE = TABLE_SPACE.concat(".switch_list");
        public static final String UNAPPROVED_SWITCH_LIST = TABLE_SPACE.concat(".switch_list_mc");

        public static final String APPROVED_ENDPOINTS = TABLE_SPACE.concat(".endpoint");
        public static final String UNAPPROVED_ENDPOINT = TABLE_SPACE.concat(".endpoint_list_mc");
        public static final String MESSAGE_TABLE = TABLE_SPACE.concat(".BCK_MESSAGES");

        public static final String UP_INFLOW_TRANSACTION = TABLE_SPACE.concat(".ibt_in_flw ");
        public static final String UP_OUTFLOW_TRANSACTION = TABLE_SPACE.concat(".ibt_out_flw");
        public static final String NIP_OUTFLOW_TRANSACTION = TABLE_SPACE.concat(".nip_out_flw_2_v3");
        public static final String NIP_INFLOW_TRANSACTION = TABLE_SPACE.concat(".nip_in_flw_v2");

        public static final String SERVICE_ID_TABLE = TABLE_SPACE.concat(".nip_svc_usr ");
        public static final String NXT_ACTION_TABLE = TABLE_SPACE.concat(".nxt_action ");

        public static final String FILE_REQUEST_TABLE = TABLE_SPACE.concat(".file_upload_mc");
        public static final String FEE_CONFIG_MC = TABLE_SPACE.concat(".FEES_CONFIG_MC");
        public static final String FEE_CONFIG = TABLE_SPACE.concat(".FEES_CONFIG");
        public static final String CONTRA_ACCOUNT_TABLE = TABLE_SPACE.concat(".CONTRA_ACC_CONFIG");
        public static final String CONTRA_ACCOUNT_MC_TABLE = TABLE_SPACE.concat(".CONTRA_ACC_CONFIG_MC");
        public static final String AIRTIME_TABLE = TABLE_SPACE.concat(".CHL_AIRTIMETOPUP_3");
        public static final String POSTING_RETRIAL = TABLE_SPACE.concat(".POSTING_RETRIAL");

        public static final String INST_ROUTE_TABLE = TABLE_SPACE.concat(".IBT_FSP_FINST_MAP");
        public static final String INST_ROUTE_TABLE_MC = TABLE_SPACE.concat(".IBT_FSP_FINST_MAP_MC");
        public static final String RESPONSE_CODE = TABLE_SPACE.concat(".NIP_OUTFLW2_SERV_CTRL_V3");
        public static final String RESPONSE_CODE_MC = TABLE_SPACE.concat(".NEXT_ACTION_MC");
        public static final String INST_LIST_TABLE = TABLE_SPACE.concat(".IBT_FINST_LIST");
        public static final String INST_LIST_TABLE_MC = TABLE_SPACE.concat(".IBT_FINST_LIST_MC");
        public static final String SERVICEID_TABLE = TABLE_SPACE.concat(".service_id");
        public static final String CHANNEL_TABLE = TABLE_SPACE.concat(".channel_apps");
        public static final String VA_SERVICE_PROVIDERS_TABLE = TABLE_SPACE.concat(".va_service_providers");
        public static final String UNAPPROVED_SERVICE_PROVIDERS = TABLE_SPACE.concat(".va_service_providers_mc");
        public static final String VIRTUAL_ACCOUNT_CONFIG = TABLE_SPACE.concat(".virtual_account_config");
        public static final String VIRTUAL_ACCOUNT_CONFIG_MC = TABLE_SPACE.concat(".virtual_account_config_mc");
        public static final String DEL_ARCHIVE = TABLE_SPACE.concat(".SWITCH_DELETION_ARCHIVE");
        public static final String SWITCH_OUTFLOW_REQUEST = TABLE_SPACE.concat(".SWITCH_REQUESTS");
        public static final String SWITCH_ALG_MC = TABLE_SPACE.concat(".SWITCH_ALG_MC");
        public static final String SWITCH_ALG = TABLE_SPACE.concat(".SWITCH_ALGORITHM");


    }

    public static class ServiceActions {
        public static final String UPDATE_SERVICE = "SERVICE:UPDATE";
        public static final String CREATE_SERVICE = "SERVICE:CREATE";
        public static final String APPROVE_SERVICE = "SERVICE:APPROVE";
        public static final String DELETE_SERVICE = "SERVICE:DELETE";

        public static final String APPROVED = "Y";
        public static final String CANCELED = "N";

    }

    public static class ApprovalType {
        public static final String APPROVED = "Y";
        public static final String CANCELLED = "N";
    }

    public static class ApprovalStatus {
        public static final String APPROVED = "approved";
        public static final String CANCELLED = "cancelled";
        public static final String UNAPPROVED = "unapproved";
    }

    public static class AppActions {
        public static final String CREATE = "create";
        public static final String UPDATE = "update";
        public static final String DEACTIVATE = "deactivate";
        public static final String ACTIVATE = "activate";
    }
    public static class Constants {
        public static final String APP_CODE = "SWITCH";
        public static final String LOCAL = "infometics";
        public static final String UBA = "uba";
    }
}
