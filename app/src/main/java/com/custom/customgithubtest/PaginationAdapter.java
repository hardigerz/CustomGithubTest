package com.custom.customgithubtest;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.custom.customgithubtest.model.Item;
import com.custom.customgithubtest.util.PaginationAdapterCallback;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class PaginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM = 0;
    private static final int LOADING = 1;


    private List<Item> itemList;
    private Context context;

    private boolean isLoadingAdded = false;
    private boolean retryPageLoad = false;

    private PaginationAdapterCallback mCallback;

    private String errorMsg;

    public PaginationAdapter(Context context) {
        this.context = context;
//        this.mCallback = (PaginationAdapterCallback) context;
        itemList = new ArrayList<>();
    }

    public List<Item> getItemList() {
        return itemList;
    }
    public void setMovies(List<Item> itemList) {
        this.itemList = itemList;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM:
                View viewItem = inflater.inflate(R.layout.item_list, parent, false);
                viewHolder = new ItemVH(viewItem);
                break;
            case LOADING:
                View viewLoading = inflater.inflate(R.layout.item_progress, parent, false);
                viewHolder = new LoadingVH(viewLoading);
                break;

        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = itemList.get(position);

        switch (getItemViewType(position)) {


            case ITEM:
                final ItemVH itemVH = (ItemVH) holder;

                itemVH.mNameTitle.setText(item.getLogin());

                // load movie thumbnail
                Glide.with(context)
                        .load(item.getAvatarUrl())
                        .into(((ItemVH) holder).mPosterImg);


                break;

            case LOADING:
                LoadingVH loadingVH = (LoadingVH) holder;

                if (retryPageLoad) {
                    loadingVH.mErrorLayout.setVisibility(View.VISIBLE);
                    loadingVH.mProgressBar.setVisibility(View.GONE);

                    loadingVH.mErrorTxt.setText(
                            errorMsg != null ?
                                    errorMsg :
                                    context.getString(R.string.error_msg_unknown));

                } else {
                    loadingVH.mErrorLayout.setVisibility(View.GONE);
                    loadingVH.mProgressBar.setVisibility(View.VISIBLE);
                }
                break;
        }

    }

    @Override
    public int getItemCount() {
        return itemList == null ? 0 : itemList.size();
    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM;
        } else {
            return (position == itemList.size() - 1 && isLoadingAdded) ? LOADING : ITEM;
        }
    }






    protected class ItemVH extends RecyclerView.ViewHolder {
        private TextView mNameTitle;
        private ProgressBar mProgress;
        private ImageView mPosterImg;




        public ItemVH(@NonNull View itemView) {
            super(itemView);
            mNameTitle = itemView.findViewById(R.id.name_title);
            mProgress = itemView.findViewById(R.id.name_progress);
            mPosterImg = itemView.findViewById(R.id.image_avatar);
        }
    }


    protected class LoadingVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ProgressBar mProgressBar;
        private ImageButton mRetryBtn;
        private TextView mErrorTxt;
        private LinearLayout mErrorLayout;

        public LoadingVH(View itemView) {
            super(itemView);

            mProgressBar = itemView.findViewById(R.id.loadmore_progress);
            mRetryBtn = itemView.findViewById(R.id.loadmore_retry);
            mErrorTxt = itemView.findViewById(R.id.loadmore_errortxt);
            mErrorLayout = itemView.findViewById(R.id.loadmore_errorlayout);
            mRetryBtn.setOnClickListener(this);
            mErrorLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.loadmore_retry:
                case R.id.loadmore_errorlayout:
                    showRetry(false, null);
                    mCallback.retryPageLoad();

                    break;
            }
        }
    }



        /*
        Helpers - Pagination
   _________________________________________________________________________________________________
    */

    public void add(Item i) {
        itemList.add(i);
        notifyItemInserted(itemList.size() - 1);
    }

    public void addAll(List<Item> items) {
        Log.e( "addAll: ", String.valueOf(items.size()));
        for (Item item : items) {
            add(item);
        }

    }

    public void remove(Item i) {
        int position = itemList.indexOf(i);
        if (position > -1) {
            itemList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        isLoadingAdded = false;
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }


    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new Item());
    }

    public void removeLoadingFooter() {
        isLoadingAdded = false;

        int position = itemList.size() - 1;
        Item result = getItem(position);

        if (result != null) {
            itemList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Item getItem(int position) {
        return itemList.get(position);
    }


    public void showRetry(boolean show, @Nullable String errorMsg) {
        retryPageLoad = show;
        notifyItemChanged(itemList.size() - 1);

        if (errorMsg != null) this.errorMsg = errorMsg;
    }

//    private GlideRequest<Drawable> loadImage(@NonNull String posterPath) {
//        return GlideApp
//                .with(context)
//                .load(posterPath)
//                .centerCrop();
//    }


}
