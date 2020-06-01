package com.majeur.psclient;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.AllItemsLoader;
import com.majeur.psclient.io.AllSpeciesLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.LearnsetLoader;
import com.majeur.psclient.io.MoveDetailsLoader;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.model.TeamPokemon;
import com.majeur.psclient.widget.CategoryAdapter;

import java.util.LinkedList;
import java.util.List;

import static com.majeur.psclient.util.Utils.addNullSafe;

public class EditTeamActivity extends AppCompatActivity {

    public static final int INTENT_REQUEST_CODE = 194;
    public static final String INTENT_EXTRA_TEAM = "intent-extra-team";
    public static final String INTENT_EXTRA_FORMATS = "intent-extra-formats";

    private AllSpeciesLoader mSpeciesLoader;
    private AllItemsLoader mItemsLoader;
    private DexPokemonLoader mDexPokemonLoader;
    private LearnsetLoader mLearnsetLoader;
    private MoveDetailsLoader mMoveDetailsLoader;
    private GlideHelper mGlideHelper;
    private DexIconLoader mDexIconLoader;

    private Team mTeam;
    private boolean mTeamNeedsName;
    private TeamPokemon[] mPokemons;

    @SuppressWarnings({"Unchecked", "unchecked", "ConstantConditions"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_team);
        setResult(RESULT_CANCELED, null);

        mSpeciesLoader = new AllSpeciesLoader(this);
        mItemsLoader = new AllItemsLoader(this);
        mDexPokemonLoader = new DexPokemonLoader(this);
        mLearnsetLoader = new LearnsetLoader(this);
        mMoveDetailsLoader = new MoveDetailsLoader(this);
        mGlideHelper = new GlideHelper(this);
        mDexIconLoader = new DexIconLoader(this);

        List<BattleFormat.Category> battleFormats = (List<BattleFormat.Category>) getIntent().getSerializableExtra(INTENT_EXTRA_FORMATS);
        mTeam = (Team) getIntent().getSerializableExtra(INTENT_EXTRA_TEAM);
        if (mTeam == null) {
            mTeam = new Team("Unnamed team", new LinkedList<>(), BattleFormat.FORMAT_OTHER.id());
            mTeamNeedsName = true;
        }

        mPokemons = new TeamPokemon[6];
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
        if (battleFormats != null) {
            int count = 1;
            for (BattleFormat.Category category : battleFormats) {
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
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                return EditPokemonFragment.create(position, mPokemons[position]);
            }

            @Override
            public int getCount() {
                return 6;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return "Slot " + (position + 1);
            }
        });

        PagerTabStrip pagerTabStrip = findViewById(R.id.teamViewPagerTabStrip);
        pagerTabStrip.setTabIndicatorColor(ContextCompat.getColor(this, R.color.secondary));
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
                    .setPositiveButton("Done", (dialogInterface, i) -> {
                        String input = editText.getText().toString();
                        String regex = "[{}:\",|\\[\\]]";
                        if (input.matches(".*" + regex + ".*")) input = input.replaceAll(regex, "");
                        if (TextUtils.isEmpty(input)) input = "Unnamed team";
                        mTeam.label = input;
                        mTeamNeedsName = false;
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
                .setPositiveButton("Yes", (dialogInterface, i) -> finish())
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

    public MoveDetailsLoader getMoveDetailsLoader() {
        return mMoveDetailsLoader;
    }

    public GlideHelper getGlideHelper() {
        return mGlideHelper;
    }

    public DexIconLoader getDexIconLoader() {
        return mDexIconLoader;
    }

    public void onPokemonUpdated(int slotIndex, TeamPokemon pokemon) {
        mPokemons[slotIndex] = pokemon;
        Log.e(getClass().getSimpleName(), "PKMN: " + pokemon);
        prepareTeam();
    }

    private void prepareTeam() {
        mTeam.pokemons.clear();
        for (TeamPokemon pokemon : mPokemons) addNullSafe(mTeam.pokemons, pokemon);
    }
}
