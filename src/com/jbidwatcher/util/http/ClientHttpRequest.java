/* Originally com.myjavatools.web; from http://www.devx.com/Java/Article/17679/1954 and heavily modified by Morgan Schweers */
/* Original license: http://www.myjavatools.com/license.txt */
package com.jbidwatcher.util.http;

import java.net.URLConnection;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.*;

/**
 * <p>Title: Client HTTP Request class</p>
 * <p>Description: this class helps to send POST HTTP requests with various form data,
 * including files. Cookies can be added to be included in the request.</p>
 *
 * @author Vlad Patryshev
 * @author Morgan Schweers
 * @version 1.1
 */
@SuppressWarnings({"JavaDoc"})
public class ClientHttpRequest {
  private URLConnection mConnection;
  private OutputStream mOutput = null;
  private Map<String,String> mCookies = new HashMap<String,String>();
  private Map<String,Object> mParameters = new LinkedHashMap<String,Object>();
  private String boundary;

  private void write(char c) throws IOException { mOutput.write(c); }
  private void write(String s) throws IOException { mOutput.write(s.getBytes()); }
  private void newline() throws IOException { write("\r\n"); }
  private void writeln(String s) throws IOException {
    write(s);
    newline();
  }

  private static Random random = new Random();
  private static String randomString() {
    return Long.toString(random.nextLong(), 36);
  }

  private void boundary() throws IOException {
    write("--");
    write(boundary);
  }

  /**
   * Creates a new multipart POST HTTP request on a freshly opened URLConnection
   *
   * @param connection an already open URL connection
   */
  public ClientHttpRequest(URLConnection connection) {
    mConnection = connection;
    ((HttpURLConnection)mConnection).setInstanceFollowRedirects(true);
    connection.setDoOutput(true);
    boundary = "---------------------------" + randomString() + randomString() + randomString();
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
  }

  /**
   * Creates a new multipart POST HTTP request for a specified URL
   *
   * @param url the URL to send request to
   */
  public ClientHttpRequest(URL url) throws IOException { this(url.openConnection()); }

  /**
   * Creates a new multipart POST HTTP request for a specified URL string
   *
   * @ param urlString the string representation of the URL to send request to
   */
//  public ClientHttpRequest(String urlString) throws IOException { this(new URL(urlString)); }

  private void postCookies() {
    StringBuffer cookieList = new StringBuffer();

    for (Iterator i = mCookies.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) (i.next());
      cookieList.append(entry.getKey().toString()).append("=").append(entry.getValue());

      if (i.hasNext()) {
        cookieList.append("; ");
      }
    }
    if (cookieList.length() > 0) {
      mConnection.setRequestProperty("Cookie", cookieList.toString());
    }
  }

  private void writeName(String name) throws IOException {
    newline();
    write("Content-Disposition: form-data; name=\"");
    write(name);
    write('"');
  }

  /**
   * adds a parameter to the request; if the parameter is a File, the file is uploaded, otherwise the string value of the parameter is passed in the request
   *
   * @param name   parameter name
   * @param value  parameter value, a File or anything else that can be stringified
   */
  public void setParameter(String name, Object value) {
    mParameters.put(name, value);
  }

  /**
   * adds a string parameter to the request
   *
   * @param name  parameter name
   * @param value parameter value
   */
  private void writeParameter(String name, String value) throws IOException {
    boundary();
    writeName(name);
    newline();
    newline();
    writeln(value);
  }

  private static void pipe(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[50000];
    int nread;
    while ((nread = in.read(buf, 0, buf.length)) >= 0) {
      out.write(buf, 0, nread);
    }
    out.flush();
  }

  /**
   * adds a file parameter to the request
   *
   * @param name     parameter name
   * @param filename the name of the file
   * @param is       input stream to read the contents of the file from
   */
  public void writeParameter(String name, String filename, InputStream is) throws IOException {
    boundary();
    writeName(name);
    write("; filename=\"");
    write(filename);
    write('"');
    newline();
    write("Content-Type: ");
    String type = URLConnection.guessContentTypeFromName(filename);
    if (type == null) type = "application/octet-stream";
    writeln(type);
    newline();
    pipe(is, mOutput);
    newline();
  }

  /**
   * adds a file parameter to the request
   *
   * @param name parameter name
   * @param file the file to upload
   */
  private void writeParameter(String name, File file) throws IOException {
    writeParameter(name, file.getPath(), new FileInputStream(file));
  }

  /**
   * adds parameters to the request
   *
   * @param parameters "name-to-value" map of parameters; if a value is a file, the file is uploaded, otherwise it is stringified and sent in the request
   */
  public void setParameters(Map<String,Object> parameters) {
    if (parameters == null) return;
    mParameters.putAll(parameters);
  }

  /**
   * posts the requests to the server, with all the cookies and parameters that were added
   *
   * @return input stream with the server response
   */
  public HttpURLConnection post() throws IOException {
    if (mOutput == null) mOutput = mConnection.getOutputStream();
    postCookies();
    for(String key : mParameters.keySet()) {
      Object value = mParameters.get(key);
      if(value instanceof File) writeParameter(key, (File)value);
      else writeParameter(key, value.toString());
    }
    boundary();
    writeln("--");
    mOutput.close();
    return (HttpURLConnection)mConnection;
  }
}