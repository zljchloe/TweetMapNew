package twitter;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;

public class SNSClient {

	
	public AmazonSNSClient ini()
	{
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
		AmazonSNSClient amazonSNSClient = new AmazonSNSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		amazonSNSClient.setRegion(usWest2);
		return amazonSNSClient;
	}
	public void publish(AmazonSNSClient amazonSNSClient, String message)
	{	
		System.out.println("*************");
		amazonSNSClient.publish("arn:aws:sns:us-west-2:287308070985:tweet", message);
	}
	

}
