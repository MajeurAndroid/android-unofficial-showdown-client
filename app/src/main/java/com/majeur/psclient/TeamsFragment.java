package com.majeur.psclient;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.Callback;
import com.majeur.psclient.util.UserTeamsStore;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;

public class TeamsFragment extends Fragment implements MainActivity.Callbacks {

    private ShowdownService mService;
    private UserTeamsStore mUserTeamsStore;
    private DexIconLoader mDexIconLoader;

    private List<Team.Group> mTeamGroups;

    private ExpandableListView mExpandableListView;
    private TeamListAdapter mTeamListAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mDexIconLoader = ((MainActivity) context).getDexIconLoader();
    }

    @Override
    public void onServiceBound(ShowdownService service) {
        mService = service;
    }

    @Override
    public void onServiceWillUnbound(ShowdownService service) {
        mService = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTeamGroups = new ArrayList<>();
        mTeamListAdapter = new TeamListAdapter();

        mUserTeamsStore = new UserTeamsStore(getContext());

        mUserTeamsStore.read(new Callback<List<Team.Group>>() {
            @Override
            public void callback(List<Team.Group> teamGroups) {
                mTeamGroups.clear();
                mTeamGroups.addAll(teamGroups);
                mTeamListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teams, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mExpandableListView = view.findViewById(R.id.teams_list_view);
        mExpandableListView.setAdapter(mTeamListAdapter);
        mExpandableListView.setEmptyView(view.findViewById(R.id.empty_hint_view));
        mExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int j, long id) {
                Team team = ((TeamListAdapter) expandableListView.getExpandableListAdapter()).getChild(i, j);
                Intent intent = new Intent(getContext(), TeamEditActivity.class);
                intent.putExtra(TeamEditActivity.INTENT_EXTRA_TEAM, team);
                startActivityForResult(intent, TeamEditActivity.INTENT_REQUEST_CODE);
                return true;
            }
        });
        mExpandableListView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                ExpandableListView.ExpandableListContextMenuInfo info =
                        (ExpandableListView.ExpandableListContextMenuInfo) contextMenuInfo;
                int type = ExpandableListView.getPackedPositionType(info.packedPosition);
                if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
                    contextMenu.add(0, 0, 0, "Delete");
            }
        });

        view.findViewById(R.id.button_new_team).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment fragment = ((MainActivity) getContext()).getHomeFragment();
                List<BattleFormat.Category> battleFormatCategories = fragment.getBattleFormats();
                ImportTeamDialog.newInstance(TeamsFragment.this, battleFormatCategories)
                        .show(getFragmentManager(), "");
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        final Team team = mTeamListAdapter.getChild(groupPos, childPos);
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Are you sure you want to delete this team ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removeTeam(team);
                        persistUserTeams();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == TeamEditActivity.INTENT_REQUEST_CODE
                && resultCode == Activity.RESULT_OK && data != null) {
            Team team = (Team) data.getSerializableExtra(TeamEditActivity.INTENT_EXTRA_TEAM);
            addTeam(team);
            persistUserTeams();
        }
    }

    private void persistUserTeams() {
        mUserTeamsStore.write(mTeamGroups, new Callback<Boolean>() {
            @Override
            public void callback(Boolean success) {
                if (!success)
                    Toast.makeText(getContext(), "Persist teams failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onTeamsImported(List<Team> teams) {
        for (Team team : teams)
            addTeam(team);

        persistUserTeams();
    }

    private void addTeam(Team team) {
        Team.Group group = getMatchingGroup(team);
        if (group == null) {
            group = new Team.Group(team.format == null ? "Other" : team.format);
            mTeamGroups.add(group);
        }
        int matchingTeamIndex = -1;
        for (int i = 0; i < group.teams.size(); i++)
            if (group.teams.get(i).uniqueId == team.uniqueId) matchingTeamIndex = i;
        if (matchingTeamIndex != -1)
            group.teams.set(matchingTeamIndex, team);
        else
            group.teams.add(team);
        mTeamListAdapter.notifyDataSetChanged();
        MainActivity activity = (MainActivity) getContext();
        activity.getHomeFragment().updateTeamSpinner();
    }

    private Team.Group getMatchingGroup(Team team) {
        if (mTeamGroups.size() == 0) return null;
        for (Team.Group group : mTeamGroups) {
            if (team.format == null && group.format.equals("Other"))
                return group;
            if (group.format.equals(team.format))
                return group;
        }
        return null;
    }

    private boolean removeTeam(Team team) {
        Team.Group group = getMatchingGroup(team);
        if (group == null) return false;
        group.teams.remove(team);
        if (group.teams.isEmpty())
            mTeamGroups.remove(group);
        mTeamListAdapter.notifyDataSetChanged();
        MainActivity activity = (MainActivity) getContext();
        activity.getHomeFragment().updateTeamSpinner();
        return true;
    }

    public List<Team.Group> getTeamGroups() {
        return mTeamGroups;
    }

    private class TeamListAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return mTeamGroups.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return getGroup(i).teams.size();
        }

        @Override
        public Team.Group getGroup(int i) {
            return mTeamGroups.get(i);
        }

        @Override
        public Team getChild(int i, int j) {
            return getGroup(i).teams.get(j);
        }

        @Override
        public View getGroupView(int i, boolean b, View view, ViewGroup parent) {
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.list_category_team, parent, false);

            ((TextView) view).setText(getGroup(i).format);

            return view;
        }

        private class ViewHolder {

            TextView labelView;
            ImageView[] pokemonViews;

            ViewHolder(View view) {
                labelView = view.findViewById(R.id.text_view_title);
                pokemonViews = new ImageView[6];
                pokemonViews[0] = view.findViewById(R.id.image_view_pokemon1);
                pokemonViews[1] = view.findViewById(R.id.image_view_pokemon2);
                pokemonViews[2] = view.findViewById(R.id.image_view_pokemon3);
                pokemonViews[3] = view.findViewById(R.id.image_view_pokemon4);
                pokemonViews[4] = view.findViewById(R.id.image_view_pokemon5);
                pokemonViews[5] = view.findViewById(R.id.image_view_pokemon6);
            }
        }

        @Override
        public View getChildView(int i, int j, boolean b, View view, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.list_item_team, parent, false);
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Team team = getChild(i, j);
            viewHolder.labelView.setText(team.label);

            if (team.pokemons.isEmpty()) {
                for (int k = 0; k < viewHolder.pokemonViews.length; k++)
                    viewHolder.pokemonViews[k].setImageDrawable(null);
                return view;
            }

            String[] queries = new String[team.pokemons.size()];
            for (int k = 0; k < queries.length; k++)
                queries[k] = toId(team.pokemons.get(k).species);
            mDexIconLoader.load(queries, new DataLoader.Callback<Bitmap>() {
                @Override
                public void onLoaded(Bitmap[] results) {
                    for (int k = 0; k < results.length; k++) {
                        Drawable icon = new BitmapDrawable(results[k]);
                        viewHolder.pokemonViews[k].setImageDrawable(icon);
                    }
                }
            });

            for (int k = queries.length; k < viewHolder.pokemonViews.length; k++)
                viewHolder.pokemonViews[k].setImageDrawable(null);

            return view;
        }

        @Override
        public long getGroupId(int i) {
            return 0;
        }

        @Override
        public long getChildId(int i, int i1) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }
    }
}
