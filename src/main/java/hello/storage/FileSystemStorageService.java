package hello.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import hello.json.JsonSimpleWrite;


@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;
    private static String bucketName = "abnamrobucket";
    private DetectTextResult result;
    ArrayList<String> textDetections = new ArrayList<String>();
    private List<TextDetection> lsttextDetection;
    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }
          
           // Files.copy(file.getInputStream(), this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            
            
            AWSCredentials credentials =  new ProfileCredentialsProvider().getCredentials();
    		
    			AmazonS3 s3client = new AmazonS3Client(credentials);
            try {
                System.out.println("Uploading a new object to S3 from a file\n");
             
                ObjectMetadata objectMetadata = new ObjectMetadata();    
                
                s3client.putObject(new PutObjectRequest(bucketName, filename, file.getInputStream(), objectMetadata));

             } catch (AmazonServiceException ase) {
                System.out.println("Caught an AmazonServiceException, which " +
                		"means your request made it " +
                        "to Amazon S3, but was rejected with an error response" +
                        " for some reason.");
                System.out.println("Error Message:    " + ase.getMessage());
                System.out.println("HTTP Status Code: " + ase.getStatusCode());
                System.out.println("AWS Error Code:   " + ase.getErrorCode());
                System.out.println("Error Type:       " + ase.getErrorType());
                System.out.println("Request ID:       " + ase.getRequestId());
            } catch (AmazonClientException ace) {
                System.out.println("Caught an AmazonClientException, which " +
                		"means the client encountered " +
                        "an internal error while trying to " +
                        "communicate with S3, " +
                        "such as not being able to access the network.");
                System.out.println("Error Message: " + ace.getMessage());
            }finally {
            	
            		textDetections.clear();
	            	for (TextDetection text: this.TextDetectionInImage(credentials, filename)) { 
	            		textDetections.add("Detected: " + text.getDetectedText());     
	            	}
            	
            }
            
            
          
 
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(path -> this.rootLocation.relativize(path));
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

	@Override
	public List<TextDetection> TextDetectionInImage(AWSCredentials credentials, String photo) {
		 AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder
	              .standard()
	              .withRegion(Regions.EU_WEST_1)
	              .withCredentials(new AWSStaticCredentialsProvider(credentials))
	              
	              .build();
	      
	      
	      DetectTextRequest request = new DetectTextRequest()
	              .withImage(new Image()           
	              .withS3Object(new S3Object()
	              .withName(photo)
	              .withBucket(bucketName)));
	    

	      try {
	         result = rekognitionClient.detectText(request);
	         
	         
	         lsttextDetection = result.getTextDetections();
	        
	         System.out.println("Detected lines and words for " + photo);
	         for (TextDetection text: lsttextDetection) {
	      
	                 System.out.println("Detected: " + text.getDetectedText());
	                 System.out.println("Confidence: " + text.getConfidence().toString());
	                 System.out.println("Id : " + text.getId());
	                 System.out.println("Parent Id: " + text.getParentId());
	                 System.out.println("Type: " + text.getType());
	                 System.out.println();
	         }
	        
	        
	         JsonSimpleWrite jw = new JsonSimpleWrite();
	         jw.JsonWrite(lsttextDetection, this.rootLocation);
	         
	      } catch(AmazonRekognitionException e) {
	         e.printStackTrace();
	      }
	      return lsttextDetection;
	      
	     
	}

	@Override
	public ArrayList<String> LoadtTextDetections() {
		 return textDetections;
	}
}
