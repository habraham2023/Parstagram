package com.habraham.parstagram.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.habraham.parstagram.models.Comment;
import com.habraham.parstagram.models.Like;
import com.habraham.parstagram.models.Post;
import com.habraham.parstagram.R;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "DetailFragment";
    Post mPost;
    String objectId;
    TextView tvUsername;
    TextView tvDescription;
    TextView tvFavCount;
    TextView tvTime;
    ImageView ivProfile;
    ImageView ivPost;
    ImageView ivFav;
    ImageView ivComment;

    public DetailFragment() {
        // Required empty public constructor
    }

    public DetailFragment(Post post) {
        mPost = post;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUsername = view.findViewById(R.id.tvUsername);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvFavCount = view.findViewById(R.id.tvFavCount);
        tvTime = view.findViewById(R.id.tvTime);
        ivProfile = view.findViewById(R.id.ivProfile);
        ivPost = view.findViewById(R.id.ivProfileImage);
        ivFav = view.findViewById(R.id.ivFav);
        ivComment = view.findViewById(R.id.ivComment);

        ivProfile.setOnClickListener(this);
        tvUsername.setOnClickListener(this);

        setPost();

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStackImmediate();
            }
        });


        ivFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Post.IDsofPostsLikedByCurrentUser.contains(mPost.getObjectId())) {
                    Log.i(TAG, "onClick: " + mPost.getObjectId());
                    ParseQuery<Like> findLike = ParseQuery.getQuery(Like.class);
                    findLike.whereEqualTo(Like.KEY_USER, ParseUser.getCurrentUser());
                    findLike.whereEqualTo(Like.KEY_POST, mPost);
                    findLike.getFirstInBackground(new GetCallback<Like>() {
                        @Override
                        public void done(Like like, ParseException e) {
                            if (e != null) {
                                Log.e(TAG, "done: ", e);
                                return;
                            }
                            Log.i(TAG, "done: " + like.getPost().getObjectId());
                            like.deleteInBackground();
                            Glide.with(getContext()).load(R.drawable.ufi_heart).into(ivFav);
                            Post.IDsofPostsLikedByCurrentUser.remove(mPost.getObjectId());
                            tvFavCount.setText(""+(Integer.parseInt(tvFavCount.getText().toString()) - 1));
                        }
                    });
                } else {
                    final Like like = new Like();
                    like.setPost(mPost);
                    like.setUser(ParseUser.getCurrentUser());
                    like.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            Glide.with(getContext()).load(R.drawable.ufi_heart_active).into(ivFav);
                            Post.IDsofPostsLikedByCurrentUser.add(mPost.getObjectId());
                            tvFavCount.setText(""+(Integer.parseInt(tvFavCount.getText().toString()) + 1));
                        }
                    });
                }
            }
        });

        ivComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommentsFragment commentsFragment = CommentsFragment.newInstance(mPost.getComments(), mPost);
                AppCompatActivity activity = (AppCompatActivity) view.getContext();
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.flContainer, commentsFragment).addToBackStack(null).commit();
            }
        });

        if (Post.IDsofPostsLikedByCurrentUser.contains(mPost.getObjectId())) {
            Glide.with(getContext()).load(R.drawable.ufi_heart_active).into(ivFav);
        }

        ParseQuery<Like> likeQuery = ParseQuery.getQuery(Like.class);
        likeQuery.whereEqualTo(Like.KEY_POST, mPost);
        likeQuery.findInBackground(new FindCallback<Like>() {
            @Override
            public void done(List<Like> likes, ParseException e) {
                tvFavCount.setText("" + likes.size());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    // Set the content of the detail page
    private void setPost() {
        tvUsername.setText(mPost.getUser().getUsername());
        tvDescription.setText(mPost.getDescription());
        tvTime.setText(setTime(mPost.getCreatedAt().toString()));

        if (mPost.getImage() != null)
            Glide.with(getContext()).load(mPost.getImage().getUrl()).into(ivPost);

        ParseFile profilePic = mPost.getUser().getParseFile("profilePic");
        if (profilePic != null)
            Glide.with(getContext()).load(profilePic.getUrl()).transform(new CircleCrop()).into(ivProfile);
        else
            Glide.with(getContext()).load(R.drawable.default_pic).transform(new CircleCrop()).into(ivProfile);
    }

    private String setTime(String createdAt) {
        String format = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(format, Locale.ENGLISH);
        sf.setLenient(true);

        String relativeDate = "";
        try {
            long dateMillis = sf.parse(createdAt).getTime();
            relativeDate = DateUtils.getRelativeTimeSpanString(dateMillis,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        return relativeDate;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivProfile:
            case R.id.tvUsername:
                ProfileFragment profileFragment = ProfileFragment.newInstance(mPost.getUser());
                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.flContainer, profileFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
        }
    }
}