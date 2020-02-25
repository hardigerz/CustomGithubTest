package com.custom.customgithubtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.custom.customgithubtest.api.UserApi;
import com.custom.customgithubtest.api.UserService;
import com.custom.customgithubtest.model.Item;
import com.custom.customgithubtest.model.UserSearch;
import com.custom.customgithubtest.util.PaginationScrollListener;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    boolean doubleBackToExitPressedOnce;
    private long mBackPressed;
    UserService userService;
    LinearLayout errorLayout;
    RelativeLayout relativeLayout;
    TextView txtError;
    EditText editText;
    String search="";
    RecyclerView recyclerView;
    ProgressBar progressBar;
    SwipeRefreshLayout swipeRefreshLayout;
    PaginationAdapter adapter;
    LinearLayoutManager linearLayoutManager;
    Button btnRetry;
    private static final int PAGE_START = 1;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int TOTAL_PAGES = 10;
    private int currentPage = PAGE_START;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.main_recycler);
        progressBar = findViewById(R.id.main_progress);
        errorLayout = findViewById(R.id.error_layout);
        btnRetry = findViewById(R.id.error_btn_retry);
        txtError = findViewById(R.id.error_txt_cause);
        swipeRefreshLayout = findViewById(R.id.main_swiperefresh);
        relativeLayout =findViewById(R.id.relative_header);
        editText =findViewById(R.id.searchEditText);
        userService = UserApi.getClient(this).create(UserService.class);

        adapter = new PaginationAdapter(this);
        progressBar.setVisibility(View.GONE);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;

                loadNextPage(search);
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });

        userService = UserApi.getClient(this).create(UserService.class);

        btnRetry.setOnClickListener(view -> loadFirstPage(search));
        swipeRefreshLayout.setOnRefreshListener(this::doRefresh);
        editText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event != null &&
                                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (event == null || !event.isShiftPressed()) {
                                // the user is done typing.
                                InputMethodManager imm = (InputMethodManager)getApplicationContext().getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                                return true; // consume.

                            }
                        }
                        return false; // pass on to other listeners.
                    }
                }
        );
    }




    private void loadFirstPage(String kata) {
        Log.e("loadFirstPage: ","loadfirstPage");
        hideErrorView();
        currentPage = PAGE_START;
        callUser(kata).enqueue(new Callback<UserSearch>() {
            @Override
            public void onResponse(Call<UserSearch> call, Response<UserSearch> response) {
//                Log.i(TAG, "onResponse: " + currentPage
//                        + (response.raw().cacheResponse() != null ? "Cache" : "Network"));
                hideErrorView();
                adapter.getItemList().clear();
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
                 progressBar.setVisibility(View.GONE);
                List<Item> items = fetchResults(response);
//               doRefresh();
                for (int i = 0; i < items.size(); i++) {
                    Log.e("onResponse: ", items.get(i).getAvatarUrl());
                    Log.e("onResponse: ", items.get(i).getLogin());
                }
                progressBar.setVisibility(View.VISIBLE);
                Log.e( "onResponsesize: ", String.valueOf(items.size()));
                adapter.addAll(items);

                if (currentPage <= TOTAL_PAGES) {
                    adapter.addLoadingFooter();
                    progressBar.setVisibility(View.GONE);
                }
                else isLastPage = true;
                if (items.isEmpty()){
                    adapter.clear();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_data), Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<UserSearch> call, Throwable t) {
                t.printStackTrace();
                showErrorView(t);
            }
        });
    }

    private Call<UserSearch> callUser(String kata) {
        return userService.getUser(
                kata,
                currentPage
        );
    }



    private void loadNextPage(String kata) {
        Log.d( "loadNextPage: " ,"loadNext"+currentPage);

        callUser(kata).enqueue(new Callback<UserSearch>() {
            @Override
            public void onResponse(Call<UserSearch> call, Response<UserSearch> response) {
//                Log.i(TAG, "onResponse: " + currentPage
//                        + (response.raw().cacheResponse() != null ? "Cache" : "Network"));

                adapter.removeLoadingFooter();
                isLoading = false;

                List<Item> results = fetchResults(response);
                adapter.addAll(results);

                if (currentPage != TOTAL_PAGES) adapter.addLoadingFooter();
                else isLastPage = true;
            }

            @Override
            public void onFailure(Call<UserSearch> call, Throwable t) {
                t.printStackTrace();
                adapter.showRetry(true, fetchErrorMessage(t));
            }
        });
    }


    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private void showErrorView(Throwable throwable) {
        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.GONE);

            txtError.setText(fetchErrorMessage(throwable));
        }


        Toast.makeText(getApplicationContext(), fetchErrorMessage(throwable), Toast.LENGTH_SHORT).show();

    }

    private String fetchErrorMessage(Throwable throwable) {
        String errorMsg = getResources().getString(R.string.error_msg_unknown);

        if (!isNetworkConnected()) {
            errorMsg = getResources().getString(R.string.error_msg_no_internet);
        } else if (throwable instanceof TimeoutException) {
            errorMsg = getResources().getString(R.string.error_msg_timeout);
        }

        return errorMsg;
    }

    private List<Item> fetchResults(Response<UserSearch> response) {
        UserSearch topRatedMovies = response.body();
        return topRatedMovies.getItems();
    }

    public void MainClick(View v) {
        switch (v.getId()) {

            case R.id.searchBtn:
                getEditText(v);
                break;
        }
    }




    void getEditText(View view){
        InputMethodManager imm = (InputMethodManager)getApplicationContext().getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (editText.getText().toString().matches("")){
            Toast.makeText(getApplicationContext(), "user tidak ditemukan", Toast.LENGTH_SHORT).show();
            adapter.getItemList().clear();
            adapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        }
        else{
            search=editText.getText().toString();
            Toast.makeText(getApplicationContext(), search, Toast.LENGTH_SHORT).show();
            Log.e("getEditText: ", search);
            loadFirstPage(search);
        }
    }
    private void hideErrorView() {
        if (errorLayout.getVisibility() == View.VISIBLE) {
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
        else{
            progressBar.setVisibility(View.GONE);
        }
    }

    private void doRefresh() {
        progressBar.setVisibility(View.VISIBLE);
        if (callUser(search).isExecuted())
            callUser(search).cancel();

        // TODO: Check if data is stale.
        //  Execute network request if cache is expired; otherwise do not update data.
        adapter.getItemList().clear();
        adapter.notifyDataSetChanged();
        loadFirstPage(search);
        swipeRefreshLayout.setRefreshing(false);
    }



    private Handler mHandler = new Handler();

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (mHandler != null) { mHandler.removeCallbacks(mRunnable); }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(mRunnable, 2000);
    }


}
