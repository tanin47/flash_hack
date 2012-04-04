import java.io.*;
import java.util.zip.*;

/**
 * Created by IntelliJ IDEA.
 * User: tanin
 * Date: 4/3/12
 * Time: 12:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class CopySwf {

    
    public static void copy(String src, String dest) {

        try {

            FileOutputStream out = new FileOutputStream(dest);
            BufferedOutputStream bOut = new BufferedOutputStream(out);

            InputStream in = new FileInputStream(new File(src));

            // General header
            for (int i=0;i<4;i++) {
                out.write(in.read());
            }

            // uncompressed length
            for (int i=0;i<4;i++) {
                bOut.write(in.read());
            }

            BitInputStream bitIn = new BitInputStream(new InflaterInputStream(in));
            DataOutputStream dOut = new DataOutputStream(new DeflaterOutputStream(bOut));
            
            // skip length
            int lengthByte = bitIn.readByte();
            int length = lengthByte >> 3;
            
            int frameRectBytes = ((length * 4) - 3) / 8;
            if ((((length * 4) - 3) %8) > 0) frameRectBytes++;

            dOut.write(lengthByte);
            for (int i=0;i<frameRectBytes;i++) dOut.write(bitIn.readByte());


            // frame rate & frame count
            writeInt(dOut, bitIn.readUnsignedInt(2), 2);
            writeInt(dOut, bitIn.readUnsignedInt(2), 2);


            // Blocks
            int run = 0;
            while (run < 100000000) {

                int header = bitIn.readUnsignedInt(2);
                int tagType = (header & 0xFFC0) >> 6;
                int tagLengthBytes = header & 0x003F;

                writeInt(dOut, header, 2);

                // variable-length block, we get the actual length
                if (tagLengthBytes == 0x3F) {
                    tagLengthBytes = bitIn.readSignedInt(4);
                    writeInt(dOut, tagLengthBytes, 4);
                }

                System.out.println("Tag type: " + tagType);
                System.out.println("Tag length: " + tagLengthBytes);

                System.out.print("Data: ");
                for (int i=0;i<tagLengthBytes;i++) {
                    int b = bitIn.readUnsignedInt(1);
                    System.out.print(toHex(b) + " ");

                    writeInt(dOut, b, 1);
                }
                System.out.println();

                System.out.println();

                if (tagType == 0) break;
                run++;

                if (tagType == 82) {
                    writeTag82Of("Addition.swf", dOut);
                }
            }


            dOut.flush();
            bOut.flush();
            out.flush(); 
            
            dOut.close();
            bOut.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void writeInt(OutputStream out, int b, int lenBytes) throws IOException {
        for (int i=0;i<lenBytes;i++) {
            out.write(b & 0xFF);
            b = b >> 8;
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


    public static void writeTag82Of(String src, OutputStream out) {
        try {

            InputStream in = new FileInputStream(new File(src));

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

                // variable-length block, we get the actual length
                if (tagLengthBytes == 0x3F) {
                    tagLengthBytes = bitIn.readSignedInt(4);
                }

                if (tagType == 82) {
                    writeInt(out, header, 2);       
                    if (tagLengthBytes >= 0x3F) {
                        writeInt(out, tagLengthBytes, 4);
                    }
                }

                System.out.println("Tag type: " + tagType);
                System.out.println("Tag length: " + tagLengthBytes);

                System.out.print("Data: ");
                for (int i=0;i<tagLengthBytes;i++) {
                    int b = bitIn.readUnsignedInt(1);
                    System.out.print(toHex(b) + " ");

                    if (tagType == 82) writeInt(out, b, 1);
                }
                System.out.println();

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
}
