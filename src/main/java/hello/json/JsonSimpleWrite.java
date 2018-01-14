package hello.json;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.amazonaws.services.rekognition.model.TextDetection;

import hello.comprehend.DetectingKeyPhrases;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonSimpleWrite {

	private String startJson="[";
	private String endJson="]";
	private String midJson=" ";
	private String finJson;
	
	
	public void JsonWrite(List<TextDetection> TextDetections,Path rootLocation) {
		
		 HashMap<String, Integer> hmap = new HashMap<String, Integer>();
	
		 for (TextDetection text: TextDetections) {
			 
			 if (!hmap.containsValue(text.getConfidence().toString())) {
				 hmap.put(text.getDetectedText(), 1);
			 } else {
				 hmap.put(text.getDetectedText(), +1);
			 }
		 }
		 
		 DetectingKeyPhrases dkp = new DetectingKeyPhrases();
		 dkp.Detect(hmap);
		 
		 hmap.forEach((k,v)-> midJson = midJson + "[\"" + k +"\",\"" + v +"\"],");
	
		
		 
		 finJson = startJson + midJson + endJson;
		 	 
		  try (FileWriter file = new FileWriter(rootLocation+"/test.json")) {

	            file.write(finJson.replace("],]", "]]"));
	            file.flush();

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		
	}
}
