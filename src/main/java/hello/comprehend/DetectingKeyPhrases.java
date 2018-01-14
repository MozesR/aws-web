package hello.comprehend;

import java.util.HashMap;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesRequest;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesResult;

public class DetectingKeyPhrases {

	String textContent;
	public void Detect(HashMap dText) {
		
		dText.forEach((k,v)-> textContent = textContent + " " + k + " " );
			
			
		String text = textContent;

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAJK5N4ILFWFC3FPOA", "eTh4ZOzhtnxbt2/TgryEYe5+SSS/b7m4GBV0heXi");

        AmazonComprehend comprehendClient = 
            AmazonComprehendClientBuilder.standard()
                                         .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                                         .withRegion(Regions.EU_WEST_1)
                                         .build();

        // Call detectKeyPhrases API
        System.out.println("Calling DetectKeyPhrases");
        DetectKeyPhrasesRequest detectKeyPhrasesRequest = new DetectKeyPhrasesRequest().withText(text)
                                                                                       .withLanguageCode("en");
        DetectKeyPhrasesResult detectKeyPhrasesResult = comprehendClient.detectKeyPhrases(detectKeyPhrasesRequest);
        detectKeyPhrasesResult.getKeyPhrases().forEach(System.out::println);
        System.out.println("End of DetectKeyPhrases\n");
	}
}
