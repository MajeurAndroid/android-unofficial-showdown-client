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
import com.majeur.psclient.model.BattleActionRequest;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.Const;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.majeur.psclient.model.Id.toId;

public abstract class BattleMessageObserver extends RoomMessageObserver {

    private BattleTextBuilder mBattleTextBuilder;
    private ActionQueue mActionQueue = new ActionQueue(Looper.getMainLooper());

    private String mP1Username;
    private String mP2Username;
    private Const mGameType;
    private boolean mBattleRunning;
    private int[] mPreviewPokemonIndexes;

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

    @Override
    public boolean onMessage(ServerMessage message) {
        if (super.onMessage(message))
            return true;
        message.args.reset();

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
                handleMove(message.args);
                break;
            case "switch":
                handleSwitch(message.args);
                break;
            case "drag":
                handleDrag(message.args);
                break;
            case "detailschange":
                handleDetailsChanged(message.args);
                break;
            case "turn":
                handleTurn(message.args);
                break;
            case "player":
                handlePlayer(message.args);
                break;
            case "upkeep":
                //
                break;
            case "faint":
                handleFaint(message.args);
                break;
            case "teamsize":
                handleTeamSize(message.args);
                break;
            case "gametype":
                handleGameType(message.args);
                break;
            case "tier":
                onPrintText(Utils.boldText(message.args.next()));
                break;
            case "rated":
                onPrintText(Utils.tagText("Rated battle"));
                break;
            case "rule":
                onPrintText(Utils.italicText(message.args.next()));
                break;
            case "clearpoke":
                mPreviewPokemonIndexes[0] = mPreviewPokemonIndexes[1] = 0;
                onPreviewStarted();
                break;
            case "poke":
                handlePreviewPokemon(message.args);
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
                handleRequest(message.args);
                break;
            case "inactive":
                handleInactive(message.args, true);
                break;
            case "inactiveoff":
                handleInactive(message.args, false);
                break;
            case "win":
                mBattleRunning = false;
                onBattleEnded();
                printMajorActionText(message.args.nextTillEnd() + " won the battle !");
                break;
            case "tie":
                mBattleRunning = false;
                onBattleEnded();
                printMajorActionText("Tie game !");
                break;
            case "cant":

                //    |cant|p1a: Persian|par//TODO
                break;
        }
    }

    // |move|p2a: Pinsir|Close Combat|p1a: Latias|[miss]
    // |move|p2a: Dialga|Flash Cannon|p1: Shiftry|[notarget]
    private void handleMove(ServerMessage.Args args) {
        String rawId = args.next();
        final PokemonId sourcePoke = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String moveName = args.next();
        rawId = args.next();
        final PokemonId targetPoke = rawId.length() > 0 ? PokemonId.fromRawId(getPlayer(rawId), rawId) : null;

        final boolean shouldAnim;
        if (args.hasNext()) {
            String next = args.next();
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

    private void handlePlayer(ServerMessage.Args args) {
        String playerId = args.next();
        if (!args.hasNext())
            return;
        String username = args.next();

        if (playerId.contains("1"))
            mP1Username = username;
        else
            mP2Username = username;

        if (mP1Username != null && mP2Username != null)
            onPlayerInit(Player.TRAINER.username(mP1Username, mP2Username, myUsername()),
                    Player.FOE.username(mP1Username, mP2Username, myUsername()));

    }

    private void handleFaint(ServerMessage.Args args) {
        String rawId = args.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                onFaint(pokemonId);
                printMajorActionText(mBattleTextBuilder.faint(pokemonId));
            }
        });
    }

    private void handleTeamSize(ServerMessage.Args args) {
        String rawId = args.next();
        Player player = getPlayer(rawId);
        int count = Integer.parseInt(args.next());
        onTeamSize(player, count);
    }

    private void handleGameType(ServerMessage.Args args) {
        switch (args.next().charAt(0)) {
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

    private void handleSwitch(ServerMessage.Args args) {
        String msg = args.nextTillEnd();
        Player player = getPlayer(msg);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, msg);

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

    private void handleDrag(ServerMessage.Args args) {
        String msg = args.nextTillEnd();
        Player player = getPlayer(msg);
        final BattlingPokemon pokemon = BattlingPokemon.fromSwitchMessage(player, msg);

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

    // |-formechange|POKEMON|SPECIES|HP STATUS
    // |detailschange|POKEMON|DETAILS|HP STATUS
    // |detailschange|p1a: Aerodactyl|Aerodactyl-Mega, M
    private void handleDetailsChanged(ServerMessage.Args args) {

    }

    private void handleTurn(ServerMessage.Args args) {
        final Spannable spannable = new SpannableString("\n — Turn " + args.next() + " — ");
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

    private void handleRequest(ServerMessage.Args args) {
        String rawJson = args.nextTillEnd();
        if (rawJson.length() <= 1)
            return;
        try {
            JSONObject jsonObject = new JSONObject(rawJson);
            final BattleActionRequest request = new BattleActionRequest(jsonObject);
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
    private void handlePreviewPokemon(ServerMessage.Args args) {
        String rawId = args.next();
        Player player = getPlayer(rawId);
        int curIndex = mPreviewPokemonIndexes[player == Player.FOE ? 1 : 0]++;
        String species = toId(args.next().split(",")[0]);
        boolean hasItem = args.hasNext();
        onAddPreviewPokemon(PokemonId.fromPosition(player, curIndex), species, hasItem);
    }

    private void handleInactive(ServerMessage.Args args, boolean on) {
        onTimerEnabled(on);
        final String text = args.next();
        if (text.startsWith("Time left:"))
            return;
        mActionQueue.enqueueAction(new Runnable() {
            @Override
            public void run() {
                printInactiveText(text);
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
                        onPrintText(message.args.nextTillEnd());
                    }
                });
                break;
            case "fail":
                handleFail(message.args);
                break;
            case "miss":
                handleMiss(message.args);
                break;
            case "damage":
                handleHealthChange(message.args, true);
                break;
            case "heal":
                handleHealthChange(message.args, false);
                break;
            case "status":
                handleStatus(message.args, false);
                break;
            case "curestatus":
                handleStatus(message.args, true);
                break;
            case "cureteam":

                break;
            case "boost":
                handleStatChange(message.args, true);
                break;
            case "unboost":
                handleStatChange(message.args, false);
                break;
            case "setboost":
                handleSetBoost(message.args);
                break;
            case "weather":
                handleWeather(message.args);
                break;
            case "fieldstart":

                break;
            case "fieldend":

                break;
            case "sidestart":
                handleSide(message.args, true);
                break;
            case "sideend":
                handleSide(message.args, false);
                break;
            case "crit":
            case "resisted":
            case "supereffective":
            case "immune":
                handleMoveEffect(message.args, command);
                break;
            case "item":

                break;
            case "enditem"://|-enditem|p2a: Magcargo|Air Balloon

                break;
            case "ability":
                handleAbility(message.args, true);
                break;
            case "endability":
                handleAbility(message.args, false);
                break;
            case "transform":

                break;
            case "mega":

                break;
            case "hint":
                mActionQueue.enqueueMinorAction(new Runnable() {
                    @Override
                    public void run() {
                        printMinorActionText("(" + message.args.next() + ")");
                    }
                });
                break;
            case "center":
                break;
            case "start":
                handleVolatileStatusStart(message.args, true);
                break;
            case "end":
                handleVolatileStatusStart(message.args, false);
                break;
//                |-hitcount|p1a: Toxicroak|1 TODO
            //    |-prepare|p2a: Arceus|Shadow Force|p1a: Mandibuzz
        }
    }

    private void handleAbility(final ServerMessage.Args args, final boolean start) {
        String rawId = args.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String ability = args.hasNext() ? args.next() : null;
        String extra = args.hasNext() ? args.next() : null;
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

    private void handleVolatileStatusStart(ServerMessage.Args args, final boolean start) {
        String rawId = args.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        if (!id.isInBattle) return;
        final String effect = args.next();
        String of = args.hasNext() ? args.next() : null;

        final Spanned text = mBattleTextBuilder.volatileStatus(id, effect, of, start);

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

    private void handleFail(ServerMessage.Args args) {
        String rawId = args.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String action = args.hasNext() ? args.next() : null;
        final Spanned text = mBattleTextBuilder.fail(pokemonId, action);
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onDisplayBattleToast(pokemonId, "Failed", Color.GRAY);
                printMinorActionText(text);
            }
        });
    }

    private void handleMiss(ServerMessage.Args args) {
        String rawId = args.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final PokemonId targetPokeId;
        if (args.hasNext()) {
            rawId = args.hasNext() ? args.next() : null;
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

    private void handleHealthChange(ServerMessage.Args args, boolean damage) {
        int index = args.getIndex();
        try {
            String rawId = args.next();
            final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
            String rawCondition = args.next();
            final Condition condition = new Condition(rawCondition);

            String mainKey = damage ? "damage" : "heal";
            String from = null;
            PokemonId of = null;
            if (args.hasNext()) from = args.next();
            if (args.hasNext()) {
                rawId = args.next();
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
        } catch (Exception e) {
            e.printStackTrace();
            args.moveTo(index);
            printInactiveText(String.format("Could not handle message (%s)\n%s", e.toString(), args.nextTillEnd()));
        }
    }

    private void handleStatus(ServerMessage.Args args, final boolean healed) {
        String rawId = args.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String status = args.next();
        final String action = args.hasNext() ? args.next() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatusChanged(id, healed ? null : status);
                printMinorActionText(mBattleTextBuilder.status(id, status, !healed, action));
            }
        });
    }

    private void handleStatChange(ServerMessage.Args args, final boolean boost) {
        // POKEMON|STAT|AMOUNT
        String rawId = args.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final String stat = args.next();
        final int amount = (boost ? 1 : -1) * Integer.parseInt(args.next());

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatChanged(id, stat, amount, false);
                printMinorActionText(mBattleTextBuilder.statModifier(id, stat, amount));
            }
        });
    }

    private void handleSetBoost(ServerMessage.Args args) {
        String rawId = args.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final String stat = args.next();
        final int amount = Integer.parseInt(args.next());

        final String from = args.hasNext() ? args.next() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatChanged(id, stat, amount, true);
                printMinorActionText(mBattleTextBuilder.statModifierSet(id, stat, amount, from));
            }
        });
    }

    private void handleWeather(ServerMessage.Args args) {
        final String weather = args.next();
        final String action = args.hasNext() ? args.next() : null;

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

    private void handleSide(ServerMessage.Args args, final boolean start) {
        final Player player = getPlayer(args.next());
        final String side = args.next();
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onSideChanged(player, side, start);
                Spanned text = mBattleTextBuilder.side(player, side, start);
                printMinorActionText(text);
            }
        });
    }

    private void handleMoveEffect(ServerMessage.Args args, final String type) {
        String rawId = args.next();
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

    protected abstract void onAddPreviewPokemon(PokemonId id, String species, boolean hasItem);

    protected abstract void onSwitch(BattlingPokemon newPokemon);

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
