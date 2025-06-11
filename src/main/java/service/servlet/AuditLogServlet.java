package service.servlet;

import dao.AuditLogDAO;
import messaging.RequestValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import services.servlets.BaseServlet;
import util.BaseBean;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuditLogServlet extends BaseServlet {

    public static final String startDateP = "startDate";
    public static final String endDateP =  "endDate";
    private final AuditLogDAO auditLogDAO;

    public AuditLogServlet() {
       auditLogDAO = new AuditLogDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding(UTF_8);
        String id = request.getParameter("id");
        String startDate = request.getParameter(startDateP);
        String endDate = request.getParameter(endDateP);
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE_TIME;

        BaseBean bb = new BaseBean();
        if (startDate != null && endDate != null) {
            try {
                JsonObject jobj = Json.createObjectBuilder()
                        .add(startDateP, startDate)
                        .add(endDateP, endDate)
                        .build();
                new RequestValidator().validateDateParameter(jobj, bb, startDateP, dateFormat);
                new RequestValidator().validateDateParameter(jobj, bb, endDateP, dateFormat);
            } catch (Exception e) {
                try {
                    response.getWriter().write(getInvalidFormatResponse());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

        }

        try {
            List<BaseBean> records = new ArrayList<>();
            if (id != null) {
                BaseBean baseBean = auditLogDAO.getAuditById(Integer.parseInt(id));
                records.add(baseBean);
            } else if (startDate != null && endDate != null) {
                records = auditLogDAO.getAuditLogs(startDate, endDate);
            } else {
                 records = auditLogDAO.getAuditLogs();
            }
             String res = convertBaseBeanListToJsonString(records);
             response.getWriter().write(res);
             response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        doGet(servletRequest, servletResponse);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    public static String convertBaseBeanToStr(BaseBean baseBean) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : baseBean.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equals("old_value") || key.equals("new_value")) {
                jsonObject.put(key, new JSONObject(value));
            } else {
                jsonObject.put(key, value);
            }
        }
        return jsonObject.toString();
    }

    public static String convertBaseBeanListToJsonString(List<BaseBean> baseBeanList) {
        JSONArray jsonArray = new JSONArray();
        for (BaseBean baseBean : baseBeanList) {
            jsonArray.put(new JSONObject(convertBaseBeanToStr(baseBean)));
        }
        return jsonArray.toString();
    }
}
