package tv.phantombot.aliaslist;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gmt2001.datastore.DataStore;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import tv.phantombot.googledocs.GoogleSheetsHelper;

public class AliaslistWebSocketClient extends WebSocketClient {

    public static final String DATABASE_TABLE_NAME = "spreadsheetsAliaslist";

    private GoogleSheetsHelper googleSheetsHelper;
    private String spreadsheetId;
    private DataStore dataStore;

    public AliaslistWebSocketClient(URI serverURI, DataStore dataStore) throws GeneralSecurityException, IOException {
        super(serverURI);
        this.spreadsheetId = this.loadSpreadsheetId(dataStore);
        this.dataStore = dataStore;
        this.googleSheetsHelper = new GoogleSheetsHelper(this.spreadsheetId);
        com.gmt2001.Console.out.println("serverURI: " + serverURI.toString());
        com.gmt2001.Console.out.println("spreadsheetId: " + this.spreadsheetId);
        com.gmt2001.Console.out.println("Access the aliaslist at: " + this.getSpreadsheetUri());
        this.updateSpreadsheet();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        com.gmt2001.Console.out.println("Aliaslist socket connected!");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        com.gmt2001.Console.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        if (message.equalsIgnoreCase("PING")) {
            this.send("PONG");
            return;
        }

        com.gmt2001.Console.out.println("String: " + message);

        try {
            handleMessage(message);
        } catch (Exception e) {
            com.gmt2001.Console.out.println("Error parsing string as JSON: " + e);
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement stackElement : stackTrace) {
                com.gmt2001.Console.out.println(stackElement.toString());
            }
        }

    }

    @Override
    public void onMessage(ByteBuffer byteBuffer) {
        // This is never used because the server only sends strings
        // com.gmt2001.Console.out.println("ByteBuffer: " + byteBuffer.toString());
    }

    @Override
    public void onError(Exception e) {
        com.gmt2001.Console.out.println("Error: " + e);
    }

    private void handleMessage(String input) throws GeneralSecurityException, IOException {
        JSONObject obj = new JSONObject(input);

        if (obj.has("aliascom") || obj.has("delalias")) {
            this.updateSpreadsheet();
        }
    }

    private void updateSpreadsheet() throws GeneralSecurityException, IOException {
        // Clear the spreadsheet
        this.googleSheetsHelper.clearRange("Sheet1!A2:B1000");

        // Format header row
        Request textFormatRequest = new Request().setRepeatCell(new RepeatCellRequest()
                .setCell(new CellData().setUserEnteredFormat(new CellFormat().setHorizontalAlignment("CENTER")
                        .setTextFormat(new TextFormat().setBold(true))))
                .setRange(new GridRange().setSheetId(0).setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(0)
                        .setEndColumnIndex(2))
                .setFields("userEnteredFormat.horizontalAlignment, userEnteredFormat.textFormat.bold"));
        Request freezeRowRequest = GoogleSheetsHelper.createFreezeRowRequest(1);

        // Submit all requests
        this.googleSheetsHelper.batchUpdate(Arrays.asList(new Request[] { textFormatRequest, freezeRowRequest }));

        // Set the header row
        ValueRange headerValueRange = new ValueRange();
        List<List<Object>> headerValues = new ArrayList<List<Object>>();
        headerValueRange.setRange("Sheet1!A1:B1");
        ArrayList<Object> headerRow = new ArrayList<Object>();
        headerRow.addAll(Arrays.asList("Alias", "Command"));
        headerValues.add(headerRow);
        headerValueRange.setValues(headerValues);
        this.googleSheetsHelper.writeRange(headerValueRange);

        // Write latest alias data
        String[] dbKeys = this.dataStore.GetKeyList("aliases", "");
        // Only write values if there are values to write
        if (dbKeys.length > 0) {
            // Construct value range to write to sheet
            ValueRange valueRange = new ValueRange();
            valueRange.setRange("Sheet1!A2:B" + dbKeys.length + 2);

            // Construct data
            List<List<Object>> values = new ArrayList<List<Object>>();
            for (int i = 0; i < dbKeys.length; i++) {
                ArrayList<Object> row = new ArrayList<Object>();
                String dbKey = dbKeys[i];
                String dbValue = this.dataStore.GetString("aliases", "", dbKey);
                
                row.add(dbKey);
                row.add(dbValue);

                values.add(row);
            }

            valueRange.setValues(values);

            this.googleSheetsHelper.writeRange(valueRange);
        }
    }

    private String loadSpreadsheetId(DataStore dataStore) throws GeneralSecurityException, IOException {
        // Check if a sheet id already exists
        if (dataStore.HasKey(AliaslistWebSocketClient.DATABASE_TABLE_NAME, "", "spreadsheetId")) {
            return dataStore.GetString(AliaslistWebSocketClient.DATABASE_TABLE_NAME, "", "spreadsheetId");
        } else {
            // Create a new sheet
            String spreadsheetId = GoogleSheetsHelper.createSheet("Aliaslist");
            dataStore.SetString(AliaslistWebSocketClient.DATABASE_TABLE_NAME, "", "spreadsheetId", spreadsheetId);
            return spreadsheetId;
        }
    }

    public String getSpreadsheetUri() {
        return "https://docs.google.com/spreadsheets/d/" + this.spreadsheetId;
    }

}
