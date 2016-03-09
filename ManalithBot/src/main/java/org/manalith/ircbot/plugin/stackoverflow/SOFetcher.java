package org.manalith.ircbot.plugin.stackoverflow;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import org.manalith.ircbot.plugin.urlshortener.GooGlProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SOFetcher {

	public String apikey;

	public String cx;

	public SOFetcher(String APIKey, String ncx) {
		apikey = APIKey;
		cx = ncx;
	}

	public String[] fetchResult(String keyword) {
		String[] result = new String[3];

		try {
			String url = String
					.format(Locale.US,
							"https://www.googleapis.com/customsearch/v1?cx=%s&key=%s&q=%s",
							cx, apikey, URLEncoder.encode(
									keyword.replaceAll("\\s", "+"), "UTF-8"));

			ObjectMapper om = new ObjectMapper();
			JsonNode node = om.readTree(new URL(url));
			node = node.get("items");

			GooGlProvider gp;

			// int sz = node.size();
			for (int i = 0; i < 3; i++) {
				result[i] = "[" + (i + 1) + "] " + node.get(i).get("title")
						+ " ";
				gp = new GooGlProvider();
				gp.setApiKey(apikey);
				result[i] += gp.shorten(node.get(i).get("link").asText());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
}
