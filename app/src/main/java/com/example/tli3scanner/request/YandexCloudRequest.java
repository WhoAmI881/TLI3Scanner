package com.example.tli3scanner.request;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class YandexCloudRequest {

    private static final int MEGABYTE = 1024 * 1024;
    private static final int TIMEOUT = 1000 * 60 * 5;
    private static final String fileFORMAT = ".pdf";
    private static final String urlREPORTS = "https://www.tli3.ru/reports/";
    private static final String urlBUCKET = "https://storage.yandexcloud.net/tigilante-bak";
    private static final String urlFUNCTION = "https://functions.yandexcloud.net/d4emhc8tgpk26qrqmtoh?file=";


    public YandexCloudRequest(){

    }

    public static int thisReportExists(String report){
        try {
            URL url = new URL(urlREPORTS + report + fileFORMAT);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(TIMEOUT);
            urlConnection.setReadTimeout(TIMEOUT);
            urlConnection.setRequestMethod("HEAD");
            int response = urlConnection.getResponseCode();
            urlConnection.disconnect();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return HttpsURLConnection.HTTP_BAD_REQUEST;
        }
    }

    public static int sendVideoInBucket(String key, File file){
        try {
            URL url = new URL(urlBUCKET);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(TIMEOUT);
            urlConnection.setReadTimeout(TIMEOUT);
            MultipartUtility multipart = new MultipartUtility(urlConnection);

            multipart.addFormField("key", key);
            multipart.addFormField("x-amz-credential", "eI39Uh4xNX3_dnr7aQrF/20220505/ru-central1/s3/aws4_request");
            multipart.addFormField("x-amz-algorithm", "AWS4-HMAC-SHA256");
            multipart.addFormField("x-amz-date", "20220505T123349Z");
            multipart.addFormField("policy", "eyJleHBpcmF0aW9uIjogIjIwMjQtMDUtMDRUMTI6MzM6NDlaIiwgImNvbmRpdGlvbnMiOiBbeyJidWNrZXQiOiAidGlnaWxhbnRlLWJhayJ9LCBbInN0YXJ0cy13aXRoIiwgIiRrZXkiLCAiIl0sIHsieC1hbXotYWxnb3JpdGhtIjogIkFXUzQtSE1BQy1TSEEyNTYifSwgeyJ4LWFtei1jcmVkZW50aWFsIjogImVJMzlVaDR4TlgzX2RucjdhUXJGLzIwMjIwNTA1L3J1LWNlbnRyYWwxL3MzL2F3czRfcmVxdWVzdCJ9LCB7IngtYW16LWRhdGUiOiAiMjAyMjA1MDVUMTIzMzQ5WiJ9XX0=");
            multipart.addFormField("x-amz-signature", "69d0080155891b0cc367551ab2e36f3e43ec563e3fde4e481f8dd0715dca5eb5");
            multipart.addFilePart("file", file);

            int response = multipart.getResponseCode();
            urlConnection.disconnect();
            return response;
        }catch(IOException e){
            e.printStackTrace();
            return HttpsURLConnection.HTTP_BAD_REQUEST;
        }
    }

    public static String getResultProcessingVideo(String filename) {
        try {
            URL url = new URL(urlFUNCTION + filename);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(TIMEOUT);
            urlConnection.setReadTimeout(TIMEOUT);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder urlResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                urlResponse.append(line);
            }
            reader.close();
            urlConnection.disconnect();
            return urlResponse.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean downloadPDFFile(String fileName, File directory) {
        try {
            URL url = new URL(urlREPORTS + fileName);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(TIMEOUT);
            urlConnection.setReadTimeout(TIMEOUT);
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(directory);
            byte[] buffer = new byte[MEGABYTE];
            int bufferLength = 0;
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bufferLength);
            }
            fileOutputStream.close();
            urlConnection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
