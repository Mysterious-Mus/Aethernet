package com.aethernet.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.print.DocFlavor.BYTE_ARRAY;

import com.aethernet.config.L2Config.ConfigTerm;
import com.aethernet.mac.MacFrame.Configs.HeaderFields;
import com.aethernet.physical.transmit.OFDM;
import com.aethernet.utils.CRC8;
import com.aethernet.utils.TypeConvertion;

import java.io.ByteArrayOutputStream;


/*
 * IEEE 802.3 based Ethernet Frame 
 * Destination Address: 1 bytes
 * Source Address: 1 bytes
 * Type: 1 bytes
 * Data: 100 bytes
 * Frame Check Sequence: 4 bytes
 */

public class MacFrame {

    public static class Configs {
        public static int addrLb = 0;
        public static int addrUb = 255;

        public static enum HeaderFields {
            DEST_ADDR,
            SRC_ADDR,
            TYPE,
            /**
             * Payload length in bytes
             */
            Len,
            SEQUENCE_NUM, // to prevent the loss of ACK
            CRC8,
            COUNT;

            public static int count() {
                return COUNT.ordinal();
            }
        }

        public static ConfigTerm<Integer> payloadMaxNumBytes = 
            new ConfigTerm<Integer>("payloadMaxNumBytes", 150, false)
        {
            @Override
            public boolean newValCheck(Integer value) {
                // print how many bytes are padded
                int symbolNbyte = OFDM.Configs.symbolCapacity.v() / 8;
                System.out.println("Padding: " + 
                    ((symbolNbyte - ((value + HeaderFields.COUNT.ordinal() + 4) % symbolNbyte)))
                    %symbolNbyte + " bytes");
                return true;
            }
        };

        public static enum Types {
            DATA((byte) 0x00),
            ACK((byte) 0xFF);

            private byte value;

            private Types(byte value) {
                this.value = value;
            }

            public byte getValue() {
                return value;
            }

            public static Types fromByte(byte b) {
                for (Types type : Types.values()) {
                    if (type.getValue() == b) {
                        return type;
                    }
                }
                // print error
                System.out.println("Error: type not found");
                return null;
            }
        }
    }

    public static class Header {

        private byte[] contents = new byte[Configs.HeaderFields.COUNT.ordinal()];

        public Header() {
        }

        // copy ctor
        public Header(Header header) {
            this.contents = Arrays.copyOf(header.contents, header.contents.length);
        }

        public Header(byte[] headerBuffer) {
            // sanity: length check
            if (headerBuffer.length != Configs.HeaderFields.COUNT.ordinal()) {
                System.out.println("Error: header length not match");
                return;
            }
            this.contents = headerBuffer;
        }

        public void SetField(Configs.HeaderFields field, Byte value) {
            contents[field.ordinal()] = value;
        }

        public byte[] stream() {
            // calc CRC8
            SetField(Configs.HeaderFields.CRC8, 
                CRC8.compute(Arrays.copyOfRange(contents, 0, Configs.HeaderFields.COUNT.ordinal() - 1)));
            return contents;
        }

        public boolean check() {
            return CRC8.compute(Arrays.copyOfRange(contents, 0, Configs.HeaderFields.COUNT.ordinal() - 1)) 
                == contents[Configs.HeaderFields.CRC8.ordinal()];
        }

        public static int getNbit() {
            return Configs.HeaderFields.COUNT.ordinal() * 8;
        }

        public byte getField(Configs.HeaderFields field) {
            return contents[field.ordinal()];
        }

        public Configs.Types getType() {
            return Configs.Types.fromByte(getField(Configs.HeaderFields.TYPE));
        }
    }

    private byte[] wholeContents; // everything including header/crc
    private Header header;
    public Header getHeader() {
        return header;
    }

    /**
     * Create the frame to be sent
     * structure: header + data + CRC32
     * @param header
     * @param data
     */
    public MacFrame (Header header, byte[] data) {
        this.header = new Header(header);
        ByteArrayOutputStream stream = new ByteArrayOutputStream( );
        try {
            stream.write(header.stream());
            CRC32 crc = new CRC32();
            crc.update(data);
            stream.write(data);
            stream.write(TypeConvertion.Long2ByteArray(crc.getValue()));
            
            this.wholeContents = stream.toByteArray();
        } 
        catch (Exception e) {
            System.out.println("Create Frame Error: " + e);
        }
    }

    /** 
     * Reconstruct the MacFrame from the byte array received
     * Won't check CRC
     * @param array: byte array
     */
    public MacFrame (byte[] frameBuffer) {
        // sanity: length check
        if (frameBuffer.length > getMaxFrameBitLen() / 8) {
            System.out.println("Error: Exceeding max frame length");
            return;
        }
        this.wholeContents = frameBuffer;
        this.header = new Header(Arrays.copyOfRange(frameBuffer, 0, Configs.HeaderFields.COUNT.ordinal()));
    }

    /** 
     * Reconstruct the MacFrame from the byte array received
     * Won't check CRC
     * @param array: byte array
     */
    public MacFrame (ArrayList<Boolean> frameBuffer) {
        this(TypeConvertion.booleanList2ByteArray(frameBuffer));
    }

    /**
     * The max number of bits contained in a frame
     */
    public static int getMaxFrameBitLen() {
        return Configs.HeaderFields.count() * 8 + Configs.payloadMaxNumBytes.v() * 8 + 32;
    }

    /**
     * The number of bits contained in a frame
     */
    public static int getFrameBitLen(Header header) {
        int payloadNBytes = header.getField(HeaderFields.Len) & 0xFF;
        return Configs.HeaderFields.count() * 8 + payloadNBytes * 8 + 32;
    }
    
    /**
     * check CRC, also check if the length of payload is correct(will report this one)
     */
    public boolean verify() {
        if (!this.header.check()) return false;
        int lenField = this.header.getField(HeaderFields.Len) & 0xFF;
        if (lenField != this.wholeContents.length - 4 - HeaderFields.COUNT.ordinal())
        {
            System.out.println("Payload length incorrect");
            return false;
        }
        // CRC (32 bits)
        CRC32 crc = new CRC32();
        crc.update(Arrays.copyOfRange(this.wholeContents, Configs.HeaderFields.COUNT.ordinal(), this.wholeContents.length - 4));
        return crc.getValue() == TypeConvertion.byteArray2Long(
            Arrays.copyOfRange(this.wholeContents, this.wholeContents.length - 4, this.wholeContents.length));
    }

    public byte[] getWhole() {
        return wholeContents;
    }

    public byte[] getData() {
        return Arrays.copyOfRange(this.wholeContents, Configs.HeaderFields.COUNT.ordinal(), this.wholeContents.length - 4);
    }
}
