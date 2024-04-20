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
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static burp.api.montoya.http.message.requests.HttpRequest.httpRequest;
import static burp.api.montoya.http.message.responses.HttpResponse.httpResponse;

public class MyExtension implements BurpExtension {
    public static MontoyaApi Api;
    private ReqLogger reqLogger;
    private final String extensionName = "Req2DB";
    private String inputText = "Req2DB";
    private final String rootPath = "E://" + extensionName + "/";
    private final String storeFileName = rootPath + extensionName + ".db";

    private final String hostName = rootPath + "host.txt";
    private final String blackHostName = rootPath + "blackHost.txt";
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
    final List<String> HOST = Arrays.asList(
            "www.baidu.com"

    );
    final List<String> blackHost = Arrays.asList(
            "www.google.com"
    );

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        Api = montoyaApi;
        try {
            Api.extension().setName(extensionName);

//            Api.proxy().registerRequestHandler(new MyProxyRequestHandler(Api));
//            Api.http().registerHttpHandler(new MyHttpHandler(Api));
            File directory = new File(rootPath);

            // 判断目录是否存在
            if (!directory.exists()) {
                // 目录不存在，尝试创建
                boolean created = directory.mkdir();
                if (!created) {
                    Api.logging().logToError("Failed to create directory: " + rootPath);
                }
            }

            reqLogger = new ReqLogger(Api, storeFileName);
            Api.extension().registerUnloadingHandler(reqLogger);
            Api.userInterface().registerSuiteTab(extensionName, new Req2DBTab());


        } catch (Exception e) {
            String eMessage = e.getMessage();
            Api.logging().logToError("initialize error" + eMessage);

        }


    }

    private class Req2DBTab extends JComponent {
        public Req2DBTab() {


            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

            //定义初始面板
            JPanel myPanel = new JPanel();



            //数据库文件名

            JLabel storeFileNameLabel = new JLabel();
            storeFileNameLabel.setText(inputText);



            //添加多选框，选择黑白名单
            // 创建 ButtonGroup
            ButtonGroup buttonGroup = new ButtonGroup();

            // 创建 "黑白名单" 的 JRadioButton
            JRadioButton whitelist = new JRadioButton("whitelist");
            JRadioButton blacklist = new JRadioButton("blacklist");

            // 将 JRadioButtons 添加到 ButtonGroup
            buttonGroup.add(whitelist);
            buttonGroup.add(blacklist);

            // 添加 JRadioButtons 到 JPanel
            myPanel.add(whitelist);
            myPanel.add(blacklist);






            //文本框
            JTextField storeFileNameTextField;
            storeFileNameTextField = new JTextField();
            //默认值
            storeFileNameTextField.setText(extensionName);


            //设置样式,添加到面板
            myPanel.add(storeFileNameLabel, new  GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
                    new Insets(0, 5, 5, 5), 0, 0));
            myPanel.add(storeFileNameTextField, new GridBagConstraints(1, 1, 6, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));
            inputText = storeFileNameTextField.getText();
            storeFileNameTextField.selectAll();
            Api.logging().logToError("storeFileNameTextField:         " + inputText);


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
            toDBButton.addActionListener(new ActionListener() {

                /**
                 * Invoked when an action occurs.
                 *
                 * @param e the event to be processed
                 */
                @Override
                public void actionPerformed(ActionEvent e) {
//                    inputText = storeFileNameTextField.getText();
//                    Api.logging().logToError(inputText);
                    // 获取选中的 JRadioButton
                    JRadioButton selectedButton = (JRadioButton) buttonGroup.getSelection();

                    Api.logging().logToError("test select:"+selectedButton.getName());


//                    saveToDB();
                }
            });
            //按钮的样式
            myPanel.add(toDBButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));
            add(myPanel);


            //TODO 进度输出到界面


            //TODO 从DB回读请求,进行越权测试
            //TODO: 测试越权插件 二开/自研 不同端口代理区分权限角色


        }

        private void saveToDB() {


            List<HttpRequestResponse> httpRequestResponses = Api.siteMap().requestResponses(node -> {

                URL url;
                try {
                    url = new URL(node.url());
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }

                String path = url.getPath();
                String host = url.getHost();

                createFileIfNotExists(hostName, HOST);
                createFileIfNotExists(blackHostName, blackHost);

                List<String> fName = readHostFileToList(hostName);
                List<String> blackName = readHostFileToList(blackHostName);

                // 将域名转换为小写并检查是否在域名白名单 true:记录
                boolean b1 = fName.stream().anyMatch(host.toLowerCase()::equals);

                // 将路径转换为小写并检查是否包含任何静态文件后缀 true:记录
                boolean b2 = STATIC_FILE_EXTENSIONS.stream().noneMatch(path.toLowerCase()::endsWith);

                // 将域名转换为小写并检查是否在域名黑名单 true:记录
                boolean b3 = blackName.stream().noneMatch(host.toLowerCase()::equals);

                return b1 & b2 & b3;

            });

            for (int i = 0; i < httpRequestResponses.size(); i++) {

                Api.logging().logToOutput("Saving--------" + (i + 1) + "/" + httpRequestResponses.size());

                HttpRequestResponse httpRequestResponse = httpRequestResponses.get(i);
                try {
                    reqLogger.siteMaplogEvent(httpRequestResponse);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        public static List<String> readHostFileToList(String filePath) {

            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                Api.logging().logToError("Error reading host file: " + e.getMessage());
            }
            return lines;
        }

        public static void createFileIfNotExists(String filePath, List<String> list) {
            if (!fileExists(filePath)) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    for (String extension : list) {
                        writer.write(extension);
                        writer.newLine();
                    }
                    Api.logging().logToOutput(filePath + "File created successfully.");
                } catch (IOException e) {
                    Api.logging().logToError("Error creating file: " + e.getMessage());
                }

            }
        }

        private static boolean fileExists(String filePath) {
            boolean exists;
            try {
                exists = Files.exists(Paths.get(filePath));
            } catch (Exception e) {
                return false;
            }
            return exists;
        }


        private void loadFromDB(ActionEvent e) {

            Api.logging().logToOutput("httpRequest.TEST");

            try (ResultSet resultSet = reqLogger.loadDB()) {

                Api.logging().logToOutput("getFetchSize++" + resultSet.getFetchSize());

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
