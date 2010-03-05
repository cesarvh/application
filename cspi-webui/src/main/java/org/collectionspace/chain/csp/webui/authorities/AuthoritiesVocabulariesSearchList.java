package org.collectionspace.chain.csp.webui.authorities;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthoritiesVocabulariesSearchList implements WebMethod {
	private Record r;
	private Instance n;
	private boolean search;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public AuthoritiesVocabulariesSearchList(Record r,boolean search) {
		this.r=r;
		this.search=search;
	}

	public AuthoritiesVocabulariesSearchList(Instance n,boolean search) {
		this.n=n;
		this.r=n.getRecord();
		this.search=search;
	}
	
	private JSONObject generateMiniRecord(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view");
		out.put("csid",csid);
		out.put("recordtype",inst_type);
		out.put("number",out.get("name"));
		out.put("summary",out.get("name")); // XXX proper summary!?
		return out;		
	}
	
	private void search_or_list_vocab(JSONArray out,Instance n,Storage storage,UIRequest ui,String param) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject restriction=new JSONObject();
		if(param!=null)
			restriction.put("name",param);
		String[] results=storage.getPaths(r.getID()+"/"+n.getTitleRef(),restriction);
		/* Get a view of each */
		for(String result : results) {
			out.put(generateMiniRecord(storage,r.getID(),n.getTitleRef(),result));
		}
	}
	
	private void search_or_list(Storage storage,UIRequest ui,String param) throws UIException {
		try {
			JSONArray results=new JSONArray();
			if(n==null) {
				// For now simply merge all the instances one after the other. XXX do something cleverer.
				for(Instance n : r.getAllInstances()) {
					search_or_list_vocab(results,n,storage,ui,param);
				}
			} else {
				search_or_list_vocab(results,n,storage,ui,param);				
			}
			JSONObject out=new JSONObject();
			if(param==null)
				out.put("items",results);
			else
				out.put("results",results);
			ui.sendJSONResponse(out);
		} catch (JSONException e) {
			throw new UIException("Cannot generate JSON",e);
		} catch (ExistException e) {
			throw new UIException("Exist exception",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented exception",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Unnderlying storage exception",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null);
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
