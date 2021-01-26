import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.XML;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
/*
Test helper class for Milestone2
 */
public class Milestone2_Test {
    @Test
    public void testTask2() throws FileNotFoundException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        JSONPointer pointer = new JSONPointer("/catalog/book/2");
        Object obj = XML.toJSONObject(br,pointer);
         System.out.println("Final "+obj.toString());
    }

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
        JSONPointer pointer = new JSONPointer("/catalog/book/2/title");
        JSONObject obj = XML.toJSONObject(br,pointer, newObject);
        System.out.println("Final "+obj.toString(2));

    }
}
