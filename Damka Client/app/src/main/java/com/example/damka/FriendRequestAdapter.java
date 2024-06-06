package com.example.damka;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class FriendRequestAdapter extends ArrayAdapter<User> {
    private final List<User> userList;
    private final LayoutInflater inflater;

    public FriendRequestAdapter(Context context, List<User> userList) {
        super(context, R.layout.friend_item, userList);
        this.userList = userList;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;

        if (view == null) {
            // Inflate the custom layout for each item
            view = inflater.inflate(R.layout.friend_request_item, parent, false);

            // Create a ViewHolder to hold references to views
            viewHolder = new ViewHolder();
            viewHolder.nameTextView = view.findViewById(R.id.nameTextView);
            viewHolder.ratingTextView = view.findViewById(R.id.ratingTextView);

            // Set the ViewHolder as a tag on the view
            view.setTag(viewHolder);
        } else {
            // Reuse the ViewHolder and views
            viewHolder = (ViewHolder) view.getTag();
        }

        // Get the User object for this position
        User user = userList.get(position);

        // Bind data to views
        viewHolder.nameTextView.setText("Name: " + user.getUsername());
        viewHolder.ratingTextView.setText("Rating: " + (int)user.getRating());

        Button acceptBtn = view.findViewById(R.id.accept_friend_button);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendCodeAndID(Constants.ACCEPT_FRIEND_CODE, user.getUserID());
            }
        });

        Button denyBtn = view.findViewById(R.id.deny_friend_button);
        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendCodeAndID(Constants.DENY_FRIEND_CODE, user.getUserID());
            }
        });


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform action when the item is clicked
                Communicator.getInstance().sendGetProfile(user.getUserID());
            }
        });

        return view;
    }

    // ViewHolder pattern to improve performance by caching views
    private static class ViewHolder {
        TextView nameTextView;
        TextView ratingTextView;
    }
}
