/*
 	org.manalith.ircbot.plugin.weather/WeatherPlugin.java
 	ManalithBot - An open source IRC bot based on the PircBot Framework.
 	Copyright (C) 2011  Ki-Beom, Kim

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.manalith.ircbot.plugin.weather;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.manalith.ircbot.annotation.Option;
import org.manalith.ircbot.common.stereotype.BotCommand;
import org.manalith.ircbot.plugin.SimplePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WeatherPlugin extends SimplePlugin {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Deprecated
	private String yahooWeatherAPIKey;

	private String wUndergroundAPIKey;

	@Override
	public String getName() {
		return "웨더채널 날씨";
	}

	@Override
	public String getHelp() {
		return "지정한 지역의 날씨를 보여줍니다.";
	}

	public void setYahooWeatherAPIKey(String apiKey) {
		yahooWeatherAPIKey = apiKey;
	}

	public void setWUndergroundAPIKey(String apiKey) {
		wUndergroundAPIKey = apiKey;
	}

	@Deprecated
	public String getYahooWeather(@Option(name = "지명", help = "한글 혹은 영문 지명") String keyword) {
		try {
			// TODO WOEID 로컬 캐싱
			final String url_woeid = "http://query.yahooapis.com/v1/public/yql"
					+ "?q=select%20woeid%20from%20geo.places%20where%20text%3D%22" + URLEncoder.encode(keyword, "UTF-8")
					+ "%20ko-KR%22%20limit%201";
			final String url_forecast = "http://weather.yahooapis.com/forecastrss?w=%s&u=c";
			final String error_woeid = "23424868";

			Document doc = Jsoup.connect(url_woeid).get();
			// example : http://query.yahooapis.com/v1/public/yql?q=select woeid
			// from geo.places where text%3D\"서울%2C ko-KR\" limit 1
			String woeid = doc.select("woeid").text();

			if (!woeid.equals(error_woeid)) {
				// example:
				// http://weather.yahooapis.com/forecastrss?w=1132599&u=c
				doc = Jsoup.connect(String.format(url_forecast, woeid)).get();
				String location = doc.getElementsByTag("yweather:location").attr("city");
				String condition = doc.getElementsByTag("yweather:condition").attr("text");
				String temp = doc.getElementsByTag("yweather:condition").attr("temp");
				String humidity = doc.getElementsByTag("yweather:atmosphere").attr("humidity");
				String windCondition = doc.getElementsByTag("yweather:wind").attr("speed");

				return String.format("[%s] %s 온도 %s℃, 습도 %s%%, 풍속 %skm/h", location, condition, temp, humidity,
						windCondition);

			} else {
				return String.format("[%s] 지명을 찾을 수 없습니다. 지명이 정확한지 다시 확인해주세요.", keyword);
			}

		} catch (IOException e) {
			logger.error("failed to run command", e);
			return "오류가 발생했습니다 : " + e.getMessage();
		}
	}

	@BotCommand("날씨")
	public String getWUndergroundWeather(@Option(name = "지명", help = "한글 혹은 영문 지명") String keyword) {
		try {

			final String zmw = getZMWCode(keyword);
			if (zmw.equals(""))
				return "해당 지역이 없거나 지역 이름을 잘못 입력하셨습니다.";

			final String reqUrl = "http://api.wunderground.com/api/%s/conditions/lang:KR/q/zmw:%s.json";
			String rurl = String.format(reqUrl, wUndergroundAPIKey, zmw);
			JsonNode node = new ObjectMapper().readTree(new URL(rurl)).get("current_observation");

			String location = node.get("display_location").get("full").asText();
			String condition = node.get("weather").asText();
			String temp = node.get("temp_c").asText();
			String humidity = node.get("relative_humidity").asText().replaceAll("%", "");
			String windDirection = node.get("wind_dir").asText();
			String windSpeed = node.get("wind_kph").asText();

			return String.format("[%s] %s 온도 %s℃, 습도 %s%%, 풍향: %s, 풍속: %skm/h", location, condition, temp, humidity,
					windDirection, windSpeed);
		} catch (IOException e) {
			return "오류가 발생했습니다 : " + e.getMessage();
		}
	}

	private String getZMWCode(String location) {
		try {
			final String autoCompleteRequestUrl = "http://autocomplete.wunderground.com/aq?query=%s";
			String encodedLocation = URLEncoder.encode(location, "UTF-8");

			HttpGet get = new HttpGet(String.format(autoCompleteRequestUrl, encodedLocation));
			get.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) "
					+ "AppleWebKit/537.36 (KHTML, like Gecko) " + "Chrome/52.0.2743.82 " + "Safari/537.36");
			get.addHeader("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
			String content = IOUtils.toString(HttpClientBuilder.create().build().execute(get).getEntity().getContent());

			JsonNode node = new ObjectMapper().readTree(content).get("RESULTS");
			return (node.size() == 0) ? "" : node.get(0).get("zmw").asText();
		} catch (Exception e) {
			return "";
		}
	}
}
