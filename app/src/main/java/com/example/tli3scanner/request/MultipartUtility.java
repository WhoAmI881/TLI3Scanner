package com.example.tli3scanner.request;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class MultipartUtility {

    private static final String LINE_FEED = "\r\n";
    private final String CHARSET = "UTF-8";
    private final String BOUNDARY = "----WebKitFormBoundaryw4sC6ROSdXahddqN";
    private final String REQUEST_METHOD = "POST";

    private HttpsURLConnection mHttpsConn;
    private OutputStream mOutputStream;
    private PrintWriter mPrintWriter;

    public MultipartUtility(HttpsURLConnection httpsConn) throws IOException {
        mHttpsConn = httpsConn;
        mHttpsConn.setRequestMethod(REQUEST_METHOD);
        mHttpsConn.setUseCaches(false);
        mHttpsConn.setDoOutput(true);
        mHttpsConn.setDoInput(true);
        mHttpsConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        mOutputStream = mHttpsConn.getOutputStream();
        mPrintWriter = new PrintWriter(new OutputStreamWriter(mOutputStream, CHARSET), true);
    }

    public void addFormField(String name, String value) {
        mPrintWriter.append("--" + BOUNDARY).append(LINE_FEED);
        mPrintWriter.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE_FEED);
        mPrintWriter.append("Content-Type: text/plain; charset=").append(CHARSET).append(LINE_FEED);
        mPrintWriter.append(LINE_FEED);
        mPrintWriter.append(value).append(LINE_FEED);
        mPrintWriter.flush();
    }

    public void addFilePart(String name, File file) throws IOException {
        String fileName = file.getName();

        mPrintWriter.append("--" + BOUNDARY).append(LINE_FEED);
        mPrintWriter.append("Content-Disposition: form-data; name=\"")
                .append(name).append("\"; filename=\"")
                .append(fileName).append("\"")
                .append(LINE_FEED);
        mPrintWriter.append("Content-Type: ")
                .append(URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        mPrintWriter.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        mPrintWriter.append(LINE_FEED);
        mPrintWriter.flush();

        try(FileInputStream inputStream = new FileInputStream(file)){
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                mOutputStream.write(buffer, 0, bytesRead);
            }
        }
        mOutputStream.flush();
        mPrintWriter.append(LINE_FEED);
        mPrintWriter.flush();
    }

    public int getResponseCode() throws IOException {
        mPrintWriter.append(LINE_FEED).flush();
        mPrintWriter.append("--" + BOUNDARY + "--").append(LINE_FEED);
        mPrintWriter.close();
        return mHttpsConn.getResponseCode();
    }

}
