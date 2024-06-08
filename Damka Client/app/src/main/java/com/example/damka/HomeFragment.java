package com.example.damka;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class HomeFragment extends Fragment {
    private static final String ARG_USER = "user";
    private static final String ARG_MAIL = "mail";
    private User loggedUser;
    private String loggedUserMail;

    private List<Notification> inboxMessages;
    private NotificationAdapter adapter;
    private boolean editSettingState = false;
    private boolean isPopupShowing = false;

    private View settingPopupView;
    private View inboxPopupView;
    private TextView usernameView;
    private TextView userIDView;
    private TextView ratingView;
    private TextView inboxNumber;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(User user, String mail) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER, user);
        args.putString(ARG_MAIL, mail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inboxMessages = new ArrayList<>();
        if (getArguments() != null) {
            this.loggedUser = getArguments().getParcelable(ARG_USER);
            this.loggedUserMail = getArguments().getString(ARG_MAIL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        usernameView = view.findViewById(R.id.username_view);
        userIDView = view.findViewById(R.id.userID_view);
        ratingView = view.findViewById(R.id.rating_view);
        inboxNumber = view.findViewById(R.id.inbox_number);
        inboxNumber.bringToFront();
        inboxNumber.setVisibility(View.GONE);
        refresh();

        Button button = view.findViewById(R.id.play_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        usernameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendGetProfile(loggedUser.getUserID());
            }
        });

        Button showSettingPopupBtn = view.findViewById(R.id.setting_button);
        showSettingPopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingPopup(v);
            }
        });

        Button showInboxPopupBtn = view.findViewById(R.id.inbox_button);
        showInboxPopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInboxPopup(v);
            }
        });

        return view;
    }

    public void refresh() {
        if (loggedUser != null) {
            usernameView.setText(loggedUser.getUsername());
            userIDView.setText(String.format("#%s", loggedUser.getUserID()));
            ratingView.setText(String.valueOf((int)loggedUser.getRating()));
        }
    }

   private void updateInbox() {
        if (inboxMessages.isEmpty())
        {
            inboxNumber.setVisibility(View.GONE);
        } else {
            inboxNumber.setVisibility(View.VISIBLE);
            inboxNumber.bringToFront();
            inboxNumber.setText(String.valueOf(inboxMessages.size()));
        }

        if(isPopupShowing && adapter != null) {
            ListView listView = inboxPopupView.findViewById(R.id.inbox_popup_view);
            TextView emptyView = inboxPopupView.findViewById(R.id.inbox_no_messages);

            if (inboxMessages.isEmpty())
            {
                listView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                listView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }

            adapter.clear();
            adapter.addAll(inboxMessages);
            adapter.notifyDataSetChanged();
        }
   }

    public void setInboxMessages(List<Notification> messages) {
        inboxMessages = messages;
        updateInbox();
    }

    public void addInboxMessages(Notification message) {
        inboxMessages.add(message);
        updateInbox();
    }

    private void play() {
        ((MenuActivity)requireActivity()).startGameActivity();
    }

    private void showSettingPopup(View anchorView) {
        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        settingPopupView = inflater.inflate(R.layout.setting_popup, null);

        TextView versionView = settingPopupView.findViewById(R.id.setting_version_label);
        versionView.setText(Constants.version);

        EditText usernameEditText = settingPopupView.findViewById(R.id.edit_username_box);
        EditText mailEditText = settingPopupView.findViewById(R.id.edit_mail_box);
        CheckBox privacyCheckbox = settingPopupView.findViewById(R.id.privacy_setting_checkbox);
        Button EditBtn = settingPopupView.findViewById(R.id.enable_edit_button);
        Button logoutBtn = settingPopupView.findViewById(R.id.logout_button);

        PopupWindow popupWindow = new PopupWindow(settingPopupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        // Set the system UI flags on the popup window's decor view
        popupWindow.getContentView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, -125);

        usernameEditText.setText(loggedUser.getUsername());
        mailEditText.setText(loggedUserMail);
        privacyCheckbox.setChecked(loggedUser.getPrivacy());
        usernameEditText.setEnabled(false);
        mailEditText.setEnabled(false);
        privacyCheckbox.setEnabled(false);

        EditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editSettingState) { // enable edit
                    usernameEditText.setEnabled(true);
                    mailEditText.setEnabled(true);
                    privacyCheckbox.setEnabled(true);

                    TextView errorText = settingPopupView.findViewById(R.id.setting_error_text);
                    errorText.setText("");
                    errorText.setVisibility(View.GONE);
                    EditBtn.setText("Submit");
                    editSettingState = true;
                } else { // submit changes
                    usernameEditText.setEnabled(false);
                    mailEditText.setEnabled(false);
                    privacyCheckbox.setEnabled(false);
                    String username = usernameEditText.getText().toString();
                    String mail = mailEditText.getText().toString();
                    boolean privacy = privacyCheckbox.isChecked();
                    int privacyInt = privacy ? 1 : 0;
                    if (username.equals(loggedUser.getUsername()))
                    {
                        username = "";
                    }
                    if (mail.equals(loggedUserMail))
                    {
                        mail = "";
                    }
                    if (privacy == loggedUser.getPrivacy())
                    {
                        privacyInt = -1;
                    }
                    if (!(username.equals("") && mail.equals("") && privacyInt == -1)) {
                        Communicator.getInstance().sendUpdateData(username, mail, privacyInt);
                    }

                    EditBtn.setText("Edit");
                    editSettingState = false;
                }
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // Set the flag to false when the popup is dismissed
                TextView errorText = settingPopupView.findViewById(R.id.setting_error_text);
                errorText.setText("");
                errorText.setVisibility(View.GONE);
            }
        });
    }

    private static String insertNewlineEveryNChars(String input) {
        StringBuilder builder = new StringBuilder(input);
        int offset = 0;
        while (offset < builder.length()) {
            offset += 20;
            if (offset < builder.length()) {
                builder.insert(offset, "\n");
                offset++; // Move past the inserted newline
            }
        }
        return builder.toString();
    }

    public void showSettingError(String field, String error) {
        String errorMsg = insertNewlineEveryNChars(error);
        String msg = "Field: " + field + "\nerror: " + errorMsg;
        TextView errorText = this.settingPopupView.findViewById(R.id.setting_error_text);
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void logout() {
        String alertMsg = "Are you sure you want to logout?\n" +
                "If you choose to continue you will have to log in again later.";
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(alertMsg)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Communicator.getInstance().sendCode(Constants.LOGOUT_CODE);
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

    public void showInboxPopup(View anchorView) {
        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inboxPopupView = inflater.inflate(R.layout.inbox_popup, null);

        ListView listView = inboxPopupView.findViewById(R.id.inbox_popup_view);
        TextView emptyView = inboxPopupView.findViewById(R.id.inbox_no_messages);

        if (inboxMessages.isEmpty())
        {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        PopupWindow popupWindow = new PopupWindow(inboxPopupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        // Set the system UI flags on the popup window's decor view
        popupWindow.getContentView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);

        sortInboxByTime();
        adapter = new NotificationAdapter(requireContext(), popupWindow, inboxMessages);
        listView.setAdapter(adapter);

        Button closeBtn = inboxPopupView.findViewById(R.id.close_inbox_button);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // Set the flag to false when the popup is dismissed
                isPopupShowing = false;
            }
        });
        isPopupShowing = true;
    }

    private void sortInboxByTime() {
        inboxMessages.sort(new Comparator<Notification>() {
            @Override
            public int compare(Notification notification1, Notification notification2) {
                // Assuming timestamp is a String in format "yyyy-MM-dd HH:mm:ss"
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                try {
                    Date date1 = sdf.parse(notification1.getTime());
                    Date date2 = sdf.parse(notification2.getTime());
                    // Compare dates
                    assert date2 != null;
                    return date2.compareTo(date1); // Descending order
                    // For ascending order, use: return date1.compareTo(date2);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
    }
}

