package twitter;

import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SimpleQueueService {

	AmazonSQS sqs;
	String myQueueUrl;
	public SimpleQueueService(){
		init();
	}
	public void init(){
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (/Users/Liang/.aws/credentials), and is in valid format.",
							e);
		}

		sqs = new AmazonSQSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sqs.setRegion(usWest2);
		CreateQueueRequest createQueueRequest = new CreateQueueRequest("MyQueue");
		myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
	}

	public void putInQueue(String usr, String content, String time, String latitude, String longitude, String index){
		String jsonString ="{ \"Usr\": "+ "\"" +usr+ "\", " +"\"Content\": " + "\""+ content + "\", "+"\"Time\": " + "\""+ time + "\", " + "\"Lat\": " + latitude + ", " + "\"Lon\": "+ longitude + ", " + "\"Index\": " + index + "}";
		sqs.sendMessage(new SendMessageRequest(myQueueUrl, jsonString));

	}
	public Message getFromQueue(){
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
		List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
		for (Message message : messages) {
			return message;
		}
		return null;
	}
	public void deleteMes(String messageRecieptHandle){
		sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageRecieptHandle));
		
	}
	public void deleteQueue(){
		sqs.deleteQueue(new DeleteQueueRequest(myQueueUrl));
	}

	public void countMessage(){
		GetQueueAttributesRequest request = new GetQueueAttributesRequest(myQueueUrl);
		request.withAttributeNames("All");
		Map<String, String> rlt =  sqs.getQueueAttributes(request).getAttributes();            

		System.out.println("number of messages:" + rlt.get("ApproximateNumberOfMessages"));
	}
	
	

}