package com.majeur.psclient.service;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import com.majeur.psclient.io.BattleTextBuilder;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.BattleActionRequest;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Colors;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.Const;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.model.StatModifiers;
import com.majeur.psclient.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.model.Id.toIdSafe;
import static com.majeur.psclient.util.Utils.indexOf;
import static com.majeur.psclient.util.Utils.isInteger;
import static com.majeur.psclient.util.Utils.parseWithDefault;
import static com.majeur.psclient.util.Utils.swap;
import static java.lang.Integer.parseInt;

public abstract class BattleMessageObserver extends RoomMessageObserver {

    private BattleTextBuilder mBattleTextBuilder;
    private final ActionQueue mActionQueue = new ActionQueue(Looper.getMainLooper());

    private String mP1Username;
    private String mP2Username;
    private Const mGameType;
    private boolean mBattleRunning = false;
    private int[] mPreviewPokemonIndexes;
    private BattleActionRequest mLastActionRequest;
    private BattlingPokemon[] mTrainerPokemons;
    private BattlingPokemon[] mFoePokemons;
    private String mActiveWeather;
    private final LinkedList<String> mActiveFieldEffects = new LinkedList<>(); // We use LinkedList specific methods.
    private String mLastMove;

    public void gotContext(Context context) {
        mBattleTextBuilder = new BattleTextBuilder(context);
        mBattleTextBuilder.setPokemonIdFactory(rawString -> {
            try {
                return PokemonId.fromRawId(getPlayer(rawString), rawString);
            } catch (NullPointerException | StringIndexOutOfBoundsException e) {
                return null;
            }
        });
    }

    @Override
    public void onRoomInit() {
        mP1Username = null;
        mP2Username = null;
        mGameType = null;
        mActionQueue.clear();
        mBattleRunning = true;
        mPreviewPokemonIndexes = new int[2];
        mLastActionRequest = null;
        mActiveWeather = null;
        mActiveFieldEffects.clear();
    }

    @Override
    public void onRoomDeInit() {
        mP1Username = null;
        mP2Username = null;
        mGameType = null;
        mActionQueue.clear();
        mBattleRunning = false;
        mPreviewPokemonIndexes = new int[2];
        mActiveWeather = null;
        mActiveFieldEffects.clear();
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

    private Player getPlayer(String rawId) {
        return Player.get(rawId, mP1Username, mP2Username, myUsername());
    }

    public void reAskForRequest() {
        if (mLastActionRequest != null)
            onRequestAsked(mLastActionRequest);
    }

    public BattlingPokemon getBattlingPokemon(PokemonId id) {
        BattlingPokemon[] arr = id.foe ? mFoePokemons : mTrainerPokemons;
        if (id.position >= 0 && id.position < arr.length) return arr[id.position];
        return null;
    }

    private BattlingPokemon getBattlingPokemon(Player player, int position) {
        BattlingPokemon[] arr = player == Player.FOE ? mFoePokemons : mTrainerPokemons;
        if (position >= 0 && position < arr.length) return arr[position];
        return null;
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
                printMessage(Utils.boldText(message.nextArg()));
                break;
            case "rated":
                printMessage(Utils.tagText("Rated battle"));
                break;
            case "rule":
                printMessage(Utils.italicText(message.nextArg()));
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
                printMessage("\n" + mBattleTextBuilder.start(mP1Username, mP2Username));
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
            case "swap":
                handleSwap(message);
                break;
        } // TODO: replace: [p2a: Zoroark, Zoroark, L78, M]
    }

    // |move|p2a: Pinsir|Close Combat|p1a: Latias|[miss]
    // |move|p2a: Dialga|Flash Cannon|p1: Shiftry|[notarget]
    private void handleMove(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId sourcePoke = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String moveName = msg.nextArg();
        rawId = msg.hasNextArg() ? msg.nextArg() : null;
        final PokemonId targetPoke = rawId != null && rawId.length() > 0
                ? PokemonId.fromRawId(getPlayer(rawId), rawId) : null;

        final boolean shouldAnim = !(msg.hasKwarg("still") || msg.hasKwarg("notarget")
                || msg.hasKwarg("miss"));

        final CharSequence text = mBattleTextBuilder.move(sourcePoke, moveName, msg.kwarg("from"),
                msg.kwarg("of"), msg.kwarg("zMove"));

        // Major action's duration would be too long here
        mActionQueue.enqueueMinorAction(() -> {
            mLastMove = moveName;
            onMove(sourcePoke, targetPoke, moveName, shouldAnim);
            displayMajorActionMessage(text);
        });
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
        mActionQueue.enqueueMajorAction(() -> {
            onFaint(pokemonId);
            displayMajorActionMessage(mBattleTextBuilder.faint(pokemonId));
        });
    }

