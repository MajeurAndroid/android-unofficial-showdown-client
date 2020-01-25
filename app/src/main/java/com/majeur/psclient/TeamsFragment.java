package com.majeur.psclient;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.Callback;
import com.majeur.psclient.util.UserTeamsStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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
    private BattleFormat mFallbackFormat = BattleFormat.FORMAT_OTHER;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                     @Nullable Bundle savedInstanceState) {
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
                List<BattleFormat.Category> battleFormats = ((MainActivity) getActivity())
                        .getHomeFragment().getBattleFormats();
                intent.putExtra(TeamEditActivity.INTENT_EXTRA_FORMATS, (Serializable) battleFormats);
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
                getActivity().getMenuInflater().inflate(R.menu.context_menu_team, contextMenu);
            }
        });

        final ExtendedFloatingActionButton fab = view.findViewById(R.id.button_new_team);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImportTeamDialog.newInstance(TeamsFragment.this)
                        .show(getFragmentManager(), "");
            }
        });

        final Runnable showFab = new Runnable() {
            @Override
            public void run() {
                fab.show();
            }
        };
        mExpandableListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    fab.postDelayed(showFab, 750);
                } else {
                    fab.removeCallbacks(showFab);
                    fab.hide();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (!(item.getMenuInfo() instanceof ExpandableListView.ExpandableListContextMenuInfo))
            return false;

        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
        final Team team = mTeamListAdapter.getChild(groupPos, childPos);

        switch (item.getItemId()) {
            case R.id.action_rename:
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_team_name, null);
                final EditText editText = dialogView.findViewById(R.id.edit_text_team_name);
                editText.setText(team.label);
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Rename team")
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String input = editText.getText().toString();
                                String regex = "[{}:\",|\\[\\]]";
                                if (input.matches(".*" + regex + ".*")) input = input.replaceAll(regex, "");
                                if (TextUtils.isEmpty(input)) input = "Unnamed team";
                                team.label = input;
                                notifyGroupChanged();
                                persistUserTeams();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setView(dialogView)
                        .show();
                editText.requestFocus();
                return true;
            case R.id.action_delete:
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Are you sure you want to delete this team ?")
                        .setMessage("This action can't be undone.")
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
            default:
                return false;
        }
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
                    Toast.makeText(getContext(), "Failed to save teams.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onTeamsImported(List<Team> teams) {
        for (Team team : teams)
            addTeam(team);
        persistUserTeams();
    }

    private void addTeam(Team newTeam) {
        if (newTeam.format == null) newTeam.format = toId(mFallbackFormat.getLabel());

        // If true, team was present and has been updated with newTeam data and we're done
        // If false, team do not exist or need to be placed into another group
        boolean teamUpdated = updateTeamInternal(newTeam);

        if (!teamUpdated) { // Team need to be added to according group
            boolean teamAdded = false;
            for (Team.Group group : mTeamGroups) {
                if (group.format.equals(newTeam.format)) {
                    group.teams.add(newTeam);
                    teamAdded = true;
                }
            }
            if (!teamAdded) { // No group matching new team was found
                Team.Group group = new Team.Group(newTeam.format);
                group.teams.add(newTeam);
                mTeamGroups.add(group);
            }
        }
        notifyGroupChanged();
    }

    private boolean updateTeamInternal(Team newTeam) {
        for (Team.Group group : mTeamGroups) {
            for (int i = 0; i < group.teams.size(); i++) {
                Team team = group.teams.get(i);
                if (team.uniqueId == newTeam.uniqueId) {
                    if (newTeam.format.equals(group.format)) {
                        group.teams.set(i, newTeam);
                        return true;
                    } else {
                        group.teams.remove(i);
                        if (group.teams.isEmpty())
                            mTeamGroups.remove(group);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private void removeTeam(Team team) {
        for (Team.Group group : mTeamGroups) {
            if (group.format.equals(team.format)) {
                group.teams.remove(team);
                if (group.teams.isEmpty())
                    mTeamGroups.remove(group);
                break;
            }
        }
        notifyGroupChanged();
    }

    private void notifyGroupChanged() {
        Collections.sort(mTeamGroups, new Comparator<Team.Group>() {
            @Override
            public int compare(Team.Group group1, Team.Group group2) {
                return group1.format.compareTo(group2.format);
            }
        });
        mTeamListAdapter.notifyDataSetChanged();
        MainActivity activity = (MainActivity) getContext();
        activity.getHomeFragment().updateTeamSpinner();
    }

    public List<Team.Group> getTeamGroups() {
        List<Team.Group> copy = new LinkedList<>();
        copy.addAll(mTeamGroups);
        return copy;
    }

    private String resolveFormatName(String formatId) {
        MainActivity activity = (MainActivity) getContext();
        if (activity == null) return formatId;
        return activity.getHomeFragment().resolveBattleFormatName(formatId);
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
            String label = resolveFormatName(getGroup(i).format);
            ((TextView) view).setText(label);
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

            for (int k = 0; k < viewHolder.pokemonViews.length; k++)
                viewHolder.pokemonViews[k].setImageDrawable(null);
            if (team.pokemons.isEmpty()) return view;
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
