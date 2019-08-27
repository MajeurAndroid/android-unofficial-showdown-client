package com.majeur.psclient;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.AllItemsLoader;
import com.majeur.psclient.io.AllSpeciesLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.LearnsetLoader;
import com.majeur.psclient.model.Pokemon;
import com.majeur.psclient.model.Team;

import java.util.LinkedList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import static com.majeur.psclient.util.Utils.addNullSafe;

public class TeamEditActivity extends AppCompatActivity {

    public static final int INTENT_REQUEST_CODE = 194;
    public static final String INTENT_EXTRA_TEAM = "intent-extra-team";

    private AllSpeciesLoader mSpeciesLoader;
    private AllItemsLoader mItemsLoader;
    private DexPokemonLoader mDexPokemonLoader;
    private LearnsetLoader mLearnsetLoader;
    private GlideHelper mGlideHelper;

    private Team mTeam;
    private Pokemon[] mPokemons;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_edit);
        setResult(RESULT_CANCELED, null);

        Team team = (Team) getIntent().getSerializableExtra(INTENT_EXTRA_TEAM);
        mTeam = team == null ? new Team("Unnamed team", new LinkedList<Pokemon>(), null) : team;

        mPokemons = new Pokemon[6];
        for (int i = 0; i < mTeam.pokemons.size(); i++) mPokemons[i] = mTeam.pokemons.get(i);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Team builder");

        mSpeciesLoader = new AllSpeciesLoader(this);
        mItemsLoader = new AllItemsLoader(this);
        mDexPokemonLoader = new DexPokemonLoader(this);
        mLearnsetLoader = new LearnsetLoader(this);
        mGlideHelper = new GlideHelper(this);

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
        Log.e(getClass().getSimpleName(), mTeam.pack());
    }

    private void prepareTeam() {
        mTeam.pokemons.clear();
        for (Pokemon pokemon : mPokemons) addNullSafe(mTeam.pokemons, pokemon);
    }
}
