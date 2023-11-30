package com.aethernet.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.aethernet.Config.ConfigTerm;

public class FileOp {
    
    public static class Configs {
        public static ConfigTerm<String> INPUT_DIR = 
            new ConfigTerm<String>("INPUT_DIR", "input/" , false);
            
        public static ConfigTerm<String> OUTPUT_DIR = 
                new ConfigTerm<String>("OUTPUT_DIR", "output/" , false);
    }

    public static void outputFloatSeq(double[] Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(Configs.OUTPUT_DIR.v() + fileName);
            for (int i = 0; i < Data.length; i++) {
                writer.append(Double.toString(Data[i]));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputFloatSeq(float[] Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(Configs.OUTPUT_DIR.v() + fileName);
            for (int i = 0; i < Data.length; i++) {
                writer.append(Float.toString(Data[i]));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputDoubleArray(ArrayList<Double> Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(Configs.OUTPUT_DIR.v() + fileName);
            for (Double d: Data) {
                writer.append(Double.toString(d));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputFloatSeq(ArrayList<Float> Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(Configs.OUTPUT_DIR.v() + fileName);
            for (Float d: Data) {
                writer.append(Float.toString(d));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputBitString(ArrayList<Boolean> Data, String fileName) {
        try {
            FileWriter writer = new FileWriter(Configs.OUTPUT_DIR.v() + fileName);
            for (Boolean d: Data) {
                writer.append(d? "1": "0");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * append the byte array to the .bin file  
     * @param Data : Byte[]
     * @param fileName : String
     * @param clearfile : whether to clear the file before writing
     */
    public static void outputBin(byte[] Data, String fileName, boolean clearfile) {
        try {
            // mkd if not exist
            Path outputPath = Paths.get(Configs.OUTPUT_DIR.v());
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            FileOutputStream writer = new FileOutputStream(Configs.OUTPUT_DIR.v() + fileName,!clearfile);
            for (Byte d: Data) {
                writer.write(d);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Boolean> inputFileRead(String fileName) {

        String fileExtension = getFileExtension(fileName);
        if (fileExtension != null) {
            switch (fileExtension) {
                case "txt":
                    return inputTxt(fileName);
                case "bin":
                    return inputBin(fileName);
                default:
                    System.err.println("Error: file extension not supported");          
                    return null;
            }
        } 
        else {
            System.err.println("Error: file extension not found");
            return null;
        }
    }
    
    /** 
     * Read the .bin file and return the bit string
     * @param fileName : String
     * @return ArrayList<Boolean> : bit string
     */
    private static ArrayList<Boolean> inputBin(String fileName) {
        ArrayList<Boolean> bitString = new ArrayList<Boolean>();
        // read the file
        try (InputStream inputStream = new FileInputStream(Configs.INPUT_DIR.v() + fileName)) {
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
        return bitString;
    }
    
    /** 
     * Read the .txt file and return the bit string
     * @param fileName : String
     * @return ArrayList<Boolean> : bit string
     */
    private static ArrayList<Boolean> inputTxt(String fileName) {
        ArrayList<Boolean> bitString = new ArrayList<Boolean>();
        // read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(Configs.INPUT_DIR.v() + fileName));
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
        return bitString;
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
    
}