    private void handleTeamSize(ServerMessage msg) {
        String rawId = msg.nextArg();
        Player player = getPlayer(rawId);
        int count = parseInt(msg.nextArg());
        onTeamSize(player, count);
    }

    private void handleGameType(ServerMessage msg) {
        switch (msg.nextArg().trim()) {
            case "doubles":
                mGameType = Const.DOUBLE;
                mTrainerPokemons = new BattlingPokemon[2];
                mFoePokemons = new BattlingPokemon[2];
                break;
            case "rotation":
            case "triples":
                mGameType = Const.TRIPLE;
                printErrorMessage("Triple battles aren't fully implemented yet. " +
                        "App crash is a matter of seconds from now!");
                mTrainerPokemons = new BattlingPokemon[3];
                mFoePokemons = new BattlingPokemon[3];
                break;
            default:
                mGameType = Const.SINGLE;
                mTrainerPokemons = new BattlingPokemon[1];
                mFoePokemons = new BattlingPokemon[1];
                break;
        }
        if (mLastActionRequest != null && !mLastActionRequest.hasGameTypeSet())
            mLastActionRequest.setGameType(mGameType);
    }

    private void handleSwitch(ServerMessage msg) {
        String raw = msg.rawArgs();
        Player player = getPlayer(raw);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, raw);
        BattlingPokemon prevPoke = getBattlingPokemon(pokemon.id);

        String username = player.username(mP1Username, mP2Username, myUsername());
        final CharSequence text1 = mBattleTextBuilder.switchOut(prevPoke, username, msg.kwarg("from"));
        final CharSequence text2 = mBattleTextBuilder.switchIn(pokemon, username);

