package twitter;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.amazonaws.services.sns.AmazonSNSClient;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class twitterStreaming {
	static SimpleQueueService tweetSQS;
	private static keywords keywordHelper = new keywords();
	
	public static void main(String[] args) throws JSONException {
//        twitterStreaming fetcher = new twitterStreaming();
//        fetcher.fetchTwits();
        tweetSQS = new SimpleQueueService();
        tweetSQS.init();
        
        ConfigurationBuilder cb = new ConfigurationBuilder();
        
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("62G2zLGAKXM25TUyXeGJgNZpY")
                .setOAuthConsumerSecret("XecKmb0ifHlhO8Y5qn0CKH7tr2orDPuObDDdTOtWX9apFw9qUT")
                .setOAuthAccessToken("2989290723-lHgusR0oVRtvMam5JTK35noAyjucMcEzdUBzstY")
                .setOAuthAccessTokenSecret("GTzDl9kAq9Wni1lAOdPJ7HJJC6T2BjonhCVrtqJ3O1e5E");

        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();       
        StatusListener listener = new StatusListener() {
        	
            @Override
            public void onStatus(Status status) {
            	
				GeoLocation loc = status.getGeoLocation();
                if (loc != null && status.getLang().equals("en")) {
                    //System.out.print(status);
                    String latitude = loc.getLatitude()+"";
					String longitude = loc.getLongitude()+"";
					String usr = status.getUser().getScreenName();
					String index = keywordHelper.keyword(status.getText());
					String content = status.getText().replaceAll("\u005c\u0022", "");
					content = content.replaceAll("(\\r|\\n|\\r\\n)+", "");
					String time = null;
					try {
						time = dateConvert( status.getCreatedAt().toString() );
					} catch (ParseException e) { };

					tweetSQS.putInQueue(usr, content, time, latitude, longitude, index);
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };

        twitterStream.addListener(listener);
        twitterStream.sample();
        
        SNSClient snsclient = new SNSClient();
		AmazonSNSClient amazonSNSClient =snsclient.ini();
		System.out.println(amazonSNSClient.toString());
		//SNSServlet snsservlet = new SNSServlet();
		Worker wk = new Worker();
		for(;;){			
			String temp =wk.getResult();
			if (temp != null){
				JSONObject jsonObj = new JSONObject(temp);
				String senti =jsonObj.get("Sentiment").toString();
				if ( senti.equals("positive") ||senti.equals("negative")||senti.equals("neutral") ){	
					//System.out.println(temp);
					snsclient.publish(amazonSNSClient, temp);
				}
			}
		} 		
    }
    
    public static String dateConvert(String date) throws ParseException{
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
		Date parsedDate = dateFormat.parse(date);
		Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
		return timestamp.toString();
	}

    
}
