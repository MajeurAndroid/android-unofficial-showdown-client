package com.majeur.psclient;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.majeur.psclient.io.AllItemsLoader;
import com.majeur.psclient.io.AllSpeciesLoader;
import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.LearnsetLoader;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Item;
import com.majeur.psclient.model.Nature;
import com.majeur.psclient.model.Species;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.model.TeamPokemon;
import com.majeur.psclient.model.Type;
import com.majeur.psclient.util.RangeNumberTextWatcher;
import com.majeur.psclient.util.SimpleTextWatcher;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.model.Stats.calculateHp;
import static com.majeur.psclient.model.Stats.calculateStat;
import static com.majeur.psclient.util.Utils.indexOf;
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

    private AllSpeciesLoader mSpeciesLoader;
    private AllItemsLoader mItemsLoader;
    private DexPokemonLoader mDexPokemonLoader;
    private LearnsetLoader mLearnsetLoader;
    private GlideHelper mGlideHelper;

    private AutoCompleteTextView mSpeciesTextView;
    private MaterialButton mClearButton;
    private ImageView mSpriteImageView;
    private EditText mNameTextView;
    private EditText mLevelTextView;
    private EditText mHappinessEditText;
    private TextView mGenderTextView;
    private CheckBox mShinyCheckbox;
    private AutoCompleteTextView mAbilityTextView;
    private AutoCompleteTextView mItemTextView;
    private AutoCompleteTextView[] mMoveTextViews;
    private TextView[] mBaseStatTextViews;
    private EditText[] mEvTextViews;
    private EditText[] mIvTextViews;
    private TextView[] mTotalStatTextViews;
    private Spinner mNatureSpinner;
    private Spinner mHpTypeSpinner;

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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSlotIndex = getArguments().getInt(ARG_SLOT_INDEX);
        TeamEditActivity activity = (TeamEditActivity) context;
        mSpeciesLoader = activity.getSpeciesLoader();
        mItemsLoader = activity.getItemsLoader();
        mDexPokemonLoader = activity.getDexPokemonLoader();
        mLearnsetLoader = activity.getLearnsetLoader();
        mGlideHelper = activity.getGlideHelper();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentEvs = new Stats(0);
        mCurrentIvs = new Stats(31);
        mCurrentMoves = new String[4];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_pokemon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMoveTextViews = new AutoCompleteTextView[4];
        mBaseStatTextViews = new TextView[6];
        mEvTextViews = new EditText[6];
        mIvTextViews = new EditText[6];
        mTotalStatTextViews = new TextView[6];
        mSpeciesTextView = view.findViewById(R.id.speciesTextView);
        mClearButton = view.findViewById(R.id.clearPokemon);
        mSpriteImageView = view.findViewById(R.id.spriteImageView);
        mNameTextView = view.findViewById(R.id.nameEditText);
        mLevelTextView = view.findViewById(R.id.levelEditText);
        mHappinessEditText = view.findViewById(R.id.happinessEditText);
        mGenderTextView = view.findViewById(R.id.genderTextView);
        mShinyCheckbox = view.findViewById(R.id.shinyCheckBox);
        mAbilityTextView = view.findViewById(R.id.abilityTextView);
        mItemTextView = view.findViewById(R.id.itemTextView);
        mMoveTextViews[0] = view.findViewById(R.id.move1TextView);
        mMoveTextViews[1] = view.findViewById(R.id.move2TextView);
        mMoveTextViews[2] = view.findViewById(R.id.move3TextView);
        mMoveTextViews[3] = view.findViewById(R.id.move4TextView);
        mBaseStatTextViews[0] = view.findViewById(R.id.hpBaseTextView);
        mEvTextViews[0] = view.findViewById(R.id.hpEvsTextView);
        mIvTextViews[0] = view.findViewById(R.id.hpIvsTextView);
        mTotalStatTextViews[0] = view.findViewById(R.id.hpTotalTextView);
        mBaseStatTextViews[1] = view.findViewById(R.id.atkBaseTextView);
        mEvTextViews[1] = view.findViewById(R.id.atkEvsTextView);
        mIvTextViews[1] = view.findViewById(R.id.atkIvsTextView);
        mTotalStatTextViews[1] = view.findViewById(R.id.atkTotalTextView);
        mBaseStatTextViews[2] = view.findViewById(R.id.defBaseTextView);
        mEvTextViews[2] = view.findViewById(R.id.defEvsTextView);
        mIvTextViews[2] = view.findViewById(R.id.defIvsTextView);
        mTotalStatTextViews[2] = view.findViewById(R.id.defTotalTextView);
        mBaseStatTextViews[3] = view.findViewById(R.id.spaBaseTextView);
        mEvTextViews[3] = view.findViewById(R.id.spaEvsTextView);
        mIvTextViews[3] = view.findViewById(R.id.spaIvsTextView);
        mTotalStatTextViews[3] = view.findViewById(R.id.spaTotalTextView);
        mBaseStatTextViews[4] = view.findViewById(R.id.spdBaseTextView);
        mEvTextViews[4] = view.findViewById(R.id.spdEvsTextView);
        mIvTextViews[4] = view.findViewById(R.id.spdIvsTextView);
        mTotalStatTextViews[4] = view.findViewById(R.id.spdTotalTextView);
        mBaseStatTextViews[5] = view.findViewById(R.id.speBaseTextView);
        mEvTextViews[5] = view.findViewById(R.id.speEvsTextView);
        mIvTextViews[5] = view.findViewById(R.id.speIvsTextView);
        mTotalStatTextViews[5] = view.findViewById(R.id.speTotalTextView);
        mNatureSpinner = view.findViewById(R.id.natureSpinner);
        mHpTypeSpinner = view.findViewById(R.id.hpTypeSpinner);

        final String[] query = {""};
        mSpeciesLoader.load(query, new DataLoader.Callback<List>() {
            @Override
            public void onLoaded(List[] results) {
                ArrayAdapter<Species> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        results[0]);
                mSpeciesTextView.setAdapter(adapter);
            }
        });
        mSpeciesTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                Species newSpecies = (Species) adapter.getItem(i);
                initializeSpecies(newSpecies, null);
            }
        });
        mSpeciesTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus || mCurrentSpecies == null) return;
                mSpeciesTextView.setText(mCurrentSpecies.name);
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
                    updatePokemonData();
                }
            }
        });

        mLevelTextView.addTextChangedListener(new RangeNumberTextWatcher(1, 100));
        mLevelTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updatePokemonTotalStats();
                updatePokemonData();
            }
        });
        mLevelTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus && mLevelTextView.length() == 0) mLevelTextView.setText("100");
            }
        });

        mShinyCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updatePokemonSprite();
                updatePokemonData();
            }
        });

        mHappinessEditText.addTextChangedListener(new RangeNumberTextWatcher(0, 255));
        mHappinessEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updatePokemonData();
            }
        });
        mHappinessEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus && mHappinessEditText.length() == 0)
                    mHappinessEditText.setText("255");
            }
        });

        mAbilityTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                mCurrentAbility = (String) adapter.getItem(i);
                updatePokemonData();
            }
        });
        mAbilityTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus)
                    if (view.isEnabled()) mAbilityTextView.showDropDown();
                    else
                        mAbilityTextView.setText(mCurrentAbility);
            }
        });

        mItemsLoader.load(query, new DataLoader.Callback<List>() {
            @Override
            public void onLoaded(List[] results) {
                ArrayAdapter<Species> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        results[0]);
                mItemTextView.setAdapter(adapter);
            }
        });
        mItemTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                mCurrentItem = (Item) adapter.getItem(i);
                updatePokemonData();
            }
        });
        mItemTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    if (view.isEnabled()) mItemTextView.showDropDown();
                } else {
                    if (mItemTextView.getText().length() == 0)
                        mCurrentItem = null;
                    else
                        mItemTextView.setText(mCurrentItem != null ? mCurrentItem.name : "");
                }
            }
        });

        for (int i = 0; i < 4; i++) {
            final int index = i;
            final AutoCompleteTextView textView = mMoveTextViews[index];
            textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Adapter adapter = adapterView.getAdapter();
                    mCurrentMoves[index] = (String) adapter.getItem(i);
                    updatePokemonData();
                }
            });
            textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        if (view.isEnabled()) textView.showDropDown();
                    } else {
                        if (textView.getText().length() == 0)
                            mCurrentMoves[index] = null;
                        else
                            textView.setText(mCurrentMoves[index] != null ? mCurrentMoves[index] : "");
                    }
                }
            });
        }

        TextWatcher evsRangeTextWatcher = new RangeNumberTextWatcher(0, 252);
        for (int i = 0; i < 6; i++) {
            final int index = i;
            mEvTextViews[index].addTextChangedListener(evsRangeTextWatcher);
            mEvTextViews[index].addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    Integer stat = parseInt(editable.toString());
                    if (stat == null) stat = 0;
                    mCurrentEvs.set(index, stat);
                    updatePokemonData();
                    updatePokemonTotalStats();
                }
            });
        }

        TextWatcher ivsRangeTextWatcher = new RangeNumberTextWatcher(0, 31);
        for (int i = 0; i < 6; i++) {
            final int index = i;
            mIvTextViews[index].addTextChangedListener(ivsRangeTextWatcher);
            mIvTextViews[index].addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    Integer stat = parseInt(editable.toString());
                    if (stat == null) stat = 0;
                    mCurrentIvs.set(index, stat);
                    updatePokemonData();
                    updatePokemonTotalStats();
                }
            });
        }

        mCurrentNature = Nature.Adamant;
        SpinnerAdapter adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                Nature.ALL);
        mNatureSpinner.setAdapter(adapter);
        mNatureSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayAdapter<Nature> adapter = (ArrayAdapter<Nature>) adapterView.getAdapter();
                mCurrentNature = adapter.getItem(i);
                updatePokemonTotalStats();
                updatePokemonData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                Type.HP_TYPES);
        mHpTypeSpinner.setAdapter(adapter);
        mHpTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) adapterView.getAdapter();
                mCurrentIvs.setForHpType(adapter.getItem(i));
                for (int j = 0; j < 6; j++)
                    mIvTextViews[j].setText(str(mCurrentIvs.get(j)));
                updatePokemonData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSelectedSpecies();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TeamPokemon pokemon = (TeamPokemon) getArguments().getSerializable(ARG_PKMN);
        toggleInputViewsEnabled(false);
        if (pokemon != null) {
            Species species = new Species();
            species.id = toId(pokemon.species);
            species.name = pokemon.species;
            initializeSpecies(species, pokemon);
        }
    }

    private void initializeSpecies(final Species species, @Nullable final TeamPokemon basePokemon) {
        String[] query = {species.id};
        mDexPokemonLoader.load(query, new DataLoader.Callback<DexPokemon>() {
            @Override
            public void onLoaded(DexPokemon[] results) {
                DexPokemon dexPokemon = results[0];
                if (dexPokemon == null) {
                    mCurrentSpecies = null;
                    updatePokemonSprite();
                    mSpeciesTextView.setText(species.name);
                    toggleInputViewsEnabled(false);
                } else {
                    mCurrentSpecies = species;
                    updatePokemonSprite();
                    onDexPokemonLoaded(dexPokemon, basePokemon);
                }
            }
        });
    }

    private void onDexPokemonLoaded(DexPokemon dexPokemon, TeamPokemon basePokemon) {
        mNameTextView.setHint(mCurrentSpecies.name);

        String[] query = {mCurrentSpecies.id};
        for (AutoCompleteTextView textView : mMoveTextViews) textView.getText().clear();
        mLearnsetLoader.load(query, new DataLoader.Callback<List>() {
            @Override
            public void onLoaded(List[] results) {
                for (AutoCompleteTextView textView : mMoveTextViews) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            results[0]);
                    textView.setAdapter(adapter);
                    textView.setThreshold(1);
                }
            }
        });

        ImageView placeHolderTop = getView().findViewById(R.id.type1);
        ImageView placeHolderBottom = getView().findViewById(R.id.type2);
        mGlideHelper.loadTypeSprite(dexPokemon.firstType, placeHolderTop);
        if (dexPokemon.secondType != null)
            mGlideHelper.loadTypeSprite(dexPokemon.secondType, placeHolderBottom);
        else
            placeHolderBottom.setImageDrawable(null);

        // TODO
        //String[] genders = dexPokemon.gender.split("(?!^)");
        mGenderTextView.setText(dexPokemon.gender);

        List<String> abilities = new LinkedList<>();
        for (int i = 0; i < dexPokemon.abilities.size(); i++)
            abilities.add(i + ": " + dexPokemon.abilities.get(i));
        if (dexPokemon.hiddenAbility != null)
            abilities.add("H: " + dexPokemon.hiddenAbility);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                abilities);
        mAbilityTextView.setAdapter(adapter);
        mAbilityTextView.getText().clear();
        if (abilities.size() > 0) {
            mCurrentAbility = abilities.get(0);
            mAbilityTextView.setHint(mCurrentAbility);
        } else
            mAbilityTextView.setHint("No ability for this pkmn");

        Stats stats = dexPokemon.baseStats;
        mCurrentBaseStats = stats;
        for (int i = 0; i < 6; i++)
            mBaseStatTextViews[i].setText(str(stats.get(i)));

        if (basePokemon != null) {
            mCurrentSpecies.name = dexPokemon.species;
            mSpeciesTextView.setText(dexPokemon.species);
            mNameTextView.setHint(dexPokemon.species);
            if (!basePokemon.species.equalsIgnoreCase(basePokemon.name))
                mNameTextView.setText(basePokemon.name);
            mLevelTextView.setText(str(basePokemon.level));
            mShinyCheckbox.setChecked(basePokemon.shiny);
            mHappinessEditText.setText(str(basePokemon.happiness));
            if (basePokemon.ability.length() > 0) {
                for (String ability : abilities)
                    if (toId(ability).contains(toId(basePokemon.ability)))
                        mAbilityTextView.setText(ability);
            }
            ArrayAdapter<Item> adapter2 = (ArrayAdapter<Item>) mItemTextView.getAdapter();
            if (adapter2 != null) {
                for (int i = 0; i < adapter2.getCount(); i++)
                    if (adapter2.getItem(i).id.equals(basePokemon.item)) {
                        mCurrentItem = adapter2.getItem(i);
                        mItemTextView.setText(mCurrentItem.name);
                    }
            }
            if (mCurrentItem == null) {
                mCurrentItem = new Item();
                mCurrentItem.id = mCurrentItem.name = basePokemon.item;
                mItemTextView.setText(basePokemon.item);
            }
            for (int i = 0; i < basePokemon.moves.length; i++) {
                mCurrentMoves[i] = basePokemon.moves[i];
                mMoveTextViews[i].setText(basePokemon.moves[i]);
            }
            mCurrentEvs = basePokemon.evs;
            mCurrentIvs = basePokemon.ivs;
            for (int i = 0; i < 6; i++) {
                mEvTextViews[i].setText(str(basePokemon.evs.get(i)));
                mIvTextViews[i].setText(str(basePokemon.ivs.get(i)));
            }
            if (basePokemon.nature != null) {
                int index = 0;
                for (int i = 0; i < Nature.ALL.length; i++)
                    if (Nature.ALL[i].name.equalsIgnoreCase(basePokemon.nature)) index = i;
                mNatureSpinner.setSelection(index);
            }
        }
        toggleInputViewsEnabled(true);
        updatePokemonTotalStats();
        mHasPokemonData = true;
        updatePokemonData();
    }

    private void clearSelectedSpecies() {
        toggleInputViewsEnabled(false);
        mHasPokemonData = false;

        mCurrentSpecies = null;
        mSpeciesTextView.getText().clear();
        mSpriteImageView.setImageResource(R.drawable.placeholder_pokeball);
        ImageView placeHolderTop = getView().findViewById(R.id.type1);
        placeHolderTop.setImageDrawable(null);
        ImageView placeHolderBottom = getView().findViewById(R.id.type2);
        placeHolderBottom.setImageDrawable(null);
        mNameTextView.getText().clear();
        mNameTextView.setHint("");
        mLevelTextView.setText("100");
        mShinyCheckbox.setChecked(false);
        mHappinessEditText.setText("255");
        mCurrentAbility = null;
        mAbilityTextView.getText().clear();
        mAbilityTextView.setHint("");
        mCurrentItem = null;
        mItemTextView.getText().clear();
        mCurrentMoves = new String[4];
        for (int i = 0; i < 4; i++)
            mMoveTextViews[i].getText().clear();
        mCurrentEvs = new Stats(0);
        mCurrentIvs = new Stats(31);
        mCurrentBaseStats = null;
        for (int i = 0; i < 6; i++) {
            mBaseStatTextViews[i].setText("");
            mEvTextViews[i].setText("0");
            mIvTextViews[i].setText("31");
            mTotalStatTextViews[i].setText("");
        }
        mCurrentNature = Nature.Adamant;
        mNatureSpinner.setSelection(0);
        ScrollView scrollView = getView().findViewById(R.id.scrollView);
        scrollView.smoothScrollTo(0, 0);

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
        mHappinessEditText.setEnabled(enabled);
        mGenderTextView.setEnabled(enabled);
        mShinyCheckbox.setEnabled(enabled);
        mAbilityTextView.setEnabled(enabled);
        mItemTextView.setEnabled(enabled);
        for (int i = 0; i < 4; i++)
            mMoveTextViews[i].setEnabled(enabled);
        for (int i = 0; i < 6; i++) {
            mEvTextViews[i].setEnabled(enabled);
            mIvTextViews[i].setEnabled(enabled);
        }
        mNatureSpinner.setEnabled(enabled);
    }

    private void updatePokemonTotalStats() {
        if (mCurrentBaseStats == null) return;
        int niv = getCurrentLevel();
        mTotalStatTextViews[0].setText(str(calculateHp(mCurrentBaseStats.hp, mCurrentIvs.hp, mCurrentEvs.hp, niv)));
        for (int i = 1; i < 6; i++) {
            mTotalStatTextViews[i].setText(
                    str(calculateStat(mCurrentBaseStats.get(i), mCurrentIvs.get(i), mCurrentEvs.get(i), niv, mCurrentNature.get(i)))
            );
        }
        int totalEvs = mCurrentEvs.sum();
        int remainingEvs = 508 - totalEvs;
        TextView evsCounter = getView().findViewById(R.id.evsCounterTextView);
        evsCounter.setText("Remaining Evs: " + str(remainingEvs));
        evsCounter.setTextColor(remainingEvs < 0 ? Color.RED : Color.DKGRAY);
        int selectionIndex = indexOf(mCurrentIvs.hpType(), Type.HP_TYPES);
        if (mHpTypeSpinner.getSelectedItemPosition() != selectionIndex)
            mHpTypeSpinner.setSelection(selectionIndex, true);
    }

    private void updatePokemonData() {
        if (!mHasPokemonData) return;
        TeamPokemon pokemon = new TeamPokemon(mCurrentSpecies.name);
        pokemon.name = mNameTextView.length() > 0 ? mNameTextView.getText()
                .toString() : null;
        pokemon.level = getCurrentLevel();
        pokemon.happiness = getCurrentHappiness();
        pokemon.shiny = mShinyCheckbox.isChecked();
        pokemon.ability = toId(mCurrentAbility.substring("0: ".length()));
        pokemon.item = mCurrentItem != null ? mCurrentItem.id : "";
        pokemon.moves = getCurrentMoves();
        pokemon.ivs = mCurrentIvs;
        pokemon.evs = mCurrentEvs;
        pokemon.nature = mCurrentNature.name;
        TeamEditActivity activity = (TeamEditActivity) getContext();
        activity.onPokemonUpdated(mSlotIndex, pokemon);
    }

    private void updatePokemonNoData() {
        TeamEditActivity activity = (TeamEditActivity) getContext();
        activity.onPokemonUpdated(mSlotIndex, null);
    }

    private int getCurrentLevel() {
        Integer niv = parseInt(mLevelTextView.getText().toString());
        if (niv == null) niv = 100;
        return niv;
    }

    private int getCurrentHappiness() {
        Integer happiness = parseInt(mHappinessEditText.getText().toString());
        if (happiness == null) happiness = 255;
        return happiness;
    }

    private String[] getCurrentMoves() {
        List<String> moves = new LinkedList<>();
        for (AutoCompleteTextView textView : mMoveTextViews) {
            String move = textView.getText().toString();
            if (move.length() != 0) moves.add(move);
        }
        return moves.toArray(new String[0]);
    }
}