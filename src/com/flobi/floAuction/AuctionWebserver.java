package com.flobi.floAuction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;

class AuctionWebserver extends NanoHTTPD {
	private Map<IHTTPSession, ASWRequest> requests = new HashMap<IHTTPSession, ASWRequest>();
	private Map<IHTTPSession, JSONObject> responses = new HashMap<IHTTPSession, JSONObject>();
	private int bukkitScheduler;

	public AuctionWebserver(String hostname, int port) {
		super(hostname, port);
	}
	
	public AuctionWebserver(int port) {
		super(port);
	}
	
	@Override public void start() throws IOException {
		super.start();
		bukkitScheduler = Bukkit.getScheduler().scheduleSyncRepeatingTask(floAuction.plugin, new Runnable() {
		    public void run() {
		    	processRequests();
		    }
		}, 1L, 1L);
	}
	
	@Override public void stop() {
		Bukkit.getScheduler().cancelTask(bukkitScheduler);
		super.stop();
	}
	
	// NanoHTTPD runs in another thread, so we have to pass the requests to the main Bukkit thread and retrieve the result
	@Override public Response serve(IHTTPSession session) {
		// We have to pass this to Bukkit and wait for a response to be thread responsible with Bukkit objects. 
		ASWRequest request = new ASWRequest();
		request.params = session.getParms();
		requests.put(session, request);
		
		long sleeplength = 10;
		do {
			try {Thread.sleep(sleeplength);} catch (InterruptedException e) {}
			sleeplength *= 1.5;
		} while (sleeplength < 100 && !responses.containsKey(session));
		
		if (!responses.containsKey(session)) {
			JSONObject json = new JSONObject();
			json.put("error", "Response timeout.");
			return new NanoHTTPD.Response(json.toJSONString());
		}
		
		JSONObject response = responses.get(session);
		return new NanoHTTPD.Response(response.toJSONString());
	}
	
	private void processRequests() {
		for (Entry<IHTTPSession, ASWRequest> request : requests.entrySet()) {
			JSONObject json = new JSONObject();

			for (int i = 0; i < AuctionScope.auctionScopesOrder.size(); i++) {
				String auctionScopeId = AuctionScope.auctionScopesOrder.get(i);
				AuctionScope auctionScope = AuctionScope.auctionScopes.get(auctionScopeId);
				JSONObject scopeDetails = new JSONObject();
				scopeDetails.put("id", auctionScopeId);
				scopeDetails.put("name", auctionScope.getName());
				scopeDetails.put("url", "?ac=" + auctionScopeId);

				json.put(auctionScopeId, scopeDetails);
			}
			
			responses.put(request.getKey(), json);
			requests.remove(request.getKey());
		}
	}
	
	protected class ASWRequest {
		private Map<String, String> params;
	}
	
}