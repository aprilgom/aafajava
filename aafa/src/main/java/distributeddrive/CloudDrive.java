package distributeddrive;
import java.io.ByteArrayOutputStream;
public interface CloudDrive{
  public void uploadFile(byte[] file,String filename);
  public ByteArrayOutputStream downloadFile(String filename);
  public void deleteFile(String filename);
  public long getSpaceLeft();

}
