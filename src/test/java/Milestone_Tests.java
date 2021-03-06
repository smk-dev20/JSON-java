import org.json.*;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
Test helper class for  261P Milestones - prints results to console
 */
public class Milestone_Tests {
    @Test
    public void testMileStone2Task2() throws FileNotFoundException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        JSONPointer pointer = new JSONPointer("/catalog/book/0");
        Object obj = XML.toJSONObject(br,pointer);
        System.out.println("Final MileStone2 task 2 "+obj.toString());
    }

    @Test
    public void testMileStone2Task5() throws FileNotFoundException {
        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        JSONPointer pointer = new JSONPointer("/catalog/book/0");
        JSONObject obj = XML.toJSONObject(br,pointer, newObject);
        System.out.println("Final MileStone2 task 5 "+obj.toString(2));

    }

    @Test
    public void testMileStone3() throws FileNotFoundException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        Function<String, String> func = x -> "swe_262P_"+x;

        JSONObject obj = XML.toJSONObject(br,func);
       System.out.println("Final MileStone3 "+obj.toString(2));
    }

    @Test
    public void testMilestone4() throws FileNotFoundException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        JSONObject obj = XML.toJSONObject(br);
        System.out.println("Milestone 4 output");
        obj.toStream().forEach(System.out::println);
    }

    @Test
    public void testAsyncToJSONObject() throws FileNotFoundException, InterruptedException, ExecutionException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        Future<JSONObject> futureObject	= XML.toJSONObjectAsync(br);

        while(!futureObject.isDone()) {
            System.out.println("I'm waiting");
            Thread.sleep(10);
            System.out.println("Still waiting");
        }
        JSONObject obj = futureObject.get();

        System.out.println("Final Milestone5  async output "+obj.toString(2));
    }

    @Test
    public void testAsyncToJSONKeyTransform() throws FileNotFoundException, InterruptedException, ExecutionException {
        File file = new File("C:\\Users\\Sherlin\\Desktop\\UCI\\Winter_2021\\262P_Programming_Styles\\JSON-java\\src\\test\\java\\org\\json\\junit\\xmls\\small1.xml");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        Function<String, String> func = x -> "swe_262P_"+x;
        Future<JSONObject> futureObject	= XML.toJSONObjectAsync(br,func);

        while(!futureObject.isDone()) {
            System.out.println("I'm waiting on key transform");
            Thread.sleep(10);
            System.out.println("Still waiting");
        }
        JSONObject obj = futureObject.get();

        System.out.println("Final Milestone5  async keytransform output "+obj.toString(2));
    }
}
