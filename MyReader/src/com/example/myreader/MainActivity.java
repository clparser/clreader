package com.example.myreader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.graphics.Bitmap;

public class MainActivity extends Activity {

	public class DownloadFile extends AsyncTask<Void, Integer, String> {
		@Override
		protected String doInBackground(Void... params) {
			try {
				Matcher m=regex.matcher(curContent);
				if (!m.find()){
					return "fail! no resource found!";
				}
				String btlink=m.group();
				String reff="";
				String hash=btlink.split("=")[1];
				publishProgress(10);

				String downloadurl="http://"+btlink.split("/")[2]+"/download.php";
				Log.e("", "link is:"+btlink);
				HttpGet httpRequest = new HttpGet(btlink);
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse httpResponse = httpclient.execute(httpRequest);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String cnt = EntityUtils.toString(httpResponse.getEntity());
					reff=cnt.split("NAME=\"reff\" value=\"")[1].split("\"")[0];
					reff=reff.replace("=", "%3D");
					publishProgress(50);
					if (reff.length()<=0){
						return "fail! get hashcode fail!";
					}
				}
				else{
					return "fail! get hashcode connection fail!";
				}
				
				
				boolean mExternalStorageWriteable = false;
				String state = Environment.getExternalStorageState();

				if (Environment.MEDIA_MOUNTED.equals(state)) {
				    // We can read and write the media
				    mExternalStorageWriteable = true;
				} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				    mExternalStorageWriteable = false;
				} else {
				    mExternalStorageWriteable = false;
				}
				
				if (!mExternalStorageWriteable) return "fail! not writable!";
				
				String btdown=downloadurl;
				Log.e("", "bt file:"+btdown);
	            URL url = new URL(btdown);
	            URLConnection connection = url.openConnection();
	            
	            connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                OutputStreamWriter request = new OutputStreamWriter(connection.getOutputStream());
                request.write("ref="+hash+"&reff="+reff);
                request.flush();
                request.close();            
	            // download the file
	            InputStream input = connection.getInputStream();
	            File f = getExternalCacheDir();
	            String btpath=f.getAbsolutePath()+"/"+hash+".torrent";
	            OutputStream output = new FileOutputStream(btpath);

	            byte data[] = new byte[1024];
	            int count;
	            while ((count = input.read(data)) != -1) {
	                output.write(data, 0, count);
	            }
	            output.flush();
	            output.close();
	            input.close();
				publishProgress(111);
	            
	            Intent i = new Intent();
                i.setAction(android.content.Intent.ACTION_VIEW);
                File bt = new File(btpath);
                i.setDataAndType(Uri.fromFile(bt), "application/x-bittorrent");
                startActivity(i);
	            
	        } catch (Exception en) {
	        	Log.e("", "",en);
	        }
	        return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mProgressDialog.dismiss();
			if ((result!=null) && (result.trim().length()>0)){
				Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);
		}
		
	}

	public String curContent;

	String regex_script = "(http://[^\\? ]*\\?hash=[0-9a-zA-Z]*)";
	Pattern regex = Pattern.compile(regex_script);
	ProgressDialog mProgressDialog;

	private String startpage="http://t66y.com/index.php";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		WebView wv=(WebView) this.findViewById(R.id.idweb);
		
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("正在下载...");
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setMax(111);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		
		boolean clientok=true;
		try{
        	getPackageManager().getLaunchIntentForPackage("com.AndroidA.DroiDownloader");
		}
		catch (Exception e){
			clientok=false;
		}

		
		class MyJavaScriptInterface 
        { 
            @SuppressWarnings("unused") 
			@JavascriptInterface
            public void processContent(String aContent) 
            { 
                curContent = aContent;
            }

            @SuppressWarnings("unused") 
			@JavascriptInterface
            public void processClear() 
            { 
                curContent = "";
            }
        } 

		wv.getSettings().setJavaScriptEnabled(true);
		wv.addJavascriptInterface(new MyJavaScriptInterface(), "INTERFACE"); 
        wv.setWebViewClient(new WebViewClient() { 
            @Override 
            public void onPageFinished(WebView view, String url) 
            { 
                view.loadUrl("javascript:INTERFACE.processContent(document.getElementsByTagName('body')[0].innerText);"); 
            } 
            @Override 
            public void onPageStarted(WebView view, String url, Bitmap favicon) 
            { 
                view.loadUrl("javascript:INTERFACE.processClear();"); 
            } 
            
            @Override 
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("market://")) {
                    view.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        }); 
        
        boolean gaeok=true;
        try{
        	getPackageManager().getLaunchIntentForPackage("org.gaeproxy");
        }
        catch (Exception e){
        	gaeok=false;
        }
        

		//wv.loadUrl("http://t66y.com/index.php");
		//wv.loadUrl("http://www.baidu.com");
        String welcome="<body><div align='center'><p>Welcome, you should know that this app must be used only when you are more than 18 years old!</p>";
        if (!clientok) welcome+="<p>This app need BT client installed but you have not, we suggest you install one by <a href='market://details?id=com.AndroidA.DroiDownloader'>click this</a></p>";
        if (!gaeok) welcome+="You'd better install <a href='market://details?id=org.gaeproxy'>this famouse proxy program</a> to avoid connection fail!";
        welcome+="<p><a href='"+startpage+"'><h1>Yes and Enter</h1></a></p>";
        welcome+="</div></body>";
		wv.loadData(welcome, "text/html", "UTF-8");
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId())//得到被点击的item的itemId
        {
        case R.id.action_settings: //对应的ID就是在add方法中所设定的Id
        	processDownload();
            break;
        }
        return true;
    }
	
	private void processDownload() {
		DownloadFile downloadFile = new DownloadFile();
		downloadFile.execute();
	}

	@Override
	public void onBackPressed(){
		WebView wv=(WebView) this.findViewById(R.id.idweb);
		if ((wv.getUrl().startsWith(startpage)) || (!wv.canGoBack())){
			super.onBackPressed();
		}
		else{			
			wv.goBack();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
