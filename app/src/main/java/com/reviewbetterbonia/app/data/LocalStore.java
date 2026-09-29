package com.reviewbetterbonia.app.data;

import android.content.Context;import android.content.SharedPreferences;import com.reviewbetterbonia.app.model.QuizResult;import java.util.*;

public class LocalStore {
    private final SharedPreferences p; public LocalStore(Context c){p=c.getSharedPreferences("reviewbetterbonia",Context.MODE_PRIVATE);}
    public SharedPreferences prefs(){return p;}
    public void saveResult(QuizResult r){List<QuizResult> all=results();all.add(0,r);write(all);}
    public void replaceResults(List<QuizResult> rs){ write(rs==null?new ArrayList<>():rs); }
    public List<QuizResult> results(){List<QuizResult> out=new ArrayList<>();String raw=p.getString("results","");if(raw.isEmpty())return out;for(String row:raw.split("\\n")){String[] x=row.split("\\|",-1);if(x.length<5)continue;try{String sub=x.length>=7?x[5]:"All Subjects",cat=x.length>=7?x[6]:"All Categories";out.add(new QuizResult(x[0],Integer.parseInt(x[1]),Integer.parseInt(x[2]),Integer.parseInt(x[3]),Long.parseLong(x[4]),sub,cat));}catch(Exception ignored){}}return out;}
    public void write(List<QuizResult> rs){StringBuilder b=new StringBuilder();for(QuizResult r:rs)b.append(r.mode).append('|').append(r.score).append('|').append(r.total).append('|').append(r.seconds).append('|').append(r.time).append('|').append(r.subject).append('|').append(r.category).append('\n');p.edit().putString("results",b.toString()).apply();}
    public void setDemoAccount(String username,String email,String hash){p.edit().putString("local_username",username).putString("local_email",email).putString("local_hash",hash).apply();}
    public boolean demoExists(String email){return email.equals(p.getString("local_email",""));}
    public boolean demoPasswordMatches(String hash){return hash.equals(p.getString("local_hash",""));}
    public String demoUsername(){return p.getString("local_username","Player");}
    public boolean loggedIn(){return p.getBoolean("logged_in",false);}
    public boolean isOfflineMode(){return p.getBoolean("offline_mode",false);}
    public void setOfflineMode(boolean offline){p.edit().putBoolean("offline_mode",offline).apply();}
    public long lastSyncedTime(){return p.getLong("last_synced_time", 0);}
    public void setLastSyncedTime(long t){p.edit().putLong("last_synced_time", t).apply();}
    public long casualClearedTime(){return p.getBoolean("logged_in",false) ? p.getLong("casual_cleared_time_" + p.getString("uid",""), 0) : 0;}
    public void setCasualClearedTime(long t){p.edit().putLong("casual_cleared_time_" + p.getString("uid",""), t).apply();}
    public long rankedClearedTime(){return p.getBoolean("logged_in",false) ? p.getLong("ranked_cleared_time_" + p.getString("uid",""), 0) : 0;}
    public void setRankedClearedTime(long t){p.edit().putLong("ranked_cleared_time_" + p.getString("uid",""), t).apply();}
    public void login(String uid,String name,String email,String role,int rating){p.edit().putBoolean("logged_in",true).putString("uid",uid).putString("username",name).putString("email",email).putString("role",role).putInt("ranked_rating",rating).apply();}
    public void logout(){p.edit().putBoolean("logged_in",false).remove("offline_mode").apply();}
}
