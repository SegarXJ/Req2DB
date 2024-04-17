package extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static burp.api.montoya.http.message.requests.HttpRequest.httpRequest;
import static burp.api.montoya.http.message.responses.HttpResponse.httpResponse;

public class MyExtension implements BurpExtension {
    public static MontoyaApi Api;
    private ReqLogger reqLogger;
    private String extensionName = "Req2DB";


    @Override
    public void initialize(MontoyaApi montoyaApi) {
        Api = montoyaApi;
        String storeFileName = "E://" + extensionName + ".db";
        try {
            Api.extension().setName(extensionName);

//            Api.proxy().registerRequestHandler(new MyProxyRequestHandler(Api));
//            Api.http().registerHttpHandler(new MyHttpHandler(Api));


            reqLogger = new ReqLogger(Api, storeFileName);
            Api.extension().registerUnloadingHandler(reqLogger);
            Api.userInterface().registerSuiteTab("Req2DB", new Req2DBTab());


        } catch (Exception e) {
            String eMessage = e.getMessage();
            Api.logging().logToError(eMessage);

        }


    }

    private class Req2DBTab extends JComponent {
        public Req2DBTab() {


            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

            //定义初始面板
            JPanel myPanel = new JPanel();

            //数据库文件名

            JLabel storeFileNameLabel = new JLabel();
            storeFileNameLabel.setText("storeFileName:");

            //文本框
            JTextField storeFileNameTextField;
            storeFileNameTextField = new JTextField();
            //默认值
            storeFileNameTextField.setText("Req2DB");


            //设置样式,添加到面板
            myPanel.add(storeFileNameLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
                    new Insets(0, 5, 5, 5), 0, 0));
            myPanel.add(storeFileNameTextField, new GridBagConstraints(1, 1, 6, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));


            //保存按钮,设置数据库文件名的值
            JButton storeFileNameSaveButton = new JButton();
            storeFileNameSaveButton.setText("Load from DB:");
            storeFileNameSaveButton.addActionListener(this::loadFromDB);

            //保存按钮的样式
            myPanel.add(storeFileNameSaveButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));


            //输出sitemap请求到数据库
            JButton toDBButton = new JButton();
            toDBButton.setText("Save to DB:");
            toDBButton.addActionListener(this::saveToDB);

            //按钮的样式
            myPanel.add(toDBButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));
            add(myPanel);


            //TODO 进度输出到界面


            //TODO 从DB回读请求,进行越权测试
            //TODO: 测试越权插件 二开/自研 不同端口代理区分权限角色


        }

        private void saveToDB(ActionEvent e) {
            List<HttpRequestResponse> httpRequestResponses = Api.siteMap().requestResponses(node -> {
                // 常见前端静态文件后缀集合
                final List<String> STATIC_FILE_EXTENSIONS = Arrays.asList(
                        ".js",
                        ".css",
                        ".jpg",
                        ".jpeg",
                        ".png",
                        ".gif",
                        ".svg",
                        ".woff",
                        ".woff2",
                        ".ttf",
                        ".eot",
                        ".html",
                        ".ico"
                );
                URL url;
                try {
                    url = new URL(node.url());
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
                String path = url.getPath();

                // 将路径转换为小写并检查是否包含任何静态文件后缀
                return STATIC_FILE_EXTENSIONS.stream()
                        .noneMatch(path.toLowerCase()::endsWith);

            });


            for (int i = 0; i < httpRequestResponses.size(); i++) {

                Api.logging().logToOutput("Saving--------" + i + "/" + httpRequestResponses.size());

                HttpRequestResponse httpRequestResponse = httpRequestResponses.get(i);
                try {
                    reqLogger.siteMaplogEvent(httpRequestResponse);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private void loadFromDB(ActionEvent e){

            Api.logging().logToOutput("httpRequest.TEST");

            try (ResultSet resultSet = reqLogger.loadDB()) {

                Api.logging().logToOutput("getFetchSize++"+resultSet.getFetchSize());

                while (resultSet.next()) {


                    String REQUEST_RAW = resultSet.getString("REQUEST_RAW");
                    String RESPONSE_RAW = resultSet.getString("RESPONSE_RAW");
                    String TARGET_URL = resultSet.getString("TARGET_URL");
                    URL url = new URL(TARGET_URL);
                    String host = url.getHost();

                    HttpRequest httpRequest = httpRequest(REQUEST_RAW);
                    HttpResponse httpResponse = httpResponse(RESPONSE_RAW);



                    HttpRequest httpRequest1 = httpRequest.withService(new HttpService() {
                        @Override
                        public String host() {
                            return host;
                        }

                        @Override
                        public int port() {
                            return 443;
                        }

                        @Override
                        public boolean secure() {
                            return true;
                        }

                        @Override
                        public String ipAddress() {
                            return null;
                        }
                    });

                    Api.logging().logToOutput("httpRequest.httpService().host():");
                    Api.logging().logToOutput(httpRequest1.httpService().host());

                    Api.repeater().sendToRepeater(httpRequest1);
                    HttpRequestResponse httpRequestResponse = HttpRequestResponse.httpRequestResponse(httpRequest1, httpResponse);


                    Api.siteMap().add(httpRequestResponse);


                }
            } catch (Exception ex) {
                Api.logging().logToOutput(String.valueOf(ex));


                throw new RuntimeException(ex);



            }


        }
    }


}
