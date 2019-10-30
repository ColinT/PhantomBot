package tv.phantombot.songlist;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.sheets.v4.model.ValueRange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class SonglistWebSocketClient extends WebSocketClient {

    private GoogleSheetsHelper googleSheetsHelper;

    public SonglistWebSocketClient(URI serverURI, String sheetId) {
        super(serverURI);
        com.gmt2001.Console.out.println("sheetId: " + sheetId);
        this.googleSheetsHelper = new GoogleSheetsHelper(sheetId);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        com.gmt2001.Console.out.println("Connection established!");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        com.gmt2001.Console.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        // com.gmt2001.Console.out.println("String: " + message);

        try {
            parseJSONObject(message);
        } catch (Exception e) {
            com.gmt2001.Console.out.println("Error parsing string as JSON: " + e);
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

    private void parseJSONObject(String input) throws GeneralSecurityException, IOException, JSONException {
        JSONObject obj = new JSONObject(input);

        if (obj.has("songlist")) {
            JSONArray array = obj.getJSONArray("songlist");
            com.gmt2001.Console.out.println("songlist received: length " + array.length());
            for (int i = 0; i < array.length(); i++) {
                array.getJSONObject(i); // Force blocking operation
                com.gmt2001.Console.out.println(array.getJSONObject(i));
            }
            
            // Clear the spreadsheet
            this.googleSheetsHelper.clearRange("Sheet1!A2:D1000");

            // Only write values if there are values to write
            if (array.length() > 0) {
                // Construct value range to write to sheet
                ValueRange valueRange = new ValueRange();
                valueRange.setRange("Sheet1!A2:D" + array.length() + 1);

                List<List<Object>> values = new ArrayList<List<Object>>();
                for (int i = 0; i < array.length(); i++) {
                    ArrayList<Object> row = new ArrayList<Object>();
                    JSONObject rowObj = array.getJSONObject(i);
                    row.add(rowObj.getString("title"));
                    row.add(rowObj.getString("duration"));
                    row.add(rowObj.getString("requester"));
                    row.add(rowObj.getString("song"));

                    values.add(row);
                }

                valueRange.setValues(values);

                this.googleSheetsHelper.writeRange(valueRange);
            }
            
        }
    }

}
