package com.nex.script;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.nex.utils.json.JsonObject;
import com.nex.utils.json.JsonObject.Member;
import org.rspeer.script.Script;
import org.rspeer.ui.Log;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;


public class Exchange {
	private final static String RSBUDDY_URL = "https://rsbuddy.com/exchange/summary.json";
	static String wikiUrl = "https://oldschool.runescape.wiki/w/Exchange:";
	private static Map<Integer, Integer> sell_prices = Collections.synchronizedMap(new WeakHashMap<Integer, Integer>());
	private static Map<Integer, Integer> buy_prices = Collections.synchronizedMap(new WeakHashMap<Integer, Integer>());
	private static Map<Integer, String> names = Collections.synchronizedMap(new WeakHashMap<Integer, String>());
	private static Map<String, Integer> ids = Collections.synchronizedMap(new WeakHashMap<String, Integer>());

	public static String getName(int item) {
		if(item == 995) return "Coins";
		String itemName = null;
		if (names.containsKey(item)) {
			return names.get(item);
		}
		try {
			JsonObject priceJSON = getJSON();
			Iterator<Member> iterator = priceJSON.iterator();

			while (iterator.hasNext()) {
				JsonObject itemJSON = priceJSON.get(iterator.next().getName()).asObject();
				int itemID = itemJSON.get("id").asInt();
				if (item == itemID) {
					itemName = itemJSON.get("name").asString();
					cacheItemJson(itemJSON, itemID, itemName);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return itemName;
	}
	public static void preCache(ArrayList<Integer> itemIDs){
		itemIDs.removeIf((id)->buy_prices.containsKey(id));
		if(itemIDs.size() == 0) return;
		try {
			JsonObject priceJSON = getJSON();
			Iterator<Member> iterator = priceJSON.iterator();
			while (iterator.hasNext()) {
				JsonObject itemJSON = priceJSON.get(iterator.next().getName()).asObject();
				Integer itemID = itemJSON.get("id").asInt();
				if (itemIDs.contains(itemID)) {
					String itemName = itemJSON.get("name").asString();
					cacheItemJson(itemJSON, itemID, itemName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static void cacheItemJson(JsonObject itemJSON, int itemID, String itemName){
		names.put(itemID, itemName);
		ids.put(itemName, itemID);
		int buy_price = itemJSON.get("buy_average").asInt();
		int sell_price = itemJSON.get("sell_average").asInt();
		if (buy_price == 0) buy_price = itemJSON.get("overall_average").asInt();
		if (sell_price == 0) sell_price = itemJSON.get("overall_average").asInt();
		if (sell_price == 0) {
			sell_price = getRSPrice(itemID);
		}
		if (buy_price == 0) buy_price = sell_price;
		buy_prices.put(itemID, buy_price);
		sell_prices.put(itemID, sell_price);
	}

	public static int getID(String itemName){
		if(itemName.equals("Coins")) return 995;
		int itemID = 0;
		if (ids.containsValue(itemName)) {
			return ids.get(itemName);
		}
		try {
			JsonObject priceJSON = getJSON();
			Iterator<Member> iterator = priceJSON.iterator();

			while (iterator.hasNext()) {
				JsonObject itemJSON = priceJSON.get(iterator.next().getName()).asObject();
				String name = itemJSON.get("name").asString();
				if (name.equals(itemName)) {
					itemID = itemJSON.get("id").asInt();
					cacheItemJson(itemJSON, itemID, itemName);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return itemID;
	}

	public static int getBuyPrice(int item) {
		if(item == 995) return 1;
		if(cache(item))
			return buy_prices.getOrDefault(item, 0);
		return 0;
	}
	public static int getSellPrice(int item) {
		if(item == 995) return 1;
		if(cache(item))
			return sell_prices.getOrDefault(item, 1);
		return 1;
	}
	public static boolean cache(int item) {
		if(item == 995) return true;
		if (ids.containsKey(item)) {
			return true;
		}
		boolean cached = false;
		try {
			JsonObject priceJSON = getJSON();
			Iterator<Member> iterator = priceJSON.iterator();

			while (iterator.hasNext()) {
				JsonObject itemJSON = priceJSON.get(iterator.next().getName()).asObject();
				int itemID = itemJSON.get("id").asInt();
				if (item == itemID) {
					String itemName = itemJSON.get("name").asString();
					cacheItemJson(itemJSON, itemID, itemName);
					cached = true;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cached;
	}

	static JsonObject getJSON(){
		try {
			URL url = new URL(RSBUDDY_URL);
			Path filename = getRSBuddySummary();
			InputStream stream;
			if (filename != null)
				stream = Files.newInputStream(filename);
			else
				stream = url.openStream();
			BufferedReader jsonFile = new BufferedReader(new InputStreamReader(stream));

			JsonObject json = com.nex.utils.json.JsonObject.readFrom(jsonFile.readLine());
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static long getSecondsFromModification(File f) throws IOException {
		Path attribPath = f.toPath();
		BasicFileAttributes basicAttribs
				= Files.readAttributes(attribPath, BasicFileAttributes.class);
		return (System.currentTimeMillis()
				- basicAttribs.lastModifiedTime().to(TimeUnit.MILLISECONDS))
				/ 1000;
	}
	static String getSummaryJson(){
		return Script.getDataDirectory().toString() + "/summary.json";
	}
	private static Path getRSBuddySummary() {
		try {
			String filename = getSummaryJson();
			File file = new File(filename);
			try {
				if (file.exists() && getSecondsFromModification(file) < 20 * 60)
					return file.toPath();
			}catch (IOException ex) { }
			HttpsURLConnection con = (HttpsURLConnection)new URL(RSBUDDY_URL).openConnection();
			if(con.getResponseCode() != 200){
				Log.info("response code " + con.getResponseCode());
				return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) con.getContent()));
			String out = reader.lines().collect(Collectors.joining());
			reader.close();
			con.disconnect();

			Path path = Paths.get(filename);
			try {
				Files.write(path, out.getBytes());
			}catch (IOException ex){}
			return path;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int getRSPrice(int id) {
		Log.fine("Downloading from RS DB price for " + getName(id));
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<Object> task = new Callable<Object>() {
			public Object call() {
				return _getRSPrice(id);
			}
		};
		Future<Object> future = executor.submit(task);
		try {
			return (int)future.get(155, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			// handle the timeout
		} catch (Exception e) {
			// handle other exceptions
		} finally {
			future.cancel(true); // may or may not desire this
		}
		return 0;
	}
	static int _getRSPrice(int id){
		try {
			HttpsURLConnection con = (HttpsURLConnection)new URL("https://services.runescape.com/m=itemdb_oldschool/api/catalogue/detail.json?item=" + id).openConnection();
			if(con.getResponseCode() != 200){
				Log.info("response code " + con.getResponseCode());
				return 0;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) con.getContent()));
			final Gson gson = new Gson().newBuilder().create();
			final String price_text = gson.fromJson(reader, com.google.gson.JsonObject.class)
					.getAsJsonObject("item")
					.getAsJsonObject("current")
					.get("price")
					.getAsString();
			final int price = Integer.parseInt(price_text.replaceAll("\\D+", ""));
			return price_text.matches("[0-9]+") ? price : price * (price_text.charAt(0) == 'k' ? 1000 : 1000000);
		}catch (Exception ex){
			Log.severe(ex);
			return 0;
		}
	}
	private static int getRealPrice(String itemName) throws IOException {
		Log.fine("Downloading from RS wiki price for " + itemName);
		URL url = new URL(wikiUrl + itemName);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
		httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
		InputStream is = httpcon.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		String price = "";
		while ((line = br.readLine()) != null) {
			if (line.contains("GEPrice")) {
				String sub = line.split("GEPrice\">")[1];
				price = sub.split("<")[0].replaceAll(",", "").replaceAll("\"", "");
			}
		}
		return (int) Double.parseDouble(price);
	}

}
