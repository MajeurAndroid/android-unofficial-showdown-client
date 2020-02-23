package com.majeur.psclient;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.majeur.psclient.io.AllItemsLoader;
import com.majeur.psclient.io.AllSpeciesLoader;
import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.LearnsetLoader;
import com.majeur.psclient.io.MoveDetailsLoader;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Item;
import com.majeur.psclient.model.Move;
import com.majeur.psclient.model.Nature;
import com.majeur.psclient.model.Species;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.model.TeamPokemon;
import com.majeur.psclient.model.Type;
import com.majeur.psclient.util.CategoryDrawable;
import com.majeur.psclient.util.RangeNumberTextWatcher;
import com.majeur.psclient.util.ShowdownTeamParser;
import com.majeur.psclient.util.SimpleOnItemSelectedListener;
import com.majeur.psclient.util.SimpleTextWatcher;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.widget.StatsTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;
import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.alphaColor;
import static com.majeur.psclient.util.Utils.array;
import static com.majeur.psclient.util.Utils.parseInt;
import static com.majeur.psclient.util.Utils.str;

public class PokemonEditFragment extends Fragment {

    private static final String ARG_SLOT_INDEX = "arg-slot-index";
    private static final String ARG_PKMN = "arg-pkmn";

    public static PokemonEditFragment create(int slotIndex, TeamPokemon pkmn) {
        PokemonEditFragment fragment = new PokemonEditFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SLOT_INDEX, slotIndex);
        args.putSerializable(ARG_PKMN, pkmn);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean mAttachedToContext;

    // Helpers
    private AllSpeciesLoader mSpeciesLoader;
    private AllItemsLoader mItemsLoader;
    private DexPokemonLoader mDexPokemonLoader;
    private LearnsetLoader mLearnsetLoader;
    private MoveDetailsLoader mMoveDetailsLoader;
    private GlideHelper mGlideHelper;

    // Views
    private AutoCompleteTextView mSpeciesTextView;
    private MaterialButton mClearButton;
    private ImageView mSpriteImageView;
    private EditText mNameTextView;
    private EditText mLevelTextView;
    private EditText mHappinessTextView;
//    private TextView mGenderTextView;
    private CheckBox mShinyCheckbox;
    private Spinner mAbilitySpinner;
    private AutoCompleteTextView mItemTextView;
    private AutoCompleteTextView[] mMoveTextViews;
    private StatsTable mStatsTable;
    private Spinner mNatureSpinner;
    private Spinner mHpTypeSpinner;
    private View mExportButton;

    // Data
    private int mSlotIndex;
    private Species mCurrentSpecies;
    private Item mCurrentItem;
    private Stats mCurrentBaseStats;
    private Stats mCurrentEvs;
    private Stats mCurrentIvs;
    private String mCurrentAbility;
    private String[] mCurrentMoves;
    private Nature mCurrentNature;

    private boolean mHasPokemonData;

    private final View.OnFocusChangeListener mACETFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            AutoCompleteTextView textView = (AutoCompleteTextView) view;
            if (hasFocus && textView.length() == 0) textView.showDropDown();
            if (!hasFocus && getActivity() != null)
                Utils.hideSoftInputMethod(getActivity());
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mAttachedToContext = true;
        mSlotIndex = getArguments().getInt(ARG_SLOT_INDEX);
        TeamEditActivity activity = (TeamEditActivity) context;
        mSpeciesLoader = activity.getSpeciesLoader();
        mItemsLoader = activity.getItemsLoader();
        mDexPokemonLoader = activity.getDexPokemonLoader();
        mLearnsetLoader = activity.getLearnsetLoader();
        mMoveDetailsLoader = activity.getMoveDetailsLoader();
        mGlideHelper = activity.getGlideHelper();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentEvs = new Stats(0);
        mCurrentIvs = new Stats(31);
        mCurrentMoves = new String[4];
        mMoveTextViews = new AutoCompleteTextView[4];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_pokemon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSpeciesTextView = view.findViewById(R.id.speciesTextView);
        mSpriteImageView = view.findViewById(R.id.spriteImageView);
        mNameTextView = view.findViewById(R.id.nameEditText);
        mLevelTextView = view.findViewById(R.id.levelEditText);
        mHappinessTextView = view.findViewById(R.id.happinessEditText);
//        mGenderTextView = view.findViewById(R.id.genderTextView);
        mShinyCheckbox = view.findViewById(R.id.shinyCheckBox);
        mAbilitySpinner = view.findViewById(R.id.abilityTextView);
        mItemTextView = view.findViewById(R.id.itemTextView);
        mMoveTextViews[0] = view.findViewById(R.id.move1TextView);
        mMoveTextViews[1] = view.findViewById(R.id.move2TextView);
        mMoveTextViews[2] = view.findViewById(R.id.move3TextView);
        mMoveTextViews[3] = view.findViewById(R.id.move4TextView);
        mStatsTable = view.findViewById(R.id.statsTable);
        mNatureSpinner = view.findViewById(R.id.natureSpinner);
        mHpTypeSpinner = view.findViewById(R.id.hpTypeSpinner);
        mClearButton = view.findViewById(R.id.clearPokemon);

