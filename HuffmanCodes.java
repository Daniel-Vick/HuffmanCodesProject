import java.util.*;
import java.lang.*;
import java.io.*;

public class HuffmanCodes {
  private Node rootNode;
  private PriorityQueue<Node> forest;
  private Map<Byte, Integer> byteMap;
  private Map<Byte, String> codeMap;
  private Map<String, Byte> decodeMap;
  private BitInputStream input;
  private BitOutputStream output;

  public void countFrequencies(byte[] data) {
    byteMap = new HashMap<Byte, Integer>();
    for (int i = 0; i < data.length; i++) {
      Integer count = byteMap.get(data[i]);
      if (count == null) {
        count = 0;
      }
      byteMap.put(data[i], count + 1);
    }
  }

  public void buildForest() {
    //System.out.println(byteMap.size());
    forest = new PriorityQueue<Node>(byteMap.size());
    for (Map.Entry<Byte, Integer> entry : byteMap.entrySet()) {
      ValueNode addNode = new ValueNode(entry.getKey(), entry.getValue());
      //System.out.println(entry.getKey() + " " + entry.getValue());
      forest.add(addNode);
    }
  }

  public void buildTree() {
    buildForest();
    //System.out.println(forest.size());
    while (forest.size() > 1) {
      //System.out.println("AddingNodeToTree");
      Node right = forest.remove();
      Node left = forest.remove();      
      if (right instanceof ValueNode) {
        ValueNode test = (ValueNode) right;
        //System.out.println(test.printNode());
      }
      if (left instanceof ValueNode) {
        ValueNode test = (ValueNode) left;
        //System.out.println(test.printNode());
      }
      DecisionNode nodeToAdd = new DecisionNode(left, right);
      nodeToAdd.setCount(left.getCount() + right.getCount());
      forest.add(nodeToAdd);
    }
    rootNode = forest.remove();
  }

  public Node getRoot() {
    return rootNode;
  }

  public void encode(String fileIn, String fileOut) throws IOException {
    String stringPrint = "";
    String messageSizeString = "";
    File file = new File(fileIn);
    File outFile = new File(fileOut);
    //BitInputStream stream = null;
    //BitOutputStream outputStream = null;
    try {
      this.input = new BitInputStream(file);
    } catch (FileNotFoundException e) {System.out.println("Error");}
    catch (IOException e) {System.out.println("Error");}
    countFrequencies(input.allBytes());
    buildTree();
    try {
      this.output = new BitOutputStream(outFile);
    } catch (FileNotFoundException e) {System.out.println("Error");}
    codeMap = rootNode.getAllCodes();
    byte[] byteArray = input.allBytes();
    output.writeInt(byteSize());
    int messageByteSize = byteSize();
    messageSizeString += Integer.toBinaryString(messageByteSize);
    String codeTreeString = writeTree(output, rootNode);
    //System.out.println(byteArray[5]);
    System.out.println(codeMap.size());
    for (int i = 0; i < byteArray.length; i++) {
      if (codeMap.containsKey(byteArray[i])) {
        String code = codeMap.get(byteArray[i]);
        for (int j = 0; j < code.length(); j ++) {
          if (code.substring(j, j+1).equals("1")) {
            try {
              stringPrint += "1";
              output.writeBit(1);
            } catch (IOException e) {
                System.out.println("Error");
              }
          } else {
            try {
              stringPrint += "0";
              output.writeBit(0);
            } catch (IOException e) {
                System.out.println("Error");
              }
          }
        }
      }  
    }
    input.close();
    output.close();

    System.out.println(messageSizeString + " " + codeTreeString + " " + stringPrint);
    printCodes();
    String testString = "0001010110010001011011111110010111101100";
    System.out.println(testString.equals(codeTreeString));
  }

  public void decode(String fileIn, String fileOut) throws IOException {
    System.out.println("Decoding");
    File in = new File(fileIn);
    File out = new File(fileOut);
    BitInputStream input = null;
    int bitsRead = 0;
    try {
      input = new BitInputStream(in);
    } catch (FileNotFoundException e) {System.out.println("Error");}
    catch (IOException e) {System.out.println("Error");}
    BitOutputStream output = null;
    try {
      output = new BitOutputStream(out);
    } catch (FileNotFoundException e) {System.out.println("Error");}
    byte[] byteArray = input.allBytes();
    int messageSize = input.readInt();
    System.out.println(messageSize);
    int fileSize = byteArray.length * 8;
    bitsRead += 32;
    System.out.println(bitsRead);
    try {
      if (input.readBit() == 0) {
        System.out.println("ReadBit() == 0");
        bitsRead++;
        System.out.println(bitsRead);
        DecisionNode tempNode = new DecisionNode();
        try {
          bitsRead += tempNode.remakeTree(input);
          System.out.println(bitsRead);
        } catch (IOException e) {System.out.println("Error");}
        rootNode = tempNode;
      } else {
        System.out.println("ReadBit() == 1");
        bitsRead += 9;
        System.out.println(bitsRead);
        rootNode = new ValueNode((byte)input.readByte());
      }
    } catch (IOException e) {System.out.println("Error");}
    decodeMap = reverseByteMap(rootNode.getAllCodes());
    System.out.println(bitsRead);
    String currByte = "";
    System.out.println("fileSize: " + fileSize);
    System.out.println(bitsRead);
    while (bitsRead < fileSize) {
      System.out.println(bitsRead);
      bitsRead++;
      if (input.readBit() == 0) {
        currByte += "0";
      } else {
        currByte += "1";
      }
      if (decodeMap.containsKey(currByte)) {
        output.writeByte(decodeMap.get(currByte));
        currByte = "";
      }
    }
    input.close();
    output.close();
  }
  
