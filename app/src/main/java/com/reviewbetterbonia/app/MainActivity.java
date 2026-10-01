package com.reviewbetterbonia.app;

import android.app.*;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import com.reviewbetterbonia.app.update.UpdateManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.reviewbetterbonia.app.data.*;import com.reviewbetterbonia.app.model.*;import com.reviewbetterbonia.app.ui.auth.WelcomeFragment;import com.reviewbetterbonia.app.ui.home.HomeFragment;
import com.reviewbetterbonia.app.ui.quiz.QuizFragment;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends FragmentActivity {
    private UpdateManager updateManager;
    public QuizRepository quiz; public LocalStore local; public FirebaseRepository firebase; public UserProfile user=new UserProfile(); public int rating=0; public String rank="Freshman";
    public List<Question> lastSession;
    public List<Integer> lastUserAnswers;
    public boolean isNetworkConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception ignored) {}
        return false;
    }
    public boolean isOffline(){return local.isOfflineMode() || !firebase.online || "local".equals(user.uid) || !isNetworkConnected();}
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(
                Color.rgb(248,249,252)
        );

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );

        setContentView(R.layout.activity_main);

        quiz = new QuizRepository(this);
        local = new LocalStore(this);
        firebase = new FirebaseRepository(this);

        restore();

        /*
         * Existing Firebase question synchronization.
         * DO NOT REMOVE THIS.
         */
        if (firebase.online && local.loggedIn()) {

            syncOfflineResultsToOnline();

            firebase.loadActiveQuestions(
                    qs -> quiz.mergeRemoteQuestions(qs)
            );

        } else if (firebase.online) {

            firebase.loadActiveQuestions(
                    qs -> quiz.mergeRemoteQuestions(qs)
            );
        }

        if (!BuildConfig.DEBUG) {
            updateManager = new UpdateManager(this);
            updateManager.resumePendingInstall();
            updateManager.checkAutomatically();
        }

        if (b == null) {

            if (user.uid.isEmpty()) {
                showWelcome(false);
            } else {
                showHome(false);
            }
        }
    }
    private void restore(){if(!local.loggedIn())return;user.uid=local.prefs().getString("uid","");user.username=local.prefs().getString("username","Player");user.email=local.prefs().getString("email","");user.role=local.prefs().getString("role","student");rating=local.prefs().getInt("ranked_rating",0);rank=rankForRating(rating);}
    public void showWelcome(boolean back){navigate(new WelcomeFragment(),back);}
    public void showHome(boolean back){navigate(new HomeFragment(),back);}
    public void navigate(Fragment f,boolean addBack){
        FragmentTransaction tx=getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).replace(R.id.fragment_container,f);if(addBack)tx.addToBackStack(null);tx.commit();}
    // Kept as a small helper so every page uses the same navigation behavior.
    public void clearAndShowHome(){getSupportFragmentManager().popBackStack(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);showHome(false);}
    public String rankForRating(int r){if(r>=1800)return "Engineer";if(r>=1500)return "Graduate";if(r>=1200)return "Senior";if(r>=900)return "Junior";if(r>=600)return "Sophomore";return "Freshman";}
    public int secondsForRank(){if("Engineer".equals(rank))return 10;if("Graduate".equals(rank))return 14;if("Senior".equals(rank))return 18;if("Junior".equals(rank))return 22;if("Sophomore".equals(rank))return 26;return 30;}
    public boolean isAdmin(){return "admin".equals(user.role)||"super_admin".equals(user.role);} public boolean isSuperAdmin(){return "super_admin".equals(user.role);}
    public void syncOfflineResultsToOnline(){
        if(firebase.online && local.loggedIn() && !isOffline()){
            long lastSynced = local.lastSyncedTime();
            long maxTime = lastSynced;
            for(QuizResult r : local.results()){
                if(r.time > lastSynced){
                    boolean ranked = r.mode != null && r.mode.toLowerCase(Locale.US).contains("ranked");
                    firebase.saveResult(user.uid, user.username, r, ranked, rating, rank);
                    if(r.time > maxTime) maxTime = r.time;
                }
            }
            local.setLastSyncedTime(maxTime);
        }
    }
    public void signIn(String name,String email,String uid,String role,int r){user.username=name;user.email=email;user.uid=uid;user.role=role;rating=r;rank=rankForRating(r);local.login(uid,name,email,role,r);syncOfflineResultsToOnline();}
    public void signOut(){if(firebase.online&&firebase.auth!=null)firebase.auth.signOut();local.logout();user=new UserProfile();rating=0;rank="Freshman";clearAndShowWelcome();}
    private void clearAndShowWelcome(){getSupportFragmentManager().popBackStack(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);showWelcome(false);}
    @Override public void onBackPressed(){
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof QuizFragment) {
            Toast.makeText(this, "You cannot go back while taking a quiz.", Toast.LENGTH_SHORT).show();
            return;
        }
        FragmentManager fm=getSupportFragmentManager();
        if(fm.getBackStackEntryCount()>0){
            fm.popBackStack();
            return;
        }
        if(current instanceof HomeFragment){
            new AlertDialog.Builder(this).setTitle("Exit Review Betterbonia?").setMessage("Are you sure you want to close the app?").setNegativeButton("Cancel",null).setPositiveButton("Exit",(d,w)->finish()).show();
        }else super.onBackPressed();
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!BuildConfig.DEBUG && updateManager != null) {
            updateManager.resumePendingInstall();
        }
    }
    @Override
    protected void onDestroy() {

        if (updateManager != null) {
            updateManager.shutdown();
        }

        super.onDestroy();
    }
}

