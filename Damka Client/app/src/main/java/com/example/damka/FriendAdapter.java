package com.example.damka;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import java.util.List;

public class FriendAdapter extends ArrayAdapter<User> {

    private final List<User> userList;
    private final LayoutInflater inflater;

    public FriendAdapter(Context context, List<User> userList) {
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
            view = inflater.inflate(R.layout.friend_item, parent, false);

            // Create a ViewHolder to hold references to views
            viewHolder = new ViewHolder();
            viewHolder.nameTextView = view.findViewById(R.id.nameTextView);
            viewHolder.ratingTextView = view.findViewById(R.id.ratingTextView);
            viewHolder.onlineTextView = view.findViewById(R.id.onlineStateTextView);

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
        setOnlineView(viewHolder.onlineTextView, user.getOnline());

        Button inviteBtnAction = view.findViewById(R.id.game_invite_friend_button);
        inviteBtnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MenuActivity)getContext()).startGameActivityFriendly(user, 0);
            }
        });
        inviteBtnAction.setEnabled(user.getOnline() == 1);
        if (user.getOnline() == 1) {
            inviteBtnAction.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.blue));
        } else {
            inviteBtnAction.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.offline_color));
        }

        Button removeBtnAction = view.findViewById(R.id.remove_friend_button);
        removeBtnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoveFriend(user);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendGetProfile(user.getUserID());
            }
        });

        return view;
    }

    private void RemoveFriend(User user) {
        String alertMsg = "Are you sure want to remove " + user.getUsername() + " from your friends?";
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(alertMsg)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Communicator.getInstance().sendCodeAndID(Constants.REMOVE_FRIEND_CODE, user.getUserID());
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setOnlineView(TextView onlineView, int online)
    {
        Drawable newDrawable = null;
        if (online == 0) {
            onlineView.setText(" Offline");
            newDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.offline_state_circle);
        } else if (online == 1) {
            onlineView.setText(" Online");
            newDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.online_state_circle);
        } else if (online == 2) {
            onlineView.setText(" In Match");
            newDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.in_match_state_circle);
        }
        onlineView.setCompoundDrawablesRelativeWithIntrinsicBounds(newDrawable, null, null, null);
    }

    // ViewHolder pattern to improve performance by caching views
    private static class ViewHolder {
        TextView nameTextView;
        TextView ratingTextView;
        TextView onlineTextView;
    }
}
