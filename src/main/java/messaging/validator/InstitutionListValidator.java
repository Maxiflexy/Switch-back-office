package messaging.validator;

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

public class InstitutionListValidator extends RowValidator implements FileValidator {

    final static Logger LOG = LogManager.getLogger(InstitutionListValidator.class);
    private static List<String> switchCodes;
    private final Map<String, Object> fileData = new HashMap<>();
    private final String[] headerColumnCount = new String[]{"service_provider_id", "institution_id", "institution_name", "its_institution_id"};
    private final String[] inputColumns = new String[]{"service_provider_id", "institution_id", "institution_name", "its_institution_id"};
    private messaging.fileUtils.Reader reader;

    public InstitutionListValidator() {
    }

    public InstitutionListValidator(messaging.fileUtils.Reader reader) {
        this.reader = reader;
    }

    @Override
    public boolean validateFileHeader(InputStream header, BaseBean validationBean) {
        Reader reader = new InputStreamReader(header);
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
        switchCodes = DBHelper.fetchSwitchCodes(new BaseBean());
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
        InstitutionListValidator validator = new InstitutionListValidator();
        switchCodes = DBHelper.fetchSwitchCodes(new BaseBean());
        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .build()) {
            String[] nextLine = csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {
                BaseBean row = new BaseBean();
                row.setString("service_provider_id", nextLine[0]);
                row.setString("institution_id", nextLine[1]);
                row.setString("institution_name", nextLine[2]);
                row.setString("its_institution_id", nextLine[3]);
                row.setString("document-id", id);
                if (validator.validateRow(row, requestBean)) {
                    tables.add(row);
                }
            }
            File file = new File(fileName + "-" + id);
            Files.copy(inputStream, file.toPath());
        }

    }

    protected boolean validateRow(BaseBean row, BaseBean requestBean) {

        requestBean.setString("statusCode", "400");
        validateFieldNotEmpty("service_provider_id", row, requestBean);
        validateFieldNotEmpty("institution_id", row, requestBean);
        validateFieldNotEmpty("institution_name", row, requestBean);
        validateFieldNotEmpty("its_institution_id", row, requestBean);

        if (!switchCodes.contains(row.getString("service_provider_id"))) {
            requestBean.setString("message", "Invalid service provider/switch ID");
            throw new CustomException(requestBean);
        }
        requestBean.remove("statusCode");
        return true;
    }

    @Override
    public Map<String, Object> fetchData() {
        return this.fileData;
    }

    @Override
    public boolean persistData(BaseBean requestBean) {
        LOG.info("Validator persist data: Document ID: {}", requestBean.getString("document-id") );
        return FileUploadDbHelper.saveNewRecord(fetchData(), requestBean);
    }
}
