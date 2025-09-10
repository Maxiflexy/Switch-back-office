package messaging.fileUtils.util;

import java.util.HashMap;



public class BaseBean extends HashMap<String,String> {
	
	public BaseBean(){
		super();
	}
	
	final public String getString(String key) {

		if(key==null || key.trim().equals("")){
			throw new RuntimeException("Invalid key");
		}else{
		   return super.get(key.toLowerCase().trim()) == null ? "" : super.get(key.toLowerCase().trim());
		}
		
	}

    public String setString(String key, String value) {
		
		if(key==null || key.trim().equals(""))
			throw new RuntimeException("Invalid key");
					
	    return super.put(key.toLowerCase().trim(),value==null?"":value);
	    
	}

	public boolean containsKey(String key) {
		if(key==null || key.trim().equals(""))
			throw new RuntimeException("Invalid key");

		return super.containsKey(key.toLowerCase().trim());
	}
	
	final public String get(String key){
       return getString(key);
	}
	
	
	final public String put(String key, String value){
		return setString(key, value);
	}
	
}
