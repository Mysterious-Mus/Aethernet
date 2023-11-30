package com.aethernet.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.aethernet.Config;

public class ECC {
  private boolean[][] generatorMatrix;
  private int[] generatorMatrix_bit;
  private int symbolLength;
  private int constraintLength;

  public static boolean[][] ECCMat = {
      { true, true, true, true, false, false, true },
      { true, false, true, true, false, true, false },
  };

  public ECC() {
    /**
     * Convolutional code constructor
     *  g: generator matrix
     *  L: constraint length
     */
    this.generatorMatrix = ECCMat;
    this.symbolLength = ECCMat.length;
    this.constraintLength = ECCMat[0].length;
    // convert boolen array to int 
    this.generatorMatrix_bit = new int[this.symbolLength];
    for (int i = 0; i < this.symbolLength; i++) {
      boolean[] row = this.generatorMatrix[i];
      this.generatorMatrix_bit[i] = 0;
      for (int j = row.length - 1; j >= 0; --j) {
        this.generatorMatrix_bit[i] = (generatorMatrix_bit[i] << 1) + (row[j] ? 1 : 0);
      }
    }
  }
  

  public boolean[] ConvolutionEncode(boolean[] input) {
    /**
     * Convolutional encode
     *  input: input bit sequence
     *  output: output bit sequence
     */
    int numOutput = input.length  * this.symbolLength ;
    boolean[] output = new boolean[numOutput];
    
    // Initialize register to all zeros
    boolean[] register = new boolean[this.constraintLength];
    
    for (int bit_pos = 0; bit_pos < input.length ; ++bit_pos) {
      // Shift register
      for (int i = this.constraintLength - 1; i > 0; i--) {
        register[i] = register[i - 1];
      }
      if (bit_pos < input.length) {
        boolean bit = input[bit_pos];
        register[0] = bit;
      }
      else
        register[0] = false;
      // Compute output
      for (int i = 0; i < this.symbolLength; i++) {
        boolean[] row = this.generatorMatrix[i];
        for (int j = 0; j < this.constraintLength; j++) {
          output[bit_pos * this.symbolLength + i] ^= (row[j] && register[j]);
        }
      }
    }
 
    return output;
  }

  private boolean[] computeByGenerator_bit(int state) {
    boolean[] state_bit = new boolean[this.constraintLength];
    boolean[] output = new boolean[this.symbolLength];
    for (int i = 0; i < this.constraintLength; ++i) {
      state_bit[i] = (state & 1) == 1;
      state >>= 1;
    }
    // Compute output
    for (int i = 0; i < this.symbolLength; i++) {
      boolean[] row = this.generatorMatrix[i];
      for (int j = 0; j < this.constraintLength; j++) {
        output[i] ^= (row[j] && state_bit[j]);
      }
    }
    return output;
  }
  
  private int computeDistance(boolean[] a, boolean[] b) {
    int distance = 0;
    for (int i = 0; i < a.length; ++i) {
      distance += (a[i] ^ b[i]) ? 1 : 0;
    }
    return distance;
  }
  
  public boolean[] viterbiDecode(boolean[] input) {
    /**
      * Viterbi decode
      *  input: input bit sequence
      *  output: output bit sequence
      */

    List<boolean[]> receivedCodewords = new ArrayList<>();
    for (int i = 0; i < input.length; i += this.symbolLength) {
      receivedCodewords.add(Arrays.copyOfRange(input, i, Math.min(i + this.symbolLength, input.length)));
    }

    // Dynamic Programming pathMetric[old/new] [states]
    int[][] pathMetric = new int[2][1 << (this.constraintLength - 1)];
    // Initial state Space 
    HashSet<Integer> stateSpaceOld = new HashSet<>();
    stateSpaceOld.add(0);
    HashSet<Integer> stateSpaceNext = new HashSet<>();
    stateSpaceNext.add(0);
    stateSpaceNext.add(1);
    // Initial pathrecord
    int[][] pre = new int[receivedCodewords.size()][1 << (constraintLength - 1)];

    int res = 0;
    for (int t = 0; t < receivedCodewords.size(); t++) {
      res ^= 1;
      HashSet<Integer> stateSpaceTemp = new HashSet<>();
      for (Integer state : stateSpaceNext) {

        // previous state 
        int state1 = state >> 1;
        int state2 = (state >> 1) + (1 << (constraintLength - 2));

        // Compute two path metric
        int f1 = Integer.MAX_VALUE;
        int f2 = Integer.MAX_VALUE;
        if (stateSpaceOld.contains(state1)) {
          f1 = pathMetric[res ^ 1][state1] + computeDistance(
              receivedCodewords.get(t), computeByGenerator_bit(state));
        }
        if (stateSpaceOld.contains(state2)) {
          f2 = pathMetric[res ^ 1][state2] + computeDistance(
              receivedCodewords.get(t), computeByGenerator_bit(state + (1 << (constraintLength - 1))));
        }
        // choose the mininal one
        if (f1 < f2) {
          pathMetric[res][state] = f1;  
          pre[t][state] = state1;
        } else {
          pathMetric[res][state] = f2;
          pre[t][state] = state2;
        }

        // add new states to state space
        if (t == receivedCodewords.size() - 1)
          continue;

        stateSpaceTemp.add(state << 1 & ((1 << (constraintLength - 1)) - 1));
        stateSpaceTemp.add(((state << 1) + 1) & ((1 << (constraintLength - 1)) - 1));
      }
      stateSpaceOld = stateSpaceNext;
      stateSpaceNext = stateSpaceTemp;
    }
    // choose the minimal path metric
    Integer minMetric = Integer.MAX_VALUE;
    Integer lastState = 0;
    for (Integer state : stateSpaceOld) {
      if (pathMetric[res][state] < minMetric) {
        minMetric = pathMetric[res][state];
        lastState = state;
      }
    }
   
    
    // Trace back
    boolean[] output = new boolean[receivedCodewords.size()];
    for (int t = receivedCodewords.size() - 1; t >= 0; t--) {
      output[t] = (lastState & 1) == 1;
      lastState = pre[t][lastState];
    }
    return output;
  }

  public void PrintBoolenArray(boolean[] array) {
    for (int i = 0; i < array.length; i++) {
      System.out.print(array[i] ? 1 : 0);
    }
    System.out.println();
  }
  
  public static void main(String[] args) {

    boolean[][] matrix = {
        { true, true, true, true, false, false, true },
        { true, false, true, true, false, true, false },
    };
    
    int arraySize = 100;
    boolean[] input = new boolean[arraySize];
    Random random = new Random();

    for (int i = 0; i < arraySize; i++) {
        input[i] = random.nextBoolean();
    }
    ECC ECC = new ECC();

   
    //ECC.PrintBoolenArray(input);
    boolean[] output = ECC.ConvolutionEncode(input);

    boolean[] output_fliped = output.clone();
    for (int i = 0; i < 2; i++) {
      int randomIndex = random.nextInt(arraySize); // randomIndex ∈ [0, arraySize)
      output_fliped[randomIndex] = !output[randomIndex]; // filp the bit
    }
    double errorRate = (double)ECC.computeDistance(output, output_fliped) / (double)arraySize;  
    System.out.println("The transmission error rate " + errorRate);
    output_fliped = ECC.viterbiDecode(output_fliped);
    //ECC.PrintBoolenArray(output);
    errorRate = (double)ECC.computeDistance(input, output_fliped) / (double)arraySize;
    System.out.println("The error rate " + errorRate);
  } 
}
