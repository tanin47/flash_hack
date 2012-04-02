import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: tanin
 * Date: 4/2/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class BitInputStream {

    private BufferedInputStream in = null;
    private int bitLeft = 0;
    private int currentByte;

    public BitInputStream(InputStream in) {
        if (in instanceof  BufferedInputStream) {
            this.in = (BufferedInputStream)in;
        } else {
            this.in = new BufferedInputStream(in);
        }
    }

    public int readBit(int len) throws IOException {

        int result = 0;

        for (long i=0;i<len;i++) {

            if (bitLeft == 0) {
                currentByte = this.in.read();
                bitLeft = 8;
            }

            int bit = currentByte &  128;

            currentByte = currentByte << 1;
            bitLeft--;

            if (bit > 0) bit = 1;

            result = result << 1;
            result = result | bit;
        }

        return result;
    }
    
    public void padding() throws IOException {
        //while (bitLeft-- > 0) this.in.read();
        bitLeft = 0;
    }
    
    public int readUnsignedInt(int numBytes) throws IOException {
        this.padding();

        int result = 0;
        for (int i=0;i<numBytes;i++) {
            result = result | (this.in.read() << (8*i));
        }

        return result;
    }

    public int readSignedInt(int numBytes) throws IOException {
        return readUnsignedInt(numBytes);
    }

}
