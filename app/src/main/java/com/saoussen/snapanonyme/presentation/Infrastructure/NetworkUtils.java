package com.saoussen.snapanonyme.presentation.Infrastructure;

import android.content.Context;

import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.saoussen.snapanonyme.presentation.model.Snap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class NetworkUtils {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    // Base URL for snaps API.
    private static final String SNAP_BASE_URL = "http://ec2-3-82-244-27.compute-1.amazonaws.com:9000/snaps";

// mes API écoutent snaps

    // Parameter for coordinate longitude.
    private static final String LONGITUDE = "longitude";
    // Parameter for coordinate latitude.
    private static final String LATITUDE = "latitude";
    // Parameter for distance.
    private static final String DISTANCE = "distance";

    private static final String PICTURE_PARAM = "picture";// ça doit être égal à picture pour que le BakEND puisse la reconnaître


    // Récuperer les données du réseau


    public NetworkUtils() {


    }


    public static List<Snap> getSnaps(Location location, Double scope) {
        //TODO Build the request and IMPLEMENT THIS
        List<Snap> snaps = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        try {

            Uri builtUri = Uri.parse(SNAP_BASE_URL).buildUpon()
                    .appendQueryParameter(LONGITUDE, String.valueOf(location.getLongitude()))
                    .appendQueryParameter(LATITUDE, String.valueOf(location.getLatitude()))
                    .appendQueryParameter(DISTANCE, String.valueOf(scope))
                    .build();
            URL requestUrl = new URL(builtUri.toString());
            urlConnection = (HttpURLConnection) requestUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();

            ObjectMapper mapper = new ObjectMapper();

            // on converit le JASON en liste de String


            snaps = mapper.readValue(inputStream, new TypeReference<List<Snap>>() {
            });

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (urlConnection != null) {
                urlConnection.disconnect();
            }

        }

        return snaps;
    }


    // Cette classe sert à construire une requête de type multipart Data

    public void postSnap(Location location, File file) {

        try {
            MultipartUtility multipart = new MultipartUtility(SNAP_BASE_URL, "UTF-8");
            multipart.addFormField(LONGITUDE, String.valueOf(location.getLongitude()));
            multipart.addFormField(LATITUDE, String.valueOf(location.getLatitude()));
            multipart.addFilePart(PICTURE_PARAM, file);
            // multipart.addHeaderField(CONTENT_TYPE, "multipart/form-data");
            multipart.finish();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private class MultipartUtility {


        private final String boundary;
        private static final String LINE_FEED = "\r\n";
        private HttpURLConnection httpConn;
        private String charset;
        private OutputStream outputStream;
        private PrintWriter writer;

        /**
         * This constructor initializes a new HTTP POST request with content type
         * is set to multipart/form-data
         *
         * @param requestURL
         * @param charset
         * @throws IOException
         */
        private MultipartUtility(String requestURL, String charset)
                throws IOException {
            this.charset = charset;

            // creates a unique boundary based on time stamp
            boundary = "---" + System.currentTimeMillis() + "---";
            URL url = new URL(requestURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true);    // indicates POST method
            httpConn.setDoInput(true);
            httpConn.setRequestProperty("content-type",
                    "multipart/form-data; boundary=" + boundary);
            outputStream = httpConn.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                    true);
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */

        // rajoute une élément de texte
        public void addFormField(String name, String value) {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a upload file section to the request
         *
         * @param fieldName  name attribute in <input type="file" name="..." />
         * @param uploadFile a File to be uploaded
         * @throws IOException
         */

        // rajoute un élément de type Fichier
        public void addFilePart(String fieldName, File uploadFile)
                throws IOException {
            String fileName = uploadFile.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            String s = URLConnection.guessContentTypeFromName(fileName);
            Log.d(LOG_TAG, "addFilePart: " + s);
            writer.append(
                    "Content-Type: "
                            + s)
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();
            writer.append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a header field to the request.
         *
         * @param name  - name of the header field
         * @param value - value of the header field
         */
        public void addHeaderField(String name, String value) {
            writer.append(name + ": " + value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Completes the request and receives response from the server.
         *
         * @return a list of Strings as response in case the server returned
         * status OK, otherwise an exception is thrown.
         * @throws IOException
         */
        public List<String> finish() throws IOException {
            List<String> response = new ArrayList<String>();
            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            // checks server's status code first
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }
                reader.close();
                httpConn.disconnect();
            } else {
                throw new IOException("Server returned non-OK status: " + status);
            }
            return response;
        }
    }


}
