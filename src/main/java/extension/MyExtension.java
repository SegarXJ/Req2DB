package extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class MyExtension implements BurpExtension {
    public static MontoyaApi Api;
    private ReqLogger reqLogger;
    private String extensionName = "Req2DB";


    @Override
    public void initialize(MontoyaApi montoyaApi) {
        Api = montoyaApi;
        String storeFileName = "D://" + extensionName + ".db";
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

            //定义面板
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
            storeFileNameSaveButton.setText("Save");
            storeFileNameSaveButton.addActionListener(e -> {
                Api.logging().logToOutput(storeFileNameTextField.getText());
            });

            //保存按钮的样式
            myPanel.add(storeFileNameSaveButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));


            //输出sitemap请求到数据库
            JButton toDBButton = new JButton();
            toDBButton.setText("Save to DB:");
            toDBButton.addActionListener(e -> {
                List<HttpRequestResponse> httpRequestResponses = Api.siteMap().requestResponses();
                for (int i = 0; i < httpRequestResponses.size(); i++) {

                    Api.logging().logToOutput(i + "/" + httpRequestResponses.size());

                    HttpRequestResponse httpRequestResponse = httpRequestResponses.get(i);
                    try {
                        reqLogger.siteMaplogEvent(httpRequestResponse);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            //按钮的样式
            myPanel.add(toDBButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 5), 0, 0));
            add(myPanel);


        }

    }


}
