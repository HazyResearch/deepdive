import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Spouses {

    public static void main (String args[]) {

    try {
        JSONParser parser = new JSONParser();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String input;

        while ((input = br.readLine()) != null) {
            JSONObject obj = (JSONObject) parser.parse(input);
            String sid = (String) obj.get("sentence_id");
            String mid = (String) obj.get("mention_id");

            JSONObject out = new JSONObject();
            out.put("sentence_id", sid);
            out.put("person1_id", mid);
            out.put("person2_id", mid + "_!");
            System.out.println(out);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
  }
}
