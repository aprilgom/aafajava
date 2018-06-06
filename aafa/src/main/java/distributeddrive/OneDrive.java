package distributeddrive;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.*;
import com.google.api.client.http.json.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.*;
import java.io.*;
import java.util.Collections;
import java.util.*;
import java.net.URL;

public class OneDrive implements CloudDrive{

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static HttpRequestFactory httpRequestFactory;
  private static NetHttpTransport HTTP_TRANSPORT;
  private static String access_token;
  private static int SEND_LIMIT = 7864320;//7864320byte = 60MiB
  public OneDrive(){
    try{
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      httpRequestFactory = HTTP_TRANSPORT.createRequestFactory();
    }catch(Exception e){

    }
    access_token = getAccessToken();
    System.out.println(access_token);
  }
  private String getAccessToken(){
    try{
      java.io.File refresh_token_file  = new java.io.File("credentials/onedrive_refresh_token");
      HttpRequest get_access_token_req;
      if(!refresh_token_file.exists()){
        refresh_token_file.createNewFile();
        //we need to get a refresh_token that remains after 6 months

        String access_code = "M4ef9a445-f46b-6f85-7770-1e5d38f03150";
        //this is my private code!! you have to login with your own account!!
        //getting an access code takes several steps..
        //http://euryale.tistory.com/88?category=225967
        get_access_token_req  = httpRequestFactory.buildPostRequest(
          new GenericUrl(new URL("https://login.microsoftonline.com/common/oauth2/v2.0/token"))
          ,new ByteArrayContent("application/x-www-form-urlencoded",
          new String("client_id=497963aa-2fff-4903-bfef-183f9c65e6f2"
          +"&redirect_uri=http%3A%2F%2Flocalhost"
          +"&client_secret=lslkRUSS290*lxjGKX82|}{"
          +"&code="+access_code
          +"&grant_type=authorization_code").getBytes())
        );
      }else{
        FileInputStream fis = new FileInputStream(refresh_token_file);
          //refresh_token that stored last time.
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[10];
        while (fis.read(buffer) != -1){
          sb.append(new String(buffer));
          buffer = new byte[10];
        }
        String old_refresh_token = sb.toString();
        get_access_token_req = httpRequestFactory.buildPostRequest(
          new GenericUrl(new URL("https://login.microsoftonline.com/common/oauth2/v2.0/token"))
          ,new ByteArrayContent("application/x-www-form-urlencoded",
          new String("client_id=497963aa-2fff-4903-bfef-183f9c65e6f2"
          +"&redirect_uri=http%3A%2F%2Flocalhost"
          +"&client_secret=lslkRUSS290*lxjGKX82|}{"
          +"&refresh_token="+old_refresh_token
          +"&grant_type=refresh_token").getBytes())
        );//this request grants new refresh_token and access_token also.
        fis.close();
      }
      String auth_json = get_access_token_req.execute()
                        .parseAsString();
      JsonParser parser = new JsonParser();
      JsonElement element = parser.parse(auth_json);
      String access_token = element.getAsJsonObject().get("access_token").getAsString();
      String refresh_token = element.getAsJsonObject().get("refresh_token").getAsString();
      FileOutputStream fos = new FileOutputStream(refresh_token_file);
      fos.write(refresh_token.getBytes());
      return access_token;
    }catch(IOException e){
      System.out.println(e);
      return null;
    }
  }
  public void uploadFile(java.io.File file,String filename){
    try{
      Map<String,Object> c = new HashMap<String,Object>();
      c.put("name",filename);

      HttpRequest c_req = httpRequestFactory.buildPostRequest(
        new GenericUrl(new URL(
           "https://graph.microsoft.com/v1.0"+
          "/me/drive/items/children"
        ))
        ,new JsonHttpContent(JSON_FACTORY,c)
      );
      c_req.getHeaders()
                    .setAuthorization("Bearer "+access_token);
      c_req.execute();

      /*
      Map<String,Object> itembody = new HashMap<String,Object>();
      itembody.put("@microsoft.graph.conflictBehavior","rename");
      itembody.put("name",filename);
      Map<String,Object> uploadsessionbody = new HashMap<String,Object>();
      uploadsessionbody.put("item",itembody);
      
      //create upload session
      HttpRequest create_upload_session_req = httpRequestFactory.buildPostRequest(
        new GenericUrl(new URL(
          "https://graph.microsoft.com/v1.0"+
          "/drives/root:/"+filename+":/createUploadSession"
        )),
        new JsonHttpContent(JSON_FACTORY,uploadsessionbody)
      );
      create_upload_session_req.getHeaders()
                    .setAuthorization("Bearer "+access_token);
      String upload_session_json = create_upload_session_req.execute()
                              .parseAsString();
      JsonParser parser = new JsonParser();
      JsonElement element = parser.parse(upload_session_json);
      String sessionUrl = element.getAsJsonObject().get("uploadUrl").getAsString();

      //java.io.File to byte[]
      FileInputStream fis = new FileInputStream(file);
      byte[] filebyte = new byte[(int)file.length()];
      fis.read(filebyte);
      fis.close();
      //upload
      if(file.length() > SEND_LIMIT){
        for(int i = 0;i<(int)file.length()/SEND_LIMIT-1;i++){
          int start_of_part = i*SEND_LIMIT;
          int end_of_part = start_of_part + SEND_LIMIT-1;
          HttpRequest upload_req = httpRequestFactory.buildPutRequest(
            new GenericUrl(new URL(sessionUrl)),
            new ByteArrayContent("application/octet-stream",
              Arrays.copyOfRange(filebyte,start_of_part,end_of_part)
            )
          );
          upload_req.getHeaders()
                    .setAuthorization("Bearer "+access_token);
          upload_req.execute();
        }
        int start_of_part = ((int)file.length()/SEND_LIMIT)*SEND_LIMIT;
        int end_of_part = (int)file.length()-1;
        HttpRequest upload_req = httpRequestFactory.buildPutRequest(
            new GenericUrl(new URL(sessionUrl)),
            new ByteArrayContent("application/octet-stream",
              Arrays.copyOfRange(filebyte,start_of_part,end_of_part)
            )
        );
        upload_req.getHeaders()
                  .setAuthorization("Bearer "+access_token);
        upload_req.execute();
      }else{
        HttpRequest upload_req = httpRequestFactory.buildPutRequest(
          new GenericUrl(new URL(sessionUrl)),
          new ByteArrayContent("application/octet-stream",filebyte)
        );
        upload_req.getHeaders()
                  .setAuthorization("Bearer "+access_token);
        upload_req.execute();
      }
      
    */

    }catch(Exception e){
      System.out.println(e);
    }

  }
  public ByteArrayOutputStream downloadFile(String filename){return null;}
  public long getSpaceLeft(){return 0;}

}