  public int byteSize() { 
    int bitCount = 0;
      for (Map.Entry<Byte, Integer> entry : this.byteMap.entrySet()) {
        bitCount += (entry.getValue() * this.codeMap.get(entry.getKey()).length());
      }
      return bitCount;
  }

  public static String writeTree(BitOutputStream outStream, Node root) {
    String stringToReturn = "";
    try {
      if (outStream.tally() == 32) {
      //outStream.writeBit(0);
      
      }
      if (root instanceof DecisionNode) {
        DecisionNode decNode = (DecisionNode)root;
        stringToReturn += "0";
        System.out.println("DecisionNode: adding: 0");
        outStream.writeBit(0);
        stringToReturn += writeTree(outStream, decNode.getLeft());
        stringToReturn += writeTree(outStream, decNode.getRight());
      } else {
        stringToReturn += "1";
        System.out.println("ValueNode: adding: 1");
        outStream.writeBit(1);
        ValueNode valNode = (ValueNode)root;
        outStream.writeByte(valNode.getValue());

      }





        /*if (decNode.getLeft() instanceof DecisionNode) {
          DecisionNode decNodeLeft = (DecisionNode)decNode.getLeft();
          stringToReturn += "0";
          System.out.println("DecisionNode: adding: 0");
          outStream.writeBit(0);
          stringToReturn += writeTree(outStream, decNodeLeft);
        } else {
          stringToReturn += "0";
          ValueNode decNodeLeft = (ValueNode)decNode.getLeft();
          System.out.println("ValueNode: adding: 0");
          outStream.writeBit(0);
          Integer byteValue = (int)decNodeLeft.getValue();
          stringToReturn += Integer.toBinaryString(byteValue);
          System.out.println("Adding byte: " + Integer.toBinaryString(byteValue));
          outStream.writeByte(decNodeLeft.getValue());
        }
        if (decNode.getRight() instanceof DecisionNode) {
          DecisionNode decNodeRight = (DecisionNode)decNode.getRight();
          stringToReturn += "1";
          System.out.println("DecisionNode: adding: 1");
          outStream.writeBit(1);
          stringToReturn += writeTree(outStream, decNodeRight);
        } else {
          stringToReturn += "1";
          ValueNode decNodeRight = (ValueNode)decNode.getRight();
          System.out.println("ValueNode: adding: 1");
          outStream.writeBit(1);
          Integer byteValue = (int)decNodeRight.getValue();
          stringToReturn += Integer.toBinaryString(byteValue);
          System.out.println("Adding byte: " + Integer.toBinaryString(byteValue));
          outStream.writeByte(decNodeRight.getValue());
        }*/
    } catch (IOException e) {System.out.println("Error");}
    return stringToReturn;
  }

  public void printCodes() {
    for (Map.Entry<Byte, String> entry: codeMap.entrySet()) {
      int intVal = entry.getKey();
      char character = (char)intVal;
      System.out.println(character + " >> " + entry.getValue());
    }
  } 

  public static Map<String, Byte> reverseByteMap(Map<Byte, String> inputMap) {
    Map<String, Byte> mapToReturn = new HashMap<String, Byte>();
    for (Map.Entry<Byte, String> entry : inputMap.entrySet()) {
      mapToReturn.put(entry.getValue(), entry.getKey());
    }
    return mapToReturn;
  }
  
  public abstract class Node implements Comparable {
    protected int count;

    public Node () {
      count = 0;
    }

    public Node (int input) {
      count = input;
    }

    public final Map<Byte, String> getAllCodes() {
      Map<Byte, String> codeTable = new HashMap<Byte, String>();
      this.putCodes(codeTable, "");
      return codeTable;
    }

    public abstract void putCodes(Map<Byte, String> table, String bits);

    @Override
    public int compareTo(Object node) {
      Node inputNode = (Node)node;
      if (node == null) {
        throw new NullPointerException();
      }
      if (!(this instanceof Node)) {
        throw new ClassCastException();
      }
      if (count > inputNode.getCount()) {
        return 1;
      } else if (count == inputNode.getCount()) {
        return 0;
      } else {
        return -1;
      }
    }

