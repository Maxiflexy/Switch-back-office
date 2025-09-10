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

public class ContraAccountValidator extends RowValidator implements FileValidator {
    public final static Logger LOG = LogManager.getLogger(ContraAccountValidator.class);
    private static List<String> channelIds;
    private static List<String> serviceIds;
    private final Map<String, Object> fileData = new HashMap<>();
    private final String[] headerColumnCount = new String[]{"SERVICE_ID", "APPLCODE", "use_msg_dr_acc_flg", "use_msg_cr_acc_flg", "dr_acc_derivatn_flg", "dr_acct_num", "dr_acc_prefix", "dr_acc_suffix", "status"};
    private final String[] inputColumns = new String[]{"SERVICE_ID", "APPLCODE", "use_msg_dr_acc_flg", "use_msg_cr_acc_flg", "dr_acc_derivatn_flg", "dr_acct_num", "dr_acc_prefix", "dr_acc_suffix", "status"};
    private messaging.fileUtils.Reader reader;

    public ContraAccountValidator() {
    }

    public ContraAccountValidator(messaging.fileUtils.Reader reader) {
        this.reader = reader;
    }

    @Override
    public boolean validateFileHeader(InputStream request, BaseBean validationBean) {
        Reader reader = new InputStreamReader(request);
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
    public boolean validateFileBody(InputStream inputStream, BaseBean validationBean) {
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

        } catch (CustomException ex) {
            LOG.error("Error occurred validating file body {}", ex.getMessage());
            ex.printStackTrace();
            throw new CustomException(ex.getBaseBean());
        } catch (CsvValidationException | IOException e) {
            LOG.error("Error occurred validating file body {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean validateFileBodyWithReader(InputStream inputStream, BaseBean validationBean) {
        List<BaseBean> tables = new ArrayList<>();
        channelIds = DBHelper.fetchUserNameIds(new BaseBean());
        serviceIds = DBHelper.fetchServiceId(new BaseBean());
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
        ContraAccountValidator validator = new ContraAccountValidator();
        channelIds = DBHelper.fetchUserNameIds(new BaseBean());
        serviceIds = DBHelper.fetchServiceId(new BaseBean());

        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .build()) {
            String[] nextLine = csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {
                BaseBean row = new BaseBean();
                row.setString("SERVICE_ID", nextLine[0]);
                row.setString("APPLCODE", nextLine[1]);
                row.setString("use_msg_dr_acc_flg", nextLine[2].toLowerCase());
                row.setString("use_msg_cr_acc_flg", nextLine[3].toLowerCase());
                row.setString("dr_acc_derivatn_flg", nextLine[4].toLowerCase());
                row.setString("dr_acct_num", nextLine[5]);
                row.setString("dr_acc_prefix", nextLine[6]);
                row.setString("dr_acc_suffix", nextLine[7]);
                row.setString("status", nextLine[8]);
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
        if (!serviceIds.contains(row.getString("service_id"))) {
            requestBean.setString("message","Invalid service ID");
            throw new CustomException(requestBean);
        }
        if (!channelIds.contains(row.getString("applcode"))) {
            requestBean.setString("message","Invalid applcode");
            throw new CustomException(requestBean);
        }

        if (row.getString("dr_acc_derivatn_flg").isEmpty() || (!row.getString("dr_acc_derivatn_flg").equals("true") && !row.getString("dr_acc_derivatn_flg").equals("false"))) {
            requestBean.setString("message", "Invalid account derivation flag");
            throw new CustomException(requestBean);
        }
        if (row.getString("dr_acc_derivatn_flg").equals("true")) {
            if (row.getString("dr_acc_prefix").isEmpty() || row.getString("dr_acc_suffix").isEmpty()) {
                requestBean.setString("message", "Invalid account derivation flag");
                throw new CustomException(requestBean);
            }
        }
        if (row.getString("dr_acc_derivatn_flg").equals("false")) {
            if (row.getString("dr_acct_num").isEmpty()) {
                requestBean.setString("message", "Invalid credit account number");
                throw new CustomException(requestBean);
            }
        }
        if (!row.getString("status").equals("inactive") && !row.getString("status").equals("active")) {
            requestBean.setString("message", "Invalid status configuration");
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
        return FileUploadDbHelper.saveNewRecord(fetchData(), requestBean);
    }
}