package tv.phantombot.songlist;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class SonglistWebSocketClient extends WebSocketClient {

    public static final String DATABASE_TABLE_NAME = "spreadsheetsSonglist";

    private GoogleSheetsHelper googleSheetsHelper;
    private String spreadsheetId;

    public SonglistWebSocketClient(URI serverURI, DataStore dataStore) throws GeneralSecurityException, IOException {
        super(serverURI);
        this.spreadsheetId = this.loadSpreadsheetId(dataStore);
        this.googleSheetsHelper = new GoogleSheetsHelper(this.spreadsheetId);
        com.gmt2001.Console.out.println("spreadsheetId: " + this.spreadsheetId);
        com.gmt2001.Console.out.println("Access the songlist at: " + this.getSpreadsheetUri());
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        com.gmt2001.Console.out.println("Songlist socket connected!");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        com.gmt2001.Console.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        // com.gmt2001.Console.out.println("String: " + message);

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

    private void handleMessage(String input) throws GeneralSecurityException, IOException, JSONException {
        JSONObject obj = new JSONObject(input);

        if (obj.has("songlist")) {
            JSONArray array = obj.getJSONArray("songlist");
            
            // Clear the spreadsheet
            this.googleSheetsHelper.clearRange("Sheet1!A3:E1000");

            // Format header row
            Request textFormatRequest = new Request().setRepeatCell(new RepeatCellRequest()
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                    .setHorizontalAlignment("CENTER")
                    .setTextFormat(new TextFormat()
                        .setBold(true)
                    )
                ))
                .setRange(new GridRange()
                    .setSheetId(0)
                    .setStartRowIndex(2).setEndRowIndex(3)
                    .setStartColumnIndex(0).setEndColumnIndex(5)
                )
                .setFields("userEnteredFormat.horizontalAlignment, userEnteredFormat.textFormat.bold")
            );
            Request freezeRowRequest = GoogleSheetsHelper.createFreezeRowRequest(1);

            // Submit all requests
            this.googleSheetsHelper.batchUpdate(Arrays.asList(new Request[] {
                textFormatRequest,
                freezeRowRequest,
            }));

            // Set the header row
            ValueRange headerValueRange = new ValueRange();
            List<List<Object>> headerValues = new ArrayList<List<Object>>();
            headerValueRange.setRange("Sheet1!A1:E1");
            ArrayList<Object> headerRow = new ArrayList<Object>();
            headerRow.addAll(Arrays.asList("Queue #", "Song Title", "Duration", "Requester", "Youtube ID"));
            headerValues.add(headerRow);
            headerValueRange.setValues(headerValues);
            this.googleSheetsHelper.writeRange(headerValueRange);

            // Only write values if there are values to write
            if (array.length() > 0) {
                // Construct value range to write to sheet
                ValueRange valueRange = new ValueRange();
                valueRange.setRange("Sheet1!A3:E" + array.length() + 3);

                List<List<Object>> values = new ArrayList<List<Object>>();
                for (int i = 0; i < array.length(); i++) {
                    ArrayList<Object> row = new ArrayList<Object>();
                    JSONObject rowObj = array.getJSONObject(i);
                    row.add((i + 1) + "");
                    row.add("=HYPERLINK(\"https://www.youtube.com/watch?v=" + rowObj.getString("song") + "\", \"" + rowObj.getString("title") + "\")");
                    row.add(rowObj.getString("duration"));
                    row.add(rowObj.getString("requester"));
                    row.add(rowObj.getString("song"));

                    values.add(row);
                }

                valueRange.setValues(values);

                this.googleSheetsHelper.writeRange(valueRange);
            }
            
        } else if (obj.has("command")) {
            JSONObject command = obj.getJSONObject("command");
            if (command.has("play")) { // Update current song
                ValueRange valueRange = new ValueRange();
                valueRange.setRange("Sheet1!A2:E2");
                List<List<Object>> values = new ArrayList<List<Object>>();
                ArrayList<Object> row = new ArrayList<Object>();
                row.add("Playing");
                row.add("=HYPERLINK(\"https://www.youtube.com/watch?v=" + command.getString("play") + "\", \"" + command.getString("title") + "\")");
                row.add(command.getString("duration"));
                row.add(command.getString("requester"));
                row.add(command.getString("play"));
                values.add(row);
                valueRange.setValues(values);
                this.googleSheetsHelper.writeRange(valueRange);
            }
        }
    }

    private String loadSpreadsheetId(DataStore dataStore) throws GeneralSecurityException, IOException {
        // Check if a sheet id already exists
        if (dataStore.HasKey(SonglistWebSocketClient.DATABASE_TABLE_NAME, "", "spreadsheetId")) {
            return dataStore.GetString(SonglistWebSocketClient.DATABASE_TABLE_NAME, "", "spreadsheetId");
        } else {
            // Create a new sheet
            String spreadsheetId = GoogleSheetsHelper.createSheet();
            dataStore.SetString(SonglistWebSocketClient.DATABASE_TABLE_NAME, "", "spreadsheetId", spreadsheetId);
            return spreadsheetId;
        }
    }

    public String getSpreadsheetUri() {
        return "https://docs.google.com/spreadsheets/d/" + this.spreadsheetId;
    }

}