    public int getCount() {
      return count;
    }

    public void setCount(int input) {
      count = input;
    }
    //public int compareTo(Node that);
    //public final Map<Byte, String> getAllCodes();
    //protected abstract void putCodes(Map<Byte, String> table, String bits);
  }

  public class DecisionNode extends Node {
    Node left;
    Node right;

    public DecisionNode(Node leftInput, Node rightInput) {
      left = leftInput;
      right = rightInput;
    }

    public DecisionNode() {
      left = null;
      right = null;
    }

    public void putCodes(Map<Byte, String> table, String bits) {
      left.putCodes(table, bits + "0");
      right.putCodes(table, bits + "1");
    }

    public Node getRight() {
      return right;
    }

    public Node getLeft() {
      return left;
    }

    public void setLeft(Node input) {
      this.left = input;
    }

    public void setRight(Node input) {
      this.right = input;
    }

    public String printNode() {
      String stringToReturn = "";
      if (left == null) {
        stringToReturn += "LeftNull ";
      } else if (left instanceof ValueNode) {
        stringToReturn += "LeftValue";
      }
      if (right == null) {
        stringToReturn += "RightNull ";
      } else if (right instanceof ValueNode) {
        stringToReturn += "RightValue";
      }
      return stringToReturn;
    }

    public int remakeTree(BitInputStream input) throws IOException {
      /*if (input.readBit() == 0) {
        DecisionNode rootLeft = new DecisionNode();
        rootLeft.remakeTree(input);
      } else {
        ValueNode rootLeft = new ValueNode(input.readByte());
      }
      if (input.readBit() == 0) {
        DecisionNode rootRight = new DecisionNode();

      }*/

      int bitNum = 2;
      if (input.readBit() == 0) {
        DecisionNode leftDec = new DecisionNode();
        System.out.println("Adding DecisionNode");
        bitNum += leftDec.remakeTree(input);
        this.setLeft(leftDec);
      } else {
        System.out.println("Adding ValueNode");
        bitNum += 8;
        ValueNode leftVal = new ValueNode((byte)input.readByte());
        this.setLeft(leftVal);
      }
      if (input.readBit() == 0) {
        System.out.println("Adding DecisionNode");
        DecisionNode rightDec = new DecisionNode();
        bitNum += rightDec.remakeTree(input);
        this.setRight(rightDec);
      } else {
        System.out.println("Adding ValueNode");
        bitNum += 8;
        this.setRight(new ValueNode((byte)input.readByte()));
      }
      return bitNum;
    }

  }

  public class ValueNode extends Node {
    byte value;

    public ValueNode(byte input) {
      value = input;
    }

    public ValueNode(byte input, int count) {
      super(count);
      value = input;
    }

    public void putCodes(Map<Byte, String> table, String bits) {
      table.put(value, bits);
    }

    public byte getValue() {
      return value;
    }

    public void setValue(byte input) {
      value = input;
    }

    public String printNode() {
      String stringToReturn = "";
      stringToReturn += value + " " + super.getCount();
      return stringToReturn;
    }
  }

  public static void main(String[] args) {
    HuffmanCodes test = new HuffmanCodes();
    try {test.decode(args[0], args[1]);}
    catch (IOException e) {System.out.println("Error");}
    
    
    /*
    byte[] data = new byte[28];    //byte[] data = {(byte)00000000, (byte)00000000, (byte)00000000, (byte)11111111, (byte)11111111};
    int temp = 1;
    for (int i = 0; i < 1; i ++) {
      data[i] = (byte)temp;
    } temp++;
    for (int i = 1; i < 3; i ++) {
      data[i] = (byte)temp;
    } temp++;
    for (int i = 3; i < 6; i ++) {
      data[i] = (byte)temp;
    } temp++;
    for (int i = 6; i < 10; i ++) {
      data[i] = (byte)temp;
    } temp++;
    for (int i = 10; i < 15; i ++) {
      data[i] = (byte)temp;
    } temp++;
    for (int i = 15; i < 21; i ++) {
      data[i] = (byte)temp;
    } temp++;
    for (int i = 21; i < 28; i ++) {
      data[i] = (byte)temp;
    } temp++;


    HuffmanCodes test = new HuffmanCodes();
    test.countFrequencies(data);
    test.buildTree();
    DecisionNode node = (DecisionNode) test.getRoot();


    if (node == null) {
      System.out.println("Node is null");
    } else {
      System.out.println(node.printNode());
    }
    //System.out.println(node.geit
    ValueNode leftNode = (ValueNode)node.getLeft();
    System.out.println(leftNode.printNode());
    DecisionNode nodeTwo = (DecisionNode)node.getRight();
    ValueNode nodeThree = (ValueNode)nodeTwo.getLeft();
    ValueNode nodeFour = (ValueNode)nodeTwo.getRight();
    System.out.println(nodeThree.printNode());
    System.out.println(nodeFour.printNode());
    */
  }
}
