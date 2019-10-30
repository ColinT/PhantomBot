package tv.phantombot.songlist;

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
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleSheetsHelper {

    private static final String APPLICATION_NAME = "PhantomBot Songlist";
    private static final String CREDENTIALS_FILE_PATH = "config/spreadsheet-credentials.json";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /** The Google spreadsheet id to write to */
    private String sheetId;

    public GoogleSheetsHelper(String sheetId) {
        this.sheetId = sheetId;
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

        // Check if file exists
        File file = new File(GoogleSheetsHelper.CREDENTIALS_FILE_PATH);
        if (!file.exists()) {
            throw new FileNotFoundException("Resource not found: " + GoogleSheetsHelper.CREDENTIALS_FILE_PATH);
        }

        FileInputStream in = new FileInputStream(GoogleSheetsHelper.CREDENTIALS_FILE_PATH);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GoogleSheetsHelper.JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JacksonFactory.getDefaultInstance(),
                clientSecrets,
                GoogleSheetsHelper.SCOPES)
            .setDataStoreFactory(new CustomFileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential auth = new AuthorizationCodeInstalledApp(flow, receiver, new CustomBrowser()).authorize("user");
        return auth;
    }

    public Sheets getSheetService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, GoogleSheetsHelper.JSON_FACTORY, this.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(GoogleSheetsHelper.APPLICATION_NAME)
            .build();

        return service;
    }

    public ValueRange readRange(String range) throws GeneralSecurityException, IOException {
        Sheets service = this.getSheetService();
        ValueRange response = service.spreadsheets().values()
            .get(this.sheetId, range)
            .execute();

        return response;
    }

    public UpdateValuesResponse writeRange(ValueRange valueRange) throws GeneralSecurityException, IOException {
        Sheets service = this.getSheetService();
        UpdateValuesResponse response = service.spreadsheets().values()
            .update(this.sheetId, valueRange.getRange(), valueRange)
            .setValueInputOption("USER_ENTERED")
            .execute();

        return response;
    }

    public void clearRange(String range) throws GeneralSecurityException, IOException {
        Sheets service = this.getSheetService();
        service.spreadsheets().values().clear(this.sheetId, range, new ClearValuesRequest()).execute();
    }

}