        mSpeciesLoader.load(array(""), new DataLoader.Callback<List>() {
            @Override
            public void onLoaded(List[] results) {
                if (!mAttachedToContext) return;
                mSpeciesTextView.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        results[0]));
            }
        });
        mSpeciesTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                Species newSpecies = (Species) adapter.getItem(i);
                trySpecies(newSpecies.id);
            }
        });

        mNameTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                String regex = "[,|\\[\\]]"; // escape |,[] characters
                if (input.matches(".*" + regex + ".*")) {
                    editable.clear();
                    editable.append(input.replaceAll(regex, ""));
                } else {
                    notifyPokemonDataChanged();
                }
            }
        });

        mLevelTextView.addTextChangedListener(new RangeNumberTextWatcher(1, 100));
        mLevelTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                mStatsTable.setLevel(getCurrentLevel());
                notifyPokemonDataChanged();
            }
        });
        mLevelTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus && mLevelTextView.length() == 0)
                    mLevelTextView.setText("100");
            }
        });

        mShinyCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updatePokemonSprite();
                notifyPokemonDataChanged();
            }
        });

        mHappinessTextView.addTextChangedListener(new RangeNumberTextWatcher(0, 255));
        mHappinessTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                notifyPokemonDataChanged();
            }
        });
        mHappinessTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus && mHappinessTextView.length() == 0)
                    mHappinessTextView.setText("255");
            }
        });

        mAbilitySpinner.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                String readableAbility = (String) adapter.getItem(i);
                mCurrentAbility = toId(readableAbility.replace(" (Hidden)", ""));
                notifyPokemonDataChanged();
            }
        });

        mItemsLoader.load(array(""), new DataLoader.Callback<List>() {
            @Override
            public void onLoaded(List[] results) {
                if (!mAttachedToContext) return;
                mItemTextView.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        results[0]));
            }
        });
        mItemTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                mCurrentItem = (Item) adapter.getItem(i);
                notifyPokemonDataChanged();
            }
        });
        mItemTextView.setOnFocusChangeListener(mACETFocusListener);

        for (int i = 0; i < 4; i++) {
            final int index = i;
            final AutoCompleteTextView textView = mMoveTextViews[i];
            textView.setThreshold(1);
            textView.setOnFocusChangeListener(mACETFocusListener);
            textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Adapter adapter = adapterView.getAdapter();
                    mCurrentMoves[index] = (String) adapter.getItem(i);
                    notifyPokemonDataChanged();
                    /* Check if we have the full name to display to user */
                    if (view.getTag() instanceof MovesAdapter.ViewHolder) {
                        CharSequence text = ((MovesAdapter.ViewHolder) view.getTag()).mNameView.getText();
                        if (text.length() > 0 && Character.isUpperCase(text.charAt(0)))
                            textView.setText(text.toString()); // Prevents highlight spans
                    }
                    if (index < 3) {
                        if (mMoveTextViews[index+1].length() == 0)
                            mMoveTextViews[index+1].requestFocus();
                        else
                            textView.clearFocus();
                    } else {
                        textView.clearFocus();
                    }
                }
            });
        }

        mStatsTable.setRowClickListener(new StatsTable.OnRowClickListener() {
            @Override
            public void onRowClicked(StatsTable statsTable, String rowName, int index) {
                EditStatDialog dialog = EditStatDialog.newInstance(rowName, mCurrentBaseStats.get(index),
                        mCurrentEvs.get(index), mCurrentIvs.get(index), getCurrentLevel(),
                        mCurrentNature.getStatModifier(index), mCurrentEvs.sum());
                dialog.setTargetFragment(PokemonEditFragment.this, 0);
                dialog.show(getFragmentManager(), "");
            }
        });

        mCurrentNature = Nature.DEFAULT;
        mNatureSpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                Nature.ALL));
        mNatureSpinner.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayAdapter<Nature> adapter = (ArrayAdapter<Nature>) adapterView.getAdapter();
                mCurrentNature = adapter.getItem(i);
                mStatsTable.setNature(mCurrentNature);
                notifyPokemonDataChanged();
            }
        });

        mHpTypeSpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                Type.HP_TYPES));
        mHpTypeSpinner.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) adapterView.getAdapter();
                mCurrentIvs.setForHpType(adapter.getItem(i));
                mStatsTable.setIVs(mCurrentIvs);
                notifyPokemonDataChanged();
            }
        });

        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSelectedSpecies();
            }
        });

        mExportButton = view.findViewById(R.id.export);
        mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mHasPokemonData) return;
                TeamPokemon pokemon = buildPokemon();
                String text = ShowdownTeamParser.fromPokemon(pokemon);
                Toast.makeText(getContext(), "Pokemon exported to clipboard", Toast.LENGTH_LONG).show();
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Exported Pokemon", text);
                clipboard.setPrimaryClip(clip);
            }
        });

        view.findViewById(R.id.importButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = clipboard.getPrimaryClip();
                if (clip == null) {
                    Toast.makeText(getContext(), "There is nothing in clipboard.",
                            Toast.LENGTH_LONG).show();
                } else if (clip.getDescription().hasMimeType(MIMETYPE_TEXT_PLAIN) && clip.getItemCount() > 0) {
                    final TeamPokemon pokemon = ShowdownTeamParser.parsePokemon(clip.getItemAt(0).getText().toString(),
                            new ShowdownTeamParser.DexPokemonFactory() {
                                @Override
                                public DexPokemon loadDexPokemon(String name) {
                                    return mDexPokemonLoader.load(array(name))[0];
                                }
                            });
                    if (pokemon != null) {
                        mDexPokemonLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<DexPokemon>() {
                            @Override
                            public void onLoaded(DexPokemon[] results) {
                                if (!mAttachedToContext) return;
                                DexPokemon dexPokemon = results[0];
                                if (dexPokemon == null) { // This pokemon does not have an entry in our dex.json
                                    Toast.makeText(getContext(), "The Pokemon you imported does not exist in current pokedex.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                if (mHasPokemonData) clearSelectedSpecies();
                                bindExistingPokemon(pokemon); // Binding our data
                                bindDexPokemon(dexPokemon); // Setting data from dex
                                mHasPokemonData = true;
                                toggleInputViewsEnabled(true);
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "No Pokemon found in clipboard.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), "There is nothing that looks like a Pokemon in clipboard.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toggleInputViewsEnabled(false);
        final TeamPokemon pokemon = (TeamPokemon) getArguments().getSerializable(ARG_PKMN);
        if (pokemon != null) {
            mDexPokemonLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<DexPokemon>() {
                @Override
                public void onLoaded(DexPokemon[] results) {
                    if (!mAttachedToContext) return;
                    DexPokemon dexPokemon = results[0];
                    if (dexPokemon == null) return; // This pokemon does not have an entry in our dex.json
                    bindExistingPokemon(pokemon); // Binding our data
                    bindDexPokemon(dexPokemon); // Setting data from dex
                    mHasPokemonData = true;
                    toggleInputViewsEnabled(true);
                }
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttachedToContext = false;
    }

    private void trySpecies(final String species) {
        String[] query = {toId(species)};
        mDexPokemonLoader.load(query, new DataLoader.Callback<DexPokemon>() {
            @Override
            public void onLoaded(DexPokemon[] results) {
                if (!mAttachedToContext) return;
                DexPokemon dexPokemon = results[0];
                if (dexPokemon == null) {
                    mSpeciesTextView.setText(mCurrentSpecies != null ? mCurrentSpecies.name : null);
                } else {
                    bindDexPokemon(dexPokemon);
                    mHasPokemonData = true;
                    toggleInputViewsEnabled(true);
                }
            }
        });
    }

    private void bindDexPokemon(DexPokemon dexPokemon) {
        mCurrentSpecies = new Species();
        mCurrentSpecies.id = toId(dexPokemon.species);
        mCurrentSpecies.name = dexPokemon.species;
        updatePokemonSprite();
        mSpeciesTextView.setText(dexPokemon.species);

        ImageView placeHolderTop = getView().findViewById(R.id.type1);
        placeHolderTop.setImageResource(Type.getResId(dexPokemon.firstType));
        ImageView placeHolderBottom = getView().findViewById(R.id.type2);
        if (dexPokemon.secondType != null) placeHolderBottom.setImageResource(Type.getResId(dexPokemon.secondType));
        else placeHolderBottom.setImageDrawable(null);

        List<String> abilities = new LinkedList<>();
        abilities.addAll(dexPokemon.abilities);
        if (dexPokemon.hiddenAbility != null)
            abilities.add(dexPokemon.hiddenAbility + " (Hidden)");
        mAbilitySpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                abilities));
        if (mCurrentAbility == null) {
            mCurrentAbility = abilities.get(0);
        } else {
            for (int i = 0; i < abilities.size(); i++) {
                String ability = abilities.get(i);
                if (toId(ability).contains(toId(mCurrentAbility)))
                    mAbilitySpinner.setSelection(i);
            }
        }

        String[] query = {mCurrentSpecies.id};
        mLearnsetLoader.load(query, new DataLoader.Callback<Set>() {
            @Override
            public void onLoaded(Set[] results) {
                if (!mAttachedToContext) return;
                final Set<String> moves = results[0];
                if (moves == null) return;
                for (int i = 0; i < 4; i++) {
                    final AutoCompleteTextView textView = mMoveTextViews[i];
                    int color = alphaColor(getResources().getColor(R.color.secondary), 0.45f);
                    MovesAdapter adapter = new MovesAdapter(mMoveDetailsLoader, moves, color);
                    textView.setAdapter(adapter);
                }
            }
        });

        Stats stats = dexPokemon.baseStats;
        mCurrentBaseStats = stats;
        mStatsTable.setBaseStats(stats);
    }

    private void bindExistingPokemon(TeamPokemon pokemon) {
            if (!pokemon.species.equalsIgnoreCase(pokemon.name))
                mNameTextView.setText(pokemon.name);
            mLevelTextView.setText(str(pokemon.level));
            mShinyCheckbox.setChecked(pokemon.shiny);
            mHappinessTextView.setText(str(pokemon.happiness));
            mCurrentAbility = pokemon.ability;
            ArrayAdapter<Item> itemAdapter = (ArrayAdapter<Item>) mItemTextView.getAdapter();
            if (itemAdapter != null) {
                for (int i = 0; i < itemAdapter.getCount(); i++) {
                    Item item = itemAdapter.getItem(i);
                    if (item.id.equals(pokemon.item)) {
                        mCurrentItem = item;
                        mItemTextView.setText(item.name);
                    }
                }
            } else {
                mCurrentItem = new Item();
                mCurrentItem.id = mCurrentItem.name = pokemon.item;
            }
            for (int i = 0; i < 4; i++)
                if (i < pokemon.moves.length) mCurrentMoves[i] = pokemon.moves[i];
            if (mCurrentMoves.length > 0) {
                // Retrieve full name for moves
                mMoveDetailsLoader.load(mCurrentMoves, new DataLoader.Callback<Move.Details>() {
                    @Override
                    public void onLoaded(Move.Details[] results) {
                        if (!mAttachedToContext) return;
                        for (int i = 0; i < results.length; i++) {
                            if (results[i] != null)
                                mMoveTextViews[i].setText(results[i].name);
                        }
                    }
                });
            }
            mCurrentEvs = pokemon.evs;
            mStatsTable.setEVs(mCurrentEvs);
            mCurrentIvs = pokemon.ivs;
            mStatsTable.setIVs(mCurrentIvs);
            if (pokemon.nature != null) {
                int index = 0;
                for (int i = 0; i < Nature.ALL.length; i++)
                    if (Nature.ALL[i].name.equalsIgnoreCase(pokemon.nature)) index = i;
                mCurrentNature = Nature.ALL[index];
                mNatureSpinner.setSelection(index);
            }
    }

    private void clearSelectedSpecies() {
        toggleInputViewsEnabled(false);
        mHasPokemonData = false;

        mCurrentSpecies = null;
        updatePokemonSprite();
        mSpeciesTextView.getText().clear();

        ImageView placeHolderTop = getView().findViewById(R.id.type1);
        placeHolderTop.setImageDrawable(null);
        ImageView placeHolderBottom = getView().findViewById(R.id.type2);
        placeHolderBottom.setImageDrawable(null);
        mNameTextView.getText().clear();
        mLevelTextView.setText("100");
        mShinyCheckbox.setChecked(false);
        mHappinessTextView.setText("255");
        mCurrentAbility = null;
        mAbilitySpinner.setAdapter(null);
        mCurrentItem = null;
        mItemTextView.getText().clear();
        mCurrentMoves = new String[4];
        for (int i = 0; i < 4; i++)
            mMoveTextViews[i].setAdapter(null);
        mCurrentEvs = new Stats(0);
        mCurrentIvs = new Stats(31);
        mCurrentBaseStats = null;
        mStatsTable.clear();
        mCurrentNature = Nature.DEFAULT;
        mNatureSpinner.setSelection(0);
        ScrollView scrollView = getView().findViewById(R.id.scrollView);
        scrollView.smoothScrollTo(0, 0);
        mSpeciesTextView.requestFocus();

        updatePokemonNoData();
    }

    private void updatePokemonSprite() {
        if (mCurrentSpecies == null)
            mSpriteImageView.setImageResource(R.drawable.placeholder_pokeball);
        else
            mGlideHelper.loadDexSprite(new BasePokemon(mCurrentSpecies.name), mShinyCheckbox.isChecked(), mSpriteImageView);
    }

    private void toggleInputViewsEnabled(boolean enabled) {
        mClearButton.setEnabled(enabled);
        mClearButton.getIcon().setAlpha(enabled ? 255 : 125);
        mNameTextView.setEnabled(enabled);
        mLevelTextView.setEnabled(enabled);
        mHappinessTextView.setEnabled(enabled);
        //mGenderTextView.setEnabled(enabled);
        mShinyCheckbox.setEnabled(enabled);
        mAbilitySpinner.setEnabled(enabled);
        mItemTextView.setEnabled(enabled);
        for (int i = 0; i < 4; i++)
            mMoveTextViews[i].setEnabled(enabled);
        mStatsTable.setEnabled(enabled);
        mNatureSpinner.setEnabled(enabled);
        mHpTypeSpinner.setEnabled(enabled);
        mExportButton.setEnabled(enabled);
    }

    public void onStatModified(String stat, int ev, int iv) {
        mCurrentEvs.set(Stats.toIndex(stat), ev);
        mCurrentIvs.set(Stats.toIndex(stat), iv);
        mStatsTable.setEVs(mCurrentEvs);
        mStatsTable.setIVs(mCurrentIvs);
        notifyPokemonDataChanged();
    }

    private void notifyPokemonDataChanged() {
        if (!mHasPokemonData) return;
        TeamEditActivity activity = (TeamEditActivity) getContext();
        activity.onPokemonUpdated(mSlotIndex, buildPokemon());
    }

    private TeamPokemon buildPokemon() {
        TeamPokemon pokemon = new TeamPokemon(mCurrentSpecies.name);
        pokemon.name = mNameTextView.length() > 0 ? mNameTextView.getText()
                .toString() : null;
        pokemon.level = getCurrentLevel();
        pokemon.happiness = getCurrentHappiness();
        pokemon.shiny = mShinyCheckbox.isChecked();
        pokemon.ability = toId(mCurrentAbility);
        pokemon.item = mCurrentItem != null ? mCurrentItem.id : "";
        pokemon.moves = getCurrentMoves();
        pokemon.ivs = mCurrentIvs;
        pokemon.evs = mCurrentEvs;
        pokemon.nature = mCurrentNature.name;
        return pokemon;
    }

    private void updatePokemonNoData() {
        TeamEditActivity activity = (TeamEditActivity) getContext();
        activity.onPokemonUpdated(mSlotIndex, null);
    }

    private int getCurrentLevel() {
        Integer level = parseInt(mLevelTextView.getText().toString());
        if (level == null) level = 100;
        return level;
    }

    private int getCurrentHappiness() {
        Integer happiness = parseInt(mHappinessTextView.getText().toString());
        if (happiness == null) happiness = 255;
        return happiness;
    }

    private String[] getCurrentMoves() {
        List<String> moves = new LinkedList<>();
        for (String move : mCurrentMoves) {
            if (move != null && move.length() > 0)
                moves.add(toId(move));
        }
        return moves.toArray(new String[0]);
    }

    private static class MovesAdapter extends BaseAdapter implements Filterable {

        private LayoutInflater mInflater;
        private MoveDetailsLoader mLoader;

        private List<String> mMoveIds;
        private List<String> mAdapterList;
        private String mCurrentConstraint;
        private int mHighlightColor;

        MovesAdapter(MoveDetailsLoader loader, Collection<String> moveIds, int highlightColor) {
            mLoader = loader;
            mMoveIds = new ArrayList<>();
            mMoveIds.addAll(moveIds);
            mAdapterList = new ArrayList<>();
            mAdapterList.addAll(moveIds);
            mHighlightColor = highlightColor;
        }

        @Override
        public int getCount() {
            return mAdapterList.size();
        }

        @Override
        public String getItem(int i) {
            return mAdapterList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).hashCode();
        }

        private static class ViewHolder {
            String moveId;
            private TextView mNameView;
            private TextView mDetailsView;
            private ImageView mTypeView;
            private ImageView mCategoryView;

            ViewHolder(View parent) {
                mNameView = parent.findViewById(R.id.name_view);
                mDetailsView = parent.findViewById(R.id.details_view);
                mTypeView = parent.findViewById(R.id.type_view);
                mCategoryView = parent.findViewById(R.id.category_view);
            }
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                if (mInflater == null) mInflater = LayoutInflater.from(parent.getContext());
                convertView = mInflater.inflate(R.layout.list_item_move, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final String moveId = getItem(position);
            holder.moveId = moveId;
            holder.mNameView.setText(moveId, TextView.BufferType.SPANNABLE);
            showMatch(holder.mNameView);
            holder.mDetailsView.setText(buildDetailsText(-1, -1, -1));
            holder.mTypeView.setImageDrawable(null);
            holder.mTypeView.animate().cancel();
            holder.mTypeView.setAlpha(0f);
            holder.mCategoryView.setImageDrawable(null);
            holder.mCategoryView.animate().cancel();
            holder.mCategoryView.setAlpha(0f);

            mLoader.load(array(moveId), new DataLoader.Callback<Move.Details>() {
                @Override
                public void onLoaded(Move.Details[] results) {
                    // Check if callback arrives in time
                    if (!holder.moveId.equals(moveId)) return;
                    Move.Details info = results[0];
                    holder.mNameView.setText(info.name, TextView.BufferType.SPANNABLE);
                    showMatch(holder.mNameView);
                    holder.mDetailsView.setText(buildDetailsText(info.pp, info.basePower, info.accuracy));
                    holder.mTypeView.setImageResource(Type.getResId(info.type));
                    holder.mTypeView.animate().alpha(1f).start();
                    holder.mCategoryView.setImageDrawable(new CategoryDrawable(info.category));
                    holder.mCategoryView.animate().alpha(1f).start();
                }
            });

            return convertView;
        }

        private void showMatch(TextView textView) {
            if (mCurrentConstraint == null) return;
            String text = textView.getText().toString().toLowerCase();
            int spaceIndex = text.indexOf(' ');
            text = text.replace(" ", "");
            if (!text.contains(mCurrentConstraint)) return;
            int startIndex = text.indexOf(mCurrentConstraint);
            if (spaceIndex > 0 && startIndex >= spaceIndex) startIndex++;
            int endIndex = startIndex + mCurrentConstraint.length();
            if (spaceIndex > 0 && startIndex < spaceIndex && endIndex > spaceIndex) endIndex++;
            Spannable spannable = (Spannable) textView.getText();
            spannable.setSpan(new BackgroundColorSpan(mHighlightColor), startIndex, endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private String buildDetailsText(int pp, int bp, int acc) {
            return "PP: " + (pp >= 0 ? pp : "–") + ", BP: " + (bp > 0 ? bp : "–")
                    + ", AC: " + (acc > 0 ? acc : "–");
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return mFilter;
        }

        private final Filter mFilter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint != null) {
                    String constraintString = constraint.toString().toLowerCase().replace(" ", "");
                    List<String> list = new LinkedList<>();
                    for (String id : mMoveIds) {
                        if (id.contains(constraintString))
                            list.add(id);
                    }
                    FilterResults filterResults = new FilterResults();
                    filterResults.values = list;
                    filterResults.count = list.size();
                    return filterResults;
                } else {
                    return new FilterResults();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCurrentConstraint = constraint != null ?
                        constraint.toString().toLowerCase().replace(" ", "") : null;
                if (results != null && results.count > 0 && results.values != null) {
                    mAdapterList.clear();
                    mAdapterList.addAll((Collection<? extends String>) results.values);
                    notifyDataSetChanged();
                }
            }
        };
    }

    public static class EditStatDialog extends DialogFragment implements SeekBar.OnSeekBarChangeListener {

        private static final String ARG_STAT_NAME = "arg-stat-name";
        private static final String ARG_STAT_BASE = "arg-stat-base";
        private static final String ARG_STAT_EV = "arg-stat-ev";
        private static final String ARG_STAT_IV = "arg-stat-iv";
        private static final String ARG_LEVEL = "arg-level";
        private static final String ARG_NATURE = "arg-nature";
        private static final String ARG_EVSUM = "arg-evsum";

        public static EditStatDialog newInstance(String name, int base, int ev,
                                 int iv, int level, float nature, int evsum) {
            Bundle args = new Bundle();
            args.putString(ARG_STAT_NAME, name);
            args.putInt(ARG_STAT_BASE, base);
            args.putInt(ARG_STAT_EV, ev);
            args.putInt(ARG_STAT_IV, iv);
            args.putInt(ARG_LEVEL, level);
            args.putFloat(ARG_NATURE, nature);
            EditStatDialog fragment = new EditStatDialog();
            fragment.setArguments(args);
            return fragment;
        }

        private String mStatName;
        private int mLevel;
        private int mBase;
        private int mEv;
        private int mIv;
        private float mNatureModifier;
        private int mEvSum;

        private TextView mEVsValueView;
        private TextView mIVsValueView;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mStatName = args.getString(ARG_STAT_NAME);
            mLevel = args.getInt(ARG_LEVEL);
            mBase = args.getInt(ARG_STAT_BASE);
            mEv = args.getInt(ARG_STAT_EV);
            mIv = args.getInt(ARG_STAT_IV);
            mEvSum = args.getInt(ARG_EVSUM);
            mNatureModifier = args.getFloat(ARG_NATURE);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.dialog_edit_stat, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            mEVsValueView = view.findViewById(R.id.evs_text_view);
            mIVsValueView = view.findViewById(R.id.ivs_text_view);
            TextView titleView = view.findViewById(R.id.title_text_view);
            titleView.setText(mStatName);
            SeekBar seekBar = view.findViewById(R.id.seek_bar_evs);
            seekBar.setOnSeekBarChangeListener(this);
            seekBar.setProgress(mEv);
            mEVsValueView.setText(Integer.toString(mEv));
            seekBar = view.findViewById(R.id.seek_bar_ivs);
            seekBar.setOnSeekBarChangeListener(this);
            seekBar.setProgress(mIv);
            mIVsValueView.setText(Integer.toString(mIv));

            view.findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PokemonEditFragment fragment = (PokemonEditFragment) getTargetFragment();
                    fragment.onStatModified(mStatName, mEv, mIv);
                    dismiss();
                }
            });
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            if (seekBar.getId() == R.id.seek_bar_evs) {
                mEVsValueView.setText(Integer.toString(progress));
                mEv = progress;
            } else {
                mIVsValueView.setText(Integer.toString(progress));
                mIv = progress;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }


    }
}