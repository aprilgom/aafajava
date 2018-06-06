package distributeddrive;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.ByteArrayContent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.*;
import java.io.*;

public class OneDrive implements CloudDrive{

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static HttpRequestFactory httpRequestFactory;
  private static NetHttpTransport HTTP_TRANSPORT;
  private static String access_token;
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
      
      String reqjson = gson.toJson(reqjsonobj);
      HttpRequest upload_req = httpRequestFactory.buildPostRequest(
        new GenericUrl(new URL(
          "https://graph.microsoft.com/v1.0"+
          "/drives/root:/"+filename+":/createUploadSession"
        )),

      );
    }catch(Exception e){
      System.out.println(e);
    }

  }
  public ByteArrayOutputStream downloadFile(String filename){return null;}
  public long getSpaceLeft(){return 0;}

}
