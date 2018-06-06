package distributeddrive;


import java.security.GeneralSecurityException;

import java.io.IOException;
import java.io.*;
public class DriveQuickstart {


    public static void main(String... args) {
        // Build a new authorized API client service.
        try{
          CloudDrive googleDrive = new GoogleDrive();
          CloudDrive dropbox = new Dropbox();
          CloudDrive oneDrive = new OneDrive();
          
          //java.io.File file = new java.io.File("files/photo.jpg");
          //googleDrive.uploadFile(file,"name");
          //dropbox.uploadFile(file,"name");
          //java.io.File downfile = new java.io.File("files/name");
          //FileOutputStream fos = new FileOutputStream(downfile);
          //googleDrive.downloadFile("name").writeTo(fos);
          //dropbox.downloadFile("photo.jpg").writeTo(fos);
          //System.out.println(googleDrive.getSpaceLeft());
          //System.out.println(dropbox.getSpaceLeft());
        }catch(IOException e){
        }catch(GeneralSecurityException gse){
        }
    }

}
