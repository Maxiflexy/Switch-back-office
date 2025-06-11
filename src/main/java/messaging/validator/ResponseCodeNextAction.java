package messaging.validator;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import exceptions.CustomException;
import messaging.FileValidator;
import messaging.RequestValidator;
import messaging.fileUtils.RowValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import persistence.FileUploadDbHelper;
import util.BaseBean;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static messaging.RequestValidator.validateFieldNotEmpty;

public class ResponseCodeNextAction extends RowValidator implements FileValidator {

    final static Logger LOG = LogManager.getLogger(ResponseCodeNextAction.class);

    private static List<String> serviceIds;
    private static List<String> actionCodes;
    private final Map<String, Object> fileData = new HashMap<>();
    private final String[] headerColumnCount = new String[]{"resp_code", "next_actn", "serviceid"};
    private final String[] inputColumns = new String[]{"resp_code", "next_actn", "serviceid"};


    private messaging.fileUtils.Reader reader;

    public ResponseCodeNextAction() {
    }

    public ResponseCodeNextAction(messaging.fileUtils.Reader reader) {
        this.reader = reader;
        serviceIds = DBHelper.fetchServiceId(new BaseBean());
        actionCodes = DBHelper.fetchNextActionCodes(new BaseBean());
    }

    @Override
    public boolean validateFileHeader(InputStream header, BaseBean validationBean) {
        Reader reader = new InputStreamReader(header);
        boolean success = false;
        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .build()) {
            String[] nextLine = csvReader.readNext();
            for (int i = 0; i < headerColumnCount.length; i++) {
                if (!Objects.equals(nextLine[i].toLowerCase(), headerColumnCount[i].toLowerCase())) {
                    validationBean.setString("message", "invalid column arrangement:: " + headerColumnCount[i]);
                    throw new CustomException(validationBean);
                }
            }
            success = true;
        } catch (CsvValidationException | IOException e) {
            throw new RuntimeException(e);
        }
        return success;
    }

    @Override
    public boolean validateFileBody(InputStream body, BaseBean validationBean) {
        boolean success = false;
        try {
            List<BaseBean> tables = new ArrayList<>();
            Map<String, Object> details = new HashMap<>();

            BaseBean fileBean = new BaseBean();
            fileBean.setString("filename", validationBean.getString("fileName"));
            fileBean.setString("document-id", validationBean.getString("document-id"));

            extractFileDetails(fileBean, tables, body);
            details.put("file", fileBean);
            details.put("table", tables);

            fileData.put(validationBean.getString("fileName"), details);
            success = true;

        } catch (CustomException ex) {
            LOG.error("Error occurred validating file body {}", ex.getMessage());
            ex.printStackTrace();
            throw new CustomException(ex.getBaseBean());
        } catch (CsvValidationException | IOException e) {
            LOG.error("Error occurred validating file body {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return success;
    }

    @Override
    public boolean validateFileBodyWithReader(InputStream inputStream, BaseBean validationBean) {
        List<BaseBean> tables = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        BaseBean fileBean = new BaseBean();
        fileBean.setString("document-id", validationBean.getString("document-id"));
        details.put("file", fileBean);
        details.put("table", tables);
        fileData.put(validationBean.getString("fileName"), details);
        reader.setRowValidator(this);
        return reader.readFile(inputStream, inputColumns, headerColumnCount, fileBean, tables);

    }

    private static void extractFileDetails(BaseBean requestBean, List<BaseBean> tables, InputStream inputStream) throws IOException, CsvValidationException {

        String id = requestBean.getString("document-id");
        String fileName = requestBean.getString("fileName");
        Reader reader = new InputStreamReader(inputStream);
        serviceIds = DBHelper.fetchServiceId(requestBean);
        actionCodes = DBHelper.fetchNextActionCodes(requestBean);
        ResponseCodeNextAction validator = new ResponseCodeNextAction();
        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .build()) {
            String[] nextLine = csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {
                BaseBean row = new BaseBean();
                row.setString("resp_code", nextLine[0]);
                row.setString("next_actn", nextLine[1]);
                row.setString("serviceid", nextLine[2]);
                row.setString("document-id", id);
                if (validator.validateRow(row, requestBean)) {
                    tables.add(row);
                }
            }
            File file = new File(fileName + "-" + id);
            Files.copy(inputStream, file.toPath());
        }

    }


    @Override
    public Map<String, Object> fetchData() {
        return this.fileData;
    }

    @Override
    public boolean persistData(BaseBean requestBean) {
        return FileUploadDbHelper.saveNewRecord(fetchData(), requestBean);
    }

    @Override
    protected boolean validateRow(BaseBean row, BaseBean requestBean) {
        requestBean.setString("statusCode", "400");

        validateFieldNotEmpty("resp_code", row, requestBean);
        validateFieldNotEmpty("next_actn", row, requestBean);
        validateFieldNotEmpty("serviceid", row, requestBean);
        if (!serviceIds.contains(row.getString("serviceid"))) {
            requestBean.setString("message", "Invalid service ID");
            throw new CustomException(requestBean);
        }
        if (!actionCodes.contains(row.getString("next_actn"))) {
            requestBean.setString("message", "Invalid Action Code");
            throw new CustomException(requestBean);
        }
        requestBean.remove("statusCode");
        return true;    }
}
