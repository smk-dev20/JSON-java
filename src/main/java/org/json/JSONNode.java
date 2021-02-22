package org.json;

/**
 * Wrapper class for JSONObject streaming functionality
 * Every object of JSONNode has the following attributes
 * key   - key in the input JSONObject
 * value - value corresponding to the above key in the JSONObject
 *         (value can be of type JSONObject,JSONArray, String etc - hence wrapped in Object type)
 * path  - Exact path from root node to reach that value
 *
 * Methods getKey(), getValue() and getPath() to retrieve from JSONNode and toString() to print
 * values in a meaningful manner
 */
public class JSONNode {
    private final String key;
    private final Object value;
    private final String path;

    public JSONNode(String k, Object v, String p) {
        this.key = k;
        this.value = v;
        this.path = p;
    }

    public String getKey() {
        return this.key;
    }

    public Object getValue() {
        return this.value;
    }

    public String getPath() {
        return this.path;
    }

    public String toString() {
        return "Node key : "+this.key + "\nNode value : "+this.value.toString()+"\nNode path : "+this.path+"\n" ;
    }
}
