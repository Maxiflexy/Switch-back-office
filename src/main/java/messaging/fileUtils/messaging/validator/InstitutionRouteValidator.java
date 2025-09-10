package messaging.fileUtils.messaging.validator;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import exceptions.CustomException;
import messaging.FileValidator;
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

public class InstitutionRouteValidator extends RowValidator implements FileValidator {

    final static Logger LOG = LogManager.getLogger(InstitutionRouteValidator.class);
    private static List<String> channelIds;
    private final Map<String, Object> fileData = new HashMap<>();
    private final String[] headerColumnCount = new String[]{"finst_id", "fsp_id", "channel_id"};
    private final String[] inputColumns = new String[]{"finst_id", "fsp_id", "channel_id"};
    private messaging.fileUtils.Reader reader;

    public InstitutionRouteValidator() {
    }

    public InstitutionRouteValidator(messaging.fileUtils.Reader reader) {
        this.reader = reader;
    }

    @Override
    public boolean validateFileHeader(InputStream request, BaseBean validationBean) {
        Reader reader = new InputStreamReader(request);
        boolean success = false;
        try (
                CSVReader csvReader = new CSVReaderBuilder(reader)
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
    public boolean validateFileBody(InputStream inputStream, BaseBean validationBean) {
        boolean success = false;
        try {
            List<BaseBean> tables = new ArrayList<>();
            Map<String, Object> details = new HashMap<>();

            BaseBean fileBean = new BaseBean();
            fileBean.setString("filename", validationBean.getString("fileName"));
            fileBean.setString("document-id", validationBean.getString("document-id"));

            extractFileDetails(fileBean, tables, inputStream);
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
        channelIds = DBHelper.fetchUserNameIds(new BaseBean());
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
        InstitutionRouteValidator validator = new InstitutionRouteValidator();
        channelIds = DBHelper.fetchUserNameIds(new BaseBean());
        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .build()) {
            String[] nextLine = csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {
                BaseBean row = new BaseBean();
                row.setString("finst_id", nextLine[0]);
                row.setString("fsp_id", nextLine[1]);
                row.setString("channel_id", nextLine[2]);
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
        validateFieldNotEmpty("channel_id", row, requestBean);
        if (!channelIds.contains(row.getString("channel_id"))) {
            requestBean.setString("message", "Invalid channel ID");
            throw new CustomException(requestBean);
        }
        validateFieldNotEmpty("finst_id", row, requestBean);
        validateFieldNotEmpty("fsp_id", row, requestBean);
        requestBean.remove("statusCode");
        return true;
    }
}
