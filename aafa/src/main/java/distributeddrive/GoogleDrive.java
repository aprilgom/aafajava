package distributeddrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.*;
import com.google.api.client.http.json.*;

import com.google.gson.Gson;

import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.*;
import java.net.URL;


import com.google.api.services.drive.model.About;

public class GoogleDrive implements CloudDrive{

  private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private static final String CLIENT_SECRET_DIR = "/client_secret.json";
  private static Credential credential;
  private static Drive service;

  private static NetHttpTransport HTTP_TRANSPORT ;
  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved credentials/ folder.
   */

  /**
   * Creates an authorized Credential object.
  * @param HTTP_TRANSPORT The network HTTP Transport.
  * @return An authorized Credential object.
  * @throws IOException If there is no client_secret.
  */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    // Load client secrets.
    InputStream in = DriveQuickstart.class.getResourceAsStream(CLIENT_SECRET_DIR);
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
            .setAccessType("offline")
            .build();
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  public GoogleDrive() throws IOException, GeneralSecurityException{
    try{
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    }catch(Exception e){

    }
    credential = getCredentials(HTTP_TRANSPORT);
    service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
  }

  public void uploadFile(java.io.File file,String filename){
    /*example : upload a simple image*/
    /*
    File fileMetadata = new File();
    fileMetadata.setName(filename);
    java.io.File filePath = new java.io.File("files/photo.jpg");
    FileContent mediaContent = new FileContent("image/jpeg", filePath);
    File file = service.files().create(fileMetadata, mediaContent)
    .setFields("id")
    .execute();
    System.out.println("File ID: " + file.getId());
    */

    /* Resumable upload*/
    try{
      //need code for checking overlapped file on drive..
      Map<String,Object> fileMetadata = new HashMap<String,Object>();
      fileMetadata.put("name",filename);

      HttpRequestFactory httpRequestFactory = HTTP_TRANSPORT.createRequestFactory();

      HttpRequest init_req = httpRequestFactory.buildPostRequest(
        new GenericUrl(new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")),
        new JsonHttpContent(JSON_FACTORY,fileMetadata)
      );

      init_req.getHeaders().setAuthorization("Bearer "+credential.getAccessToken());
      System.out.println("access token:"+credential.getAccessToken());
      //init_req.getHeaders().setContentLength((long)0); - this behavior is done by EmptyContent()

      String sessionUrl = init_req.execute().getHeaders().getLocation();
      System.out.println("location:"+sessionUrl);

      HttpRequest upload_req = httpRequestFactory.buildPutRequest(
        new GenericUrl(new URL(sessionUrl)),
        new FileContent("application/octet-stream",file)
      );
      upload_req.getHeaders().setContentLength((long)file.length());
      upload_req.execute();
      //need code for interrupted files...

    }catch(Exception e){
      System.out.println("google drive error while uploading file..."+e);
    }
  }
  public ByteArrayOutputStream downloadFile(String filename){
    try{
      FileList result = service.files().list()
                        .setQ("name=\'"+filename+"\'") // finds file that equals 'filename'
                        .execute();
      String fileId = "default";
      for(File file : result.getFiles()){
        fileId = file.getId();
      }//there could be more than one file..
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      service.files().get(fileId)
      .executeMediaAndDownloadTo(outputStream);
      return outputStream;
    }catch(Exception e){
      System.out.println("google drive error while downloading file...:"+e);
      return null;
    }
  }
  public long getSpaceLeft(){
    try{
      About in = service.about().get()
                    .setFields("storageQuota")
                    .execute();
      About.StorageQuota sq = in.getStorageQuota();

      return sq.getLimit() - sq.getUsage();
    }catch(Exception e){
      System.out.println("google drive error while getting space left..."+e);
      return 0;
    }
  }
  public void listFile() throws IOException, GeneralSecurityException {
    FileList result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
    List<File> files = result.getFiles();
    if (files == null || files.isEmpty()) {
        System.out.println("No files found.");
    } else {
        System.out.println("Files:");
        for (File file : files) {
            System.out.printf("%s (%s)\n", file.getName(), file.getId());
        }
    }
  }

}
