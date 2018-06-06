package distributeddrive;


import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.callbacks.DbxGlobalCallbackFactory;
import com.dropbox.core.v2.callbacks.DbxRouteErrorCallback;
import com.dropbox.core.v2.callbacks.DbxNetworkErrorCallback;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.users.SpaceUsage;

import java.io.*;

public class Dropbox implements CloudDrive{

  private static final String ACCESS_TOKEN ="t-na_lS9wWAAAAAAAAAAGZN0ewf8U71YT7XWaLhFTOiSCsgfx5AywvhmavdXfEhG";
  private static DbxRequestConfig config;
  private static DbxClientV2 client;


  public Dropbox(){
    config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
    client = new DbxClientV2(config, ACCESS_TOKEN);
  }

  public void uploadFile(java.io.File file,String filename){
    try(InputStream in = new FileInputStream(file)){
      FileMetadata metadata = client.files().uploadBuilder("/"+file.getName()).
                              uploadAndFinish(in);
    }catch(Exception e){

    }
  }

  public ByteArrayOutputStream downloadFile(String filename){
    try{
      DbxDownloader<FileMetadata> downloader = client.files().download("/"+filename);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      downloader.download(out);
      return out;
    }catch(Exception e){
      System.out.println(e.getMessage());
      return null;
    }
  }
  public long getSpaceLeft(){
    try{
      SpaceUsage spaceusage = client.users().getSpaceUsage();
      long limit = spaceusage.getAllocation()
                            .getIndividualValue()
                            .getAllocated();
      long usage = spaceusage.getUsed();
      return limit - usage;
    }catch(Exception e){
      System.out.println(e);
      return 0;
    }
  }
}
