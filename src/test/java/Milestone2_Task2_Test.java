import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.XML;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Milestone2_Task2_Test {
  /*  public static void main(String[] args) throws FileNotFoundException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        JSONObject obj = XML.toJSONObject(br);

       // System.out.println(obj.toString(2));
    }*/

    @Test
    public void testTask2() throws FileNotFoundException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        JSONPointer pointer = new JSONPointer("/catalog");
        Object obj = XML.toJSONObject(br,pointer);
         System.out.println("Final "+obj.toString());
    }
//test diff xml // diff json paths
    //stuff you tested
    @Test
    public void testTask5() throws FileNotFoundException {
        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        JSONPointer pointer = new JSONPointer("/catalog/book/0");
        Object obj = XML.toJSONObject(br,pointer, newObject);
        System.out.println("Final "+obj.toString());

    }
}
