package com.majeur.psclient;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.service.TeamsMessageObserver;
import com.majeur.psclient.util.Callback;
import com.majeur.psclient.util.UserTeamsStore;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;

public class TeamsFragment extends Fragment implements MainActivity.Callbacks {

    private TeamsMessageObserver mObserver = new TeamsMessageObserver();
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
    public void onShowdownServiceBound(ShowdownService service) {
        service.registerMessageObserver(mObserver, false);
    }

    @Override
    public void onShowdownServiceWillUnbound(ShowdownService service) {
        service.unregisterMessageObserver(mObserver);
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
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                startActivity(new Intent(getContext(), TeamEditActivity.class));
                return true;
            }
        });

        view.findViewById(R.id.button_new_team).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<BattleFormat.Category> battleFormatCategories = mObserver.getBattleFormatCategories();
                ImportTeamDialog.newInstance(TeamsFragment.this, battleFormatCategories)
                        .show(getFragmentManager(), "");
            }
        });
    }

    private void persistUserTeams() {
        mUserTeamsStore.write(mTeamGroups, new Callback<Boolean>() {
            @Override
            public void callback(Boolean success) {
                if (!success)
                    Toast.makeText(getContext(), "Persist teams failed", 0).show();
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

        group.teams.add(team);
        mTeamListAdapter.notifyDataSetChanged();
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
