package in.net.maitri.parkingticket.bluetooth;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

public class BPrintService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	public void createBitmap(final Activity act, Context con, final File file, String content){
		final WebView webView = new WebView(con);
		webView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(final WebView view, String url)
			{
				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						if(webView.getWidth()>0&&webView.getHeight()>0){
							act.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									//capture webview and create bitmap
									Toast.makeText(getApplicationContext(), "Creating bitmap", Toast.LENGTH_SHORT).show();
									//createBitmap(webView,file,view);								
								}
							});
						}
					}
				}, 1500);				
			}
		});

		int pageWidthInPixels = (int) Math.round(48*8);

		webView.setLayoutParams(new LinearLayout.LayoutParams(pageWidthInPixels, LayoutParams.WRAP_CONTENT));
		webView.getSettings().setAllowFileAccess(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
	}
}
