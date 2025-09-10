package messaging.fileUtils.service.servlet;

import dao.AuditLogDAO;
import dao.CustomDAO;
import dao.SwitchRegistryDAO;
import messaging.ValidateResponse;
import services.servlets.BaseServlet;
import util.BaseBean;
import util.JsonUtil;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static constants.RegAppConstants.ITS_SWITCH_REGISTRY;
import static constants.RegAppConstants.TB_SWITCH_REGISTRY;
import static util.JwtUtil.getUsername;

public class SwitchRegistryServlet extends BaseServlet {

    private final SwitchRegistryDAO switchRegistryDAO;
    private final AuditLogDAO auditLogDAO;
    private final CustomDAO customDAO;


    public SwitchRegistryServlet() {
        this.switchRegistryDAO = new SwitchRegistryDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.customDAO = new CustomDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(APPLICATION_JSON);
        resp.setCharacterEncoding(UTF_8);

        String id = req.getParameter("id");
        String uri = req.getRequestURI();

        try {
            if (id != null) {
                handleRequestWithId(id, uri, resp);
            } else {
                handleRequestWithoutId(uri, resp);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        if (uri.equals("/switch/switchregistry/create")) {
            handleCreateRequest(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URI");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        String uri = req.getRequestURI();

        if (id != null) {
            try {
                if (uri.startsWith("/switch/switchregistry/update")) {
                    updateApiReg(req, resp, id);
                } else if (uri.startsWith("/switch/switchregistry/approval")) {
                    handleApproval(req, resp, id);
                } else if (uri.startsWith("/switch/switchregistry/enable_disable")) {
                    handleEnableDisable(req, resp, id);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URI");
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing ID");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }

    // ************************* POST ******************************************
    private void handleCreateRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = getBody(req);
        JsonObject jsonObject = JsonUtil.toJsonObject(body);
        BaseBean bean = createBaseBean(jsonObject);

        try {
            if (ValidateResponse.isValidResponse(jsonObject, bean)) {
                processValidRequest(req, resp, body, bean);
            } else {
                sendErrorResponse(resp, "Bad request", HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    private BaseBean createBaseBean(JsonObject jsonObject) {
        BaseBean bean = new BaseBean();
        bean.put("switch_code", JsonUtil.getJsonObjValue(jsonObject, "switch_code"));
        bean.put("switch_name", JsonUtil.getJsonObjValue(jsonObject, "switch_name"));
        return bean;
    }

    private void processValidRequest(HttpServletRequest req, HttpServletResponse resp, String body, BaseBean bean) throws SQLException, IOException {
        bean.put("created_by", getUsername(req));
        bean.put("approval", "pending");
        bean.put("disabled", "true");

        boolean status = switchRegistryDAO.insertSwitchRegistry(bean);

        if (!status) {
            sendErrorResponse(resp, "Unable to add request", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            logAudit(body, bean);
            sendSuccessResponse(resp, "CREATED", HttpServletResponse.SC_CREATED);
        }
    }

    private void logAudit(String body, BaseBean bean) throws SQLException {
        BaseBean auditBean = new BaseBean();
        auditBean.put("table_name", TB_SWITCH_REGISTRY);
        auditBean.put("operation", "create");
        auditBean.put("operated_by", bean.get("created_by"));
        auditBean.put("old_value", "");
        auditBean.put("new_value", body);

        auditLogDAO.insertIntoAuditLog(auditBean);
    }

    private void sendErrorResponse(HttpServletResponse resp, String message, int statusCode) throws IOException {
        resp.getWriter().write("{\"message\": \"" + message + "\"}");
        resp.setStatus(statusCode);
    }

    private void sendSuccessResponse(HttpServletResponse resp, String message, int statusCode) throws IOException {
        resp.getWriter().write("{\"message\": \"" + message + "\"}");
        resp.setStatus(statusCode);
    }

    // ************************  GET ***********************
    private String getById(List<BaseBean> beans, String id, String status) {
        for (BaseBean bean : beans) {

            if (bean.get("id").equals(id) && bean.get("approval").equalsIgnoreCase(status)) {
                return JsonUtil.convertBaseBeanToStr(bean);
            }
        }
        return new BaseBean().put("message", "not found");
    }

    private void handleRequestWithId(String id, String uri, HttpServletResponse resp) throws IOException, SQLException {
        List<BaseBean> beans = switchRegistryDAO.getAllSwitchRegistries();

        if (uri.startsWith("/switch/switchregistry/get_pending_id")) {
            writeResponse(resp, getById(beans, id, "pending"));
        } else if (uri.startsWith("/switch/switchregistry/get_approved_by_id")) {
            writeResponse(resp, getById(beans, id, "approved"));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URI");
        }
    }

    private void handleRequestWithoutId(String uri, HttpServletResponse resp) throws IOException, SQLException {
        if (uri.equals("/switch/switchregistry/get_approved")) {
            List<BaseBean> records = switchRegistryDAO.getSwitchByApproval("approved");
            writeResponse(resp, JsonUtil.convertBaseBeanListToJsonString(records));
        } else if (uri.equals("/switch/switchregistry/get_pending")) {
            List<BaseBean> records = switchRegistryDAO.getSwitchByApproval("pending");
            writeResponse(resp, JsonUtil.convertBaseBeanListToJsonString(records));
        }
    }

    private void writeResponse(HttpServletResponse resp, String jsonResponse) throws IOException {
        resp.getWriter().write(jsonResponse);
    }

    // ***************************** PUT **********************************
    private void handleApproval(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException, SQLException {
        String requestBody = getBody(req);
        JsonObject jsonObject = JsonUtil.toJsonObject(requestBody);
        BaseBean bean = new BaseBean();
        String approval = JsonUtil.getJsonObjValue(jsonObject, "approval").toLowerCase().trim();
        bean.put("approval", approval);

        if (ValidateResponse.isValidResponse(jsonObject, bean)) {
            bean.put("approve_by", getUsername(req));
            bean.put("id", id);
            bean.put("disabled", approval.equals("approved") ? "false" : "true");

            customDAO.approveOrReject(bean, ITS_SWITCH_REGISTRY);
            sendSuccessResponse(resp, "request " + approval, HttpServletResponse.SC_OK);
        } else {
            sendErrorResponse(resp, "Bad request", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleEnableDisable(HttpServletRequest req, HttpServletResponse resp, String id) throws SQLException, IOException {
        String requestBody = getBody(req);
        JsonObject jsonObject = JsonUtil.toJsonObject(requestBody);
        String isDisabled = JsonUtil.getJsonObjValue(jsonObject, "disabled");

        BaseBean bean = new BaseBean();
        bean.put("disabled", isDisabled);

        if (ValidateResponse.isValidResponse(jsonObject, bean)) {
            bean.put("id", id);
            customDAO.disableRow(bean, ITS_SWITCH_REGISTRY);
            sendSuccessResponse(resp, "disabled: " + isDisabled, HttpServletResponse.SC_OK);
        } else {
            sendErrorResponse(resp, "Bad request", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void updateApiReg(HttpServletRequest req, HttpServletResponse resp, String id) throws SQLException, IOException {
        String body = getBody(req);
        JsonObject jsonObject = JsonUtil.toJsonObject(body);
        BaseBean bean = new BaseBean();
        bean.put("switch_code", JsonUtil.getJsonObjValue(jsonObject, "switch_code"));
        bean.put("switch_name", JsonUtil.getJsonObjValue(jsonObject, "switch_name"));


        if (ValidateResponse.isValidResponse(jsonObject, bean)) {
            bean.put("id", id);
            bean.put("last_modify_by", getUsername(req));
            bean.put("disable", "true");

            String oldValue = JsonUtil.convertBaseBeanToStr(switchRegistryDAO.getSwitchRegistryById(Integer.parseInt(id)));
            boolean success = switchRegistryDAO.updateSwitchRegistry(bean);
            String newValue = JsonUtil.convertBaseBeanToStr(switchRegistryDAO.getSwitchRegistryById(Integer.parseInt(id)));

            if (success) {
                logAudit(bean, oldValue, newValue, getUsername(req));
                sendSuccessResponse(resp, "updated", HttpServletResponse.SC_OK);
            } else {
                sendErrorResponse(resp, "Unable to update request", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            sendErrorResponse(resp, "Bad request", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void logAudit(BaseBean bean, String oldValue, String newValue, String username) throws SQLException {
        BaseBean auditBean = new BaseBean();
        auditBean.put("table_name", TB_SWITCH_REGISTRY);
        auditBean.put("operation", "update");
        auditBean.put("operated_by", username);
        auditBean.put("old_value", oldValue);
        auditBean.put("new_value", newValue);

        auditLogDAO.insertIntoAuditLog(auditBean);
    }
}
