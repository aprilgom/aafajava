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
      System.out.println(e);
    }
    access_token = getAccessToken();
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
  public void uploadFile(byte[] file,String filename){
    try{

      Map<String,Object> uploadsessionbody = new HashMap<String,Object>();
      uploadsessionbody.put("@microsoft.graph.conflictBehavior","rename");
      uploadsessionbody.put("name",filename);
       /*
      Map<String,Object> uss = new HashMap<String,Object>();
      Map<String,Object> hashesType = new HashMap<String,Object>();
      hashesType.put("crc32Hash",null);
      hashesType.put("sha1Hash",null);
      hashesType.put("quickXorHash",null);
      uss.put("hashes",hashesType);
      uss.put("mimeType","application/octet-stream");

      Map<String,Object> fjd = new HashMap<String,Object>();
      fjd.put("name",filename);
      fjd.put("file",uss);



      HttpRequest usreq = httpRequestFactory.buildPostRequest(
        new GenericUrl(new URL(
          "https://graph.microsoft.com/v1.0"+
          "/me/drive/items/92660A9B05948702!877/createUploadSession"
        )),
        new JsonHttpContent(JSON_FACTORY,fjd)
      );
      usreq.getHeaders()
                    .setAccept("application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8");
      usreq.getHeaders()
                    .setAuthorization("Bearer "+access_token);
      usreq.execute();
      System.out.println("fdasfdasf");
      */
      //create upload session
      HttpRequest create_upload_session_req = httpRequestFactory.buildPostRequest(
        new GenericUrl(new URL(
          "https://graph.microsoft.com/v1.0"+
          "/drive/root:/"+filename+":/createUploadSession"
        )),
        new JsonHttpContent(JSON_FACTORY,uploadsessionbody)
      );
      create_upload_session_req.getHeaders()
                    .setAuthorization("Bearer "+access_token)
                    .setAccept("application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8");

      String upload_session_json = create_upload_session_req.execute()
                              .parseAsString();
      JsonParser parser = new JsonParser();
      JsonElement element = parser.parse(upload_session_json);
      String sessionUrl = element.getAsJsonObject().get("uploadUrl").getAsString();

      //java.io.File to byte[]
      /*
      FileInputStream fis = new FileInputStream(file);
      byte[] filebyte = new byte[(int)file.length()];
      fis.read(filebyte);
      fis.close();
      */

      //upload
      int length_of_file = file.length;

      if(length_of_file > SEND_LIMIT){
        for(int i = 0;i<length_of_file/SEND_LIMIT;i++){
          int start_of_part = i*SEND_LIMIT;
          int end_of_part = start_of_part + SEND_LIMIT;
          HttpRequest upload_req = httpRequestFactory.buildPutRequest(
            new GenericUrl(new URL(sessionUrl)),
            new ByteArrayContent("application/octet-stream",
              Arrays.copyOfRange(file,start_of_part,end_of_part)
            )
          );

          upload_req.getHeaders()
                    .setAuthorization("Bearer "+access_token)
                    .setContentLength((long)SEND_LIMIT)
                    .setContentRange("bytes "+start_of_part+"-"+(end_of_part-1)+"/"+length_of_file);
          System.out.println(upload_req.getHeaders().getContentRange());
          upload_req.execute();

          System.out.println("part"+i);
        }
        int start_of_part = (length_of_file/SEND_LIMIT)*SEND_LIMIT;
        System.out.println(start_of_part);
        int end_of_part = length_of_file;
        HttpRequest upload_req = httpRequestFactory.buildPutRequest(
            new GenericUrl(new URL(sessionUrl)),
            new ByteArrayContent("application/octet-stream",
              Arrays.copyOfRange(file,start_of_part,end_of_part)
            )
        );
        upload_req.getHeaders()
                  .setAuthorization("Bearer "+access_token)
                  .setContentRange("bytes "+start_of_part+"-"+(end_of_part-1)+"/"+length_of_file);
        upload_req.execute();
      }else{
        HttpRequest upload_req = httpRequestFactory.buildPutRequest(
          new GenericUrl(new URL(sessionUrl)),
          new ByteArrayContent("application/octet-stream",file)
        );
        upload_req.getHeaders()
                  .setAuthorization("Bearer "+access_token)
                  .setContentRange("bytes "+"0-"+(length_of_file-1)+"/"+length_of_file);
        upload_req.execute();
      }
    }catch(Exception e){
      System.out.println("error while uploading from onedrive:"+e);
    }

  }
  public ByteArrayOutputStream downloadFile(String filename){
    try{
      HttpRequest download_location_req = httpRequestFactory.buildGetRequest(
        new GenericUrl(new URL( "https://graph.microsoft.com/v1.0"+
            "/drive/root:/"+filename+":/content"))
      );
      download_location_req.getHeaders().setAuthorization("Bearer "+access_token);
      HttpResponse location_res = download_location_req.execute();


      ByteArrayOutputStream result = new ByteArrayOutputStream();
      location_res.download(result);
      return result;
    }catch(Exception e){
      System.out.println(e);
      return null;
    }
  }

  public void deleteFile(String filename){
    try{
      HttpRequest delete_req = httpRequestFactory.buildDeleteRequest(
        new GenericUrl(new URL( "https://graph.microsoft.com/v1.0"+
            "/drive/root:/"+filename))
      );
      delete_req.getHeaders().setAuthorization("Bearer "+access_token);
      delete_req.execute();
    }catch(Exception e){
      System.out.println(e);
    }
  }

  public long getSpaceLeft(){
    try{
      HttpRequest get_drive_req = httpRequestFactory.buildGetRequest(
        new GenericUrl(new URL( "https://graph.microsoft.com/v1.0/drive"))
      );
      get_drive_req.getHeaders()
                  .setAuthorization("Bearer "+access_token)
                  .setAccept("application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8");
      String drive_json = get_drive_req.execute().parseAsString();
      JsonParser parser = new JsonParser();
      JsonElement element = parser.parse(drive_json);
      long spaceleft = element.getAsJsonObject()
                                .getAsJsonObject("quota")
                                .get("remaining")
                                .getAsLong();
      return spaceleft;
    }catch(Exception e){
      System.out.println("error while downloading from onedrive:"+e);
      return 0;
    }
  }

}
