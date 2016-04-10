package twitter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.sqs.model.Message;

public class Worker implements Runnable{

	volatile static String finalResult;
	volatile static SimpleQueueService tq = new SimpleQueueService();
	
	public Worker(){	
	}
	
	public String getResult(){
		Thread t = new Thread(new Worker());
		t.start();
		
		try {
			Thread.sleep(1000);
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return finalResult;
	}
	@Override
	public void run() {
		Message msg = tq.getFromQueue();
		if(msg != null){
			tq.deleteMes(msg.getReceiptHandle());
			String rsvTwt = msg.getBody().replaceAll("\\\\", "");
			System.out.println("rsvTwt:" +rsvTwt);
			String content = null;
			String sentmt = "unknown";
			String tmpResult = null;
			if(rsvTwt!=null){		
				try {
					content = this.extractContent(rsvTwt);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				sentmt = extractSentiment(content);
				
				rsvTwt = rsvTwt.substring(0, rsvTwt.length()-1);
				tmpResult = rsvTwt + ",\"Sentiment\":\""+sentmt+"\"}";
				//tmpResult = rsvTwt + ",\"Sentiment\": \"positive\" }";
				finalResult = tmpResult;
			}
		}
	}
	//extract sentiment from raw sentiment result. e.g.: positive, negative, neutral
	public String extractSentiment(String rTwt){
		String rawSenti = "not appliable";
		StringTokenizer st = null;
		String tmpStr = "unknown";
		try {
			rawSenti = this.httpGet(rTwt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//extract<type>
		if(rawSenti != "not appliable"){
			st = new StringTokenizer(rawSenti,"\n ");
			while(st.hasMoreTokens()){
				tmpStr = st.nextToken();
				if(tmpStr.contains("<type>")){
					tmpStr = tmpStr.substring(6, tmpStr.length()-7);
					break;
				}
			}
		}
		return tmpStr;
	}
	
	//extract Content from JSON string
	public String extractContent(String rTwt) throws JSONException{
		String content = null;
		JSONObject json = new JSONObject(rTwt);
		content = json.get("Content").toString();
			return content;
	}
	
	public String httpGet(String text) throws IOException {
		String urlStr = "http://gateway-a.watsonplatform.net/calls/text/TextGetTextSentiment?apikey=d1e88d02ad22edbb07517814dbc9e94dcb7fee5e&text="+text;
		String rawSentiment = null;
		URL url = new URL(urlStr);
		URI uri = null;
		try {
			uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		url = uri.toURL();  

		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url.toString());
		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		rawSentiment = EntityUtils.toString(entity);
		
		return rawSentiment;
		}
}

