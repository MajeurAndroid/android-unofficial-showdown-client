package com.majeur.psclient;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.AllItemsLoader;
import com.majeur.psclient.io.AllSpeciesLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.LearnsetLoader;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.Pokemon;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.widget.CategoryAdapter;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import static com.majeur.psclient.util.Utils.addNullSafe;

public class TeamEditActivity extends AppCompatActivity {

    public static final int INTENT_REQUEST_CODE = 194;
    public static final String INTENT_EXTRA_TEAM = "intent-extra-team";
    public static final String INTENT_EXTRA_FORMATS = "intent-extra-formats";

    private AllSpeciesLoader mSpeciesLoader;
    private AllItemsLoader mItemsLoader;
    private DexPokemonLoader mDexPokemonLoader;
    private LearnsetLoader mLearnsetLoader;
    private GlideHelper mGlideHelper;

    private List<BattleFormat.Category> mBattleFormats;
    private Team mTeam;
    private boolean mTeamNeedsName;
    private Pokemon[] mPokemons;

    @SuppressWarnings("Unchecked")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_team);
        setResult(RESULT_CANCELED, null);

        mSpeciesLoader = new AllSpeciesLoader(this);
        mItemsLoader = new AllItemsLoader(this);
        mDexPokemonLoader = new DexPokemonLoader(this);
        mLearnsetLoader = new LearnsetLoader(this);
        mGlideHelper = new GlideHelper(this);

        mBattleFormats = (List<BattleFormat.Category>) getIntent().getSerializableExtra(INTENT_EXTRA_FORMATS);
        mTeam = (Team) getIntent().getSerializableExtra(INTENT_EXTRA_TEAM);
        if (mTeam == null) {
            mTeam = new Team("Unnamed team", new LinkedList<Pokemon>(), BattleFormat.FORMAT_OTHER.id());
            mTeamNeedsName = true;
        }

        mPokemons = new Pokemon[6];
        for (int i = 0; i < mTeam.pokemons.size(); i++) mPokemons[i] = mTeam.pokemons.get(i);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        Spinner spinner = new Spinner(actionBar.getThemedContext());
        actionBar.setCustomView(spinner);
        CategoryAdapter adapter = new CategoryAdapter(actionBar.getThemedContext()) {
            @Override
            protected boolean isCategoryItem(int position) {
                return getItem(position) instanceof BattleFormat.Category;
            }

            @Override
            protected String getCategoryLabel(int position) {
                return ((BattleFormat.Category) getItem(position)).getLabel();
            }

            @Override
            protected String getItemLabel(int position) {
                return ((BattleFormat) getItem(position)).getLabel();
            }
        };
        spinner.setAdapter(adapter);
        adapter.addItem(BattleFormat.FORMAT_OTHER);
        if (mBattleFormats != null) {
            int count = 1;
            for (BattleFormat.Category category : mBattleFormats) {
                adapter.addItem(category); count++;
                for (BattleFormat format : category.getBattleFormats()) {
                    if (!format.isTeamNeeded()) continue;
                    adapter.addItem(format);
                    if (mTeam != null && format.id().equalsIgnoreCase(mTeam.format))
                        spinner.setSelection(count);
                    count++;
                }
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                CategoryAdapter adapter = (CategoryAdapter) adapterView.getAdapter();
                BattleFormat format = (BattleFormat) adapter.getItem(position);
                mTeam.format = format.id();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        ViewPager viewPager = findViewById(R.id.teamViewPager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                return PokemonEditFragment.create(position, mPokemons[position]);
            }

            @Override
            public int getCount() {
                return 6;
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return "Slot " + (position + 1);
            }
        });

        PagerTabStrip pagerTabStrip = findViewById(R.id.teamViewPagerTabStrip);
        pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.accent));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_team, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                prepareTeam();
                Intent intent = new Intent();
                intent.putExtra(INTENT_EXTRA_TEAM, mTeam);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTeamNeedsName) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_team_name, null);
            final EditText editText = dialogView.findViewById(R.id.edit_text_team_name);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Team name")
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String input = editText.getText().toString();
                            String regex = "[{}:\",|\\[\\]]";
                            if (input.matches(".*" + regex + ".*")) input = input.replaceAll(regex, "");
                            if (TextUtils.isEmpty(input)) input = "Unnamed team";
                            mTeam.label = input;
                            mTeamNeedsName = false;
                        }
                    })
                    .setCancelable(false)
                    .setView(dialogView)
                    .show();
            editText.requestFocus();
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Changes will be lost")
                .setMessage("Are you sure you want to quit without applying changes ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public AllSpeciesLoader getSpeciesLoader() {
        return mSpeciesLoader;
    }

    public AllItemsLoader getItemsLoader() {
        return mItemsLoader;
    }

    public DexPokemonLoader getDexPokemonLoader() {
        return mDexPokemonLoader;
    }

    public LearnsetLoader getLearnsetLoader() {
        return mLearnsetLoader;
    }

    public GlideHelper getGlideHelper() {
        return mGlideHelper;
    }

    public void onPokemonUpdated(int slotIndex, Pokemon pokemon) {
        mPokemons[slotIndex] = pokemon;
        prepareTeam();
    }

    private void prepareTeam() {
        mTeam.pokemons.clear();
        for (Pokemon pokemon : mPokemons) addNullSafe(mTeam.pokemons, pokemon);
    }
}
