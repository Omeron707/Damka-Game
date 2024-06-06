package com.example.damka;


import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FriendsFragment extends Fragment {

    private TextView friendRequestTitle;
    private TextView friendsTitle;
    private ListView friendsListView;
    private ListView requestsListView;
    private FriendAdapter friendsAdapter;
    private FriendRequestAdapter requestAdapter;
    private EditText searchUserBar;
    private List<User> friendsList;
    private List<User> friendsRequestsList;

    public FriendsFragment() {
        // Required empty public constructor
    }

    public static FriendsFragment newInstance(List<User> friendsList, List<User> requestsList) {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        if (friendsList != null && requestsList != null) {
            args.putParcelableArrayList("friendsList", (ArrayList<User>) friendsList);
            args.putParcelableArrayList("requestsFriendsList", (ArrayList<User>) requestsList);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            friendsList = getArguments().getParcelableArrayList("friendsList");
            friendsRequestsList = getArguments().getParcelableArrayList("requestsFriendsList");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        friendsListView = view.findViewById(R.id.friends_list_view);
        requestsListView = view.findViewById(R.id.friends_requests_list_view);

        friendRequestTitle = view.findViewById(R.id.friends_requests_title_view);
        friendsTitle = view.findViewById(R.id.friends_title_view);

        // Initialize the adapter
        friendsAdapter = new FriendAdapter(requireContext(), friendsList);
        friendsListView.setAdapter(friendsAdapter);

        requestAdapter = new FriendRequestAdapter(requireContext(), friendsRequestsList);
        requestsListView.setAdapter(requestAdapter);

        searchUserBar = view.findViewById(R.id.search_users);

        Button refreshBtn = view.findViewById(R.id.refresh_user_button);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
            }
        });

        Button searchBtn = view.findViewById(R.id.add_user_button);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUser();
            }
        });

        searchUserBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    // Perform your action here
                    searchUser();
                    return true;
                }
                return false;
            }
        });
        setUIElements();
        return view;
    }

    private void searchUser() {
        String userID = searchUserBar.getText().toString();
        if (!userID.isEmpty())
        {
            // Check if user input starts with '#'
            // if it does remove it
            if (userID.startsWith("#")) {
                userID = userID.substring(1);
            }
            Communicator.getInstance().sendCodeAndID(Constants.ADD_FRIEND_CODE, userID);
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchUserBar.getWindowToken(), 0);
        }
        searchUserBar.setText("");
    }

    private void setUIElements() {
        if (friendsList.isEmpty() && friendsRequestsList.isEmpty()) // nothing
        {
            this.friendRequestTitle.setText("Nothing to see here yet! \ntry to find some friends to add!");
            this.friendRequestTitle.setVisibility(View.VISIBLE);
            this.friendsTitle.setVisibility(View.GONE);
        }
        else if (friendsList.isEmpty()) // only requests
        {
            this.friendsTitle.setVisibility(View.GONE);
            this.friendRequestTitle.setText("Friend Requests");
            this.friendRequestTitle.setVisibility(View.VISIBLE);
        }
        else if (friendsRequestsList.isEmpty()) // only friends
        {
            this.friendRequestTitle.setVisibility(View.GONE);
        }
        else // both
        {
            this.friendRequestTitle.setText("Friend Requests");
            this.friendRequestTitle.setVisibility(View.VISIBLE);
            this.friendsTitle.setVisibility(View.VISIBLE);
        }
    }

    // Method to set the user list
    public void setFriendsList(List<User> friendsList, List<User> requestsFriendsList) {
        this.friendsList = friendsList;
        this.friendsRequestsList = requestsFriendsList;
        setUIElements();

        if (friendsAdapter != null) {
            friendsAdapter.clear();
            friendsAdapter.addAll(friendsList);
            friendsAdapter.notifyDataSetChanged();
        }
        if (requestAdapter != null) {
            requestAdapter.clear();
            requestAdapter.addAll(requestsFriendsList);
            requestAdapter.notifyDataSetChanged();
        }
    }

    public void highlightFriendRequest(String id) {
        int position = -1;

        // Find the position of the item in the adapter based on its ID
        for (int i = 0; i < this.requestAdapter.getCount(); i++) {
            User user = this.requestAdapter.getItem(i);
            if (user != null && Objects.equals(user.getUserID(), id)) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            // Get the view of the item at the found position
            View friendRequest = requestsListView.getChildAt(position);

            // Highlight the item with animation
            friendRequest.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));

            // Delay for 1 second to remove highlighting
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Reset background color after 1 second
                    friendRequest.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.friend_element_background));
                }
            }, 1000); // 1000 milliseconds = 1 second
        }
    }

    public User getFriend(String id) {
        for (User friend: this.friendsList) {
            if (Objects.equals(friend.getUserID(), id)) {
                return friend;
            }
        }
        return null;
    }

    public void highlightFriend(String id) {
        int position = -1;

        // Find the position of the item in the adapter based on its ID
        for (int i = 0; i < this.friendsAdapter.getCount(); i++) {
            User user = this.friendsAdapter.getItem(i);
            if (user != null && Objects.equals(user.getUserID(), id)) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            // Get the view of the item at the found position
            View friend = friendsListView.getChildAt(position);

            // Highlight the item with animation
            friend.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));

            // Delay for 1 second to remove highlighting
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Reset background color after 1 second
                    friend.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.friend_element_background));
                }
            }, 1000); // 1000 milliseconds = 1 second
        }
    }
}