package com.example.damka;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import java.util.List;

public class LeaderboardAdapter extends ArrayAdapter<User> {

    private final List<User> userList;
    private final LayoutInflater inflater;

    public LeaderboardAdapter(Context context, List<User> userList) {
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
            view = inflater.inflate(R.layout.leaderboard_item, parent, false);

            // Create a ViewHolder to hold references to views
            viewHolder = new ViewHolder();
            viewHolder.nameTextView = view.findViewById(R.id.leaderboard_username);
            viewHolder.ratingTextView = view.findViewById(R.id.leaderboard_rating);
            viewHolder.topNumber = view.findViewById(R.id.leaderboard_top_number);

            // Set the ViewHolder as a tag on the view
            view.setTag(viewHolder);
        } else {
            // Reuse the ViewHolder and views
            viewHolder = (ViewHolder) view.getTag();
        }

        // Get the User object for this position
        User user = userList.get(position);

        // Bind data to views
        viewHolder.nameTextView.setText(user.getUsername());
        viewHolder.ratingTextView.setText(String.valueOf((int)user.getRating()));
        viewHolder.topNumber.setText(String.valueOf(position + 1));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendGetProfile(user.getUserID());
            }
        });

        return view;
    }

    // ViewHolder pattern to improve performance by caching views
    private static class ViewHolder {
        TextView nameTextView;
        TextView ratingTextView;
        TextView topNumber;
    }
}
