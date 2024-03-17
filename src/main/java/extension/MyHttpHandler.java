package extension;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class MyHttpHandler implements HttpHandler {
    public static MontoyaApi Api;
    private ReqLogger reqLogger;

    public MyHttpHandler(MontoyaApi api) {
        Api= api;

    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {


        httpRequestToBeSent.toolSource();

        HttpRequest httpRequest = httpRequestToBeSent.copyToTempFile();

//        reqLogger.logEvent(httpRequestToBeSent);

//        Api.logging().logToOutput(httpRequest.toString());



        return null;
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {


        HttpResponse httpResponse = httpResponseReceived.copyToTempFile();




        return null;
    }
}
