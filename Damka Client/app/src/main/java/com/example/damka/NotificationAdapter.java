package com.example.damka;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class NotificationAdapter extends ArrayAdapter<Notification> {

    private final List<Notification> notificationsList;
    private final PopupWindow mPopupWindow;
    private final LayoutInflater inflater;

    public NotificationAdapter(Context context, PopupWindow popup, List<Notification> notificationsList) {
        super(context, R.layout.inbox_item, notificationsList);
        this.notificationsList = notificationsList;
        this.mPopupWindow = popup;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;

        if (view == null) {
            // Inflate the custom layout for each item
            view = inflater.inflate(R.layout.inbox_item, parent, false);


            // Create a ViewHolder to hold references to views
            viewHolder = new ViewHolder();
            viewHolder.titleTextView = view.findViewById(R.id.inbox_notification_title);
            viewHolder.contentTextView = view.findViewById(R.id.inbox_notification_description);
            viewHolder.timeTextView = view.findViewById(R.id.inbox_notification_time);

            // Set the ViewHolder as a tag on the view
            view.setTag(viewHolder);
        } else {
            // Reuse the ViewHolder and views
            viewHolder = (ViewHolder) view.getTag();
        }

        // Get the User object for this position
        Notification notification = notificationsList.get(position);

        String title = Constants.NotificationTypes.get(notification.getType());
        viewHolder.titleTextView.setText(title);
        viewHolder.contentTextView.setText(notification.getContent());
        viewHolder.timeTextView.setText(notification.getTime());

        Button buttonAction = view.findViewById(R.id.remove_notification);
        buttonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MenuActivity)getContext()).sendMarkNotificationAsRead(notification.getId());
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                handleClick(notification);
            }
        });

        return view;
    }

    private void handleClick(Notification notification) {
        switch (notification.getType()) {
            case 1: //friend request
                ((MenuActivity)getContext()).FriendRequestNotificationClicked(notification.getSourceID());
                break;
            case 2: // friend accept
                ((MenuActivity)getContext()).FriendNotificationClicked(notification.getSourceID());
                break;
        }
    }

    // ViewHolder pattern to improve performance by caching views
    private static class ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView timeTextView;
    }
}
