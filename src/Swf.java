import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: tanin
 * Date: 4/3/12
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class Swf {
    
    private static HashMap<Integer, String> semanticBlock = new HashMap<Integer, String>();

    static {
        semanticBlock.put(4, "PlaceObject");
        semanticBlock.put(26, "PlaceObject2");
        semanticBlock.put(70, "PlaceObject3");
        semanticBlock.put(5, "RemoveObject");
        semanticBlock.put(28, "RemoveObject2");
        semanticBlock.put(1, "ShowFrame");

        semanticBlock.put(9, "SetBackgroundColor");
        semanticBlock.put(43, "FrameLabel or NameAnchor");
        semanticBlock.put(24, "Protect");
        semanticBlock.put(0, "End");
        semanticBlock.put(56, "ExportAssets");
        semanticBlock.put(57, "ImportAssets");
        semanticBlock.put(58, "EnableDebugger");
        semanticBlock.put(64, "EnableDebugger2");
        semanticBlock.put(65, "ScriptLimits");
        semanticBlock.put(66, "SetTabIndex");
        semanticBlock.put(69, "FileAttributes");
        semanticBlock.put(71, "ImportAssets2");
        semanticBlock.put(76, "SymbolClass");
        semanticBlock.put(77, "Metadata");
        semanticBlock.put(78, "DefineScalingGrid");
        semanticBlock.put(86, "DefineSceneAndFrameLabelData");

        semanticBlock.put(12, "DoAction");
        semanticBlock.put(59, "DoInitAction");
        semanticBlock.put(82, "DoABC");

        semanticBlock.put(2, "DefineShape");
        semanticBlock.put(22, "DefineShape2");
        semanticBlock.put(32, "DefineShape3");
        semanticBlock.put(83, "DefineShape4");
        semanticBlock.put(6, "DefineBits");
        semanticBlock.put(8, "JPEGTables");

        semanticBlock.put(21, "DefineBitsJPEG2");
        semanticBlock.put(35, "DefineBitsJPEG3");
        semanticBlock.put(20, "DefineBitsLossless");
        semanticBlock.put(36, "DefineBitsLossless2");
        semanticBlock.put(90, "DefineBitsJPEG4");
        semanticBlock.put(46, "DefineMorphShape");
        semanticBlock.put(84, "DefineMorphShape2");
        semanticBlock.put(10, "DefineFont");
        semanticBlock.put(13, "DefineFontInfo");
        semanticBlock.put(62, "DefineFontInfo2");
        semanticBlock.put(48, "DefineFont2");
        semanticBlock.put(75, "DefineFont3");
        semanticBlock.put(73, "DefineFontAlignZones");
        semanticBlock.put(88, "DefineFontName");
        semanticBlock.put(11, "DefineText");
        semanticBlock.put(33, "DefineText2");
        semanticBlock.put(37, "DefineEditText");
        semanticBlock.put(74, "CSMTextSettings");
        semanticBlock.put(91, "DefineFont4");
        semanticBlock.put(14, "DefineSound");
        semanticBlock.put(15, "StartSound");
        semanticBlock.put(89, "StartSound2");
        semanticBlock.put(18, "SoundStreamHead");
        semanticBlock.put(45, "SoundStreamHead2");
        semanticBlock.put(19, "SoundStreamBlock");
        semanticBlock.put(7, "DefineButton");
        semanticBlock.put(34, "DefineButton2");
        semanticBlock.put(23, "DefineButtonCxform");
        semanticBlock.put(17, "DefineButtonSound");
        semanticBlock.put(39, "DefineSprite");
        semanticBlock.put(60, "DefineVideoStream");
        semanticBlock.put(61, "VideoFrame");
        semanticBlock.put(87, "DefineBinaryData");
        semanticBlock.put(2, "DefineShape");
        semanticBlock.put(8, "JPEGTables");
        semanticBlock.put(8, "JPEGTables");
        semanticBlock.put(8, "JPEGTables");
        semanticBlock.put(8, "JPEGTables");
        semanticBlock.put(8, "JPEGTables");
        semanticBlock.put(8, "JPEGTables");


    }
    
    public static int[] readHeader(InputStream in) throws IOException {
        int[] header = new int[4];
        for (int i=0;i<4;i++) {
            header[i] = in.read();
        }

        return header;
    }

    public static int readLength(InputStream in) throws IOException {

        int fileLength = 0;
        int multi = 1;
        for (int i=0;i<4;i++) {
            fileLength += multi * in.read();
            multi *= 16 * 16;
        }

        return fileLength;
    }
    
    public static int[] readFrameRect(BitInputStream in) throws IOException {
        int coordinateBitLength = in.readBit(5);

        int minX = in.readBit(coordinateBitLength);
        int maxX = in.readBit(coordinateBitLength);
        int minY = in.readBit(coordinateBitLength);
        int maxY = in.readBit(coordinateBitLength);
        
        in.padding();
        
        return new int[] {minX, maxX, minY, maxY};
    }

    public static int readFrameRate(BitInputStream in) throws IOException {
        return in.readUnsignedInt(2);
    }

    public static int readFrameCount(BitInputStream in) throws IOException {
        return in.readUnsignedInt(2);
    }


    public static byte[] getBlock(int wantedTagType, String src) throws Exception {

        FileInputStream in = new FileInputStream(new File(src));
        readHeader(in);
        readLength(in);

        BitInputStream bitIn = new BitInputStream(new InflaterInputStream(in));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        readFrameRect(bitIn);
        readFrameRate(bitIn);
        readFrameCount(bitIn);


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

            if (tagType == wantedTagType) {
                writeInt(out, header, 2);
                if (tagLengthBytes >= 0x3F) writeInt(out, tagLengthBytes, 4);

                for (int i=0;i<tagLengthBytes;i++) {
                    writeInt(out, bitIn.readUnsignedInt(1), 1);
                }
            }
            else
            {
                for (int i=0;i<tagLengthBytes;i++) {
                    bitIn.readUnsignedInt(1);
                }
            }

            if (tagType == 0) break;
            run++;
        }


        return out.toByteArray();
    }

    
    public static String convertToString(int[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<b.length;i++) {
            sb.append(toHex(b[i]) + " ");
        }

        return sb.toString();
    }


    private String srcFile;

    public Swf(String src) {

        try {
            this.srcFile = src;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printStructure() {

        try {
            FileInputStream in = new FileInputStream(new File(this.srcFile));

            System.out.println("File's name: " + this.srcFile);
            System.out.println("Header: " + convertToString(readHeader(in)));
            System.out.println("File's length: " + readLength(in));
    
            BitInputStream bitIn = new BitInputStream(new InflaterInputStream(in));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
    
            int[] rect = readFrameRect(bitIn);
            System.out.println("Rect: (" + rect[0] + ", " + rect[2] + ") -> (" + rect[1] + ", " + rect[3] + ")");
            System.out.println("Frame's Rate: " + readFrameRate(bitIn));
            System.out.println("Frame's Count: " + readFrameCount(bitIn));

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

                String blockMeaning = (String)(semanticBlock.get(tagType));
                if (blockMeaning == null) blockMeaning = "Unknown";

                System.out.println("Tag type: " + blockMeaning + " (" + tagType + ")");
                System.out.println("Tag length: " + tagLengthBytes);

                System.out.print("Data: ");
                for (int i=0;i<tagLengthBytes;i++) {
                    System.out.print(toHex(bitIn.readUnsignedInt(1)) + " ");
                }
                System.out.println();

                System.out.println();

                if (tagType == 0) break;
                run++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void inject(String dest, int afterTagType, byte[] injectBytes) {

        try {
            FileOutputStream out = new FileOutputStream(dest);
            FileInputStream in = new FileInputStream(new File(this.srcFile));

            {
                int[] header = readHeader(in);
                for (int i=0;i<header.length;i++) out.write(header[i]);
            }

            int fileLength = readLength(in);
            writeInt(out, fileLength + injectBytes.length, 4);
            out.flush();


            BitInputStream bitIn = new BitInputStream(new InflaterInputStream(in));
            DeflaterOutputStream zipOut = new DeflaterOutputStream(out);

            int frameRectLengthIndicatorByte = bitIn.readByte();
            int frameRectLengthInBit = (frameRectLengthIndicatorByte >> 3);

            int frameRectLengthInByte = (frameRectLengthInBit * 4 - 3) / 8;
            if (((frameRectLengthInBit * 4 - 3) % 8) > 0) frameRectLengthInByte++;

            zipOut.write(frameRectLengthIndicatorByte);
            for (int i=0;i<frameRectLengthInByte;i++) zipOut.write(bitIn.readByte());

            writeInt(zipOut, readFrameRate(bitIn), 2);
            writeInt(zipOut, readFrameCount(bitIn), 2);

            // first tag
            int run = 0;
            while (run < 100000000) {

                int header = bitIn.readUnsignedInt(2);
                int tagType = (header & 0xFFC0) >> 6;
                int tagLengthBytes = header & 0x003F;

                if (tagType == afterTagType) {
                    zipOut.write(injectBytes);
                }

                writeInt(zipOut, header, 2);

                // variable-length block, we get the actual length
                if (tagLengthBytes == 0x3F) {
                    tagLengthBytes = bitIn.readSignedInt(4);
                    writeInt(zipOut, tagLengthBytes, 4);
                }

                for (int i=0;i<tagLengthBytes;i++) {
                    writeInt(zipOut, bitIn.readUnsignedInt(1), 1);
                }

                if (tagType == 0) break;
                run++;


            }

            zipOut.flush();
            out.flush();

            zipOut.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void writeInt(OutputStream out, int b, int lengthInBytes) throws IOException {
        for (int i=0;i<lengthInBytes;i++) {
            out.write(b & 0xFF);
            b = b >> 8;
        }
    }

    public void writeFileLength(int lengthInBytes) {

    }

    public static String toHex(int b) {
        String hex = Integer.toHexString(0xFF & (byte)b);
        if (hex.length() == 1) {
            // could use a for loop, but we're only dealing with a single byte
            hex = "0" + hex;
        }
        return hex;
    }

}
