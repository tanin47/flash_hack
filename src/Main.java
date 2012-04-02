import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: tanin
 * Date: 4/2/12
 * Time: 8:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    
    public static void main(String[] args) {

        try {

            InputStream in = new FileInputStream(new File("HelloWorld.swf"));

            // General header
            System.out.print("Header: ");
            for (int i=0;i<4;i++) {
                System.out.print("0x" + toHex(in.read()) + " ");
            }
            System.out.println();
            
            // uncompressed length
            long fileLength = 0;
            long multi = 1;
            for (int i=0;i<4;i++) {
                fileLength += multi * in.read();
                multi *= 16 * 16;
            }
            System.out.print("Length: " + fileLength + " (include the first 8 bytes, header and length)");
            System.out.println();



            InflaterInputStream zipIn = new InflaterInputStream(in);

//            int count = 0;
//            while (zipIn.read() >= 0) count++;
//            System.out.println(count);
            // count should equal length + 8

            BitInputStream bitIn = new BitInputStream(zipIn);

            int coordinateBitLength = bitIn.readBit(5);
            System.out.println("Coord bit's length = " + coordinateBitLength);

            int minX = bitIn.readBit(coordinateBitLength);
            int maxX = bitIn.readBit(coordinateBitLength);
            int minY = bitIn.readBit(coordinateBitLength);
            int maxY = bitIn.readBit(coordinateBitLength);
            
            System.out.println("Frame (in twips): (" + minX + ", " + minY + ") (" + maxX + ", " + maxY + ")");

            bitIn.padding();

            System.out.println("Frame rate: " + bitIn.readUnsignedInt(2));
            System.out.println("Frame count: " + bitIn.readUnsignedInt(2));
            System.out.println();

            // first tag
            int run = 0;
            while (run < 100000000) {

                int header = bitIn.readUnsignedInt(2);
                int tagType = (header & 0xFFC0) >> 6;
                int tagLengthBytes = header & 0x003F;

                if (tagLengthBytes == 0x3F) {
                    //System.out.println("variable-length");
                    tagLengthBytes = bitIn.readSignedInt(4);
                }

                System.out.println("Tag type: " + tagType);
                System.out.println("Tag length: " + tagLengthBytes);

                System.out.print("Data: ");
                for (int i=0;i<tagLengthBytes;i++) {
                    System.out.print(toHex(bitIn.readUnsignedInt(1)) + " ");
                }
                System.out.println();
//                bitIn.readBit(tagLengthBytes * 8);

                System.out.println();

                if (tagType == 0) break;
                run++;
            }

            //0000000010 000100
            //00000000 10000100

            //0000000000 100001

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public static String toHex(int b) {
        String hex = Integer.toHexString(0xFF & (byte)b);
        if (hex.length() == 1) {
            // could use a for loop, but we're only dealing with a single byte
            hex = "0" + hex;
        }
        return hex;
    }

    // Returns the contents of the file in a byte array.
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
    
}
