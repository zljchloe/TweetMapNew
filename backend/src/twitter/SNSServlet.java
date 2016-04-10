package twitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

import sns.SNSHelper;
import sns.SNSMessage;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import twitter4j.Status;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class SNSServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	/**
	 * @throws SQLException 
	 * @see HttpServlet#HttpServlet()
	 */
	public SNSServlet() {
		super();
//		String driver = "com.mysql.jdbc.Driver";
//		try {
//			Class.forName(driver);
//		} catch (ClassNotFoundException e1) {
//			e1.printStackTrace();
//		}        
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("haha");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SecurityException{
		System.out.println("DOPOST!!!!!");
		//Get the message type header.
		String messagetype = request.getHeader("x-amz-sns-message-type");
		//If message doesn't have the message type header, don't process it.
		if (messagetype == null)
			return;

		// Parse the JSON message in the message body
		// and hydrate a Message object with its contents 
		// so that we have easy access to the name/value pairs 
		// from the JSON message.
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(request.getInputStream());
		StringBuilder builder = new StringBuilder();
		while (scan.hasNextLine()) {
			builder.append(scan.nextLine());
		}

	  SNSMessage msg = readMessageFromJson(builder.toString());


		// The signature is based on SignatureVersion 1. 
		// If the sig version is something other than 1, 
		// throw an exception.
		if (msg.getSignatureVersion().equals("1")) {
			// Check the signature and throw an exception if the signature verification fails.
			if (isMessageSignatureValid(msg))
				System.out.println(">>Signature verification succeeded");
			else {
				System.out.println(">>Signature verification failed");
				throw new SecurityException("Signature verification failed.");
			}
		}
		else {
			System.out.println(">>Unexpected signature version. Unable to verify signature.");
			throw new SecurityException("Unexpected signature version. Unable to verify signature.");
		}

		// Process the message based on type.
		if (messagetype.equals("Notification")) {
			String logMsgAndSubject = ">>Notification received from topic " + msg.getTopicArn();
			if (msg.getSubject() != null)
				logMsgAndSubject += " Subject: " + msg.getSubject();
			logMsgAndSubject += " Message: " + msg.getMessage();
		//	System.out.println(logMsgAndSubject);
			
			JSONObject jsonObj = null;
			try {
				jsonObj = new JSONObject(msg.getMessage());
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			keywords keywordHelper = new keywords();
			String index = null;
			try {
				index = keywordHelper.keyword(jsonObj.get("Content").toString());
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        JestClient client = null;
	        
	        if(index != null) {
	            try {
	                JestClientFactory factory = new JestClientFactory();
	                factory.setHttpClientConfig(new HttpClientConfig
	                        .Builder("search-cloud-assignment1-pz2210-dh6pge4mqegru4g4zgv7p74f34.us-west-2.es.amazonaws.com")
	                        .multiThreaded(true)
	                        .build());
	                client = factory.getObject();
	
	                String source = jsonBuilder()
	                        .startObject()
	                        .field("user", jsonObj.get("Usr").toString())
	                        .field("timestamp", jsonObj.get("Time").toString())
	                        .field("text", jsonObj.get("Content").toString())
	                        .field("keyword",index)
	                        .field("latitude", jsonObj.get("Lat").toString())
	                        .field("longtitude",jsonObj.get("Lon").toString())
	                        .field("sentiment", jsonObj.get("Sentiment").toString())
	                        .endObject().string();
					
	                Index putIndex = new Index.Builder(source).index(index).type("tweet").build();
	                client.execute(putIndex);
	            } catch (UnknownHostException e) {
	                e.printStackTrace();
	            } catch (IOException e) {
	                e.printStackTrace();
	            } catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }

		else if (messagetype.equals("SubscriptionConfirmation"))
		{
			System.out.println("111");
			//TODO: You should make sure that this subscription is from the topic you expect. Compare topicARN to your list of topics 
			//that you want to enable to add this endpoint as a subscription.

			//Confirm the subscription by going to the subscribeURL location 
			//and capture the return value (XML message body as a string)
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(new URL(msg.getSubscribeURL()).openStream());
			StringBuilder sb = new StringBuilder();
			while (sc.hasNextLine()) {
				sb.append(sc.nextLine());
			}
			System.out.println(">>Subscription confirmation (" + msg.getSubscribeURL() +") Return value: " + sb.toString());
			//TODO: Process the return value to ensure the endpoint is subscribed.
			SNSHelper.INSTANCE.confirmTopicSubmission(msg);
		}
		else if (messagetype.equals("UnsubscribeConfirmation")) {
			//TODO: Handle UnsubscribeConfirmation message. 
			//For example, take action if unsubscribing should not have occurred.
			//You can read the SubscribeURL from this message and 
			//re-subscribe the endpoint.
			System.out.println(">>Unsubscribe confirmation: " + msg.getMessage());
		}
		else {
			//TODO: Handle unknown message type.
			System.out.println(">>Unknown message type.");
		}
		System.out.println(">>Done processing message: " + msg.getMessageId());
		}
	}


	private boolean isMessageSignatureValid(SNSMessage msg) {

		try {
			URL url = new URL(msg.getSigningCertUrl());
			InputStream inStream = url.openStream();
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
			inStream.close();

			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(cert.getPublicKey());
			sig.update(getMessageBytesToSign(msg));
			return sig.verify(Base64.decodeBase64(msg.getSignature().getBytes()));
		}
		catch (Exception e) {
			throw new SecurityException("Verify method failed.", e);

		}
	}

	private byte[] getMessageBytesToSign(SNSMessage msg) {

		byte [] bytesToSign = null;
		if (msg.getType().equals("Notification"))
			bytesToSign = buildNotificationStringToSign(msg).getBytes();
		else if (msg.getType().equals("SubscriptionConfirmation") || msg.getType().equals("UnsubscribeConfirmation"))
			bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
		return bytesToSign;
	}

	//Build the string to sign for Notification messages.
	private static String buildNotificationStringToSign( SNSMessage msg) {
		String stringToSign = null;

		//Build the string to sign from the values in the message.
		//Name and values separated by newline characters
		//The name value pairs are sorted by name 
		//in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		if (msg.getSubject() != null) {
			stringToSign += "Subject\n";
			stringToSign += msg.getSubject() + "\n";
		}
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}
	//
	//	//Build the string to sign for SubscriptionConfirmation 
	//	//and UnsubscribeConfirmation messages.
	private static String buildSubscriptionStringToSign(SNSMessage msg) {
		String stringToSign = null;
		//Build the string to sign from the values in the message.
		//Name and values separated by newline characters
		//The name value pairs are sorted by name 
		//in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		stringToSign += "SubscribeURL\n";
		stringToSign += msg.getSubscribeURL() + "\n";
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "Token\n";
		stringToSign += msg.getToken() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}
	//
	//
	private SNSMessage readMessageFromJson(String string) {
		ObjectMapper mapper = new ObjectMapper(); 
		SNSMessage message = null;
		try {
			message = mapper.readValue(string, SNSMessage.class);
		}  catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return message;
	}

}
