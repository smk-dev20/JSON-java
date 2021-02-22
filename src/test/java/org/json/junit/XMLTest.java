package org.json.junit;

/*
Copyright (c) 2020 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.*;


/**
 * Tests for JSON-Java XML.java
 * Note: noSpace() will be tested by JSONMLTest
 */
public class XMLTest {
    /**
     * JUnit supports temporary files and folders that are cleaned up after the test.
     * https://garygregory.wordpress.com/2010/01/20/junit-tip-use-rules-to-manage-temporary-files-and-folders/ 
     */
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * JSONObject from a null XML string.
     * Expects a NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void shouldHandleNullXML() {
        String xmlStr = null;
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("jsonObject should be empty", jsonObject.isEmpty());
    }

    /**
     * Empty JSONObject from an empty XML string.
     */
    @Test
    public void shouldHandleEmptyXML() {

        String xmlStr = "";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("jsonObject should be empty", jsonObject.isEmpty());
    }

    /**
     * Empty JSONObject from a non-XML string.
     */
    @Test
    public void shouldHandleNonXML() {
        String xmlStr = "{ \"this is\": \"not xml\"}";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("xml string should be empty", jsonObject.isEmpty());
    }

    /**
     * Invalid XML string (tag contains a frontslash).
     * Expects a JSONException
     */
    @Test
    public void shouldHandleInvalidSlashInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/x>\n"+
            "       <street>abc street</street>\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Misshaped tag at 176 [character 14 line 4]",
                    e.getMessage());
        }
    }

    /**
     * Invalid XML string ('!' char in tag)
     * Expects a JSONException
     */
    @Test
    public void shouldHandleInvalidBangInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <!>\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Misshaped meta tag at 214 [character 12 line 7]",
                    e.getMessage());
        }
    }

    /**
     * Invalid XML string ('!' char and no closing tag brace)
     * Expects a JSONException
     */
    @Test
    public void shouldHandleInvalidBangNoCloseInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <!\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Misshaped meta tag at 213 [character 12 line 7]",
                    e.getMessage());
        }
    }

    /**
     * Invalid XML string (no end brace for tag)
     * Expects JSONException
     */
    @Test
    public void shouldHandleNoCloseStartTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <abc\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Misplaced '<' at 193 [character 4 line 6]",
                    e.getMessage());
        }
    }

    /**
     * Invalid XML string (partial CDATA chars in tag name)
     * Expects JSONException
     */
    @Test
    public void shouldHandleInvalidCDATABangInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name>Joe Tester</name>\n"+
            "       <![[]>\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Expected 'CDATA[' at 204 [character 11 line 5]",
                    e.getMessage());
        }
    }

    /**
     * Null JSONObject in XML.toString()
     */
    @Test
    public void shouldHandleNullJSONXML() {
        JSONObject jsonObject= null;
        String actualXml=XML.toString(jsonObject);
        assertEquals("generated XML does not equal expected XML","\"null\"",actualXml);
    }

    /**
     * Empty JSONObject in XML.toString()
     */
    @Test
    public void shouldHandleEmptyJSONXML() {
        JSONObject jsonObject= new JSONObject();
        String xmlStr = XML.toString(jsonObject);
        assertTrue("xml string should be empty", xmlStr.isEmpty());
    }

    /**
     * No SML start tag. The ending tag ends up being treated as content.
     */
    @Test
    public void shouldHandleNoStartTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <nocontent/>>\n"+
            "   </address>\n"+
            "</addresses>";
        String expectedStr = 
            "{\"addresses\":{\"address\":{\"name\":\"\",\"nocontent\":\"\",\""+
            "content\":\">\"},\"xsi:noNamespaceSchemaLocation\":\"test.xsd\",\""+
            "xmlns:xsi\":\"http://www.w3.org/2001/XMLSchema-instance\"}}";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Valid XML to JSONObject
     */
    @Test
    public void shouldHandleSimpleXML() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "   <address>\n"+
            "       <name>Joe Tester</name>\n"+
            "       <street>[CDATA[Baker street 5]</street>\n"+
            "       <NothingHere/>\n"+
            "       <TrueValue>true</TrueValue>\n"+
            "       <FalseValue>false</FalseValue>\n"+
            "       <NullValue>null</NullValue>\n"+
            "       <PositiveValue>42</PositiveValue>\n"+
            "       <NegativeValue>-23</NegativeValue>\n"+
            "       <DoubleValue>-23.45</DoubleValue>\n"+
            "       <Nan>-23x.45</Nan>\n"+
            "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
            "   </address>\n"+
            "</addresses>";

        String expectedStr = 
            "{\"addresses\":{\"address\":{\"street\":\"[CDATA[Baker street 5]\","+
            "\"name\":\"Joe Tester\",\"NothingHere\":\"\",TrueValue:true,\n"+
            "\"FalseValue\":false,\"NullValue\":null,\"PositiveValue\":42,\n"+
            "\"NegativeValue\":-23,\"DoubleValue\":-23.45,\"Nan\":-23x.45,\n"+
            "\"ArrayOfNum\":\"1, 2, 3, 4.1, 5.2\"\n"+
            "},\"xsi:noNamespaceSchemaLocation\":"+
            "\"test.xsd\",\"xmlns:xsi\":\"http://www.w3.org/2001/"+
            "XMLSchema-instance\"}}";

        compareStringToJSONObject(xmlStr, expectedStr);
        compareReaderToJSONObject(xmlStr, expectedStr);
        compareFileToJSONObject(xmlStr, expectedStr);
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    public void testXmlEscapeToJson(){
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<root>"+
            "<rawQuote>\"</rawQuote>"+
            "<euro>A &#8364;33</euro>"+
            "<euroX>A &#x20ac;22&#x20AC;</euroX>"+
            "<unknown>some text &copy;</unknown>"+
            "<known>&#x0022; &quot; &amp; &apos; &lt; &gt;</known>"+
            "<high>&#x1D122; &#x10165;</high>" +
            "</root>";
        String expectedStr = 
            "{\"root\":{" +
            "\"rawQuote\":\"\\\"\"," +
            "\"euro\":\"A €33\"," +
            "\"euroX\":\"A €22€\"," +
            "\"unknown\":\"some text &copy;\"," +
            "\"known\":\"\\\" \\\" & ' < >\"," +
            "\"high\":\"𝄢 𐅥\""+
            "}}";
        
        compareStringToJSONObject(xmlStr, expectedStr);
        compareReaderToJSONObject(xmlStr, expectedStr);
        compareFileToJSONObject(xmlStr, expectedStr);
    }
    
    /**
     * Tests that control characters are escaped.
     */
    @Test
    public void testJsonToXmlEscape(){
        final String jsonSrc = "{\"amount\":\"10,00 €\","
                + "\"description\":\"Ação Válida\u0085\","
                + "\"xmlEntities\":\"\\\" ' & < >\""
                + "}";
        JSONObject json = new JSONObject(jsonSrc);
        String xml = XML.toString(json);
        //test control character not existing
        assertFalse("Escaping \u0085 failed. Found in XML output.", xml.contains("\u0085"));
        assertTrue("Escaping \u0085 failed. Entity not found in XML output.", xml.contains("&#x85;"));
        // test normal unicode existing
        assertTrue("Escaping € failed. Not found in XML output.", xml.contains("€"));
        assertTrue("Escaping ç failed. Not found in XML output.", xml.contains("ç"));
        assertTrue("Escaping ã failed. Not found in XML output.", xml.contains("ã"));
        assertTrue("Escaping á failed. Not found in XML output.", xml.contains("á"));
        // test XML Entities converted
        assertTrue("Escaping \" failed. Not found in XML output.", xml.contains("&quot;"));
        assertTrue("Escaping ' failed. Not found in XML output.", xml.contains("&apos;"));
        assertTrue("Escaping & failed. Not found in XML output.", xml.contains("&amp;"));
        assertTrue("Escaping < failed. Not found in XML output.", xml.contains("&lt;"));
        assertTrue("Escaping > failed. Not found in XML output.", xml.contains("&gt;"));
    }

    /**
     * Valid XML with comments to JSONObject
     */
    @Test
    public void shouldHandleCommentsInXML() {

        String xmlStr = 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                "<!-- this is a comment -->\n"+
                "<addresses>\n"+
                "   <address>\n"+
                "       <![CDATA[ this is -- <another> comment ]]>\n"+
                "       <name>Joe Tester</name>\n"+
                "       <!-- this is a - multi line \n"+
                "            comment -->\n"+
                "       <street>Baker street 5</street>\n"+
                "   </address>\n"+
                "</addresses>";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        String expectedStr = "{\"addresses\":{\"address\":{\"street\":\"Baker "+
                "street 5\",\"name\":\"Joe Tester\",\"content\":\" this is -- "+
                "<another> comment \"}}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Valid XML to XML.toString()
     */
    @Test
    public void shouldHandleToString() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "   <address>\n"+
            "       <name>[CDATA[Joe &amp; T &gt; e &lt; s &quot; t &apos; er]]</name>\n"+
            "       <street>Baker street 5</street>\n"+
            "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
            "   </address>\n"+
            "</addresses>";

        String expectedStr = 
                "{\"addresses\":{\"address\":{\"street\":\"Baker street 5\","+
                "\"name\":\"[CDATA[Joe & T > e < s \\\" t \\\' er]]\","+
                "\"ArrayOfNum\":\"1, 2, 3, 4.1, 5.2\"\n"+
                "},\"xsi:noNamespaceSchemaLocation\":"+
                "\"test.xsd\",\"xmlns:xsi\":\"http://www.w3.org/2001/"+
                "XMLSchema-instance\"}}";
        
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        String xmlToStr = XML.toString(jsonObject);
        JSONObject finalJsonObject = XML.toJSONObject(xmlToStr);
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
        Util.compareActualVsExpectedJsonObjects(finalJsonObject,expectedJsonObject);
    }

    /**
     * Converting a JSON doc containing '>' content to JSONObject, then
     * XML.toString() should result in valid XML.
     */
    @Test
    public void shouldHandleContentNoArraytoString() {
        String expectedStr = "{\"addresses\":{\"content\":\">\"}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        String finalStr = XML.toString(expectedJsonObject);
        String expectedFinalStr = "<addresses>&gt;</addresses>";
        assertEquals("Should handle expectedFinal: ["+expectedStr+"] final: ["+
                finalStr+"]", expectedFinalStr, finalStr);
    }

    /**
     * Converting a JSON doc containing a 'content' array to JSONObject, then
     * XML.toString() should result in valid XML.
     * TODO: This is probably an error in how the 'content' keyword is used.
     */
    @Test
    public void shouldHandleContentArraytoString() {
        String expectedStr = 
            "{\"addresses\":{" +
            "\"content\":[1, 2, 3]}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        String finalStr = XML.toString(expectedJsonObject);
        String expectedFinalStr = "<addresses>"+
                "1\n2\n3</addresses>";
        assertEquals("Should handle expectedFinal: ["+expectedStr+"] final: ["+
                finalStr+"]", expectedFinalStr, finalStr);
    }

    /**
     * Converting a JSON doc containing a named array to JSONObject, then
     * XML.toString() should result in valid XML.
     */
    @Test
    public void shouldHandleArraytoString() {
        String expectedStr = 
            "{\"addresses\":{"+
            "\"something\":[1, 2, 3]}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        String finalStr = XML.toString(expectedJsonObject);
        String expectedFinalStr = "<addresses>"+
                "<something>1</something><something>2</something><something>3</something>"+
                "</addresses>";
        assertEquals("Should handle expectedFinal: ["+expectedStr+"] final: ["+
                finalStr+"]", expectedFinalStr, finalStr);
    }
    
    /**
     * Tests that the XML output for empty arrays is consistent.
     */
    @Test
    public void shouldHandleEmptyArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("array",new Object[]{});
        final JSONObject jo2 = new JSONObject();
        jo2.put("array",new JSONArray());

        final String expected = "<jo></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals("Expected an empty root tag", expected, output1);
        String output2 = XML.toString(jo2,"jo");
        assertEquals("Expected an empty root tag", expected, output2);
    }
    
    /**
     * Tests that the XML output for arrays is consistent when an internal array is empty.
     */
    @Test
    public void shouldHandleEmptyMultiArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("arr",new Object[]{"One", new String[]{}, "Four"});
        final JSONObject jo2 = new JSONObject();
        jo2.put("arr",new JSONArray(new Object[]{"One", new JSONArray(new String[]{}), "Four"}));

        final String expected = "<jo><arr>One</arr><arr></arr><arr>Four</arr></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals("Expected a matching array", expected, output1);
        String output2 = XML.toString(jo2,"jo");
        assertEquals("Expected a matching array", expected, output2);
    }
   
    /**
     * Tests that the XML output for arrays is consistent when arrays are not empty.
     */
    @Test
    public void shouldHandleNonEmptyArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("arr",new String[]{"One", "Two", "Three"});
        final JSONObject jo2 = new JSONObject();
        jo2.put("arr",new JSONArray(new String[]{"One", "Two", "Three"}));

        final String expected = "<jo><arr>One</arr><arr>Two</arr><arr>Three</arr></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals("Expected a non empty root tag", expected, output1);
        String output2 = XML.toString(jo2,"jo");
        assertEquals("Expected a non empty root tag", expected, output2);
    }

    /**
     * Tests that the XML output for arrays is consistent when arrays are not empty and contain internal arrays.
     */
    @Test
    public void shouldHandleMultiArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("arr",new Object[]{"One", new String[]{"Two", "Three"}, "Four"});
        final JSONObject jo2 = new JSONObject();
        jo2.put("arr",new JSONArray(new Object[]{"One", new JSONArray(new String[]{"Two", "Three"}), "Four"}));

        final String expected = "<jo><arr>One</arr><arr><array>Two</array><array>Three</array></arr><arr>Four</arr></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals("Expected a matching array", expected, output1);
        String output2 = XML.toString(jo2,"jo");
        assertEquals("Expected a matching array", expected, output2);
    }

    /**
     * Converting a JSON doc containing a named array of nested arrays to
     * JSONObject, then XML.toString() should result in valid XML.
     */
    @Test
    public void shouldHandleNestedArraytoString() {
        String xmlStr = 
            "{\"addresses\":{\"address\":{\"name\":\"\",\"nocontent\":\"\","+
            "\"outer\":[[1], [2], [3]]},\"xsi:noNamespaceSchemaLocation\":\"test.xsd\",\""+
            "xmlns:xsi\":\"http://www.w3.org/2001/XMLSchema-instance\"}}";
        JSONObject jsonObject = new JSONObject(xmlStr);
        String finalStr = XML.toString(jsonObject);
        JSONObject finalJsonObject = XML.toJSONObject(finalStr);
        String expectedStr = "<addresses><address><name/><nocontent/>"+
                "<outer><array>1</array></outer><outer><array>2</array>"+
                "</outer><outer><array>3</array></outer>"+
                "</address><xsi:noNamespaceSchemaLocation>test.xsd</xsi:noName"+
                "spaceSchemaLocation><xmlns:xsi>http://www.w3.org/2001/XMLSche"+
                "ma-instance</xmlns:xsi></addresses>";
        JSONObject expectedJsonObject = XML.toJSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(finalJsonObject,expectedJsonObject);
    }


    /**
     * Possible bug: 
     * Illegal node-names must be converted to legal XML-node-names.
     * The given example shows 2 nodes which are valid for JSON, but not for XML.
     * Therefore illegal arguments should be converted to e.g. an underscore (_).
     */
    @Test
    public void shouldHandleIllegalJSONNodeNames()
    {
        JSONObject inputJSON = new JSONObject();
        inputJSON.append("123IllegalNode", "someValue1");
        inputJSON.append("Illegal@node", "someValue2");

        String result = XML.toString(inputJSON);

        /*
         * This is invalid XML. Names should not begin with digits or contain
         * certain values, including '@'. One possible solution is to replace
         * illegal chars with '_', in which case the expected output would be:
         * <___IllegalNode>someValue1</___IllegalNode><Illegal_node>someValue2</Illegal_node>
         */
        String expected = "<123IllegalNode>someValue1</123IllegalNode><Illegal@node>someValue2</Illegal@node>";

        assertEquals("length",expected.length(), result.length());
        assertTrue("123IllegalNode",result.contains("<123IllegalNode>someValue1</123IllegalNode>"));
        assertTrue("Illegal@node",result.contains("<Illegal@node>someValue2</Illegal@node>"));
    }

    /**
     * JSONObject with NULL value, to XML.toString()
     */
    @Test
    public void shouldHandleNullNodeValue()
    {
        JSONObject inputJSON = new JSONObject();
        inputJSON.put("nullValue", JSONObject.NULL);
        // This is a possible preferred result
        // String expectedXML = "<nullValue/>";
        /**
         * This is the current behavior. JSONObject.NULL is emitted as 
         * the string, "null".
         */
        String actualXML = "<nullValue>null</nullValue>";
        String resultXML = XML.toString(inputJSON);
        assertEquals(actualXML, resultXML);
    }

    /**
     * Investigate exactly how the "content" keyword works
     */
    @Test
    public void contentOperations() {
        /*
         * When a standalone <!CDATA[...]] structure is found while parsing XML into a
         * JSONObject, the contents are placed in a string value with key="content".
         */
        String xmlStr = "<tag1></tag1><![CDATA[if (a < b && a > 0) then return]]><tag2></tag2>";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("1. 3 items", 3 == jsonObject.length());
        assertTrue("1. empty tag1", "".equals(jsonObject.get("tag1")));
        assertTrue("1. empty tag2", "".equals(jsonObject.get("tag2")));
        assertTrue("1. content found", "if (a < b && a > 0) then return".equals(jsonObject.get("content")));

        // multiple consecutive standalone cdatas are accumulated into an array
        xmlStr = "<tag1></tag1><![CDATA[if (a < b && a > 0) then return]]><tag2></tag2><![CDATA[here is another cdata]]>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("2. 3 items", 3 == jsonObject.length());
        assertTrue("2. empty tag1", "".equals(jsonObject.get("tag1")));
        assertTrue("2. empty tag2", "".equals(jsonObject.get("tag2")));
        assertTrue("2. content array found", jsonObject.get("content") instanceof JSONArray);
        JSONArray jsonArray = jsonObject.getJSONArray("content");
        assertTrue("2. array size", jsonArray.length() == 2);
        assertTrue("2. content array entry 0", "if (a < b && a > 0) then return".equals(jsonArray.get(0)));
        assertTrue("2. content array entry 1", "here is another cdata".equals(jsonArray.get(1)));

        /*
         * text content is accumulated in a "content" inside a local JSONObject.
         * If there is only one instance, it is saved in the context (a different JSONObject 
         * from the calling code. and the content element is discarded. 
         */
        xmlStr =  "<tag1>value 1</tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("3. 2 items", 1 == jsonObject.length());
        assertTrue("3. value tag1", "value 1".equals(jsonObject.get("tag1")));

        /*
         * array-style text content (multiple tags with the same name) is 
         * accumulated in a local JSONObject with key="content" and value=JSONArray,
         * saved in the context, and then the local JSONObject is discarded.
         */
        xmlStr =  "<tag1>value 1</tag1><tag1>2</tag1><tag1>true</tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("4. 1 item", 1 == jsonObject.length());
        assertTrue("4. content array found", jsonObject.get("tag1") instanceof JSONArray);
        jsonArray = jsonObject.getJSONArray("tag1");
        assertTrue("4. array size", jsonArray.length() == 3);
        assertTrue("4. content array entry 0", "value 1".equals(jsonArray.get(0)));
        assertTrue("4. content array entry 1", jsonArray.getInt(1) == 2);
        assertTrue("4. content array entry 2", jsonArray.getBoolean(2) == true);

        /*
         * Complex content is accumulated in a "content" field. For example, an element
         * may contain a mix of child elements and text. Each text segment is 
         * accumulated to content. 
         */
        xmlStr =  "<tag1>val1<tag2/>val2</tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("5. 1 item", 1 == jsonObject.length());
        assertTrue("5. jsonObject found", jsonObject.get("tag1") instanceof JSONObject);
        jsonObject = jsonObject.getJSONObject("tag1");
        assertTrue("5. 2 contained items", 2 == jsonObject.length());
        assertTrue("5. contained tag", "".equals(jsonObject.get("tag2")));
        assertTrue("5. contained content jsonArray found", jsonObject.get("content") instanceof JSONArray);
        jsonArray = jsonObject.getJSONArray("content");
        assertTrue("5. array size", jsonArray.length() == 2);
        assertTrue("5. content array entry 0", "val1".equals(jsonArray.get(0)));
        assertTrue("5. content array entry 1", "val2".equals(jsonArray.get(1)));

        /*
         * If there is only 1 complex text content, then it is accumulated in a 
         * "content" field as a string.
         */
        xmlStr =  "<tag1>val1<tag2/></tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("6. 1 item", 1 == jsonObject.length());
        assertTrue("6. jsonObject found", jsonObject.get("tag1") instanceof JSONObject);
        jsonObject = jsonObject.getJSONObject("tag1");
        assertTrue("6. contained content found", "val1".equals(jsonObject.get("content")));
        assertTrue("6. contained tag2", "".equals(jsonObject.get("tag2")));

        /*
         * In this corner case, the content sibling happens to have key=content
         * We end up with an array within an array, and no content element.
         * This is probably a bug. 
         */
        xmlStr =  "<tag1>val1<content/></tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertTrue("7. 1 item", 1 == jsonObject.length());
        assertTrue("7. jsonArray found", jsonObject.get("tag1") instanceof JSONArray);
        jsonArray = jsonObject.getJSONArray("tag1");
        assertTrue("array size 1", jsonArray.length() == 1);
        assertTrue("7. contained array found", jsonArray.get(0) instanceof JSONArray);
        jsonArray = jsonArray.getJSONArray(0);
        assertTrue("7. inner array size 2", jsonArray.length() == 2);
        assertTrue("7. inner array item 0", "val1".equals(jsonArray.get(0)));
        assertTrue("7. inner array item 1", "".equals(jsonArray.get(1)));

        /*
         * Confirm behavior of original issue
         */
        String jsonStr = 
                "{"+
                    "\"Profile\": {"+
                        "\"list\": {"+
                            "\"history\": {"+
                                "\"entries\": ["+
                                    "{"+
                                        "\"deviceId\": \"id\","+
                                        "\"content\": {"+
                                            "\"material\": ["+
                                                "{"+
                                                    "\"stuff\": false"+
                                                "}"+
                                            "]"+
                                        "}"+
                                    "}"+
                                "]"+
                            "}"+
                        "}"+
                    "}"+
                "}";
        jsonObject = new JSONObject(jsonStr);
        xmlStr = XML.toString(jsonObject);
        /*
         * This is the created XML. Looks like content was mistaken for
         * complex (child node + text) XML. 
         *  <Profile>
         *      <list>
         *          <history>
         *              <entries>
         *                  <deviceId>id</deviceId>
         *                  {&quot;material&quot;:[{&quot;stuff&quot;:false}]}
         *              </entries>
         *          </history>
         *      </list>
         *  </Profile>
         */
        assertTrue("nothing to test here, see comment on created XML, above", true);
    }

    /**
     * Convenience method, given an input string and expected result,
     * convert to JSONObject and compare actual to expected result.
     * @param xmlStr the string to parse
     * @param expectedStr the expected JSON string
     */
    private void compareStringToJSONObject(String xmlStr, String expectedStr) {
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Convenience method, given an input string and expected result,
     * convert to JSONObject via reader and compare actual to expected result.
     * @param xmlStr the string to parse
     * @param expectedStr the expected JSON string
     */
    private void compareReaderToJSONObject(String xmlStr, String expectedStr) {
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Reader reader = new StringReader(xmlStr);
        JSONObject jsonObject = XML.toJSONObject(reader);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Convenience method, given an input string and expected result, convert to
     * JSONObject via file and compare actual to expected result.
     * 
     * @param xmlStr
     *            the string to parse
     * @param expectedStr
     *            the expected JSON string
     * @throws IOException
     */

    private void compareFileToJSONObject(String xmlStr, String expectedStr) {
        try {
            JSONObject expectedJsonObject = new JSONObject(expectedStr);
            File tempFile = this.testFolder.newFile("fileToJSONObject.xml");
            FileWriter fileWriter = new FileWriter(tempFile);
            try {
                fileWriter.write(xmlStr);
            } finally {
                fileWriter.close();
            }

            Reader reader = new FileReader(tempFile);
            try {
                JSONObject jsonObject = XML.toJSONObject(reader);
                Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            fail("Error: " +e.getMessage());
        }
    }

    /**
     * JSON string lost leading zero and converted "True" to true.
     */
    @Test
    public void testToJSONArray_jsonOutput() {
        final String originalXml = "<root><id>01</id><id>1</id><id>00</id><id>0</id><item id=\"01\"/><title>True</title></root>";
        final JSONObject expectedJson = new JSONObject("{\"root\":{\"item\":{\"id\":\"01\"},\"id\":[\"01\",1,\"00\",0],\"title\":true}}");
        final JSONObject actualJsonOutput = XML.toJSONObject(originalXml, false);

        Util.compareActualVsExpectedJsonObjects(actualJsonOutput,expectedJson);
    }

    /**
     * JSON string cannot be reverted to original xml.
     */
    @Test
    public void testToJSONArray_reversibility() {
        final String originalXml = "<root><id>01</id><id>1</id><id>00</id><id>0</id><item id=\"01\"/><title>True</title></root>";
        final String revertedXml = XML.toString(XML.toJSONObject(originalXml, false));

        assertNotEquals(revertedXml, originalXml);
    }

    /**
     * test passes when using the new method toJsonArray.
     */
    @Test
    public void testToJsonXML() {
        final String originalXml = "<root><id>01</id><id>1</id><id>00</id><id>0</id><item id=\"01\"/><title>True</title></root>";
        final JSONObject expected = new JSONObject("{\"root\":{\"item\":{\"id\":\"01\"},\"id\":[\"01\",\"1\",\"00\",\"0\"],\"title\":\"True\"}}");

        final JSONObject actual = XML.toJSONObject(originalXml,true);
        
        Util.compareActualVsExpectedJsonObjects(actual, expected);
        
        final String reverseXml = XML.toString(actual);
        // this reversal isn't exactly the same. use JSONML for an exact reversal
        // the order of the elements may be differnet as well.
        final String expectedReverseXml = "<root><item><id>01</id></item><id>01</id><id>1</id><id>00</id><id>0</id><title>True</title></root>";

        assertEquals("length",expectedReverseXml.length(), reverseXml.length());
        assertTrue("array contents", reverseXml.contains("<id>01</id><id>1</id><id>00</id><id>0</id>"));
        assertTrue("item contents", reverseXml.contains("<item><id>01</id></item>"));
        assertTrue("title contents", reverseXml.contains("<title>True</title>"));
    }
    
    /**
     * test to validate certain conditions of XML unescaping.
     */
    @Test
    public void testUnescape() {
        assertEquals("{\"xml\":\"Can cope <;\"}",
                XML.toJSONObject("<xml>Can cope &lt;; </xml>").toString());
        assertEquals("Can cope <; ", XML.unescape("Can cope &lt;; "));

        assertEquals("{\"xml\":\"Can cope & ;\"}",
                XML.toJSONObject("<xml>Can cope &amp; ; </xml>").toString());
        assertEquals("Can cope & ; ", XML.unescape("Can cope &amp; ; "));

        assertEquals("{\"xml\":\"Can cope &;\"}",
                XML.toJSONObject("<xml>Can cope &amp;; </xml>").toString());
        assertEquals("Can cope &; ", XML.unescape("Can cope &amp;; "));

        // unicode entity
        assertEquals("{\"xml\":\"Can cope 4;\"}",
                XML.toJSONObject("<xml>Can cope &#x34;; </xml>").toString());
        assertEquals("Can cope 4; ", XML.unescape("Can cope &#x34;; "));

        // double escaped
        assertEquals("{\"xml\":\"Can cope &lt;\"}",
                XML.toJSONObject("<xml>Can cope &amp;lt; </xml>").toString());
        assertEquals("Can cope &lt; ", XML.unescape("Can cope &amp;lt; "));
        
        assertEquals("{\"xml\":\"Can cope &#x34;\"}",
                XML.toJSONObject("<xml>Can cope &amp;#x34; </xml>").toString());
        assertEquals("Can cope &#x34; ", XML.unescape("Can cope &amp;#x34; "));

   }

    /**
     * test passes when xsi:nil="true" converting to null (JSON specification-like nil conversion enabled)
     */
    @Test
    public void testToJsonWithNullWhenNilConversionEnabled() {
        final String originalXml = "<root><id xsi:nil=\"true\"/></root>";
        final String expectedJsonString = "{\"root\":{\"id\":null}}";

        final JSONObject json = XML.toJSONObject(originalXml,
                new XMLParserConfiguration()
                    .withKeepStrings(false)
                    .withcDataTagName("content")
                    .withConvertNilAttributeToNull(true));
        assertEquals(expectedJsonString, json.toString());
    }

    /**
     * test passes when xsi:nil="true" not converting to null (JSON specification-like nil conversion disabled)
     */
    @Test
    public void testToJsonWithNullWhenNilConversionDisabled() {
        final String originalXml = "<root><id xsi:nil=\"true\"/></root>";
        final String expectedJsonString = "{\"root\":{\"id\":{\"xsi:nil\":true}}}";

        final JSONObject json = XML.toJSONObject(originalXml, new XMLParserConfiguration());
        assertEquals(expectedJsonString, json.toString());
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    public void testIssue537CaseSensitiveHexEscapeMinimal(){
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<root>Neutrophils.Hypersegmented &#X7C; Bld-Ser-Plas</root>";
        String expectedStr = 
            "{\"root\":\"Neutrophils.Hypersegmented | Bld-Ser-Plas\"}";
        JSONObject xmlJSONObj = XML.toJSONObject(xmlStr, true);
        JSONObject expected = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(xmlJSONObj, expected);
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    public void testIssue537CaseSensitiveHexEscapeFullFile(){
        try {
            InputStream xmlStream = null;
            try {
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObject(xmlReader, true);
                InputStream jsonStream = null;
                try {
                    jsonStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.json");
                    final JSONObject expected = new JSONObject(new JSONTokener(jsonStream));
                    Util.compareActualVsExpectedJsonObjects(actual,expected);
                } finally {
                    if (jsonStream != null) {
                        jsonStream.close();
                    }
                }
            } finally {
                if (xmlStream != null) {
                    xmlStream.close();
                }
            }
        } catch (IOException e) {
            fail("file writer error: " +e.getMessage());
        }
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    public void testIssue537CaseSensitiveHexUnEscapeDirect(){
        String origStr = 
            "Neutrophils.Hypersegmented &#X7C; Bld-Ser-Plas";
        String expectedStr = 
            "Neutrophils.Hypersegmented | Bld-Ser-Plas";
        String actualStr = XML.unescape(origStr);
        
        assertEquals("Case insensitive Entity unescape",  expectedStr, actualStr);
    }

    /**
     * test passes when xsi:type="java.lang.String" not converting to string
     */
    @Test
    public void testToJsonWithTypeWhenTypeConversionDisabled() {
        String originalXml = "<root><id xsi:type=\"string\">1234</id></root>";
        String expectedJsonString = "{\"root\":{\"id\":{\"xsi:type\":\"string\",\"content\":1234}}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration());
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    /**
     * test passes when xsi:type="java.lang.String" converting to String
     */
    @Test
    public void testToJsonWithTypeWhenTypeConversionEnabled() {
        String originalXml = "<root><id1 xsi:type=\"string\">1234</id1>"
                + "<id2 xsi:type=\"integer\">1234</id2></root>";
        String expectedJsonString = "{\"root\":{\"id2\":1234,\"id1\":\"1234\"}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        xsiTypeMap.put("integer", new XMLXsiTypeConverter<Integer>() {
            @Override public Integer convert(final String value) {
                return Integer.valueOf(value);
            }
        });
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap));
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJsonWithXSITypeWhenTypeConversionEnabled() {
        String originalXml = "<root><asString xsi:type=\"string\">12345</asString><asInt "
                + "xsi:type=\"integer\">54321</asInt></root>";
        String expectedJsonString = "{\"root\":{\"asString\":\"12345\",\"asInt\":54321}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        xsiTypeMap.put("integer", new XMLXsiTypeConverter<Integer>() {
            @Override public Integer convert(final String value) {
                return Integer.valueOf(value);
            }
        });
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap));
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJsonWithXSITypeWhenTypeConversionNotEnabledOnOne() {
        String originalXml = "<root><asString xsi:type=\"string\">12345</asString><asInt>54321</asInt></root>";
        String expectedJsonString = "{\"root\":{\"asString\":\"12345\",\"asInt\":54321}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap));
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testXSITypeMapNotModifiable() {
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        XMLParserConfiguration config = new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap);
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        assertEquals("Config Conversion Map size is expected to be 0", 0, config.getXsiTypeMap().size());

        try {
            config.getXsiTypeMap().put("boolean", new XMLXsiTypeConverter<Boolean>() {
                @Override public Boolean convert(final String value) {
                    return Boolean.valueOf(value);
                }
            });
            fail("Expected to be unable to modify the config");
        } catch (Exception ignored) { }
    }
//Added tests for Milestone2
    @Test
    public void testToJSONObjectGetSubObjectSimple(){
        String xmlStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                        "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
                        "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
                        "   <address>\n"+
                        "       <name>Joe Tester</name>\n"+
                        "       <street>[CDATA[Baker street 5]</street>\n"+
                        "       <NothingHere/>\n"+
                        "       <TrueValue>true</TrueValue>\n"+
                        "       <FalseValue>false</FalseValue>\n"+
                        "       <NullValue>null</NullValue>\n"+
                        "       <PositiveValue>42</PositiveValue>\n"+
                        "       <NegativeValue>-23</NegativeValue>\n"+
                        "       <DoubleValue>-23.45</DoubleValue>\n"+
                        "       <Nan>-23x.45</Nan>\n"+
                        "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
                        "   </address>\n"+
                        "</addresses>";
        Reader reader = new StringReader(xmlStr);
        JSONPointer pointer = new JSONPointer("/addresses/address");

        String expectedJsonString = "{\"ArrayOfNum\":\"1, 2, 3, 4.1, 5.2\",\"TrueValue\":true,\"DoubleValue\":-23.45,\"street\":\"[CDATA[Baker street 5]\",\"NegativeValue\":-23,\"name\":\"Joe Tester\",\"NothingHere\":\"\",\"Nan\":\"-23x.45\",\"PositiveValue\":42,\"FalseValue\":false}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
       JSONObject actualJson = XML.toJSONObject(reader,pointer);
      Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectGetSubObjectNested(){
        InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
        Reader xmlReader = new InputStreamReader(xmlStream);
        JSONPointer pointer = new JSONPointer("/clinical_study/sponsors/lead_sponsor");
        String expectedJsonString = "{\"agency\": \"NYU Langone Health\", \"agency_class\": \"Other\"}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(xmlReader,pointer);
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectGetSubObjectAtIndex(){
        String xmlStr =
                "<?xml version=\"1.0\"?>\n" +
                        "<catalog>\n" +
                        "   <book id=\"bk101\">\n" +
                        "      <author>Gambardella, Matthew</author>\n" +
                        "      <title>XML Developer's Guide</title>\n" +
                        "      <genre>Computer</genre>\n" +
                        "      <price>44.95</price>\n" +
                        "      <publish_date>2000-10-01</publish_date>\n" +
                        "      <description>An in-depth look at creating applications \n" +
                        "      with XML.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk102\">\n" +
                        "      <author>Ralls, Kim</author>\n" +
                        "      <title>Midnight Rain</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2000-12-16</publish_date>\n" +
                        "      <description>A former architect battles corporate zombies, \n" +
                        "      an evil sorceress, and her own childhood to become queen \n" +
                        "      of the world.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk103\">\n" +
                        "      <author>Corets, Eva</author>\n" +
                        "      <title>Maeve Ascendant</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2000-11-17</publish_date>\n" +
                        "      <description>After the collapse of a nanotechnology \n" +
                        "      society in England, the young survivors lay the \n" +
                        "      foundation for a new society.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk104\">\n" +
                        "      <author>Corets, Eva</author>\n" +
                        "      <title>Oberon's Legacy</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2001-03-10</publish_date>\n" +
                        "      <description>In post-apocalypse England, the mysterious \n" +
                        "      agent known only as Oberon helps to create a new life \n" +
                        "      for the inhabitants of London. Sequel to Maeve \n" +
                        "      Ascendant.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk105\">\n" +
                        "      <author>Corets, Eva</author>\n" +
                        "      <title>The Sundered Grail</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2001-09-10</publish_date>\n" +
                        "      <description>The two daughters of Maeve, half-sisters, \n" +
                        "      battle one another for control of England. Sequel to \n" +
                        "      Oberon's Legacy.</description>\n" +
                        "   </book>\n"+
                        "</catalog>";
        JSONPointer pointer = new JSONPointer("/catalog/book/2");
        Reader reader = new StringReader(xmlStr);
        String expectedJsonString = "{  \"author\": \"Corets, Eva\",\n" +
                "  \"price\": 5.95,\n" +
                "  \"genre\": \"Fantasy\",\n" +
                "  \"description\": \"After the collapse of a nanotechnology \\n      society in England, the young survivors lay the \\n      foundation for a new society.\",\n" +
                "  \"id\": \"bk103\",\n" +
                "  \"title\": \"Maeve Ascendant\",\n" +
                "  \"publish_date\": \"2000-11-17\"}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(reader,pointer);
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectReplaceSimple(){
        String xmlStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                        "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
                        "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
                        "   <address>\n"+
                        "       <name>Joe Tester</name>\n"+
                        "       <street>[CDATA[Baker street 5]</street>\n"+
                        "       <NothingHere/>\n"+
                        "       <TrueValue>true</TrueValue>\n"+
                        "       <FalseValue>false</FalseValue>\n"+
                        "       <NullValue>null</NullValue>\n"+
                        "       <PositiveValue>42</PositiveValue>\n"+
                        "       <NegativeValue>-23</NegativeValue>\n"+
                        "       <DoubleValue>-23.45</DoubleValue>\n"+
                        "       <Nan>-23x.45</Nan>\n"+
                        "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
                        "   </address>\n"+
                        "</addresses>";
        Reader reader = new StringReader(xmlStr);
        JSONPointer pointer = new JSONPointer("/addresses/address");

        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

        String expectedJsonString = "{\"addresses\":{\"address\":{\"School\":\"ICS\",\"Program\":\"MSWE\",\"University\":\"UCI\",\"Year\":2021}," +
                "\"xsi:noNamespaceSchemaLocation\":\"test.xsd\",\"xmlns:xsi\":\"http://www.w3.org/2001/XMLSchema-instance\"}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(reader,pointer,newObject);
       Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectReplaceNested(){
        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

        InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
        Reader xmlReader = new InputStreamReader(xmlStream);
        JSONPointer pointer = new JSONPointer("/clinical_study/secondary_outcome/1");

        String expectedJsonString = "{\"clinical_study\": {\n" +
                "  \"brief_summary\": {\"textblock\": \"CLEAR SYNERGY is an international multi center 2x2 randomized placebo controlled trial of\"},\n" +
                "  \"brief_title\": \"CLEAR SYNERGY Neutrophil Substudy\",\n" +
                "  \"overall_status\": \"Recruiting\",\n" +
                "  \"eligibility\": {\n" +
                "    \"study_pop\": {\"textblock\": \"Patients who are randomized to the drug RCT portion of the CLEAR SYNERGY (OASIS 9) trial\"},\n" +
                "    \"minimum_age\": \"19 Years\",\n" +
                "    \"sampling_method\": \"Non-Probability Sample\",\n" +
                "    \"gender\": \"All\",\n" +
                "    \"criteria\": {\"textblock\": \"Inclusion Criteria:\"},\n" +
                "    \"healthy_volunteers\": \"No\",\n" +
                "    \"maximum_age\": \"110 Years\"\n" +
                "  },\n" +
                "  \"number_of_groups\": 2,\n" +
                "  \"source\": \"NYU Langone Health\",\n" +
                "  \"location_countries\": {\"country\": \"United States\"},\n" +
                "  \"study_design_info\": {\n" +
                "    \"time_perspective\": \"Prospective\",\n" +
                "    \"observational_model\": \"Other\"\n" +
                "  },\n" +
                "  \"last_update_submitted_qc\": \"September 10, 2019\",\n" +
                "  \"intervention_browse\": {\"mesh_term\": \"Colchicine\"},\n" +
                "  \"official_title\": \"Studies on the Effects of Colchicine on Neutrophil Biology in Acute Myocardial Infarction: A Substudy of the CLEAR SYNERGY (OASIS 9) Trial\",\n" +
                "  \"primary_completion_date\": {\n" +
                "    \"type\": \"Anticipated\",\n" +
                "    \"content\": \"February 1, 2021\"\n" +
                "  },\n" +
                "  \"sponsors\": {\n" +
                "    \"lead_sponsor\": {\n" +
                "      \"agency_class\": \"Other\",\n" +
                "      \"agency\": \"NYU Langone Health\"\n" +
                "    },\n" +
                "    \"collaborator\": [\n" +
                "      {\n" +
                "        \"agency_class\": \"Other\",\n" +
                "        \"agency\": \"Population Health Research Institute\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"agency_class\": \"NIH\",\n" +
                "        \"agency\": \"National Heart, Lung, and Blood Institute (NHLBI)\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"overall_official\": {\n" +
                "    \"role\": \"Principal Investigator\",\n" +
                "    \"affiliation\": \"NYU School of Medicine\",\n" +
                "    \"last_name\": \"Binita Shah, MD\"\n" +
                "  },\n" +
                "  \"overall_contact_backup\": {\"last_name\": \"Binita Shah, MD\"},\n" +
                "  \"condition_browse\": {\"mesh_term\": [\n" +
                "    \"Myocardial Infarction\",\n" +
                "    \"ST Elevation Myocardial Infarction\",\n" +
                "    \"Infarction\"\n" +
                "  ]},\n" +
                "  \"overall_contact\": {\n" +
                "    \"phone\": \"646-501-9648\",\n" +
                "    \"last_name\": \"Fatmira Curovic\",\n" +
                "    \"email\": \"fatmira.curovic@nyumc.org\"\n" +
                "  },\n" +
                "  \"responsible_party\": {\n" +
                "    \"responsible_party_type\": \"Principal Investigator\",\n" +
                "    \"investigator_title\": \"Assistant Professor of Medicine\",\n" +
                "    \"investigator_full_name\": \"Binita Shah\",\n" +
                "    \"investigator_affiliation\": \"NYU Langone Health\"\n" +
                "  },\n" +
                "  \"study_first_submitted_qc\": \"March 12, 2019\",\n" +
                "  \"start_date\": {\n" +
                "    \"type\": \"Actual\",\n" +
                "    \"content\": \"March 4, 2019\"\n" +
                "  },\n" +
                "  \"has_expanded_access\": \"No\",\n" +
                "  \"study_first_posted\": {\n" +
                "    \"type\": \"Actual\",\n" +
                "    \"content\": \"March 14, 2019\"\n" +
                "  },\n" +
                "  \"arm_group\": [\n" +
                "    {\"arm_group_label\": \"Colchicine\"},\n" +
                "    {\"arm_group_label\": \"Placebo\"}\n" +
                "  ],\n" +
                "  \"primary_outcome\": {\n" +
                "    \"measure\": \"soluble L-selectin\",\n" +
                "    \"time_frame\": \"between baseline and 3 months\",\n" +
                "    \"description\": \"Change in soluble L-selectin between baseline and 3 mo after STEMI in the placebo vs. colchicine groups.\"\n" +
                "  },\n" +
                "  \"secondary_outcome\": [\n" +
                "    {\n" +
                "      \"measure\": \"Other soluble markers of neutrophil activity\",\n" +
                "      \"time_frame\": \"between baseline and 3 months\",\n" +
                "      \"description\": \"Other markers of neutrophil activity will be evaluated at baseline and 3 months after STEMI (myeloperoxidase, matrix metalloproteinase-9, neutrophil gelatinase-associated lipocalin, neutrophil elastase, intercellular/vascular cellular adhesion molecules)\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"School\": \"ICS\",\n" +
                "      \"Program\": \"MSWE\",\n" +
                "      \"University\": \"UCI\",\n" +
                "      \"Year\": 2021\n" +
                "    },\n" +
                "    {\n" +
                "      \"measure\": \"Neutrophil-driven responses that may further propagate injury\",\n" +
                "      \"time_frame\": \"between baseline and 3 months\",\n" +
                "      \"description\": \"Neutrophil-driven responses that may further propagate injury will be evaluated at baseline and 3 months after STEMI (neutrophil extracellular traps, neutrophil-derived microparticles)\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"oversight_info\": {\n" +
                "    \"is_fda_regulated_drug\": \"No\",\n" +
                "    \"is_fda_regulated_device\": \"No\",\n" +
                "    \"has_dmc\": \"No\"\n" +
                "  },\n" +
                "  \"last_update_posted\": {\n" +
                "    \"type\": \"Actual\",\n" +
                "    \"content\": \"September 12, 2019\"\n" +
                "  },\n" +
                "  \"id_info\": {\n" +
                "    \"nct_id\": \"NCT03874338\",\n" +
                "    \"org_study_id\": \"18-01323\",\n" +
                "    \"secondary_id\": \"1R01HL146206\"\n" +
                "  },\n" +
                "  \"enrollment\": {\n" +
                "    \"type\": \"Anticipated\",\n" +
                "    \"content\": 670\n" +
                "  },\n" +
                "  \"study_first_submitted\": \"March 12, 2019\",\n" +
                "  \"condition\": [\n" +
                "    \"Neutrophils.Hypersegmented | Bld-Ser-Plas\",\n" +
                "    \"STEMI - ST Elevation Myocardial Infarction\"\n" +
                "  ],\n" +
                "  \"study_type\": \"Observational\",\n" +
                "  \"required_header\": {\n" +
                "    \"download_date\": \"ClinicalTrials.gov processed this data on July 19, 2020\",\n" +
                "    \"link_text\": \"Link to the current ClinicalTrials.gov record.\",\n" +
                "    \"url\": \"https://clinicaltrials.gov/show/NCT03874338\"\n" +
                "  },\n" +
                "  \"last_update_submitted\": \"September 10, 2019\",\n" +
                "  \"completion_date\": {\n" +
                "    \"type\": \"Anticipated\",\n" +
                "    \"content\": \"February 1, 2022\"\n" +
                "  },\n" +
                "  \"location\": {\n" +
                "    \"contact\": {\n" +
                "      \"phone\": \"646-501-9648\",\n" +
                "      \"last_name\": \"Fatmira Curovic\",\n" +
                "      \"email\": \"fatmira.curovic@nyumc.org\"\n" +
                "    },\n" +
                "    \"facility\": {\n" +
                "      \"address\": {\n" +
                "        \"zip\": 10016,\n" +
                "        \"country\": \"United States\",\n" +
                "        \"city\": \"New York\",\n" +
                "        \"state\": \"New York\"\n" +
                "      },\n" +
                "      \"name\": \"NYU School of Medicine\"\n" +
                "    },\n" +
                "    \"status\": \"Recruiting\",\n" +
                "    \"contact_backup\": {\"last_name\": \"Binita Shah, MD\"}\n" +
                "  },\n" +
                "  \"intervention\": {\n" +
                "    \"intervention_type\": \"Drug\",\n" +
                "    \"arm_group_label\": [\n" +
                "      \"Colchicine\",\n" +
                "      \"Placebo\"\n" +
                "    ],\n" +
                "    \"description\": \"Participants in the main CLEAR SYNERGY trial are randomized to colchicine/spironolactone versus placebo in a 2x2 factorial design. The substudy is interested in the evaluation of biospecimens obtained from patients in the colchicine vs placebo group.\",\n" +
                "    \"intervention_name\": \"Colchicine Pill\"\n" +
                "  },\n" +
                "  \"patient_data\": {\"sharing_ipd\": \"No\"},\n" +
                "  \"verification_date\": \"September 2019\"\n" +
                "}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(xmlReader,pointer,newObject);
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);

    }

    @Test
    public void testToJSONObjectReplaceAtIndex(){
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<catalog>\n" +
                "   <book id=\"bk101\">\n" +
                "      <author>Gambardella, Matthew</author>\n" +
                "      <title>XML Developer's Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>44.95</price>\n" +
                "      <publish_date>2000-10-01</publish_date>\n" +
                "      <description>An in-depth look at creating applications \n" +
                "      with XML.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk102\">\n" +
                "      <author>Ralls, Kim</author>\n" +
                "      <title>Midnight Rain</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-12-16</publish_date>\n" +
                "      <description>A former architect battles corporate zombies, \n" +
                "      an evil sorceress, and her own childhood to become queen \n" +
                "      of the world.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk103\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Maeve Ascendant</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-11-17</publish_date>\n" +
                "      <description>After the collapse of a nanotechnology \n" +
                "      society in England, the young survivors lay the \n" +
                "      foundation for a new society.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk104\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Oberon's Legacy</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-03-10</publish_date>\n" +
                "      <description>In post-apocalypse England, the mysterious \n" +
                "      agent known only as Oberon helps to create a new life \n" +
                "      for the inhabitants of London. Sequel to Maeve \n" +
                "      Ascendant.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk105\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>The Sundered Grail</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-09-10</publish_date>\n" +
                "      <description>The two daughters of Maeve, half-sisters, \n" +
                "      battle one another for control of England. Sequel to \n" +
                "      Oberon's Legacy.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk106\">\n" +
                "      <author>Randall, Cynthia</author>\n" +
                "      <title>Lover Birds</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-09-02</publish_date>\n" +
                "      <description>When Carla meets Paul at an ornithology \n" +
                "      conference, tempers fly as feathers get ruffled.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk107\">\n" +
                "      <author>Thurman, Paula</author>\n" +
                "      <title>Splish Splash</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>A deep sea diver finds true love twenty \n" +
                "      thousand leagues beneath the sea.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk108\">\n" +
                "      <author>Knorr, Stefan</author>\n" +
                "      <title>Creepy Crawlies</title>\n" +
                "      <genre>Horror</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-12-06</publish_date>\n" +
                "      <description>An anthology of horror stories about roaches,\n" +
                "      centipedes, scorpions  and other insects.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk109\">\n" +
                "      <author>Kress, Peter</author>\n" +
                "      <title>Paradox Lost</title>\n" +
                "      <genre>Science Fiction</genre>\n" +
                "      <price>6.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>After an inadvertant trip through a Heisenberg\n" +
                "      Uncertainty Device, James Salway discovers the problems \n" +
                "      of being quantum.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk110\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>Microsoft .NET: The Programming Bible</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-09</publish_date>\n" +
                "      <description>Microsoft's .NET initiative is explored in \n" +
                "      detail in this deep programmer's reference.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk111\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>MSXML3: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-01</publish_date>\n" +
                "      <description>The Microsoft MSXML3 parser is covered in \n" +
                "      detail, with attention to XML DOM interfaces, XSLT processing, \n" +
                "      SAX and more.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk112\">\n" +
                "      <author>Galos, Mike</author>\n" +
                "      <title>Visual Studio 7: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>49.95</price>\n" +
                "      <publish_date>2001-04-16</publish_date>\n" +
                "      <description>Microsoft Visual Studio 7 is explored in depth,\n" +
                "      looking at how Visual Basic, Visual C++, C#, and ASP+ are \n" +
                "      integrated into a comprehensive development \n" +
                "      environment.</description>\n" +
                "   </book>\n" +
                "</catalog>";
        Reader reader = new StringReader(xmlStr);
        JSONPointer pointer = new JSONPointer("/catalog/book/0");

        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

        String expectedJsonString = "{\"catalog\": {\"book\": [\n" +
                "  {\n" +
                "    \"School\": \"ICS\",\n" +
                "    \"Program\": \"MSWE\",\n" +
                "    \"University\": \"UCI\",\n" +
                "    \"Year\": 2021\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Ralls, Kim\",\n" +
                "    \"price\": 5.95,\n" +
                "    \"genre\": \"Fantasy\",\n" +
                "    \"description\": \"A former architect battles corporate zombies, \\n      an evil sorceress, and her own childhood to become queen \\n      of the world.\",\n" +
                "    \"id\": \"bk102\",\n" +
                "    \"title\": \"Midnight Rain\",\n" +
                "    \"publish_date\": \"2000-12-16\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Corets, Eva\",\n" +
                "    \"price\": 5.95,\n" +
                "    \"genre\": \"Fantasy\",\n" +
                "    \"description\": \"After the collapse of a nanotechnology \\n      society in England, the young survivors lay the \\n      foundation for a new society.\",\n" +
                "    \"id\": \"bk103\",\n" +
                "    \"title\": \"Maeve Ascendant\",\n" +
                "    \"publish_date\": \"2000-11-17\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Corets, Eva\",\n" +
                "    \"price\": 5.95,\n" +
                "    \"genre\": \"Fantasy\",\n" +
                "    \"description\": \"In post-apocalypse England, the mysterious \\n      agent known only as Oberon helps to create a new life \\n      for the inhabitants of London. Sequel to Maeve \\n      Ascendant.\",\n" +
                "    \"id\": \"bk104\",\n" +
                "    \"title\": \"Oberon's Legacy\",\n" +
                "    \"publish_date\": \"2001-03-10\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Corets, Eva\",\n" +
                "    \"price\": 5.95,\n" +
                "    \"genre\": \"Fantasy\",\n" +
                "    \"description\": \"The two daughters of Maeve, half-sisters, \\n      battle one another for control of England. Sequel to \\n      Oberon's Legacy.\",\n" +
                "    \"id\": \"bk105\",\n" +
                "    \"title\": \"The Sundered Grail\",\n" +
                "    \"publish_date\": \"2001-09-10\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Randall, Cynthia\",\n" +
                "    \"price\": 4.95,\n" +
                "    \"genre\": \"Romance\",\n" +
                "    \"description\": \"When Carla meets Paul at an ornithology \\n      conference, tempers fly as feathers get ruffled.\",\n" +
                "    \"id\": \"bk106\",\n" +
                "    \"title\": \"Lover Birds\",\n" +
                "    \"publish_date\": \"2000-09-02\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Thurman, Paula\",\n" +
                "    \"price\": 4.95,\n" +
                "    \"genre\": \"Romance\",\n" +
                "    \"description\": \"A deep sea diver finds true love twenty \\n      thousand leagues beneath the sea.\",\n" +
                "    \"id\": \"bk107\",\n" +
                "    \"title\": \"Splish Splash\",\n" +
                "    \"publish_date\": \"2000-11-02\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Knorr, Stefan\",\n" +
                "    \"price\": 4.95,\n" +
                "    \"genre\": \"Horror\",\n" +
                "    \"description\": \"An anthology of horror stories about roaches,\\n      centipedes, scorpions  and other insects.\",\n" +
                "    \"id\": \"bk108\",\n" +
                "    \"title\": \"Creepy Crawlies\",\n" +
                "    \"publish_date\": \"2000-12-06\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Kress, Peter\",\n" +
                "    \"price\": 6.95,\n" +
                "    \"genre\": \"Science Fiction\",\n" +
                "    \"description\": \"After an inadvertant trip through a Heisenberg\\n      Uncertainty Device, James Salway discovers the problems \\n      of being quantum.\",\n" +
                "    \"id\": \"bk109\",\n" +
                "    \"title\": \"Paradox Lost\",\n" +
                "    \"publish_date\": \"2000-11-02\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"O'Brien, Tim\",\n" +
                "    \"price\": 36.95,\n" +
                "    \"genre\": \"Computer\",\n" +
                "    \"description\": \"Microsoft's .NET initiative is explored in \\n      detail in this deep programmer's reference.\",\n" +
                "    \"id\": \"bk110\",\n" +
                "    \"title\": \"Microsoft .NET: The Programming Bible\",\n" +
                "    \"publish_date\": \"2000-12-09\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"O'Brien, Tim\",\n" +
                "    \"price\": 36.95,\n" +
                "    \"genre\": \"Computer\",\n" +
                "    \"description\": \"The Microsoft MSXML3 parser is covered in \\n      detail, with attention to XML DOM interfaces, XSLT processing, \\n      SAX and more.\",\n" +
                "    \"id\": \"bk111\",\n" +
                "    \"title\": \"MSXML3: A Comprehensive Guide\",\n" +
                "    \"publish_date\": \"2000-12-01\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"author\": \"Galos, Mike\",\n" +
                "    \"price\": 49.95,\n" +
                "    \"genre\": \"Computer\",\n" +
                "    \"description\": \"Microsoft Visual Studio 7 is explored in depth,\\n      looking at how Visual Basic, Visual C++, C#, and ASP+ are \\n      integrated into a comprehensive development \\n      environment.\",\n" +
                "    \"id\": \"bk112\",\n" +
                "    \"title\": \"Visual Studio 7: A Comprehensive Guide\",\n" +
                "    \"publish_date\": \"2001-04-16\"\n" +
                "  }\n" +
                "]}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(reader,pointer,newObject);
      Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectGetSubObjectCastException(){
        InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
        Reader xmlReader = new InputStreamReader(xmlStream);
        JSONPointer pointer = new JSONPointer("/clinical_study/sponsors/lead_sponsor/agency");

        try {
            XML.toJSONObject(xmlReader,pointer);
            fail("Expecting a Exception because value at keyPath is String and not JSONObject");
        } catch (ClassCastException e) {
            assertEquals("Expecting an exception message",
                    "class java.lang.String cannot be cast to class org.json.JSONObject (java.lang.String is in module java.base of loader 'bootstrap'; org.json.JSONObject is in unnamed module of loader 'app')",
                    e.getMessage());
        }
    }

    @Test
    public void testToJSONObjectReplaceException(){
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<catalog>\n" +
                "   <book id=\"bk101\">\n" +
                "      <author>Gambardella, Matthew</author>\n" +
                "      <title>XML Developer's Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>44.95</price>\n" +
                "      <publish_date>2000-10-01</publish_date>\n" +
                "      <description>An in-depth look at creating applications \n" +
                "      with XML.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk102\">\n" +
                "      <author>Ralls, Kim</author>\n" +
                "      <title>Midnight Rain</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-12-16</publish_date>\n" +
                "      <description>A former architect battles corporate zombies, \n" +
                "      an evil sorceress, and her own childhood to become queen \n" +
                "      of the world.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk103\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Maeve Ascendant</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-11-17</publish_date>\n" +
                "      <description>After the collapse of a nanotechnology \n" +
                "      society in England, the young survivors lay the \n" +
                "      foundation for a new society.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk104\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Oberon's Legacy</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-03-10</publish_date>\n" +
                "      <description>In post-apocalypse England, the mysterious \n" +
                "      agent known only as Oberon helps to create a new life \n" +
                "      for the inhabitants of London. Sequel to Maeve \n" +
                "      Ascendant.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk105\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>The Sundered Grail</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-09-10</publish_date>\n" +
                "      <description>The two daughters of Maeve, half-sisters, \n" +
                "      battle one another for control of England. Sequel to \n" +
                "      Oberon's Legacy.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk106\">\n" +
                "      <author>Randall, Cynthia</author>\n" +
                "      <title>Lover Birds</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-09-02</publish_date>\n" +
                "      <description>When Carla meets Paul at an ornithology \n" +
                "      conference, tempers fly as feathers get ruffled.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk107\">\n" +
                "      <author>Thurman, Paula</author>\n" +
                "      <title>Splish Splash</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>A deep sea diver finds true love twenty \n" +
                "      thousand leagues beneath the sea.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk108\">\n" +
                "      <author>Knorr, Stefan</author>\n" +
                "      <title>Creepy Crawlies</title>\n" +
                "      <genre>Horror</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-12-06</publish_date>\n" +
                "      <description>An anthology of horror stories about roaches,\n" +
                "      centipedes, scorpions  and other insects.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk109\">\n" +
                "      <author>Kress, Peter</author>\n" +
                "      <title>Paradox Lost</title>\n" +
                "      <genre>Science Fiction</genre>\n" +
                "      <price>6.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>After an inadvertant trip through a Heisenberg\n" +
                "      Uncertainty Device, James Salway discovers the problems \n" +
                "      of being quantum.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk110\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>Microsoft .NET: The Programming Bible</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-09</publish_date>\n" +
                "      <description>Microsoft's .NET initiative is explored in \n" +
                "      detail in this deep programmer's reference.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk111\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>MSXML3: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-01</publish_date>\n" +
                "      <description>The Microsoft MSXML3 parser is covered in \n" +
                "      detail, with attention to XML DOM interfaces, XSLT processing, \n" +
                "      SAX and more.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk112\">\n" +
                "      <author>Galos, Mike</author>\n" +
                "      <title>Visual Studio 7: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>49.95</price>\n" +
                "      <publish_date>2001-04-16</publish_date>\n" +
                "      <description>Microsoft Visual Studio 7 is explored in depth,\n" +
                "      looking at how Visual Basic, Visual C++, C#, and ASP+ are \n" +
                "      integrated into a comprehensive development \n" +
                "      environment.</description>\n" +
                "   </book>\n" +
                "</catalog>";
        Reader reader = new StringReader(xmlStr);
        JSONPointer pointer = new JSONPointer("/catalog/nosuchkey/1");

        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

          JSONObject obj =  XML.toJSONObject(reader,pointer,newObject);
          //pointer path does not exist expecting NULL to be returned
          Assert.assertNull(obj);

    }

    @Test
    public void testToJSONOjectReplaceXMLError(){
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<catalog>\n" +
                "   <book id=\"bk101\">\n" +
                "      <author>Gambardella, Matthew</author>\n" +
                "      <title>XML Developer's Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>44.95</price>\n" +
                "      <publish_date>2000-10-01</publish_date>\n" +
                "      <description>An in-depth look at creating applications \n" +
                "      with XML.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk102\">\n" +
                "      <author>Ralls, Kim</author>\n" +
                "      <title>Midnight Rain</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-12-16</publish_date>\n" +
                "      <description>A former architect battles corporate zombies, \n" +
                "      an evil sorceress, and her own childhood to become queen \n" +
                "      of the world.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk103\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Maeve Ascendant</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-11-17</publish_date>\n" +
                "      <description>After the collapse of a nanotechnology \n" +
                "      society in England, the young survivors lay the \n" +
                "      foundation for a new society.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk104\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Oberon's Legacy</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-03-10</publish_date>\n" +
                "      <description>In post-apocalypse England, the mysterious \n" +
                "      agent known only as Oberon helps to create a new life \n" +
                "      for the inhabitants of London. Sequel to Maeve \n" +
                "      Ascendant.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk105\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>The Sundered Grail</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-09-10</publish_date>\n" +
                "      <description>The two daughters of Maeve, half-sisters, \n" +
                "      battle one another for control of England. Sequel to \n" +
                "      Oberon's Legacy.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk106\">\n" +
                "      <author>Randall, Cynthia</author>\n" +
                "      <title>Lover Birds</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-09-02</publish_date>\n" +
                "      <description>When Carla meets Paul at an ornithology \n" +
                "      conference, tempers fly as feathers get ruffled.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk107\">\n" +
                "      <author>Thurman, Paula</author>\n" +
                "      <title>Splish Splash</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>A deep sea diver finds true love twenty \n" +
                "      thousand leagues beneath the sea.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk108\">\n" +
                "      <author>Knorr, Stefan</author>\n" +
                "      <title>Creepy Crawlies</title>\n" +
                "      <genre>Horror</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-12-06</publish_date>\n" +
                "      <description>An anthology of horror stories about roaches,\n" +
                "      centipedes, scorpions  and other insects.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk109\">\n" +
                "      <author>Kress, Peter</author>\n" +
                "      <title>Paradox Lost</title>\n" +
                "      <genre>Science Fiction</genre>\n" +
                "      <price>6.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>After an inadvertant trip through a Heisenberg\n" +
                "      Uncertainty Device, James Salway discovers the problems \n" +
                "      of being quantum.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk110\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>Microsoft .NET: The Programming Bible</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-09</publish_date>\n" +
                "      <description>Microsoft's .NET initiative is explored in \n" +
                "      detail in this deep programmer's reference.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk111\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>MSXML3: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-01</publish_date>\n" +
                "      <description>The Microsoft MSXML3 parser is covered in \n" +
                "      detail, with attention to XML DOM interfaces, XSLT processing, \n" +
                "      SAX and more.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk112\">\n" +
                "      <author>Galos, Mike</author>\n" +
                "      <title>Visual Studio 7: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>49.95</price>\n" +
                "      <publish_date>2001-04-16</publish_date>\n" +
                "      <description>Microsoft Visual Studio 7 is explored in depth,\n" +
                "      looking at how Visual Basic, Visual C++, C#, and ASP+ are \n" +
                "      integrated into a comprehensive development \n" +
                "      environment.</description>\n" +
                "   \n" +
                "</catalog>";
        Reader reader = new StringReader(xmlStr);
        JSONPointer pointer = new JSONPointer("/catalog/book/1");

        JSONObject newObject = new JSONObject();
        newObject.put("University", "UCI");
        newObject.put("School", "ICS");
        newObject.put("Program", "MSWE");
        newObject.put("Year", 2021);

        try {
            JSONObject obj =  XML.toJSONObject(reader,pointer,newObject);
            fail("Expecting a Exception");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Mismatched book and catalog at 4421 [character 9 line 120]",
                    e.getMessage());
        }
    }

    @Test
    public void testToJSONObjectGetSubObjectXMLError(){
        String xmlStr =
                "<?xml version=\"1.0\"?>\n" +
                        "<catalog\n" +
                        "   <book id=\"bk101\">\n" +
                        "      <author>Gambardella, Matthew</author>\n" +
                        "      <title>XML Developer's Guide</title>\n" +
                        "      <genre>Computer</genre>\n" +
                        "      <price>44.95</price>\n" +
                        "      <publish_date>2000-10-01</publish_date>\n" +
                        "      <description>An in-depth look at creating applications \n" +
                        "      with XML.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk102\">\n" +
                        "      <author>Ralls, Kim</author>\n" +
                        "      <title>Midnight Rain</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2000-12-16</publish_date>\n" +
                        "      <description>A former architect battles corporate zombies, \n" +
                        "      an evil sorceress, and her own childhood to become queen \n" +
                        "      of the world.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk103\">\n" +
                        "      <author>Corets, Eva</author>\n" +
                        "      <title>Maeve Ascendant</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2000-11-17</publish_date>\n" +
                        "      <description>After the collapse of a nanotechnology \n" +
                        "      society in England, the young survivors lay the \n" +
                        "      foundation for a new society.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk104\">\n" +
                        "      <author>Corets, Eva</author>\n" +
                        "      <title>Oberon's Legacy</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2001-03-10</publish_date>\n" +
                        "      <description>In post-apocalypse England, the mysterious \n" +
                        "      agent known only as Oberon helps to create a new life \n" +
                        "      for the inhabitants of London. Sequel to Maeve \n" +
                        "      Ascendant.</description>\n" +
                        "   </book>\n" +
                        "   <book id=\"bk105\">\n" +
                        "      <author>Corets, Eva</author>\n" +
                        "      <title>The Sundered Grail</title>\n" +
                        "      <genre>Fantasy</genre>\n" +
                        "      <price>5.95</price>\n" +
                        "      <publish_date>2001-09-10</publish_date>\n" +
                        "      <description>The two daughters of Maeve, half-sisters, \n" +
                        "      battle one another for control of England. Sequel to \n" +
                        "      Oberon's Legacy.</description>\n" +
                        "   </book>\n"+
                        "</catalog>";
        JSONPointer pointer = new JSONPointer("/catalog/book/2");
        Reader reader = new StringReader(xmlStr);

        try {
            JSONObject actualJson = XML.toJSONObject(reader,pointer);
            fail("Expecting a Exception");
        } catch (JSONException e) {
            assertEquals("Expecting an exception message",
                    "Misplaced '<' at 35 [character 4 line 3]",
                    e.getMessage());
        }
    }

    @Test
    public void testToJSONObjectRenameKeySimple(){
        String xmlStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                        "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
                        "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
                        "   <address>\n"+
                        "       <name>Joe Tester</name>\n"+
                        "       <street>[CDATA[Baker street 5]</street>\n"+
                        "       <NothingHere/>\n"+
                        "       <TrueValue>true</TrueValue>\n"+
                        "       <FalseValue>false</FalseValue>\n"+
                        "       <NullValue>null</NullValue>\n"+
                        "       <PositiveValue>42</PositiveValue>\n"+
                        "       <NegativeValue>-23</NegativeValue>\n"+
                        "       <DoubleValue>-23.45</DoubleValue>\n"+
                        "       <Nan>-23x.45</Nan>\n"+
                        "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
                        "   </address>\n"+
                        "</addresses>";

        String expectedJsonString ="{\"swe_262P_addresses\": {\n" +
                "  \"swe_262P_xsi:noNamespaceSchemaLocation\": \"test.xsd\",\n" +
                "  \"swe_262P_address\": {\n" +
                "    \"swe_262P_Nan\": \"-23x.45\",\n" +
                "    \"swe_262P_street\": \"[CDATA[Baker street 5]\",\n" +
                "    \"swe_262P_FalseValue\": false,\n" +
                "    \"swe_262P_ArrayOfNum\": \"1, 2, 3, 4.1, 5.2\",\n" +
                "    \"swe_262P_NothingHere\": \"\",\n" +
                "    \"swe_262P_NegativeValue\": -23,\n" +
                "    \"swe_262P_DoubleValue\": -23.45,\n" +
                "    \"swe_262P_TrueValue\": true,\n" +
                "    \"swe_262P_name\": \"Joe Tester\",\n" +
                "    \"swe_262P_NullValue\": null,\n" +
                "    \"swe_262P_PositiveValue\": 42\n" +
                "  },\n" +
                "  \"swe_262P_xmlns:xsi\": \"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "}}";
        Reader reader = new StringReader(xmlStr);
        Function<String, String> func = x -> "swe_262P_"+x;
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(reader,func);
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectRenameKeyNested(){
        InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
        Reader xmlReader = new InputStreamReader(xmlStream);


        String expectedJsonString ="{\"clinical_studysweP\": {\n" +
                "  \"brief_summarysweP\": {\"textblocksweP\": \"CLEAR SYNERGY is an international multi center 2x2 randomized placebo controlled trial of\"},\n" +
                "  \"arm_groupsweP\": [\n" +
                "    {\"arm_group_labelsweP\": \"Colchicine\"},\n" +
                "    {\"arm_group_labelsweP\": \"Placebo\"}\n" +
                "  ],\n" +
                "  \"enrollmentsweP\": {\n" +
                "    \"typesweP\": \"Anticipated\",\n" +
                "    \"content\": 670\n" +
                "  },\n" +
                "  \"sourcesweP\": \"NYU Langone Health\",\n" +
                "  \"study_first_postedsweP\": {\n" +
                "    \"typesweP\": \"Actual\",\n" +
                "    \"content\": \"March 14, 2019\"\n" +
                "  },\n" +
                "  \"study_first_submittedsweP\": \"March 12, 2019\",\n" +
                "  \"location_countriessweP\": {\"countrysweP\": \"United States\"},\n" +
                "  \"study_first_submitted_qcsweP\": \"March 12, 2019\",\n" +
                "  \"id_infosweP\": {\n" +
                "    \"secondary_idsweP\": \"1R01HL146206\",\n" +
                "    \"org_study_idsweP\": \"18-01323\",\n" +
                "    \"nct_idsweP\": \"NCT03874338\"\n" +
                "  },\n" +
                "  \"intervention_browsesweP\": {\"mesh_termsweP\": \"Colchicine\"},\n" +
                "  \"overall_statussweP\": \"Recruiting\",\n" +
                "  \"overall_officialsweP\": {\n" +
                "    \"last_namesweP\": \"Binita Shah, MD\",\n" +
                "    \"rolesweP\": \"Principal Investigator\",\n" +
                "    \"affiliationsweP\": \"NYU School of Medicine\"\n" +
                "  },\n" +
                "  \"has_expanded_accesssweP\": \"No\",\n" +
                "  \"conditionsweP\": [\n" +
                "    \"Neutrophils.Hypersegmented | Bld-Ser-Plas\",\n" +
                "    \"STEMI - ST Elevation Myocardial Infarction\"\n" +
                "  ],\n" +
                "  \"interventionsweP\": {\n" +
                "    \"intervention_namesweP\": \"Colchicine Pill\",\n" +
                "    \"descriptionsweP\": \"Participants in the main CLEAR SYNERGY trial are randomized to colchicine/spironolactone versus placebo in a 2x2 factorial design. The substudy is interested in the evaluation of biospecimens obtained from patients in the colchicine vs placebo group.\",\n" +
                "    \"arm_group_labelsweP\": [\n" +
                "      \"Colchicine\",\n" +
                "      \"Placebo\"\n" +
                "    ],\n" +
                "    \"intervention_typesweP\": \"Drug\"\n" +
                "  },\n" +
                "  \"overall_contactsweP\": {\n" +
                "    \"last_namesweP\": \"Fatmira Curovic\",\n" +
                "    \"phonesweP\": \"646-501-9648\",\n" +
                "    \"emailsweP\": \"fatmira.curovic@nyumc.org\"\n" +
                "  },\n" +
                "  \"sponsorssweP\": {\n" +
                "    \"collaboratorsweP\": [\n" +
                "      {\n" +
                "        \"agencysweP\": \"Population Health Research Institute\",\n" +
                "        \"agency_classsweP\": \"Other\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"agencysweP\": \"National Heart, Lung, and Blood Institute (NHLBI)\",\n" +
                "        \"agency_classsweP\": \"NIH\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"lead_sponsorsweP\": {\n" +
                "      \"agencysweP\": \"NYU Langone Health\",\n" +
                "      \"agency_classsweP\": \"Other\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"condition_browsesweP\": {\"mesh_termsweP\": [\n" +
                "    \"Myocardial Infarction\",\n" +
                "    \"ST Elevation Myocardial Infarction\",\n" +
                "    \"Infarction\"\n" +
                "  ]},\n" +
                "  \"primary_completion_datesweP\": {\n" +
                "    \"typesweP\": \"Anticipated\",\n" +
                "    \"content\": \"February 1, 2021\"\n" +
                "  },\n" +
                "  \"start_datesweP\": {\n" +
                "    \"typesweP\": \"Actual\",\n" +
                "    \"content\": \"March 4, 2019\"\n" +
                "  },\n" +
                "  \"overall_contact_backupsweP\": {\"last_namesweP\": \"Binita Shah, MD\"},\n" +
                "  \"number_of_groupssweP\": 2,\n" +
                "  \"study_design_infosweP\": {\n" +
                "    \"observational_modelsweP\": \"Other\",\n" +
                "    \"time_perspectivesweP\": \"Prospective\"\n" +
                "  },\n" +
                "  \"required_headersweP\": {\n" +
                "    \"urlsweP\": \"https://clinicaltrials.gov/show/NCT03874338\",\n" +
                "    \"link_textsweP\": \"Link to the current ClinicalTrials.gov record.\",\n" +
                "    \"download_datesweP\": \"ClinicalTrials.gov processed this data on July 19, 2020\"\n" +
                "  },\n" +
                "  \"responsible_partysweP\": {\n" +
                "    \"investigator_full_namesweP\": \"Binita Shah\",\n" +
                "    \"investigator_affiliationsweP\": \"NYU Langone Health\",\n" +
                "    \"investigator_titlesweP\": \"Assistant Professor of Medicine\",\n" +
                "    \"responsible_party_typesweP\": \"Principal Investigator\"\n" +
                "  },\n" +
                "  \"secondary_outcomesweP\": [\n" +
                "    {\n" +
                "      \"time_framesweP\": \"between baseline and 3 months\",\n" +
                "      \"measuresweP\": \"Other soluble markers of neutrophil activity\",\n" +
                "      \"descriptionsweP\": \"Other markers of neutrophil activity will be evaluated at baseline and 3 months after STEMI (myeloperoxidase, matrix metalloproteinase-9, neutrophil gelatinase-associated lipocalin, neutrophil elastase, intercellular/vascular cellular adhesion molecules)\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"time_framesweP\": \"between baseline and 3 months\",\n" +
                "      \"measuresweP\": \"Markers of systemic inflammation\",\n" +
                "      \"descriptionsweP\": \"Markers of systemic inflammation will be evaluated at baseline and 3 months after STEMI (high sensitive CRP, IL-1Î²)\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"time_framesweP\": \"between baseline and 3 months\",\n" +
                "      \"measuresweP\": \"Neutrophil-driven responses that may further propagate injury\",\n" +
                "      \"descriptionsweP\": \"Neutrophil-driven responses that may further propagate injury will be evaluated at baseline and 3 months after STEMI (neutrophil extracellular traps, neutrophil-derived microparticles)\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"official_titlesweP\": \"Studies on the Effects of Colchicine on Neutrophil Biology in Acute Myocardial Infarction: A Substudy of the CLEAR SYNERGY (OASIS 9) Trial\",\n" +
                "  \"eligibilitysweP\": {\n" +
                "    \"sampling_methodsweP\": \"Non-Probability Sample\",\n" +
                "    \"gendersweP\": \"All\",\n" +
                "    \"study_popsweP\": {\"textblocksweP\": \"Patients who are randomized to the drug RCT portion of the CLEAR SYNERGY (OASIS 9) trial\"},\n" +
                "    \"healthy_volunteerssweP\": \"No\",\n" +
                "    \"maximum_agesweP\": \"110 Years\",\n" +
                "    \"minimum_agesweP\": \"19 Years\",\n" +
                "    \"criteriasweP\": {\"textblocksweP\": \"Inclusion Criteria:\"}\n" +
                "  },\n" +
                "  \"primary_outcomesweP\": {\n" +
                "    \"time_framesweP\": \"between baseline and 3 months\",\n" +
                "    \"measuresweP\": \"soluble L-selectin\",\n" +
                "    \"descriptionsweP\": \"Change in soluble L-selectin between baseline and 3 mo after STEMI in the placebo vs. colchicine groups.\"\n" +
                "  },\n" +
                "  \"locationsweP\": {\n" +
                "    \"contactsweP\": {\n" +
                "      \"last_namesweP\": \"Fatmira Curovic\",\n" +
                "      \"phonesweP\": \"646-501-9648\",\n" +
                "      \"emailsweP\": \"fatmira.curovic@nyumc.org\"\n" +
                "    },\n" +
                "    \"facilitysweP\": {\n" +
                "      \"addresssweP\": {\n" +
                "        \"zipsweP\": 10016,\n" +
                "        \"citysweP\": \"New York\",\n" +
                "        \"statesweP\": \"New York\",\n" +
                "        \"countrysweP\": \"United States\"\n" +
                "      },\n" +
                "      \"namesweP\": \"NYU School of Medicine\"\n" +
                "    },\n" +
                "    \"statussweP\": \"Recruiting\",\n" +
                "    \"contact_backupsweP\": {\"last_namesweP\": \"Binita Shah, MD\"}\n" +
                "  },\n" +
                "  \"last_update_postedsweP\": {\n" +
                "    \"typesweP\": \"Actual\",\n" +
                "    \"content\": \"September 12, 2019\"\n" +
                "  },\n" +
                "  \"last_update_submittedsweP\": \"September 10, 2019\",\n" +
                "  \"brief_titlesweP\": \"CLEAR SYNERGY Neutrophil Substudy\",\n" +
                "  \"oversight_infosweP\": {\n" +
                "    \"has_dmcsweP\": \"No\",\n" +
                "    \"is_fda_regulated_drugsweP\": \"No\",\n" +
                "    \"is_fda_regulated_devicesweP\": \"No\"\n" +
                "  },\n" +
                "  \"verification_datesweP\": \"September 2019\",\n" +
                "  \"patient_datasweP\": {\"sharing_ipdsweP\": \"No\"},\n" +
                "  \"last_update_submitted_qcsweP\": \"September 10, 2019\",\n" +
                "  \"completion_datesweP\": {\n" +
                "    \"typesweP\": \"Anticipated\",\n" +
                "    \"content\": \"February 1, 2022\"\n" +
                "  },\n" +
                "  \"study_typesweP\": \"Observational\"\n" +
                "}}";

        Function<String, String> func = x -> x.concat("sweP");

       JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(xmlReader,func);
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToJSONObjectRenameNullFunction(){
        String xmlStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                        "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
                        "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
                        "   <address>\n"+
                        "       <name>Joe Tester</name>\n"+
                        "       <street>[CDATA[Baker street 5]</street>\n"+
                        "       <NothingHere/>\n"+
                        "       <TrueValue>true</TrueValue>\n"+
                        "       <FalseValue>false</FalseValue>\n"+
                        "       <NullValue>null</NullValue>\n"+
                        "       <PositiveValue>42</PositiveValue>\n"+
                        "       <NegativeValue>-23</NegativeValue>\n"+
                        "       <DoubleValue>-23.45</DoubleValue>\n"+
                        "       <Nan>-23x.45</Nan>\n"+
                        "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
                        "   </address>\n"+
                        "</addresses>";

        Reader reader = new StringReader(xmlStr);
        Function func = null;
        JSONObject actualJson = XML.toJSONObject(reader,func);
        assertNull(actualJson);
    }

    @Test
    public void testToJSONObjectRenameMultilineFunction(){
        String xmlStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                        "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
                        "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
                        "   <address>\n"+
                        "       <name>Joe Tester</name>\n"+
                        "       <street>[CDATA[Baker street 5]</street>\n"+
                        "       <NothingHere/>\n"+
                        "       <TrueValue>true</TrueValue>\n"+
                        "       <FalseValue>false</FalseValue>\n"+
                        "       <NullValue>null</NullValue>\n"+
                        "       <PositiveValue>42</PositiveValue>\n"+
                        "       <NegativeValue>-23</NegativeValue>\n"+
                        "       <DoubleValue>-23.45</DoubleValue>\n"+
                        "       <Nan>-23x.45</Nan>\n"+
                        "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
                        "   </address>\n"+
                        "</addresses>";

        String expectedJsonString ="{\"sesserdda\": {\n" +
                "  \"isx:snlmx\": \"http://www.w3.org/2001/XMLSchema-instance\",\n" +
                "  \"sserdda\": {\n" +
                "    \"muNfOyarrA\": \"1, 2, 3, 4.1, 5.2\",\n" +
                "    \"eulaVeslaF\": false,\n" +
                "    \"eulaVlluN\": null,\n" +
                "    \"eulaVeurT\": true,\n" +
                "    \"eman\": \"Joe Tester\",\n" +
                "    \"naN\": \"-23x.45\",\n" +
                "    \"teerts\": \"[CDATA[Baker street 5]\",\n" +
                "    \"eulaVevitisoP\": 42,\n" +
                "    \"eulaVevitageN\": -23,\n" +
                "    \"eulaVelbuoD\": -23.45,\n" +
                "    \"ereHgnihtoN\": \"\"\n" +
                "  },\n" +
                "  \"noitacoLamehcSecapsemaNon:isx\": \"test.xsd\"\n" +
                "}}";
        Reader reader = new StringReader(xmlStr);
        Function<String, String> func = x -> { StringBuilder sb = new StringBuilder(x);
                                                sb = sb.reverse();
                                                return sb.toString(); };
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(reader,func);
       Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    public void testToStreamGetAllPaths(){

        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<catalog>\n" +
                "   <book id=\"bk101\">\n" +
                "      <author>Gambardella, Matthew</author>\n" +
                "      <title>XML Developer's Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>44.95</price>\n" +
                "      <publish_date>2000-10-01</publish_date>\n" +
                "      <description>An in-depth look at creating applications \n" +
                "      with XML.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk102\">\n" +
                "      <author>Ralls, Kim</author>\n" +
                "      <title>Midnight Rain</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-12-16</publish_date>\n" +
                "      <description>A former architect battles corporate zombies, \n" +
                "      an evil sorceress, and her own childhood to become queen \n" +
                "      of the world.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk103\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Maeve Ascendant</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-11-17</publish_date>\n" +
                "      <description>After the collapse of a nanotechnology \n" +
                "      society in England, the young survivors lay the \n" +
                "      foundation for a new society.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk104\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Oberon's Legacy</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-03-10</publish_date>\n" +
                "      <description>In post-apocalypse England, the mysterious \n" +
                "      agent known only as Oberon helps to create a new life \n" +
                "      for the inhabitants of London. Sequel to Maeve \n" +
                "      Ascendant.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk105\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>The Sundered Grail</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-09-10</publish_date>\n" +
                "      <description>The two daughters of Maeve, half-sisters, \n" +
                "      battle one another for control of England. Sequel to \n" +
                "      Oberon's Legacy.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk106\">\n" +
                "      <author>Randall, Cynthia</author>\n" +
                "      <title>Lover Birds</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-09-02</publish_date>\n" +
                "      <description>When Carla meets Paul at an ornithology \n" +
                "      conference, tempers fly as feathers get ruffled.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk107\">\n" +
                "      <author>Thurman, Paula</author>\n" +
                "      <title>Splish Splash</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>A deep sea diver finds true love twenty \n" +
                "      thousand leagues beneath the sea.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk108\">\n" +
                "      <author>Knorr, Stefan</author>\n" +
                "      <title>Creepy Crawlies</title>\n" +
                "      <genre>Horror</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-12-06</publish_date>\n" +
                "      <description>An anthology of horror stories about roaches,\n" +
                "      centipedes, scorpions  and other insects.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk109\">\n" +
                "      <author>Kress, Peter</author>\n" +
                "      <title>Paradox Lost</title>\n" +
                "      <genre>Science Fiction</genre>\n" +
                "      <price>6.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>After an inadvertant trip through a Heisenberg\n" +
                "      Uncertainty Device, James Salway discovers the problems \n" +
                "      of being quantum.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk110\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>Microsoft .NET: The Programming Bible</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-09</publish_date>\n" +
                "      <description>Microsoft's .NET initiative is explored in \n" +
                "      detail in this deep programmer's reference.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk111\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>MSXML3: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-01</publish_date>\n" +
                "      <description>The Microsoft MSXML3 parser is covered in \n" +
                "      detail, with attention to XML DOM interfaces, XSLT processing, \n" +
                "      SAX and more.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk112\">\n" +
                "      <author>Galos, Mike</author>\n" +
                "      <title>Visual Studio 7: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>49.95</price>\n" +
                "      <publish_date>2001-04-16</publish_date>\n" +
                "      <description>Microsoft Visual Studio 7 is explored in depth,\n" +
                "      looking at how Visual Basic, Visual C++, C#, and ASP+ are \n" +
                "      integrated into a comprehensive development \n" +
                "      environment.</description>\n" +
                "   </book>\n" +
                "</catalog>";
        Reader reader = new StringReader(xmlStr);
        JSONObject obj = XML.toJSONObject(reader);

        String expectedOutput ="[/catalog, /catalog/book, /catalog/book/0, /catalog/book/1, /catalog/book/2, /catalog/book/3, /catalog/book/4, /catalog/book/5, /catalog/book/6, /catalog/book/7, /catalog/book/8, /catalog/book/9, /catalog/book/10, /catalog/book/11]";
        List<String> paths = obj.toStream()
                .map(node -> node.getPath())
                .collect(Collectors.toList());
      assertEquals(expectedOutput,  paths.toString());
    }

    @Test
    public void testToStreamFilterAuthors(){
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<catalog>\n" +
                "   <book id=\"bk101\">\n" +
                "      <author>Gambardella, Matthew</author>\n" +
                "      <title>XML Developer's Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>44.95</price>\n" +
                "      <publish_date>2000-10-01</publish_date>\n" +
                "      <description>An in-depth look at creating applications \n" +
                "      with XML.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk102\">\n" +
                "      <author>Ralls, Kim</author>\n" +
                "      <title>Midnight Rain</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-12-16</publish_date>\n" +
                "      <description>A former architect battles corporate zombies, \n" +
                "      an evil sorceress, and her own childhood to become queen \n" +
                "      of the world.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk103\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Maeve Ascendant</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-11-17</publish_date>\n" +
                "      <description>After the collapse of a nanotechnology \n" +
                "      society in England, the young survivors lay the \n" +
                "      foundation for a new society.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk104\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Oberon's Legacy</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-03-10</publish_date>\n" +
                "      <description>In post-apocalypse England, the mysterious \n" +
                "      agent known only as Oberon helps to create a new life \n" +
                "      for the inhabitants of London. Sequel to Maeve \n" +
                "      Ascendant.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk105\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>The Sundered Grail</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-09-10</publish_date>\n" +
                "      <description>The two daughters of Maeve, half-sisters, \n" +
                "      battle one another for control of England. Sequel to \n" +
                "      Oberon's Legacy.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk106\">\n" +
                "      <author>Randall, Cynthia</author>\n" +
                "      <title>Lover Birds</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-09-02</publish_date>\n" +
                "      <description>When Carla meets Paul at an ornithology \n" +
                "      conference, tempers fly as feathers get ruffled.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk107\">\n" +
                "      <author>Thurman, Paula</author>\n" +
                "      <title>Splish Splash</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>A deep sea diver finds true love twenty \n" +
                "      thousand leagues beneath the sea.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk108\">\n" +
                "      <author>Knorr, Stefan</author>\n" +
                "      <title>Creepy Crawlies</title>\n" +
                "      <genre>Horror</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-12-06</publish_date>\n" +
                "      <description>An anthology of horror stories about roaches,\n" +
                "      centipedes, scorpions  and other insects.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk109\">\n" +
                "      <author>Kress, Peter</author>\n" +
                "      <title>Paradox Lost</title>\n" +
                "      <genre>Science Fiction</genre>\n" +
                "      <price>6.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>After an inadvertant trip through a Heisenberg\n" +
                "      Uncertainty Device, James Salway discovers the problems \n" +
                "      of being quantum.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk110\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>Microsoft .NET: The Programming Bible</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-09</publish_date>\n" +
                "      <description>Microsoft's .NET initiative is explored in \n" +
                "      detail in this deep programmer's reference.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk111\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>MSXML3: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-01</publish_date>\n" +
                "      <description>The Microsoft MSXML3 parser is covered in \n" +
                "      detail, with attention to XML DOM interfaces, XSLT processing, \n" +
                "      SAX and more.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk112\">\n" +
                "      <author>Galos, Mike</author>\n" +
                "      <title>Visual Studio 7: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>49.95</price>\n" +
                "      <publish_date>2001-04-16</publish_date>\n" +
                "      <description>Microsoft Visual Studio 7 is explored in depth,\n" +
                "      looking at how Visual Basic, Visual C++, C#, and ASP+ are \n" +
                "      integrated into a comprehensive development \n" +
                "      environment.</description>\n" +
                "   </book>\n" +
                "</catalog>";
        Reader reader = new StringReader(xmlStr);
        JSONObject obj = XML.toJSONObject(reader);

        String expectedOutput ="[Gambardella, Matthew, Ralls, Kim, Corets, Eva, Randall, Cynthia, Thurman, Paula, Knorr, Stefan, Kress, Peter, O'Brien, Tim, Galos, Mike]";
        List<String> authors = obj.toStream()
		.filter(node ->(node.getKey().equals("book")))
		.filter(node -> !(node.getValue() instanceof JSONArray))
		.map(node ->((String)((JSONObject)node.getValue()).get("author")))
		.distinct()
		.collect(Collectors.toList());

        assertEquals(expectedOutput,authors.toString());
    }
    @Test
    public void testToStreamGetKeys(){
        InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
        Reader xmlReader = new InputStreamReader(xmlStream);
        JSONObject obj = XML.toJSONObject(xmlReader);

        String expectedOutput ="[clinical_study, brief_summary, eligibility, study_pop, criteria, location_countries, study_design_info, intervention_browse, primary_completion_date, sponsors, lead_sponsor, collaborator, overall_official, overall_contact_backup, condition_browse, mesh_term, overall_contact, responsible_party, start_date, study_first_posted, arm_group, primary_outcome, secondary_outcome, oversight_info, last_update_posted, id_info, enrollment, condition, required_header, completion_date, location, contact, facility, address, contact_backup, intervention, arm_group_label, patient_data]";
        List<String> paths = obj.toStream()
                .map(node -> node.getKey())
                .distinct()
                .collect(Collectors.toList());

        assertEquals(expectedOutput,  paths.toString());
    }

    @Test
    public void testToStreamTransformKeys(){
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<catalog>\n" +
                "   <book id=\"bk101\">\n" +
                "      <author>Gambardella, Matthew</author>\n" +
                "      <title>XML Developer's Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>44.95</price>\n" +
                "      <publish_date>2000-10-01</publish_date>\n" +
                "      <description>An in-depth look at creating applications \n" +
                "      with XML.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk102\">\n" +
                "      <author>Ralls, Kim</author>\n" +
                "      <title>Midnight Rain</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-12-16</publish_date>\n" +
                "      <description>A former architect battles corporate zombies, \n" +
                "      an evil sorceress, and her own childhood to become queen \n" +
                "      of the world.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk103\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Maeve Ascendant</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2000-11-17</publish_date>\n" +
                "      <description>After the collapse of a nanotechnology \n" +
                "      society in England, the young survivors lay the \n" +
                "      foundation for a new society.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk104\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>Oberon's Legacy</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-03-10</publish_date>\n" +
                "      <description>In post-apocalypse England, the mysterious \n" +
                "      agent known only as Oberon helps to create a new life \n" +
                "      for the inhabitants of London. Sequel to Maeve \n" +
                "      Ascendant.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk105\">\n" +
                "      <author>Corets, Eva</author>\n" +
                "      <title>The Sundered Grail</title>\n" +
                "      <genre>Fantasy</genre>\n" +
                "      <price>5.95</price>\n" +
                "      <publish_date>2001-09-10</publish_date>\n" +
                "      <description>The two daughters of Maeve, half-sisters, \n" +
                "      battle one another for control of England. Sequel to \n" +
                "      Oberon's Legacy.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk106\">\n" +
                "      <author>Randall, Cynthia</author>\n" +
                "      <title>Lover Birds</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-09-02</publish_date>\n" +
                "      <description>When Carla meets Paul at an ornithology \n" +
                "      conference, tempers fly as feathers get ruffled.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk107\">\n" +
                "      <author>Thurman, Paula</author>\n" +
                "      <title>Splish Splash</title>\n" +
                "      <genre>Romance</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>A deep sea diver finds true love twenty \n" +
                "      thousand leagues beneath the sea.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk108\">\n" +
                "      <author>Knorr, Stefan</author>\n" +
                "      <title>Creepy Crawlies</title>\n" +
                "      <genre>Horror</genre>\n" +
                "      <price>4.95</price>\n" +
                "      <publish_date>2000-12-06</publish_date>\n" +
                "      <description>An anthology of horror stories about roaches,\n" +
                "      centipedes, scorpions  and other insects.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk109\">\n" +
                "      <author>Kress, Peter</author>\n" +
                "      <title>Paradox Lost</title>\n" +
                "      <genre>Science Fiction</genre>\n" +
                "      <price>6.95</price>\n" +
                "      <publish_date>2000-11-02</publish_date>\n" +
                "      <description>After an inadvertant trip through a Heisenberg\n" +
                "      Uncertainty Device, James Salway discovers the problems \n" +
                "      of being quantum.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk110\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>Microsoft .NET: The Programming Bible</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-09</publish_date>\n" +
                "      <description>Microsoft's .NET initiative is explored in \n" +
                "      detail in this deep programmer's reference.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk111\">\n" +
                "      <author>O'Brien, Tim</author>\n" +
                "      <title>MSXML3: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>36.95</price>\n" +
                "      <publish_date>2000-12-01</publish_date>\n" +
                "      <description>The Microsoft MSXML3 parser is covered in \n" +
                "      detail, with attention to XML DOM interfaces, XSLT processing, \n" +
                "      SAX and more.</description>\n" +
                "   </book>\n" +
                "   <book id=\"bk112\">\n" +
                "      <author>Galos, Mike</author>\n" +
                "      <title>Visual Studio 7: A Comprehensive Guide</title>\n" +
                "      <genre>Computer</genre>\n" +
                "      <price>49.95</price>\n" +
                "      <publish_date>2001-04-16</publish_date>\n" +
                "      <description>Microsoft Visual Studio 7 is explored in depth,\n" +
                "      looking at how Visual Basic, Visual C++, C#, and ASP+ are \n" +
                "      integrated into a comprehensive development \n" +
                "      environment.</description>\n" +
                "   </book>\n" +
                "</catalog>";
        Reader reader = new StringReader(xmlStr);
        JSONObject obj = XML.toJSONObject(reader);

        String expectedOutput ="[Node key : book_sweP\n" +
                "Node value : [{\"author\":\"Gambardella, Matthew\",\"price\":44.95,\"genre\":\"Computer\",\"description\":\"An in-depth look at creating applications \\n      with XML.\",\"id\":\"bk101\",\"title\":\"XML Developer's Guide\",\"publish_date\":\"2000-10-01\"},{\"author\":\"Ralls, Kim\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"A former architect battles corporate zombies, \\n      an evil sorceress, and her own childhood to become queen \\n      of the world.\",\"id\":\"bk102\",\"title\":\"Midnight Rain\",\"publish_date\":\"2000-12-16\"},{\"author\":\"Corets, Eva\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"After the collapse of a nanotechnology \\n      society in England, the young survivors lay the \\n      foundation for a new society.\",\"id\":\"bk103\",\"title\":\"Maeve Ascendant\",\"publish_date\":\"2000-11-17\"},{\"author\":\"Corets, Eva\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"In post-apocalypse England, the mysterious \\n      agent known only as Oberon helps to create a new life \\n      for the inhabitants of London. Sequel to Maeve \\n      Ascendant.\",\"id\":\"bk104\",\"title\":\"Oberon's Legacy\",\"publish_date\":\"2001-03-10\"},{\"author\":\"Corets, Eva\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"The two daughters of Maeve, half-sisters, \\n      battle one another for control of England. Sequel to \\n      Oberon's Legacy.\",\"id\":\"bk105\",\"title\":\"The Sundered Grail\",\"publish_date\":\"2001-09-10\"},{\"author\":\"Randall, Cynthia\",\"price\":4.95,\"genre\":\"Romance\",\"description\":\"When Carla meets Paul at an ornithology \\n      conference, tempers fly as feathers get ruffled.\",\"id\":\"bk106\",\"title\":\"Lover Birds\",\"publish_date\":\"2000-09-02\"},{\"author\":\"Thurman, Paula\",\"price\":4.95,\"genre\":\"Romance\",\"description\":\"A deep sea diver finds true love twenty \\n      thousand leagues beneath the sea.\",\"id\":\"bk107\",\"title\":\"Splish Splash\",\"publish_date\":\"2000-11-02\"},{\"author\":\"Knorr, Stefan\",\"price\":4.95,\"genre\":\"Horror\",\"description\":\"An anthology of horror stories about roaches,\\n      centipedes, scorpions  and other insects.\",\"id\":\"bk108\",\"title\":\"Creepy Crawlies\",\"publish_date\":\"2000-12-06\"},{\"author\":\"Kress, Peter\",\"price\":6.95,\"genre\":\"Science Fiction\",\"description\":\"After an inadvertant trip through a Heisenberg\\n      Uncertainty Device, James Salway discovers the problems \\n      of being quantum.\",\"id\":\"bk109\",\"title\":\"Paradox Lost\",\"publish_date\":\"2000-11-02\"},{\"author\":\"O'Brien, Tim\",\"price\":36.95,\"genre\":\"Computer\",\"description\":\"Microsoft's .NET initiative is explored in \\n      detail in this deep programmer's reference.\",\"id\":\"bk110\",\"title\":\"Microsoft .NET: The Programming Bible\",\"publish_date\":\"2000-12-09\"},{\"author\":\"O'Brien, Tim\",\"price\":36.95,\"genre\":\"Computer\",\"description\":\"The Microsoft MSXML3 parser is covered in \\n      detail, with attention to XML DOM interfaces, XSLT processing, \\n      SAX and more.\",\"id\":\"bk111\",\"title\":\"MSXML3: A Comprehensive Guide\",\"publish_date\":\"2000-12-01\"},{\"author\":\"Galos, Mike\",\"price\":49.95,\"genre\":\"Computer\",\"description\":\"Microsoft Visual Studio 7 is explored in depth,\\n      looking at how Visual Basic, Visual C++, C#, and ASP+ are \\n      integrated into a comprehensive development \\n      environment.\",\"id\":\"bk112\",\"title\":\"Visual Studio 7: A Comprehensive Guide\",\"publish_date\":\"2001-04-16\"}]\n" +
                "Node path : /catalog/book\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Gambardella, Matthew\",\"price\":44.95,\"genre\":\"Computer\",\"description\":\"An in-depth look at creating applications \\n      with XML.\",\"id\":\"bk101\",\"title\":\"XML Developer's Guide\",\"publish_date\":\"2000-10-01\"}\n" +
                "Node path : /catalog/book/0\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Ralls, Kim\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"A former architect battles corporate zombies, \\n      an evil sorceress, and her own childhood to become queen \\n      of the world.\",\"id\":\"bk102\",\"title\":\"Midnight Rain\",\"publish_date\":\"2000-12-16\"}\n" +
                "Node path : /catalog/book/1\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Corets, Eva\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"After the collapse of a nanotechnology \\n      society in England, the young survivors lay the \\n      foundation for a new society.\",\"id\":\"bk103\",\"title\":\"Maeve Ascendant\",\"publish_date\":\"2000-11-17\"}\n" +
                "Node path : /catalog/book/2\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Corets, Eva\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"In post-apocalypse England, the mysterious \\n      agent known only as Oberon helps to create a new life \\n      for the inhabitants of London. Sequel to Maeve \\n      Ascendant.\",\"id\":\"bk104\",\"title\":\"Oberon's Legacy\",\"publish_date\":\"2001-03-10\"}\n" +
                "Node path : /catalog/book/3\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Corets, Eva\",\"price\":5.95,\"genre\":\"Fantasy\",\"description\":\"The two daughters of Maeve, half-sisters, \\n      battle one another for control of England. Sequel to \\n      Oberon's Legacy.\",\"id\":\"bk105\",\"title\":\"The Sundered Grail\",\"publish_date\":\"2001-09-10\"}\n" +
                "Node path : /catalog/book/4\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Randall, Cynthia\",\"price\":4.95,\"genre\":\"Romance\",\"description\":\"When Carla meets Paul at an ornithology \\n      conference, tempers fly as feathers get ruffled.\",\"id\":\"bk106\",\"title\":\"Lover Birds\",\"publish_date\":\"2000-09-02\"}\n" +
                "Node path : /catalog/book/5\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Thurman, Paula\",\"price\":4.95,\"genre\":\"Romance\",\"description\":\"A deep sea diver finds true love twenty \\n      thousand leagues beneath the sea.\",\"id\":\"bk107\",\"title\":\"Splish Splash\",\"publish_date\":\"2000-11-02\"}\n" +
                "Node path : /catalog/book/6\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Knorr, Stefan\",\"price\":4.95,\"genre\":\"Horror\",\"description\":\"An anthology of horror stories about roaches,\\n      centipedes, scorpions  and other insects.\",\"id\":\"bk108\",\"title\":\"Creepy Crawlies\",\"publish_date\":\"2000-12-06\"}\n" +
                "Node path : /catalog/book/7\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Kress, Peter\",\"price\":6.95,\"genre\":\"Science Fiction\",\"description\":\"After an inadvertant trip through a Heisenberg\\n      Uncertainty Device, James Salway discovers the problems \\n      of being quantum.\",\"id\":\"bk109\",\"title\":\"Paradox Lost\",\"publish_date\":\"2000-11-02\"}\n" +
                "Node path : /catalog/book/8\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"O'Brien, Tim\",\"price\":36.95,\"genre\":\"Computer\",\"description\":\"Microsoft's .NET initiative is explored in \\n      detail in this deep programmer's reference.\",\"id\":\"bk110\",\"title\":\"Microsoft .NET: The Programming Bible\",\"publish_date\":\"2000-12-09\"}\n" +
                "Node path : /catalog/book/9\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"O'Brien, Tim\",\"price\":36.95,\"genre\":\"Computer\",\"description\":\"The Microsoft MSXML3 parser is covered in \\n      detail, with attention to XML DOM interfaces, XSLT processing, \\n      SAX and more.\",\"id\":\"bk111\",\"title\":\"MSXML3: A Comprehensive Guide\",\"publish_date\":\"2000-12-01\"}\n" +
                "Node path : /catalog/book/10\n" +
                ", Node key : book_sweP\n" +
                "Node value : {\"author\":\"Galos, Mike\",\"price\":49.95,\"genre\":\"Computer\",\"description\":\"Microsoft Visual Studio 7 is explored in depth,\\n      looking at how Visual Basic, Visual C++, C#, and ASP+ are \\n      integrated into a comprehensive development \\n      environment.\",\"id\":\"bk112\",\"title\":\"Visual Studio 7: A Comprehensive Guide\",\"publish_date\":\"2001-04-16\"}\n" +
                "Node path : /catalog/book/11\n" +
                "]";
        Function<JSONNode, JSONNode> func =x -> {
            JSONNode modified = new JSONNode(x.getKey()+"_sweP",x.getValue(),x.getPath());
										return modified;};
       List<JSONNode> nodes = obj.toStream()
		.filter(node -> node.getKey().equals("book"))
		.map(func)
		.collect(Collectors.toList());
       assertEquals(expectedOutput,nodes.toString());
    }

    @Test
    public void testToStreamReplaceAtPath(){
        InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
        Reader xmlReader = new InputStreamReader(xmlStream);
        JSONObject obj = XML.toJSONObject(xmlReader);

        JSONObject newObject = new JSONObject();
		newObject.put("University", "UCI");
		newObject.put("School", "ICS");
		newObject.put("Program", "MSWE");
		newObject.put("Year", 2021);

		AtomicReference<String> resultBefore = new AtomicReference<String>();
        AtomicReference<String> resultAfter = new AtomicReference<String>();
		obj.toStream()
                .forEach(entry -> {
                            if ((entry.getPath()).equals("/clinical_study/secondary_outcome/0")) {
                                resultBefore.set(entry.getKey() + ": " + entry.getValue() + "\n");
                                resultAfter.set(entry.getKey() + ": " + newObject + "\n");
                            }
                        }
                );

        assertNotEquals(resultBefore,resultAfter);
    }

}
