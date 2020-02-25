package com.custom.customgithubtest.api;

import com.custom.customgithubtest.model.UserSearch;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserService {

    @GET("search/users")
    Call<UserSearch> getUser(
            @Query("q") String name,
            @Query("page") int pageIndex
    );
}
