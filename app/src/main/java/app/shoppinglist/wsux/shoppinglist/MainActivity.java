package app.shoppinglist.wsux.shoppinglist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.UserInfo;

public class MainActivity extends AppCompatActivity
        implements FireBaseManager.FireBaseEventsInterface,
        View.OnClickListener, MainDrawer.MainDrawerInterface {

    private static final String TAG = "MAIN_ACTIVITY";

    // objects
    private FireBaseManager fireBaseManager;
    private MainDrawer mainDrawer;
    private UserInfo userInfo;

    // layouts
    private LinearLayout loginScreenWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         fireBaseManager = new FireBaseManager(this, this);
         fireBaseManager.onCreate();

        setContentView(R.layout.activity_main);
        setLoginScreen();

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        setShopListView();
        mainDrawer = new MainDrawer(this, topToolBar, this);

        findViewById(R.id.drawer_sign_out).setOnClickListener(this);
    }

    private void setLoginScreen() {
        LoginScreen loginScreen = (LoginScreen) getSupportFragmentManager()
                .findFragmentById(R.id.login_screen_fragment);
        loginScreen.setFirebaseManager(fireBaseManager);

        loginScreenWrapper = findViewById(R.id.login_screen_wrapper);
    }

    private void hideLoginScreen() {
        loginScreenWrapper.setVisibility(View.GONE);
    }

    private void showLoginScreen() {
        loginScreenWrapper.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        fireBaseManager.onStart();
    }

    private void setShopListView() {
        RecyclerView recyclerView =  findViewById(R.id.shopping_list_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        TaskAdapter mAdapter = new TaskAdapter(null);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEventOccurred(int what, Object data, Exception e) {
        switch (what) {
            case FireBaseManager.ON_SIGN_IN:
                onLogin((UserInfo) data);
                break;

            case FireBaseManager.ON_SIGN_OUT:
            case FireBaseManager.ON_SIGN_ERR:
                onLogout();
                break;
            case FireBaseManager.ON_USER_LIST_UPDATED:
                mainDrawer.reportListChange();
                break;
        }

        Log.d("FIREBASE_EVENTS",
                String.format(
                        "%s - %s - %s",
                        what,
                        data != null ? data.toString() : "null",
                        e != null ? e.getMessage() : "null"
                        )
        );
    }

    private void onLogin(UserInfo userInfo) {
        this.userInfo = userInfo;
        mainDrawer.setUserInfo(userInfo);
        hideLoginScreen();
    }

    private void onLogout() {
        userInfo = null;
        mainDrawer.setUserInfo(userInfo);
        mainDrawer.close();
        showLoginScreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fireBaseManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.drawer_sign_out:
                fireBaseManager.getLoginManager().requestLogout();
                break;
        }
    }

    @Override
    public void addNewListPressed() {

        View popupLayout = getLayoutInflater().inflate(R.layout.add_new_list_popup_layout, null);
        final EditText titleEt = popupLayout.findViewById(R.id.new_list_popup_title);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(popupLayout)
                .create();

        popupLayout.findViewById(R.id.new_list_popup_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEt.getText().toString();
                dialog.dismiss();

                if (userInfo == null) {
                    return;
                }

                userInfo.createNewList(title);
            }
        });

        dialog.show();
    }

    @Override
    public void selectedList(ShopList shopList) {

    }
}
