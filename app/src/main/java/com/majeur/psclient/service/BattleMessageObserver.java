package com.majeur.psclient.service;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.majeur.psclient.io.BattleTextBuilder;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.BattleActionRequest;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.Const;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BattleMessageObserver extends RoomMessageObserver {

    private BattleTextBuilder mBattleTextBuilder;
    private ActionQueue mActionQueue = new ActionQueue(Looper.getMainLooper());

    private String mP1Username;
    private String mP2Username;
    private Const mGameType;
    private boolean mBattleRunning = false;
    private int[] mPreviewPokemonIndexes;
    private BattleActionRequest mLastActionRequest;

    public void gotContext(Context context) {
        mBattleTextBuilder = new BattleTextBuilder(context);
    }

    @Override
    public void onRoomInit() {
        mP1Username = null;
        mP2Username = null;
        mGameType = null;
        mActionQueue.clear();
        mBattleRunning = true;
        mPreviewPokemonIndexes = new int[2];
    }

    @Override
    public void onRoomDeInit() {
        onPrintText("~ deinit ~");

    }

    public Const getGameType() {
        return mGameType;
    }


    public boolean battleRunning() {
        return mBattleRunning;
    }

    private String myUsername() {
        return getService().getSharedData("username");
    }

    public void reAskForRequest() {
        if (mLastActionRequest != null)
            onRequestAsked(mLastActionRequest);
    }

    @Override
    public boolean onMessage(ServerMessage message) {
        if (super.onMessage(message))
            return true;
        message.resetArgsIteration();

        if (message.command.charAt(0) == '-')
            handleMinorActionCommand(message);
        else
            handleRegularCommand(message);

        return true;
    }

    private void handleRegularCommand(ServerMessage message) {
        switch (message.command) {
            case "break":
                onMarkBreak();
                break;
            case "move":
                handleMove(message);
                break;
            case "switch":
                handleSwitch(message);
                break;
            case "drag":
                handleDrag(message);
                break;
            case "detailschange":
                handleDetailsChanged(message);
                break;
            case "turn":
                handleTurn(message);
                break;
            case "player":
                handlePlayer(message);
                break;
            case "upkeep":
                //
                break;
            case "faint":
                handleFaint(message);
                break;
            case "teamsize":
                handleTeamSize(message);
                break;
            case "gametype":
                handleGameType(message);
                break;
            case "tier":
                onPrintText(Utils.boldText(message.nextArg()));
                break;
            case "rated":
                onPrintText(Utils.tagText("Rated battle"));
                break;
            case "rule":
                onPrintText(Utils.italicText(message.nextArg()));
                break;
            case "clearpoke":
                mPreviewPokemonIndexes[0] = mPreviewPokemonIndexes[1] = 0;
                onPreviewStarted();
                break;
            case "poke":
                handlePreviewPokemon(message);
                break;
            case "teampreview":
                // Used to trigger action looping in case nothing has been posted before
                mActionQueue.enqueueAction(ActionQueue.EMPTY_ACTION);
                break;
            case "start":
                onPrintText("\n" + mBattleTextBuilder.startBattle(mP1Username, mP2Username));
                onBattleStarted();
                break;
            case "request":
                handleRequest(message);
                break;
            case "inactive":
                handleInactive(message, true);
                break;
            case "inactiveoff":
                handleInactive(message, false);
                break;
            case "win":
                handleWin(message, false);
                break;
            case "tie":
                handleWin(message, true);
                break;
            case "cant":
                handleCant(message);
                break;
        }
    }

    // |move|p2a: Pinsir|Close Combat|p1a: Latias|[miss]
    // |move|p2a: Dialga|Flash Cannon|p1: Shiftry|[notarget]
    private void handleMove(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId sourcePoke = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String moveName = msg.nextArg();
        rawId = msg.nextArg();
        final PokemonId targetPoke = rawId.length() > 0 ? PokemonId.fromRawId(getPlayer(rawId), rawId) : null;

        final boolean shouldAnim;
        if (msg.hasNextArg()) {
            String next = msg.nextArg();
            shouldAnim = !(next.contains("still") || next.contains("notarget"));
        } else {
            shouldAnim = true;
        }

        final Spanned text = mBattleTextBuilder.move(sourcePoke, moveName);

        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                onMove(sourcePoke, targetPoke, moveName, shouldAnim);
                printMajorActionText(text);
            }
        });
    }

    private Player getPlayer(String rawId) {
        return Player.get(rawId, mP1Username, mP2Username, myUsername());
    }

    private void handlePlayer(ServerMessage msg) {
        String playerId = msg.nextArg();
        if (!msg.hasNextArg())
            return;
        String username = msg.nextArg();

        if (playerId.contains("1"))
            mP1Username = username;
        else
            mP2Username = username;

        if (mP1Username != null && mP2Username != null)
            onPlayerInit(Player.TRAINER.username(mP1Username, mP2Username, myUsername()),
                    Player.FOE.username(mP1Username, mP2Username, myUsername()));

    }

    private void handleFaint(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                onFaint(pokemonId);
                printMajorActionText(mBattleTextBuilder.faint(pokemonId));
            }
        });
    }

    private void handleTeamSize(ServerMessage msg) {
        String rawId = msg.nextArg();
        Player player = getPlayer(rawId);
        int count = Integer.parseInt(msg.nextArg());
        onTeamSize(player, count);
    }

    private void handleGameType(ServerMessage msg) {
        switch (msg.nextArg().charAt(0)) {
            case 's':
                mGameType = Const.SINGLE;
                break;
            case 'd':
                mGameType = Const.DOUBLE;
                break;
            case 't':
                mGameType = Const.TRIPLE;
                break;
        }
    }

    private void handleSwitch(ServerMessage msg) {
        String raw = msg.rawArgs();
        Player player = getPlayer(raw);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, raw);

        //TODO Handle switch out
        String username = player.username(mP1Username, mP2Username, myUsername());
        final Spanned text1 = mBattleTextBuilder.switcOut(player, username, "PREV PKMN");
        final Spanned text2 = mBattleTextBuilder.switchIn(player, username, pokemon.name);

        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                onSwitch(pokemon);
                //printMajorActionText(text1);
                printMajorActionText(text2);
            }
        });
    }

    private void handleDrag(ServerMessage msg) {
        String raw = msg.rawArgs();
        Player player = getPlayer(raw);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, raw);

        //TODO Handle switch out
        String username = player.username(mP1Username, mP2Username, myUsername());
        final Spanned text1 = mBattleTextBuilder.drag("PREV PKMN");
        final Spanned text2 = mBattleTextBuilder.switchIn(player, username, pokemon.name);

        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                onSwitch(pokemon);
                //printMajorActionText(text1);
                printMajorActionText(text2);
            }
        });
    }

    // |detailschange|POKEMON|DETAILS|HP STATUS
    // |detailschange|p1a: Aerodactyl|Aerodactyl-Mega, M
    private void handleDetailsChanged(ServerMessage msg) {
        String raw = msg.rawArgs();
        Player player = getPlayer(raw);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, raw);

        mActionQueue.enqueueAction(new Runnable() {
            @Override
            public void run() {
                onDetailsChanged(pokemon);
            }
        });
    }

    private void handleTurn(ServerMessage msg) {
        final Spannable spannable = new SpannableString("\n — Turn " + msg.nextArg() + " — ");
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 1,
                spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(1.2f), 1,
                spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mActionQueue.enqueueTurnAction(new Runnable() {
            @Override
            public void run() {
                onPrintText(spannable);
            }
        });
    }

    private void handleRequest(ServerMessage msg) {
        String rawJson = msg.rawArgs();
        if (rawJson.length() <= 1)
            return;
        try {
            JSONObject jsonObject = new JSONObject(rawJson);
            final BattleActionRequest request = new BattleActionRequest(jsonObject);
            mLastActionRequest = request;
            mActionQueue.setLastAction(new Runnable() {
                @Override
                public void run() {
                    onRequestAsked(request);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // p1|Zoroark, M|item
    // p2|Crobat, M|
    // p1|Shedinja|item
    private void handlePreviewPokemon(ServerMessage msg) {
        String rawId = msg.nextArg();
        Player player = getPlayer(rawId);
        int curIndex = mPreviewPokemonIndexes[player == Player.FOE ? 1 : 0]++;
        String species = msg.nextArg().split(",")[0];
        BasePokemon pokemon = new BasePokemon(species);
        boolean hasItem = msg.hasNextArg();
        onAddPreviewPokemon(PokemonId.fromPosition(player, curIndex), pokemon, hasItem);
    }

    private void handleInactive(ServerMessage msg, boolean on) {
        onTimerEnabled(on);
        final String text = msg.nextArg();
        if (text.startsWith("Time left:")) return;
        mActionQueue.enqueueAction(new Runnable() {
            @Override
            public void run() {
                printInactiveText(text);
            }
        });
    }

    private void handleWin(ServerMessage msg, boolean tie) {
        String username = msg.hasNextArg() ? msg.nextArg() : null;
        final Spanned text = tie ? mBattleTextBuilder.tie(mP1Username, mP2Username)
                : mBattleTextBuilder.win(username);
        mActionQueue.enqueueAction(new Runnable() {
            @Override
            public void run() {
                mBattleRunning = false;
                onBattleEnded();
                printMajorActionText(text);
                mActionQueue.setLastAction(null);
            }
        });
    }

    // |cant|POKEMON|REASON(|MOVE)
    private void handleCant(ServerMessage msg) {
        String rawId = msg.nextArg();
        PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String reason = msg.nextArg();
        String move = msg.hasNextArg() ? msg.nextArg() : null;

        final Spanned text = mBattleTextBuilder.cant(pokemonId, reason, move);

        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                printMajorActionText(text);
            }
        });
    }

    private void handleMinorActionCommand(final ServerMessage message) {
        String command = message.command.substring(1);
        switch (command) {
            case "message":
                mActionQueue.enqueueMinorAction(new Runnable() {
                    @Override
                    public void run() {
                        onPrintText(message.rawArgs());
                    }
                });
                break;
            case "fail":
                handleFail(message);
                break;
            case "miss":
                handleMiss(message);
                break;
            case "damage":
                handleHealthChange(message, true);
                break;
            case "heal":
                handleHealthChange(message, false);
                break;
            case "status":
                handleStatus(message, false);
                break;
            case "curestatus":
                handleStatus(message, true);
                break;
            case "cureteam":

                break;
            case "boost":
                handleStatChange(message, true);
                break;
            case "unboost":
                handleStatChange(message, false);
                break;
            case "setboost":
                handleSetBoost(message);
                break;
            case "weather":
                handleWeather(message);
                break;
            case "fieldstart":

                break;
            case "fieldend":

                break;
            case "sidestart":
                handleSide(message, true);
                break;
            case "sideend":
                handleSide(message, false);
                break;
            case "crit":
            case "resisted":
            case "supereffective":
            case "immune":
                handleMoveEffect(message, command);
                break;
            case "item":

                break;
            case "enditem"://|-enditem|p2a: Magcargo|Air Balloon

                break;
            case "ability":
                handleAbility(message, true);
                break;
            case "endability":
                handleAbility(message, false);
                break;
            case "transform":

                break;
            case "mega":
                handleMega(message);
                break;
            case "formechange":
                handleFormeChange(message);
                break;
            case "hint":
                mActionQueue.enqueueMinorAction(new Runnable() {
                    @Override
                    public void run() {
                        printMinorActionText("(" + message.nextArg() + ")");
                    }
                });
                break;
            case "center":
                break;
            case "start":
                handleVolatileStatusStart(message, true);
                break;
            case "end":
                handleVolatileStatusStart(message, false);
                break;
//                |-hitcount|p1a: Toxicroak|1 TODO
            //    |-prepare|p2a: Arceus|Shadow Force|p1a: Mandibuzz
            //E/-sethp: Args {p1a: Barbaracle|52/100|[from] move: Pain Split|[silent]}
            //W/ServerMessage: rommId: battle-gen7randombattle-972529271, data: |-sethp|p2a: Chandelure|133/233|[from] move: Pain Split
            //E/-sethp: Args {p2a: Chandelure|133/233|[from] move: Pain Split}
        }
    }

    private void handleAbility(final ServerMessage msg, final boolean start) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String ability = msg.hasNextArg() ? msg.nextArg() : null;
        String extra = msg.hasNextArg() ? msg.nextArg() : null;
        Player player = null;
        if ("unnerve".equalsIgnoreCase(ability))
            player = getPlayer(extra);

        final Spanned[] texts = mBattleTextBuilder.ability(pokemonId, ability, extra, player, start);

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                for (Spanned text : texts)
                    if (text != null) printMinorActionText(text);

                if (start)
                    onDisplayBattleToast(pokemonId, ability, Color.GREEN);
            }
        });
    }

    // |-mega|POKEMON|MEGASTONE
    // |-mega|p1a: Aerodactyl|Aerodactyl|Aerodactylite
    private void handleMega(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String username = pokemonId.player.username(mP1Username, mP2Username, myUsername());
        String item = msg.hasNextArg() ? msg.nextArg() : null;
        if (item != null) {
            if (item.equalsIgnoreCase(pokemonId.name))
                if (msg.hasNextArg()) item = msg.nextArg();
        }

        final String finalItem = item;
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                printMinorActionText(mBattleTextBuilder.mega(pokemonId, finalItem, username));
            }
        });
    }

    // p1a: Kecleon|typechange|Fighting|[from] ability: Protean

    // -start|POKEMON|EFFECT
    // -start|p2a: Blissey|move: Yawn|[of] p1a: Meowstic
    // -start|p1a: Wigglytuff|Disable|Dazzling Gleam|[from] ability: Cursed Body|[of] p2a: Froslass
    // -end|p1: Regigigas|Slow Start|[silent]
    private void handleVolatileStatusStart(ServerMessage msg, final boolean start) {
        boolean silent = msg.hasKwarg("silent");
        if (silent) return;

        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        if (!id.isInBattle) return;

        final String effect = msg.nextArg();
        String what = msg.hasNextArg() ? msg.nextArg() : null;
        String of = msg.kwarg("of");
        String from = msg.kwarg("from");

        final Spanned text;
        if ("typechange".equals(effect)) {

            text = mBattleTextBuilder.typeChange(id, what, from);
        } else {
            text = mBattleTextBuilder.volatileStatus(id, effect, what, of, from, start);
        }

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                if (effect.contains(":"))
                    onVolatileStatusChanged(id, effect.substring(effect.indexOf(':') + 1), start);
                else
                    onVolatileStatusChanged(id, effect, start);

                if (text != null)
                    printMinorActionText(text);
            }
        });
    }

    private void handleFail(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String action = msg.hasNextArg() ? msg.nextArg() : null;
        final Spanned text = mBattleTextBuilder.fail(pokemonId, action);
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onDisplayBattleToast(pokemonId, "Failed", Color.GRAY);
                printMinorActionText(text);
            }
        });
    }

    private void handleMiss(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final PokemonId targetPokeId;
        if (msg.hasNextArg()) {
            rawId = msg.hasNextArg() ? msg.nextArg() : null;
            targetPokeId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        } else {
            targetPokeId = null;
        }
        final Spanned text = mBattleTextBuilder.miss(pokemonId, targetPokeId);
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onDisplayBattleToast(targetPokeId == null ? targetPokeId : pokemonId, "Missed", Color.GRAY);
                printMinorActionText(text);
            }
        });
    }

    private void handleHealthChange(ServerMessage msg, boolean damage) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String rawCondition = msg.nextArg();
        final Condition condition = new Condition(rawCondition);
        // |-heal|p1a: Gliscor|57/100 tox|[from] ability: Poison Heal
        String mainKey = damage ? "damage" : "heal";
        String from = null;
        PokemonId of = null;
        if (msg.hasNextArg()) from = msg.nextArg();
        if (msg.hasNextArg()) {
            rawId = msg.nextArg();
            if (rawId.contains("[wisher]"))
                of = PokemonId.mockNameOnly(rawId.substring("[wisher]".length() + 1));
            else
                of = PokemonId.fromRawId(getPlayer(rawId), rawId);
        }

        final Spanned[] texts = mBattleTextBuilder.healthChange(mainKey, id, condition, from, of);

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onHealthChanged(id, condition);
                for (Spanned text : texts)
                    if (text != null) printMinorActionText(text);
            }
        });
    }

    private void handleStatus(ServerMessage msg, final boolean healed) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String status = msg.nextArg();
        final String action = msg.hasNextArg() ? msg.nextArg() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatusChanged(id, healed ? null : status);
                printMinorActionText(mBattleTextBuilder.status(id, status, !healed, action));
            }
        });
    }

    private void handleStatChange(ServerMessage msg, final boolean boost) {
        // POKEMON|STAT|AMOUNT
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final String stat = msg.nextArg();
        final int amount = (boost ? 1 : -1) * Integer.parseInt(msg.nextArg());

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatChanged(id, stat, amount, false);
                printMinorActionText(mBattleTextBuilder.statModifier(id, stat, amount));
            }
        });
    }

    private void handleSetBoost(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final String stat = msg.nextArg();
        final int amount = Integer.parseInt(msg.nextArg());

        final String from = msg.hasNextArg() ? msg.nextArg() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatChanged(id, stat, amount, true);
                printMinorActionText(mBattleTextBuilder.statModifierSet(id, stat, amount, from));
            }
        });
    }

    private void handleWeather(ServerMessage msg) {
        final String weather = msg.nextArg();
        final String action = msg.hasNextArg() ? msg.nextArg() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                // TODO print ability activation when [from] ability: xxx
                Spanned text = mBattleTextBuilder.weather(weather, action);
                if (text != null)
                    printMinorActionText(text);

                if (action == null || !action.contains("upkeep"))
                    onWeatherChanged(weather);
            }
        });
    }

    private void handleSide(ServerMessage msg, final boolean start) {
        final Player player = getPlayer(msg.nextArg());
        final String side = msg.nextArg();
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onSideChanged(player, side, start);
                Spanned text = mBattleTextBuilder.side(player, side, start);
                printMinorActionText(text);
            }
        });
    }

    private void handleMoveEffect(ServerMessage msg, final String type) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                Spanned[] texts = mBattleTextBuilder.moveEffect(type, pokemonId);
                printMinorActionText(texts[0]);
                onDisplayBattleToast(pokemonId, texts[1].toString(), Color.RED);
            }
        });
    }

    // |-formechange|POKEMON|SPECIES|HP STATUS
    private void handleFormeChange(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String species = msg.nextArg();

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                //TODO
            }
        });
    }

    private void printMajorActionText(CharSequence text) {
        onPrintText(text);
        onPrintBattleMessage(text);
    }

    private void printMinorActionText(CharSequence text) {
        Spannable spannable = new SpannableString(text);
        spannable.setSpan(new RelativeSizeSpan(0.8f), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        onPrintText(spannable);
        onPrintBattleMessage(spannable);
    }

    private void printInactiveText(String text) {
        Spannable spannable = new SpannableString(text);
        spannable.setSpan(new RelativeSizeSpan(0.8f), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(0xFF8B0000), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        onPrintText(spannable);
    }

    protected abstract void onPlayerInit(String playerUsername, String foeUsername);

    protected abstract void onFaint(PokemonId id);

    protected abstract void onTeamSize(Player player, int size);

    protected abstract void onBattleStarted();

    protected abstract void onBattleEnded();

    protected abstract void onTimerEnabled(boolean enabled);

    protected abstract void onPreviewStarted();

    protected abstract void onAddPreviewPokemon(PokemonId id, BasePokemon pokemon, boolean hasItem);

    protected abstract void onSwitch(BattlingPokemon newPokemon);

    protected abstract void onDetailsChanged(BattlingPokemon newPokemon);

    protected abstract void onMove(PokemonId sourceId, PokemonId targetId, String moveName, boolean shouldAnim);

    protected abstract void onRequestAsked(BattleActionRequest request);

    protected abstract void onHealthChanged(PokemonId id, Condition condition);

    protected abstract void onStatusChanged(PokemonId id, String status);

    protected abstract void onStatChanged(PokemonId id, String stat, int amount, boolean set);

    protected abstract void onDisplayBattleToast(PokemonId id, String text, int color);

    protected abstract void onWeatherChanged(String weather);

    protected abstract void onSideChanged(Player player, String side, boolean start);

    protected abstract void onVolatileStatusChanged(PokemonId id, String vStatus, boolean start);

    protected abstract void onPrintBattleMessage(CharSequence message);

    protected void onMarkBreak() {
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                //onPrintText("");
            }
        });
    }
}
