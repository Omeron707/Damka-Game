package com.example.damka;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


public class LeaderBoardFragment extends Fragment {

    private static final String ARG_PARAM = "leaderboard_list";

    private LeaderboardAdapter leaderboardAdapter;
    private List<User> leaderboardList;

    public LeaderBoardFragment() {
        // Required empty public constructor
    }

    public static LeaderBoardFragment newInstance(List<User> users) {
        LeaderBoardFragment fragment = new LeaderBoardFragment();
        Bundle args = new Bundle();
        if (users != null) {
            args.putParcelableArrayList(ARG_PARAM, (ArrayList<User>) users);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            leaderboardList = getArguments().getParcelableArrayList(ARG_PARAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_leader_board, container, false);
        ListView leaderboardListView = view.findViewById(R.id.leaderboard_list);

        leaderboardAdapter = new LeaderboardAdapter(requireContext(), leaderboardList);
        leaderboardListView.setAdapter(leaderboardAdapter);

        Button refreshLeaderBoard = view.findViewById(R.id.refresh_leaderboard);
        refreshLeaderBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communicator.getInstance().sendCode(Constants.GET_LEADERBOARD_CODE);
            }
        });

        return view;
    }

    // Method to set the user list
    public void setLeaderboardList(List<User> users) {
        this.leaderboardList = users;
        if (leaderboardAdapter != null) {
            leaderboardAdapter.clear();
            leaderboardAdapter.addAll(leaderboardList);
            leaderboardAdapter.notifyDataSetChanged();
        }
    }
}