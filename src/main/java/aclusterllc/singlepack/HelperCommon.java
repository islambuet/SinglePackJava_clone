package aclusterllc.singlepack;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;

public class HelperCommon {
    public static String getStackTraceString(Exception ex){
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }
    public static int bytesToInt(byte[] bytes)
    {
        return new BigInteger(bytes).intValue();
    }
    public static long bytesToLong(byte[] bytes)
    {
        return new BigInteger(bytes).longValue();
    }
    public static byte[] bitsFromBytes(byte[] source,int group){
        byte[] bits=new byte[source.length*8];
        for(int i=0;i<source.length/group;i++){
            for(int j=0;j<group;j++) {
                int byteIndex=i*group+j;
                int bitIndex=(i*group+(group-j)-1)*8;
                byte s=source[byteIndex];
                for(int b=0;b<8;b++){
                    bits[bitIndex+b]= (byte) ((s>>b)&1);
                }
            }
        }
        return bits;
    }
}
