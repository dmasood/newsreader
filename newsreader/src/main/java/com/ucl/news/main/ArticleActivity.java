package com.ucl.news.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ucl.news.adapters.ViewPagerAdapter;
import com.ucl.news.api.ArticleDAO;
import com.ucl.news.api.LoggingReadingBehavior;
import com.ucl.news.api.LoggingReadingScroll;
import com.ucl.news.articles.ArticleWebView;
import com.ucl.news.articles.ArticleWebView.OnBottomReachedListener;
import com.ucl.news.dao.ArticleMetaDataDAO;
import com.ucl.news.reader.RSSItems;
import com.ucl.news.utils.AutoLogin;
import com.ucl.newsreader.R;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;

public class ArticleActivity extends AppCompatActivity implements
		OnBottomReachedListener {

	private ArticleWebView webView;
	private ProgressBar progressBarArticle;
	private ArticleDAO aDAO;
	public static ArrayList<ArticleMetaDataDAO> articleMetaData;
	private long startReading;
	private long endReading;
	public static long articleID;
	private static int numberOfWordsInArticle;
	private static Boolean isScrollUsed = false;
	private Boolean isScrollReachedBottom = false;
	public static final String UPDATE = "com.ucl.news.main.ArticleActivity.action.UPDATE";
	public static final String MSG_SEND = "com.ucl.news.main.ArticleActivity.MSG_SEND";

	public void setArticleMetaDataDAO(ArticleMetaDataDAO amdDAO) {
		articleMetaData.add(amdDAO);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.articles);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarArticleActivity);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);

		progressBarArticle = (ProgressBar) findViewById(R.id.progressBarArticleActivity);
		progressBarArticle.setVisibility(View.VISIBLE);

		TextView headerTitle = (TextView) findViewById(R.id.headerArticleTitle);

		ImageView mbtnBackButton = (ImageView) findViewById(R.id.back_button_image);
		mbtnBackButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		final RSSItems rss = getIntent().getParcelableExtra(
				ViewPagerAdapter.EXTRA_MESSAGE);

		headerTitle.setText(rss.getTitle());

		// Change the url to mobile version
		String rssLink = rss.getLink();
		String mLinkURL = rssLink.replaceAll("www", "m");



		//System.out.println("RSS LINK: " + rss.getLink());
		System.out.println("M LINK: " + mLinkURL);
		new ParseHTML().execute(mLinkURL);

		Log.e("RESULT READING Scroll Enter Activity", "test");
		aDAO = new ArticleDAO();
		articleMetaData = new ArrayList<ArticleMetaDataDAO>();

		articleID = extractArticleID(rss.getLink());

		aDAO.setUserID(AutoLogin.getUserID(AutoLogin
				.getSettingsFile(getApplicationContext())));
		aDAO.setUserSession(AutoLogin.getUserSession(AutoLogin
				.getSettingsFile(getApplicationContext())));
		aDAO.setArticleID(articleID);
		aDAO.setArticleName(rss.getTitle());
		aDAO.setArticleURL(rss.getLink());


		startReading = new Date().getTime();
		aDAO.setStartTimestamp(startReading);
	}

	private long extractArticleID(String articleURL) {

		String[] URLTokens;
		/*
		 * Parse it as sport article.
		 */
		if (articleURL.contains("/sport/0/")) {

			URLTokens = articleURL.split("/");
		} else {
			String[] URLTokensTemp = articleURL.split("#");
			if (URLTokensTemp[0].contains("-"))
				URLTokens = URLTokensTemp[0].split("-");
			else
				URLTokens = URLTokensTemp[0].split("/");
		}

		// for(int i = 0; i < URLTokens.length; i++)
		// System.out.println("storyID: " + URLTokens[URLTokens.length - 1]);
		return Long.parseLong(URLTokens[URLTokens.length - 1]);
	}

	@Override
	public void onBackPressed() {

		super.onBackPressed();

		endReading = new Date().getTime();
		aDAO.setEndTimestamp(endReading);
		aDAO.setReadingDuration(TimeUnit.MILLISECONDS.toSeconds(endReading
				- startReading));
		aDAO.setNumberOfWordsInArticle(numberOfWordsInArticle);
		aDAO.setIsScrollReachedBottom(isScrollReachedBottom);
		aDAO.setIsScrollUsed(isScrollUsed);

		//Store ReadingBehaviour (e.g. reading duration, article's words, etc.)
		LoggingReadingBehavior loggingReadingBehavior = new LoggingReadingBehavior(getApplicationContext(), this, aDAO);

		//Store Reading Scroll (e.g. precise scroll positions)
		LoggingReadingScroll loggingReadingScroll = new LoggingReadingScroll(getApplicationContext(), this, articleMetaData);

		MainActivity.CallingFromArticleActivity = true;

		/*
		 * Store Reading Scroll in the file. Commented coz it is stored in the
		 * server.
		 */

		// for (int i = 0; i < articleMetaData.size(); i++) {
		// LoggingReadingScroll logReadingScrollhpt = new LoggingReadingScroll(
		// getApplicationContext(), this, articleMetaData.get(i));
		// }

		// File scrollFile = new File(Environment.getExternalStorageDirectory()
		// + File.separator + "HabitoNews_Study/scroll_position.txt");
		//
		// if (scrollFile.exists()) {
		// try {
		// BufferedWriter bW;
		//
		// bW = new BufferedWriter(new FileWriter(scrollFile, true));
		// for (int i = 0; i < articleMetaData.size(); i++) {
		//
		// String delimeter = ";";
		// String row = articleMetaData.get(i).getUserID() + delimeter
		// + articleMetaData.get(i).getUserSession()
		// + delimeter + articleMetaData.get(i).getArticleID()
		// + delimeter
		// + articleMetaData.get(i).getScrollRange()
		// + delimeter
		// + articleMetaData.get(i).getScrollExtent()
		// + delimeter
		// + articleMetaData.get(i).getScrollOffset()
		// + delimeter + articleMetaData.get(i).getDateTime()
		// + delimeter;
		//
		// bW.write(row);
		// bW.newLine();
		// bW.flush();
		// }
		//
		// bW.close();
		// } catch (Exception e) {
		//
		// }
		// } else {
		// // Do something else.
		// System.out.println("scroll_position file not found");
		// }
	}

	public void storeReadingScroll(String result) {

		Log.e("RESULT READING Scroll", result);
	}

	public void storeReadingBehavior(String result) {

		// Log.e("RESULT READING", result);
	}

	public static void setIsScrollUsedCustom(Boolean _isScrollUsed) {
		isScrollUsed = _isScrollUsed;
	}

	private void broadcastMSG(ArticleDAO aDAO) {
		Intent intent = new Intent(UPDATE);
		// System.out.println("Dao: " + aDAO);

		intent.putExtra(MSG_SEND, aDAO);
		sendBroadcast(intent);
	}

	private class MyWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return false;
		}
	}

	public int countWords(String str) {
		if (str == null || str.isEmpty())
			return 0;

		int count = 0;
		for (int e = 0; e < str.length(); e++) {
			if (str.charAt(e) != ' ') {
				count++;
				while (str.charAt(e) != ' ' && e < str.length() - 1) {
					e++;
				}
			}
		}
		return count;
	}

	private class ParseHTML extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

			Document doc;

			String htmlcode = "";

			System.out.println("hereHTML: " + htmlcode);

			try {
				doc = Jsoup.connect(params[0]).get();

				System.out.println("################");
				System.out.println(doc.html());

				Log.e("dochtml", doc.html());
				String scriptContent;

//				Elements scriptElements = doc.select("script");
//				try{
//					JSONObject scriptContents = new JSONObject(scriptElements.first().html().toString());
//
//					JSONObject articleImages = new JSONObject(scriptContents.getString("image"));
//
//					JSONArray imageList = articleImages.getJSONArray("@list");
//
//					Log.e("image", imageList.get(0).toString());
//
//					Log.e("headline", scriptContents.getString("headline"));
//
//				}catch (JSONException e) {
//					e.printStackTrace();
//				}
//
//
//				Log.e("sizescript", scriptElements.size()+"");
//
//				Log.e("element", scriptElements.first().html());
//
//
//				Log.e("sizescript", scriptElements.size()+"");

				Elements el = doc.select("div#orb-footer");
				Log.e("el size: ", el.size() + "");
				Log.e("el first: ", el.first().html() + "");

				doc.select("script").remove();
				doc.select("div.tags-container").remove();
				doc.select("div#core-navigation").remove();
				doc.select("div#comp-pattern-library-2").remove();
				doc.select("div#orb-footer").remove();
				doc.select("footer").remove();
				doc.select("div.share").remove();
				doc.select("div.bbccom_display_none").remove();
				doc.select("div.navigation--footer").remove();
				doc.select("img#livestats").remove();
				doc.select("div.push").remove();
				doc.select("div.story-more-push").remove();
				doc.select("div.wrapper").remove();
				doc.select("div.column--secondary").remove();
				doc.select("div.orb-nav-section").remove();
				doc.select("div.site-brand-inner").remove();
				doc.select("div.orb-header").remove();
				doc.select("div#js-navigation-sections-wrapper").remove();
				doc.select("div.story-more").remove();
				doc.select("script").remove();
				doc.select("div.more-index").remove();
				doc.select("h2.small-promo-group__title").remove();
				doc.select("div.small-promo-group__body").remove();
				doc.select("span.index-title").remove();
				doc.select("span.index-title__container").remove();
				doc.select("div.nav-top").remove();
				doc.select("div.share-this").remove();
				doc.select("div.nav-bottom").remove();
				doc.select("div.alert").remove();
				doc.select("div.pagination-bottom").remove();
				doc.select("div.mod.ll.see-alsos.bg").remove();
				doc.select("div.mod.ll.bg").remove();
				doc.select("div#services-bar").remove();

				doc.select("figure.media-landscape body-width no-caption").remove();
				doc.select("p.story-body__introduction").remove();
				doc.select("div.contact-form optional-is-default ghost-column").remove();
				doc.select("form");
				doc.select("input.contact-form__input--submit").remove();
				doc.select("a.story-body__link-external").remove();


				//extra for sports news
				doc.select("button#sb-2").remove();
				doc.select("h2#related-urls-title").remove();
				doc.select("h2.component__title").remove();
				//doc.select("div.gel-layout").remove();
				doc.select("ul.gel-layout").remove();
				doc.select("div.share-tools").remove();
				doc.select("section#top-stories").remove();
				doc.select("section#related-stories").remove();
				doc.select("section#multi-thumb-promo-1").remove();
				doc.select("section#get-inspired").remove();



				// System.out
				// .println("image size: " + doc.select("figure").size());
				// // doc.select("div#most-popular").remove();
				// doc.select("div.layout-block-b").remove();
				// doc.select("div.share-body-bottom").remove();
				// doc.select("div#page-bookmark-links-head").remove();
				// doc.select("div#id-status-nav").remove();
				// doc.select("div#blq-sign-in").remove();
				// doc.select("div#blq-acc-links").remove();
				// doc.select("div#blq-nav").remove();
				// doc.select("div#related-services").remove();
				// doc.select("div#news-related-sites").remove();
				// doc.select("div#blq-foot").remove();
				// doc.select("div#header-wrapper").remove();
				// doc.select("div#blq-masthead").remove();
				// //doc.select("div#blq-container-outer").remove();
				//
				//
				// // Remove new stuff added
				// doc.select("div#blq-global").remove();
				// doc.select("div.story-related").remove();
				htmlcode = doc.html();
				// System.out.println(doc);

				System.out.println("hereDOC: " + doc.toString());

				numberOfWordsInArticle = countWords(htmlcode);

				// System.out.println("countWords4: " + numberOfWordsInArticle);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return htmlcode;
		}

		@SuppressLint("SetJavaScriptEnabled")
		@Override
		protected void onPostExecute(String result) {
			webView = (ArticleWebView) findViewById(R.id.webViewArticleStory);
			// webView = new ArticleWebView(getApplicationContext(),
			// ArticleActivity.this);
			// RelativeLayout rl = new RelativeLayout(getApplicationContext());
			// RelativeLayout.LayoutParams rlp = new
			// RelativeLayout.LayoutParams(
			// RelativeLayout.LayoutParams.FILL_PARENT,
			// RelativeLayout.LayoutParams.FILL_PARENT);
			// webView.setLayoutParams(rlp);

			webView.getSettings().setJavaScriptEnabled(true);
			webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
			webView.setWebViewClient(new MyWebViewClient());
			webView.loadData(result, "text/html", null);
			webView.setOnBottomReachedListener(ArticleActivity.this, 300);
			progressBarArticle.setVisibility(View.GONE);
			// rl.addView(webView);
			// setContentView(rl, rlp);
		}
	}

	@Override
	public void onBottomReached(View v) {
		// TODO Auto-generated method stub
		System.out.println("Scroll down");
		isScrollReachedBottom = true;
		// Toast.makeText(this, "Bottom reached", Toast.LENGTH_LONG).show();
	}
}