        mActionQueue.enqueueMajorAction(() -> {
            if (pokemon.id.isInBattle) {
                if (toIdSafe(mLastMove).equals("batonpass") || toIdSafe(mLastMove).equals("zbatonpass"))
                    pokemon.copyVolatiles(prevPoke, false);
                if (pokemon.foe) mFoePokemons[pokemon.position] = pokemon;
                else mTrainerPokemons[pokemon.position] = pokemon;
            }
            onSwitch(pokemon);
            if (text1 != null) displayMajorActionMessage(text1);
            displayMajorActionMessage(text2);
        });
    }

    private void handleDrag(ServerMessage msg) {
        String raw = msg.rawArgs();
        Player player = getPlayer(raw);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, raw);

        final CharSequence text = mBattleTextBuilder.drag(pokemon);

        mActionQueue.enqueueMajorAction(() -> {
            onSwitch(pokemon);
            displayMajorActionMessage(text);
        });
    }

    private void handleDetailsChanged(ServerMessage msg) {
        String raw = msg.rawArgs();
        Player player = getPlayer(raw);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, raw);
        msg.resetArgsIteration(); msg.nextArg();
        String arg2 = msg.hasNextArg() ? msg.nextArg() : null;
        String arg3 = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.pokemonChange(msg.command, pokemon.id, arg2, arg3,
                msg.kwarg("of"), msg.kwarg("from"));

        mActionQueue.enqueueAction(() -> {
            BattlingPokemon battlingPokemon = getBattlingPokemon(pokemon.id);
            battlingPokemon.species = pokemon.species;
            battlingPokemon.baseSpecies = pokemon.baseSpecies;
            battlingPokemon.forme = pokemon.forme;
            battlingPokemon.spriteId = pokemon.spriteId;

            onDetailsChanged(pokemon);
            displayMajorActionMessage(text);
        });
    }

    private void handleTurn(ServerMessage msg) {
        final Spannable spannable = new SpannableString("\n — Turn " + msg.nextArg() + " — ");
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 1,
                spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(1.2f), 1,
                spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mActionQueue.enqueueTurnAction(() -> {
            // Prevents from queuing message print
            BattleMessageObserver.super.printMessage(spannable);
        });
    }

    private void handleRequest(ServerMessage msg) {
        String rawJson = msg.rawArgs();
        if (rawJson.length() <= 1)
            return;
        try {
            JSONObject jsonObject = new JSONObject(rawJson);
            final BattleActionRequest request = new BattleActionRequest(jsonObject, mGameType);
            mLastActionRequest = request;
            mActionQueue.setLastAction(() -> onRequestAsked(request));
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing request json", e);
            printErrorMessage("An error has occurred while receiving choices.");
        }
    }

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
        printInactiveText(text);
    }

    private void handleWin(ServerMessage msg, boolean tie) {
        final String username = msg.hasNextArg() ? msg.nextArg() : null;
        final CharSequence text = tie ? mBattleTextBuilder.tie(mP1Username, mP2Username)
                : mBattleTextBuilder.win(username);
        mActionQueue.enqueueAction(() -> {
            mBattleRunning = false;
            onBattleEnded(username);
            displayMajorActionMessage(text);
            mActionQueue.setLastAction(null);
        });
    }

    private void handleCant(ServerMessage msg) {
        String rawId = msg.nextArg();
        PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String reason = msg.nextArg();
        String move = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.cant(pokemonId, reason, move, msg.kwarg("of"));

        mActionQueue.enqueueMajorAction(() -> displayMajorActionMessage(text));
    }

    // |swap|p2a: Dugtrio|1|[from] move: Ally Switch
    private void handleSwap(ServerMessage msg) {
        String rawId = msg.nextArg();
        PokemonId sourceId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        int sourceIndex = indexOf(getBattlingPokemon(sourceId),
                sourceId.foe ? mFoePokemons : mTrainerPokemons);
        String with = msg.hasNextArg() ? msg.nextArg() : "-1";
        int targetIndex;
        if (isInteger(with)) {
            targetIndex = parseInt(with);
        } else { // Not tested, old showdown feature
            PokemonId otherId = PokemonId.fromRawId(getPlayer(with), with);
            targetIndex = indexOf(getBattlingPokemon(otherId),
                    otherId.foe ? mFoePokemons : mTrainerPokemons);
        }
        if (targetIndex == sourceIndex || targetIndex < 0) return;

        mActionQueue.enqueueMajorAction(() -> {
            onSwap(sourceId, targetIndex);
            BattlingPokemon targetPoke = getBattlingPokemon(sourceId.player, targetIndex);
            displayMajorActionMessage(mBattleTextBuilder.swap(sourceId, targetPoke != null ? targetPoke.id : null));
            swap(sourceId.foe ? mFoePokemons : mTrainerPokemons, sourceIndex, targetIndex);
        });
    }

    private void handleMinorActionCommand(final ServerMessage message) {
        String command = message.command.substring(1);
        switch (command) {
            case "message":
                printMessage(message.rawArgs());
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
                handleCureTeam(message);
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
            case "clearboost":
            case "clearpositiveboost":
            case "clearnegativeboost":
                handleClearBoost(message);
                break;
            case "clearallboost":
                handleClearAllBoost(message);
                break;
            case "invertboost":
                handleInvertBoost(message);
                break;
            case "weather":
                handleWeather(message);
                break;
            case "fieldactivate":
            case "fieldstart":
                handleField(message, true);
                break;
            case "fieldend":
                handleField(message, false);
                break;
            case "activate":
                handleActivate(message);
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
                handleMoveEffect(message);
                break;
            case "immune":
                handleImmune(message);
                break;
            case "item":
                handleItem(message, true);
                break;
            case "enditem":
                handleItem(message, false);
                break;
            case "ability":
                handleAbility(message, true);
                break;
            case "endability":
                handleAbility(message, false);
                break;
            case "mega":
                handleMega(message, false);
                break;
            case "primal":
                handleMega(message, true);
                break;
            case "formechange":
            case "transform":
                handleFormeChange(message);
                break;
            case "hint":
                mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage("(" + message.nextArg() + ")"));
                break;
            case "center":
                break;
            case "start":
                handleVolatileStatus(message, true);
                break;
            case "end":
                handleVolatileStatus(message, false);
                break;
            case "block":
                handleBlock(message);
                break;
            case "ohko":
                handleOhko();
                break;
            case "combine":
                handleCombine();
                break;
            case "notarget":
                handleNoTarget();
                break;
            case "prepare":
                handlePrepare(message);
                break;
            case "zpower":
                handleZPower(message, false);
                break;
            case "zbroken":
                handleZPower(message, true);
                break;
            case "hitcount":
                handleHitCount(message);
                break;
            case "sethp":
                handleSetHp(message);
                break;
            case "singleturn":
            case "singlemove":
                handleSingle(message);
                break;
        }
    }

    private void handleFail(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String effect = msg.hasNextArg() ? msg.nextArg() : null;
        String stat = msg.hasNextArg() ? msg.nextArg() : null;
        final CharSequence text = mBattleTextBuilder.fail(pokemonId, effect, stat, msg.kwarg("from"),
                msg.kwarg("of"), msg.kwarg("msg"), msg.kwarg("heavy"), msg.kwarg("weak"),
                msg.kwarg("forme"));

        mActionQueue.enqueueMinorAction(() -> {
            onDisplayBattleToast(pokemonId, "Failed", Colors.GRAY);
            displayMinorActionMessage(text);
        });
    }

    private void handleMiss(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String targetRawId = msg.hasNextArg() ? msg.nextArg() : null;
        final PokemonId targetPokeId = targetRawId != null ?
                PokemonId.fromRawId(getPlayer(targetRawId), targetRawId) : null;

        final CharSequence text = mBattleTextBuilder.miss(pokemonId, targetPokeId, msg.kwarg("from"),
                msg.kwarg("of"));
        mActionQueue.enqueueMinorAction(() -> {
            onDisplayBattleToast(targetPokeId != null ? targetPokeId : pokemonId, "Missed", Colors.GRAY);
            displayMinorActionMessage(text);
        });
    }

    private void handleHealthChange(final ServerMessage msg, final boolean damage) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String rawCondition = msg.nextArg();
        final Condition condition = new Condition(rawCondition);

        mActionQueue.enqueueMinorAction(() -> {
            // Here we need to do text creation and percentage computation in the action queue to
            // prevent pkmn's condition to be updated too early (ex: damage then heal)
            String percentage = computePercentage(getBattlingPokemon(id).condition, condition);
            final CharSequence text;
            if (damage)
                text = mBattleTextBuilder.damage(id, percentage, msg.kwarg("from"), msg.kwarg("of"),
                        msg.kwarg("partiallytrapped"));
            else
                text = mBattleTextBuilder.heal(id, msg.kwarg("from"), msg.kwarg("of"), msg.kwarg("wisher"));
            getBattlingPokemon(id).condition = condition;
            onHealthChanged(id, condition);
            displayMinorActionMessage(text);
            onDisplayBattleToast(id,
                    (damage ? "-" : "+") + percentage,
                    damage ? Colors.RED : Colors.GREEN);
        });
    }

    private String computePercentage(Condition old, Condition neW) {
        if (old != null)
            return Math.round(100f * Math.abs(neW.hp - old.hp) / old.maxHp) + "%";
        else
            return "[" + neW.hp + "/" + neW.maxHp + "]";
    }

    private void handleStatus(ServerMessage msg, final boolean cure) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String status = msg.nextArg();

        final CharSequence text;
        if (!cure)
            text = mBattleTextBuilder.status(id, status, msg.kwarg("from"), msg.kwarg("of"));
        else
            text = mBattleTextBuilder.curestatus(id, status, msg.kwarg("from"), msg.kwarg("of"), msg.kwarg("thaw"));

        mActionQueue.enqueueMinorAction(() -> {
            if (id.isInBattle) {
                getBattlingPokemon(id).condition.status = cure ? null : status;
                onStatusChanged(id, cure ? null : status);
            }
            displayMinorActionMessage(text);
        });
    }

    private void handleCureTeam(ServerMessage msg) {
        final CharSequence text = mBattleTextBuilder.cureTeam(msg.kwarg("from"));
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }


    private void handleStatChange(ServerMessage msg, final boolean boost) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String stat = msg.hasNextArg() ? msg.nextArg() : null;
        String amount = msg.hasNextArg() ? msg.nextArg() : null;
        final int amountValue = parseWithDefault(amount, 0) * (boost ? 1 : -1);

        final CharSequence text = mBattleTextBuilder.boost(msg.command, id, stat, amount,
                msg.kwarg("from"), msg.kwarg("of"), msg.kwarg("multiple"), msg.kwarg("zeffect"));

        mActionQueue.enqueueMinorAction(() -> {
            StatModifiers statModifiers = getBattlingPokemon(id).statModifiers;
            statModifiers.inc(stat, amountValue);
            onStatChanged(id);
            displayMinorActionMessage(text);
        });
    }

    private void handleSetBoost(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String stat = msg.nextArg();
        final int amount = parseWithDefault(msg.nextArg(), 0);

        final CharSequence text = mBattleTextBuilder.setboost(id, msg.kwarg("from"), msg.kwarg("of"));

        mActionQueue.enqueueMinorAction(() -> {
            StatModifiers statModifiers = getBattlingPokemon(id).statModifiers;
            statModifiers.set(stat, amount);
            onStatChanged(id);
            displayMinorActionMessage(text);
        });
    }

    private void handleClearBoost(final ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String source = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.clearBoost(id, source, msg.kwarg("from"),
                msg.kwarg("of"), msg.kwarg("zeffect"));

        mActionQueue.enqueueMinorAction(() -> {
            StatModifiers statModifiers = getBattlingPokemon(id).statModifiers;
            if (msg.command.contains("positive"))
                statModifiers.clearPositive();
            else if (msg.command.contains("negative"))
                statModifiers.clearNegative();
            else
                statModifiers.clear();
            onStatChanged(id);
            displayMinorActionMessage(text);
        });
    }

    private void handleClearAllBoost(ServerMessage msg) {
        final CharSequence text = mBattleTextBuilder.clearAllBoost(msg.kwarg("from"));
        mActionQueue.enqueueMinorAction(() -> {
            for (BattlingPokemon pokemon : mTrainerPokemons) {
                pokemon.statModifiers.clear();
                onStatChanged(pokemon.id);
            }
            for (BattlingPokemon pokemon : mFoePokemons) {
                pokemon.statModifiers.clear();
                onStatChanged(pokemon.id);
            }
            displayMinorActionMessage(text);
        });
    }

    private void handleInvertBoost(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final CharSequence text = mBattleTextBuilder.invertBoost(id, msg.kwarg("from"), msg.kwarg("of"));
        mActionQueue.enqueueMinorAction(() -> {
            BattlingPokemon pokemon = getBattlingPokemon(id);
            pokemon.statModifiers.invert();
            onStatChanged(pokemon.id);
            displayMinorActionMessage(text);
        });
    }

    private void handleWeather(ServerMessage msg) {
        final String weather = msg.nextArg();

        final CharSequence text = mBattleTextBuilder.weather(weather, mActiveWeather,
                msg.kwarg("from"), msg.kwarg("of"), msg.kwarg("upkeep"));

        mActionQueue.enqueueMinorAction(() -> {
            mActiveWeather = "none".equals(weather) ? null : weather;
            if (mActiveWeather != null)
                onFieldEffectChanged(weather);
            else if (mActiveFieldEffects.size() > 0)
                onFieldEffectChanged(mActiveFieldEffects.get(0));
            else
                onFieldEffectChanged(null);
            displayMinorActionMessage(text);
        });
    }

    private void handleField(ServerMessage msg, boolean start) {
        String effect = msg.nextArg();
        final String fieldEffect;
        if (effect != null && effect.contains(":"))
            fieldEffect = toId(effect.substring(effect.indexOf(':') + 1));
        else fieldEffect = toId(effect, null);

        final CharSequence text;
        if (start)
            text = mBattleTextBuilder.field(msg.command, effect, msg.kwarg("from"),
                    msg.kwarg("of"));
        else
            text = mBattleTextBuilder.fieldend(effect);

        mActionQueue.enqueueMinorAction(() -> {
            if (start) {
                mActiveFieldEffects.add(fieldEffect);
                if (mActiveWeather == null) {
                    if (mActiveFieldEffects.size() == 1)
                        onFieldEffectChanged(fieldEffect);
                }
            } else {
                mActiveFieldEffects.remove(fieldEffect);
                if (mActiveWeather == null) {
                    if (mActiveFieldEffects.size() > 0)
                        onFieldEffectChanged(mActiveFieldEffects.get(0));
                    else
                        onFieldEffectChanged(null);
                }
            }
            displayMinorActionMessage(text);
        });
    }

    private void handleActivate(ServerMessage msg) {
        String rawId = msg.nextArg();
        PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String effect = msg.hasNextArg() ? msg.nextArg() : null;
        String target = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.activate(id, effect, target, msg.kwarg("of"),
                msg.kwarg("ability"), msg.kwarg("ability2"), msg.kwarg("move"), msg.kwarg("number"),
                msg.kwarg("item"), msg.kwarg("name"));

        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleSide(ServerMessage msg, final boolean start) {
        final Player player = getPlayer(msg.nextArg());
        String effect = msg.hasNextArg() ? msg.nextArg() : null;
        final String sideName;
        if (effect != null && effect.contains(":"))
            sideName = effect.substring(effect.indexOf(':') + 1);
        else sideName = effect;

        final CharSequence text;
        if (start)
            text = mBattleTextBuilder.sidestart(player, effect);
        else
            text = mBattleTextBuilder.sideend(player, effect);

        mActionQueue.enqueueMinorAction(() -> {
            onSideChanged(player, sideName, start);
            displayMinorActionMessage(text);
        });
    }

    private void handleMoveEffect(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final CharSequence text = mBattleTextBuilder.moveeffect(msg.command,
                pokemonId, msg.kwarg("spread"));
        final String toastText;
        switch (msg.command) {
            case "crit": toastText = "Critical"; break;
            case "resisted": toastText = "Resisted"; break;
            case "supereffective": toastText = "Supper effective"; break;
            default: toastText = null; break;
        }

        mActionQueue.enqueueMinorAction(() -> {
            displayMinorActionMessage(text);
            onDisplayBattleToast(pokemonId, toastText, Colors.RED);
        });
    }

    private void handleImmune(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final CharSequence text = mBattleTextBuilder.immune(pokemonId, msg.kwarg("from"),
                msg.kwarg("of"), msg.kwarg("ohko"));

        mActionQueue.enqueueMinorAction(() -> {
            displayMinorActionMessage(text);
            onDisplayBattleToast(pokemonId, "Immune", Colors.GRAY);
        });
    }

    private void handleItem(ServerMessage msg, boolean start) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String item = msg.hasNextArg() ? msg.nextArg() : null;
        final CharSequence text;
        if (start)
            text = mBattleTextBuilder.item(id, item, msg.kwarg("from"), msg.kwarg("of"));
        else
            text = mBattleTextBuilder.enditem(id, item, msg.kwarg("from"), msg.kwarg("of"),
                    msg.kwarg("eat"), msg.kwarg("move"), msg.kwarg("weaken"));
        mActionQueue.enqueueMinorAction(() -> {
            // TODO Maybe show a toast ?
            displayMinorActionMessage(text);
        });
    }

    private void handleAbility(final ServerMessage msg, final boolean start) {
        String rawId = msg.nextArg();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String ability = msg.hasNextArg() ? msg.nextArg() : null;
        String oldAbility = msg.hasNextArg() ? msg.nextArg() : null;
        String arg4 = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text;
        if (start)
            text = mBattleTextBuilder.ability(pokemonId, ability, oldAbility, arg4,
                    msg.kwarg("from"), msg.kwarg("of"), msg.kwarg("fail"));
        else
            text = mBattleTextBuilder.endability(pokemonId, ability, msg.kwarg("from"),
                    msg.kwarg("of"));

        mActionQueue.enqueueMinorAction(() -> {
            displayMinorActionMessage(text);
            if (start)
                onDisplayBattleToast(pokemonId, ability, Colors.BLUE);
        });
    }

    private void handleMega(ServerMessage msg, boolean primal) {
        String rawId = msg.nextArg();
        PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String species = msg.hasNextArg() ? msg.nextArg() : null;
        String item = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.mega(pokemonId, species, item, primal);
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleFormeChange(final ServerMessage msg) {
        String rawId = msg.nextArg();
        // TODO: If a pokemon swap occurs in the action queue after this,
        //  we might display an incorrect target when calling onDetailsChanged().
        //  This should be called on the action queue callback.
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String arg2 = msg.hasNextArg() ? msg.nextArg() : null;
        String arg3 = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.pokemonChange(msg.command,
                pokemonId, arg2, arg3, msg.kwarg("of"), msg.kwarg("from"));

        mActionQueue.enqueueMinorAction(() -> {
            displayMinorActionMessage(text);

            if (msg.command.contains("transform") && arg2 != null) {
                PokemonId targetId = PokemonId.fromRawId(getPlayer(arg2), arg2);
                if (pokemonId.equals(targetId)) return;

                BattlingPokemon pokemon = getBattlingPokemon(pokemonId);
                BattlingPokemon tpokemon = getBattlingPokemon(targetId);
                pokemon.transformSpecies = tpokemon.spriteId;
                onDetailsChanged(pokemon);
                for (String vStatus : pokemon.volatiles) onVolatileStatusChanged(pokemonId, vStatus, false);
                for (String vStatus : tpokemon.volatiles) onVolatileStatusChanged(pokemonId, vStatus, true);
                onVolatileStatusChanged(pokemonId, "transform", true);
                pokemon.statModifiers.set(tpokemon.statModifiers);
                onStatChanged(pokemonId);
            } else if (msg.command.contains("formechange") && arg2 != null) {
                BattlingPokemon pokemon = getBattlingPokemon(pokemonId);
                pokemon.spriteId = new BasePokemon(arg2).spriteId;
                onDetailsChanged(pokemon);
            }
        });
    }

    private void handleVolatileStatus(ServerMessage msg, final boolean start) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String effect = msg.nextArg();
        String arg3 = msg.hasNextArg() ? msg.nextArg() : null;
        boolean silent = msg.hasKwarg("silent");

        final CharSequence text;
        if (start)
            text = mBattleTextBuilder.start(id, effect, arg3, msg.kwarg("from"), msg.kwarg("of"),
                    msg.kwarg("already"), msg.kwarg("fatigue"), msg.kwarg("zeffect"),
                    msg.kwarg("damage"), msg.kwarg("block"), msg.kwarg("upkeep"));
        else
            text = mBattleTextBuilder.end(id, effect, msg.kwarg("from"), msg.kwarg("of"));

        mActionQueue.enqueueMinorAction(() -> {
            String effectId = toId(effect.contains(":") ? effect.substring(effect.indexOf(':') + 1) : effect);

            onVolatileStatusChanged(id, effectId, start);

            BattlingPokemon pokemon = getBattlingPokemon(id);
            if (pokemon != null) {
                if (effectId.startsWith("stockpile")) effectId = "stockpile";
                if (effectId.startsWith("perish")) effectId = "perish";
                if (start && effectId.equals("smackdown")) {
                    pokemon.volatiles.remove("magnetrise");
                    pokemon.volatiles.remove("telekinesis");
                }
                if (start) pokemon.volatiles.add(effectId);
                else pokemon.volatiles.remove(effectId);
            }

            if (!silent) displayMinorActionMessage(text);
        });
    }

    private void handleBlock(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String effect = msg.hasNextArg() ? msg.nextArg() : null;
        String move = msg.hasNextArg() ? msg.nextArg() : null;
        String attacker = msg.hasNextArg() ? msg.nextArg() : null;

        final CharSequence text = mBattleTextBuilder.block(id, effect, move, attacker,
                msg.kwarg("from"), msg.kwarg("of"));

        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleOhko() {
        final CharSequence text = mBattleTextBuilder.ohko();
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleCombine() {
        final CharSequence text = mBattleTextBuilder.combine();
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleNoTarget() {
        final CharSequence text = mBattleTextBuilder.notarget();
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handlePrepare(ServerMessage msg) {
        String rawId = msg.nextArg();
        PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String effect = msg.hasNextArg() ? msg.nextArg() : null;
        String target = msg.hasNextArg() ? msg.nextArg() : null;
        final CharSequence text = mBattleTextBuilder.prepare(id, effect, target);
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleZPower(ServerMessage msg, boolean broken) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final CharSequence text = broken ? mBattleTextBuilder.zbroken(id)
                : mBattleTextBuilder.zpower(id);
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text)); // Todo Animate callback
    }

    private void handleHitCount(ServerMessage msg) {
        if (msg.hasNextArg()) msg.nextArg();
        String count = msg.hasNextArg() ? msg.nextArg() : null;
        final CharSequence text = mBattleTextBuilder.hitcount(count);
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    private void handleSetHp(ServerMessage msg) {
        String rawId = msg.nextArg();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String rawCondition = msg.nextArg();
        final Condition condition = new Condition(rawCondition);

        final CharSequence text = mBattleTextBuilder.sethp(msg.kwarg("from"));
        mActionQueue.enqueueMinorAction(() -> {
            if (id.isInBattle) onHealthChanged(id, condition);
            displayMinorActionMessage(text);
        });
    }

    private void handleSingle(ServerMessage msg) {
        String rawId = msg.nextArg();
        PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String effect = msg.hasNextArg() ? msg.nextArg() : null;
        final CharSequence text = mBattleTextBuilder.single(id, effect, msg.kwarg("from"),
                msg.kwarg("of"));
        mActionQueue.enqueueMinorAction(() -> displayMinorActionMessage(text));
    }

    // This should be called only from action queue runnables
    private void displayMajorActionMessage(CharSequence text) {
        if (text == null) return;
        // Calling super to prevent queuing
        super.printMessage(text);
        onPrintBattleMessage(text);
    }

    // This should be called only from action queue runnables
    private void displayMinorActionMessage(CharSequence text) {
        if (text == null) return;
        Spannable spannable = new SpannableString(text);
        spannable.setSpan(new RelativeSizeSpan(0.8f), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        // Calling super to prevent queuing
        super.printMessage(spannable);
        onPrintBattleMessage(spannable);
    }

    private void printInactiveText(String text) {
        if (text == null) return;
        Spannable spannable = new SpannableString(text);
        spannable.setSpan(new RelativeSizeSpan(0.8f), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(0xFF8B0000), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        printMessage(spannable);
    }

    @Override
    protected void printMessage(final CharSequence text) {
        // Include eventual message prints from super class in the action queue.
        mActionQueue.enqueueAction(() -> BattleMessageObserver.super.printMessage(text));
    }

    @Override
    protected void printHtml(final String html) {
        // Include eventual html prints from super class in the action queue.
        mActionQueue.enqueueAction(() -> BattleMessageObserver.super.printHtml(html));
    }

    protected abstract void onPlayerInit(String playerUsername, String foeUsername);

    protected abstract void onFaint(PokemonId id);

    protected abstract void onTeamSize(Player player, int size);

    protected abstract void onBattleStarted();

    protected abstract void onBattleEnded(String winner);

    protected abstract void onTimerEnabled(boolean enabled);

    protected abstract void onPreviewStarted();

    protected abstract void onAddPreviewPokemon(PokemonId id, BasePokemon pokemon, boolean hasItem);

    protected abstract void onSwitch(BattlingPokemon newPokemon);

    protected abstract void onDetailsChanged(BattlingPokemon newPokemon);

    protected abstract void onMove(PokemonId sourceId, PokemonId targetId, String moveName, boolean shouldAnim);

    protected abstract void onSwap(PokemonId id, int targetIndex);

    protected abstract void onRequestAsked(BattleActionRequest request);

    protected abstract void onHealthChanged(PokemonId id, Condition condition);

    protected abstract void onStatusChanged(PokemonId id, String status);

    protected abstract void onStatChanged(PokemonId id);

    protected abstract void onDisplayBattleToast(PokemonId id, String text, int color);

    protected abstract void onFieldEffectChanged(String weather);

    protected abstract void onSideChanged(Player player, String side, boolean start);

    protected abstract void onVolatileStatusChanged(PokemonId id, String vStatus, boolean start);

    protected abstract void onPrintBattleMessage(CharSequence message);

    protected void onMarkBreak() {
//        mActionQueue.enqueueMinorAction(new Runnable() {
//            @Override
//            public void run() {
//                printMessage("");
//            }
//        });
    }
}
