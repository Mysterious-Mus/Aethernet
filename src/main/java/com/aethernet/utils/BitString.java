package com.aethernet.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.FileInputStream;

public class BitString {
    public String filename;
    public ArrayList<Boolean> bitString;

    public BitString(String filename) {
        this.filename = filename;
        this.bitString = new ArrayList<Boolean>();

        String fileExtension = getFileExtension(filename);
        if (fileExtension != null) {
            switch (fileExtension) {
                case "txt":
                    handleTxtFile();
                    break;
                case "bin":
                    handleBinFile();
                    break;
                default:
                    System.err.println("Error: file extension not supported");          
            }
        } 
        else {
            System.err.println("Error: file extension not found");
        }
    }

    private void handleTxtFile() {
         // read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.filename));
            String line;
            while ((line = br.readLine()) != null) {
                for (int i = 0; i < line.length(); i++) { // each bit is a separate character
                    bitString.add(line.charAt(i) == '1' ? true : false);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleBinFile() {
         // read the file
        try (InputStream inputStream = new FileInputStream(this.filename)) {
            int data;
            while ((data = inputStream.read()) != -1) {
                String binaryString = String.format("%8s", Integer.toBinaryString(data)).replace(' ', '0');

                for (int i = 0; i < binaryString.length(); i++) {
                    bitString.add(binaryString.charAt(i) == '1' ? true : false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(String filePath) {
        if (filePath == null) {
            return null;
        }
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }
    

    public ArrayList<Boolean> getBitString() {
        return bitString;
    }
}
