package tv.phantombot.googledocs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleSheetsHelper {

    private static final String APPLICATION_NAME = "PhantomBot Songlist";
    private static final String CREDENTIALS_FILE_PATH = "config\\spreadsheet-credentials.json";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.DRIVE_FILE);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /** The Google spreadsheet id to write to */
    private String spreadsheetId;

    /**
     * @param spreadsheetId The id of the spreadsheet that this helper will perform operations on.
     */
    public GoogleSheetsHelper(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    /**
     * Creates a Google sheet with the title "Songlist".
     * @return The id of the created sheet.
     */
    public static String createSheet() throws GeneralSecurityException, IOException {
        return GoogleSheetsHelper.createSheet("Songlist");
    }

    /**
     * Creates a Google sheet with the specified title.
     * @param title The title to name the created sheet.
     * @return The id of the created sheet.
     */
    public static String createSheet(String title) throws GeneralSecurityException, IOException {
        Sheets sheetService = GoogleSheetsHelper.getSheetService();
        Spreadsheet spreadsheet = sheetService.spreadsheets().create(
            new Spreadsheet().setProperties(
                new SpreadsheetProperties().setTitle(title)
            )
        ).setFields("spreadsheetId").execute();
        String spreadsheetId = spreadsheet.getSpreadsheetId();

        // Set permissions; anyone can read
        Drive driveService = GoogleSheetsHelper.getDriveService();
        driveService.permissions().create(
            spreadsheetId,
            new Permission()
                .setType("anyone")
                .setRole("reader")
        ).execute();

        return spreadsheetId;
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Check if file exists
        String filePath = new File("").getAbsolutePath() + "\\" + GoogleSheetsHelper.CREDENTIALS_FILE_PATH;

        FileInputStream in;
        try {
            in = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            com.gmt2001.Console.out.println("Resource not found: " + filePath);
            throw e;
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GoogleSheetsHelper.JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JacksonFactory.getDefaultInstance(),
                clientSecrets,
                GoogleSheetsHelper.SCOPES)
            .setDataStoreFactory(new CustomFileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();

        CustomLocalServerReceiver receiver = new CustomLocalServerReceiver.Builder().setPort(8888).build();
        Credential auth = new AuthorizationCodeInstalledApp(flow, receiver, new CustomBrowser()).authorize("user");
        return auth;
    }

    /**
     * Returns whether or not this client is authenticated properly.
     * @return Whether or not this client is authenticated properly.
     */
    public static boolean isAuthenticated() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleSheetsHelper.getCredentials(HTTP_TRANSPORT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the Google drive service for this client.
     * @return The Google drive service for this client.
     */
    public static Drive getDriveService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, GoogleSheetsHelper.JSON_FACTORY, GoogleSheetsHelper.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(GoogleSheetsHelper.APPLICATION_NAME)
            .build();

        return service;
    }

    /**
     * Returns the Google sheet service for this client.
     * @return The Google sheet service for this client.
     */
    public static Sheets getSheetService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, GoogleSheetsHelper.JSON_FACTORY, GoogleSheetsHelper.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(GoogleSheetsHelper.APPLICATION_NAME)
            .build();

        return service;
    }

    /**
     * Executes a read range operation on the specified A1 format range.
     * @param range A1 formatted range to read values from.
     * @return The response of the read range operation.
     */
    public ValueRange readRange(String range) throws GeneralSecurityException, IOException {
        Sheets service = GoogleSheetsHelper.getSheetService();
        ValueRange response = service.spreadsheets().values()
            .get(this.spreadsheetId, range)
            .execute();

        return response;
    }

    /**
     * Executes a write range operation on the specified value range.
     * @param valueRange The value range of values to write.
     * @return The response of the write range operation.
     */
    public UpdateValuesResponse writeRange(ValueRange valueRange) throws GeneralSecurityException, IOException {
        Sheets service = GoogleSheetsHelper.getSheetService();
        UpdateValuesResponse response = service.spreadsheets().values()
            .update(this.spreadsheetId, valueRange.getRange(), valueRange)
            .setValueInputOption("USER_ENTERED")
            .execute();

        return response;
    }

    /**
     * Executes a clear range operation on the apecified A1 format range.
     * @param range A1 formatted range to delete values from.
     * @return The response of the clear range operation.
     */
    public void clearRange(String range) throws GeneralSecurityException, IOException {
        Sheets service = GoogleSheetsHelper.getSheetService();
        service.spreadsheets().values().clear(this.spreadsheetId, range, new ClearValuesRequest()).execute();
    }

    /**
     * Creates a freeze row request with the specified number of rows to freeze.
     * @param frozenRowCount The number of rows to freeze
     * @return A request to freeze the specified number of rows.
     */
    public static Request createFreezeRowRequest(int frozenRowCount) {
        return new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
            .setProperties(new SheetProperties()
                .setSheetId(0)
                .setGridProperties(new GridProperties()
                    .setFrozenRowCount(frozenRowCount)
                )
            )
            .setFields("gridProperties.frozenRowCount")
        );
    }

    /**
     * Executes a batch update request.
     * @param requests The requests to execute.
     * @return The response of the request.
     */
    public BatchUpdateSpreadsheetResponse batchUpdate(List<Request> requests) throws GeneralSecurityException, IOException {
        Sheets service = GoogleSheetsHelper.getSheetService();
        BatchUpdateSpreadsheetResponse response = service.spreadsheets()
            .batchUpdate(this.spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
            .execute();

        return response;
    }

}
