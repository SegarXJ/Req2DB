package extension;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.message.HttpRequestResponse;

import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReqLogger implements ExtensionUnloadingHandler {


    /**
     * SQL instructions.
     */
    private static final String SQL_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS ACTIVITY (LOCAL_SOURCE_IP TEXT, HTTP_METHOD TEXT,TARGET_URL TEXT,  SUB_URL TEXT UNIQUE , REQUEST_RAW TEXT, SEND_DATETIME TEXT, HTTP_STATUS_CODE TEXT, RESPONSE_RAW TEXT)";
    //    private static final String SQL_TABLE_INSERT = "INSERT INTO ACTIVITY (LOCAL_SOURCE_IP,HTTP_METHOD,TARGET_URL,SUB_URL,REQUEST_RAW,SEND_DATETIME,HTTP_STATUS_CODE,RESPONSE_RAW) VALUES(?,?,?,?,?,?,?,?)";
    private static final String SQL_TABLE_UPSERT = """
                INSERT INTO ACTIVITY (LOCAL_SOURCE_IP, HTTP_METHOD, TARGET_URL, SUB_URL, REQUEST_RAW, SEND_DATETIME, HTTP_STATUS_CODE, RESPONSE_RAW)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(SUB_URL) DO UPDATE SET
                    LOCAL_SOURCE_IP = excluded.LOCAL_SOURCE_IP,
                    HTTP_METHOD = excluded.HTTP_METHOD,
                    TARGET_URL = excluded.TARGET_URL,
                    REQUEST_RAW = excluded.REQUEST_RAW,
                    SEND_DATETIME = excluded.SEND_DATETIME,
                    HTTP_STATUS_CODE = excluded.HTTP_STATUS_CODE,
                    RESPONSE_RAW = excluded.RESPONSE_RAW
            """;

    private static final String SQL_TABLE_SELECT = "SELECT * FROM ACTIVITY";


    private Connection storageConnection;
    public static MontoyaApi Api;

    /**
     * DB URL
     */
    private String url;

    /**
     * Formatter for date/time.
     */
    private final DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public ReqLogger(MontoyaApi api, String storeFileName) throws ClassNotFoundException, SQLException {
        //Load the SQLite driver
        Class.forName("org.sqlite.JDBC");
        this.url = "jdbc:sqlite:" + storeFileName;
        Api = api;
        Api.logging().logToOutput("REQ will be save to :'" + storeFileName + "'.");
        this.storageConnection = DriverManager.getConnection(url);
        this.storageConnection.setAutoCommit(true);
        Api.logging().logToOutput("Open new connection to the storage.");
        //Create the table
        try (Statement stmt = this.storageConnection.createStatement()) {
            stmt.execute(SQL_TABLE_CREATE);
            Api.logging().logToOutput("Recording table initialized.");
        }

    }

    void siteMaplogEvent(HttpRequestResponse reqInfo) throws Exception {
        //Verify that the DB connection is still opened
        this.ensureDBState();
        //Insert the event into the storage
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(SQL_TABLE_UPSERT)) {
            //LOCAL_SOURCE_IP 发起请求的ip
            stmt.setString(1, InetAddress.getLocalHost().getHostAddress());

            //HTTP_METHOD 请求方法
            stmt.setString(2, reqInfo.request().method());
            //TARGET_URL 请求url
            stmt.setString(3, reqInfo.request().url());


            String urlString = reqInfo.request().url();

            int queryIndex = urlString.indexOf("?");

            String urlWithoutQuery = queryIndex != -1 ? urlString.substring(0, queryIndex) : urlString;

            stmt.setString(4, urlWithoutQuery);


            //请求
            //REQUEST_RAW 请求包
            stmt.setString(5, reqInfo.request().toString());
            //SEND_DATETIME 发起请求的时间
            stmt.setString(6, LocalDateTime.now().format(this.datetimeFormatter));
            //响应
            //HTTP_STATUS_CODE 响应码
//            stmt.setString(7, String.valueOf(reqInfo.response().statusCode()));
            stmt.setString(7, (reqInfo.response() != null) ? String.valueOf(reqInfo.response().statusCode()) : "");
            //RESPONSE_RAW 响应包
            stmt.setString(8, (reqInfo.response() != null) ? reqInfo.response().toString() : "");
//            Api.logging().logToOutput("INSERT SUCCESS");

            int count = stmt.executeUpdate();
            if (count != 1) {
                Api.logging().logToOutput("Request was not inserted, no detail available (insertion counter = " + count + ") !");
            }
        } catch (Exception e) {
            Api.logging().logToError("log error"+e.getMessage());
        }
    }

    ResultSet loadDB() throws Exception {
        //Verify that the DB connection is still opened
        this.ensureDBState();

        try (PreparedStatement stmt = this.storageConnection.prepareStatement(SQL_TABLE_SELECT)) {

            return stmt.executeQuery();
        }
    }


    private void ensureDBState() throws Exception {
        //Verify that the DB connection is still opened
        if (this.storageConnection.isClosed()) {
            //Get new one
            Api.logging().logToOutput("Open new connection to the storage.");
            this.storageConnection = DriverManager.getConnection(url);
            this.storageConnection.setAutoCommit(true);
        }

    }

    /**
     * This method is invoked when the extension is unloaded.
     */
    @Override
    public void extensionUnloaded() {

//        List<HttpRequestResponse> httpRequestResponses = Api.siteMap().requestResponses();
//        for (int i = 0; i < httpRequestResponses.size(); i++) {
//
//            Api.logging().logToOutput(i+"/"+httpRequestResponses.size());
//
//            HttpRequestResponse httpRequestResponse = httpRequestResponses.get(i);
//            try {
//                siteMaplogEvent(httpRequestResponse);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }


        try {
            if (this.storageConnection != null && !this.storageConnection.isClosed()) {
                this.storageConnection.close();
                Api.logging().logToOutput("Connection to the storage released.");
            }
        } catch (Exception e) {
            Api.logging().logToError("Cannot close the connection to the storage: " + e.getMessage());
        }

    }
}
