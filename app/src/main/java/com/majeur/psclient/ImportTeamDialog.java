package com.majeur.psclient;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.Callback;
import com.majeur.psclient.util.ShowdownTeamParser;
import com.majeur.psclient.util.SimpleTextWatcher;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.widget.SwitchLayout;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.array;
import static com.majeur.psclient.util.Utils.toArrayList;

public class ImportTeamDialog extends DialogFragment {

    public static ImportTeamDialog newInstance(TeamsFragment teamsFragment, List<BattleFormat.Category> battleFormatCategories) {
        ImportTeamDialog dialog = new ImportTeamDialog();
        dialog.setTargetFragment(teamsFragment, 0);
        Bundle args = new Bundle();
        args.putSerializable("formats", toArrayList(battleFormatCategories));
        dialog.setArguments(args);
        return dialog;
    }


    private static final int IMPORT_TYPE_PASTEBIN = 0;
    private static final int IMPORT_TYPE_RAW_TEXT = 1;


    private ClipboardManager mClipboardManager;
    private DexPokemonLoader mDexPokemonLoader;

    private int mImportType;

    private SwitchLayout mSwitchLayout;
    private ProgressBar mProgressBar;
    private EditText mEditText;
    private Button mImportButton;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        mDexPokemonLoader = new DexPokemonLoader(getContext());

        List<BattleFormat.Category> d = (List<BattleFormat.Category>) getArguments().getSerializable("formats");
        mImportType = -1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_import_team, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.teambuilder_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), TeamEditActivity.class);
                getFragmentManager().findFragmentById(R.id.fragment_teams)
                        .startActivityForResult(intent, TeamEditActivity.INTENT_REQUEST_CODE);
                dismiss();
            }
        });
        mSwitchLayout = view.findViewById(R.id.switch_layout);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mEditText = view.findViewById(R.id.edit_text_import);

        final RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        radioGroup.check(R.id.radio_import_pastebin);
        if (!mClipboardManager.hasPrimaryClip())
            radioGroup.findViewById(R.id.radio_import_clipboard).setEnabled(false);

        mImportButton = view.findViewById(R.id.button_);
        mImportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mImportType == -1) {
                    int radioId = radioGroup.getCheckedRadioButtonId();
                    moveToSecondStage(radioId);
                } else {
                    moveToThirdStage();
                }
            }
        });
    }

    private void moveToSecondStage(int checkedRadioId) {
        mProgressBar.setVisibility(View.INVISIBLE);

        switch (checkedRadioId) {
            case R.id.radio_import_pastebin:
                mImportButton.setEnabled(false);
                mEditText.setHint("Enter Pastebin URL or 8 characters long Paste's key");
                mEditText.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        mImportButton.setEnabled(charSequence.length() >= 8);
                    }
                });
                mEditText.setMaxLines(2);
                mEditText.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);

                mImportType = IMPORT_TYPE_PASTEBIN;
                break;

            case R.id.radio_import_clipboard:
                mImportButton.setEnabled(false);
                mEditText.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        mImportButton.setEnabled(charSequence.length() > 0);
                    }
                });
                boolean hasClipBoardData = false;
                if (mClipboardManager.hasPrimaryClip()) {
                    ClipData clipData = mClipboardManager.getPrimaryClip();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        ClipData.Item item = clipData.getItemAt(0);
                        if (item.getText() != null) {
                            mEditText.setText(item.getText());
                            mEditText.setTextSize(Utils.dpToPx(6));
                            mEditText.setSelection(item.getText().length());
                            hasClipBoardData = true;
                        }
                    }
                }

                if (!hasClipBoardData) {
                    mEditText.setHint("No text found in clipboard...");
                }

                mImportType = IMPORT_TYPE_RAW_TEXT;
                break;

            case R.id.radio_import_manually:
                mEditText.setHint("Type team here, good luck with that !");

                mImportType = IMPORT_TYPE_RAW_TEXT;
                break;
        }
        mSwitchLayout.smoothSwitchTo(1);
    }

    private void moveToThirdStage() {
        mProgressBar.setVisibility(View.VISIBLE);
        mImportButton.setEnabled(false);
        mEditText.setEnabled(false);
        String text = mEditText.getText().toString();

        final ShowdownTeamParser parser = new ShowdownTeamParser();
        parser.setDexPokemonFactory(new ShowdownTeamParser.DexPokemonFactory() {
            @Override
            public DexPokemon loadDexPokemon(String name) {
                return mDexPokemonLoader.load(array(toId(name)))[0];
            }
        });

        switch (mImportType) {
            case IMPORT_TYPE_PASTEBIN:
                text = text.substring(text.length() - 8);
                makePastebinRequest(text, new Callback<String>() {
                    @Override
                    public void callback(String s) {

                        if (s == null) {
                            handleParseResult(null);
                        } else {
                            parser.setInput(s);
                            parser.parse(new Callback<List<Team>>() {
                                @Override
                                public void callback(List<Team> teams) {
                                    handleParseResult(teams);
                                }
                            });
                        }
                    }
                });

                break;

            case IMPORT_TYPE_RAW_TEXT:
                parser.setInput(text);
                parser.parse(new Callback<List<Team>>() {
                    @Override
                    public void callback(List<Team> teams) {
                        handleParseResult(teams);
                    }
                });
                break;
        }
    }

    private void handleParseResult(List<Team> teams) {
        boolean assert1 = teams != null && teams.size() != 0;
        boolean assert2 = false;
        if (assert1) {
            for (Team team : teams)
                if (team.pokemons.size() != 0)
                    assert2 = true;
        }


        if (assert1 && assert2) {
            TeamsFragment teamsFragment = (TeamsFragment) getTargetFragment();
            teamsFragment.onTeamsImported(teams);
            dismiss();
        } else {
            String msg = "Something went wrong when importing your team, make sure the team is well formatted.";
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            mProgressBar.setVisibility(View.INVISIBLE);
            mImportButton.setText("Import");
            mImportButton.setEnabled(true);
            mEditText.setEnabled(true);
        }
    }

    private void makePastebinRequest(final String pasteKey, final Callback<String> callback) {
        ShowdownService showdownService = ((MainActivity) getActivity()).getService();
        if (showdownService == null) {
            //TODO
            return;
        }

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("pastebin.com")
                .addPathSegment("raw")
                .addPathSegment(pasteKey)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        showdownService.getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String rawText;

                if (response.code() == 200) // Prevents from reading Pastebin.com 404 error web page
                    rawText = response.body().string();
                else
                    rawText = null;

                if (getActivity() == null) return;

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.callback(rawText);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }
}
