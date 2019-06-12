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
import android.util.Log;

import com.majeur.psclient.util.S;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.io.BattleTextBuilder;
import com.majeur.psclient.model.BattleActionRequest;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.Const;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BattleRoomMessageHandler extends RoomMessageHandler {

    private BattleTextBuilder mBattleTextBuilder;
    private ActionQueue mActionQueue = new ActionQueue(Looper.getMainLooper());

    private String mP1Username;
    private String mP2Username;
    private Const mGameType;
    private boolean mBattleRunning;

    public void gotContext(Context context) {
        mBattleTextBuilder = new BattleTextBuilder(context);
    }

    @Override
    public boolean shouldHandleMessages(String messages) {
        if (S.run)
            return true;
        return mRoomId != null && messages.startsWith(">" + mRoomId);
    }

    @Override
    public void onRoomInit() {
        mP1Username = null;
        mP2Username = null;
        mGameType = null;
        mActionQueue.clear();
        mBattleRunning = true;
    }

    @Override
    public void onRoomDeInit() {

    }

    public void startBattle(String roomId, boolean autoJoin) {
        if (roomId.equals(mRoomId))
            return;

        if (mBattleRunning) {
            Log.e(getClass().getSimpleName(), "Cannot start battle while another one is running, ignoring.");
            return;
        }

        if (autoJoin) {
            setAutoJoinedRoom(roomId);
        } else {
            joinRoom(roomId);
        }
    }

    public Const getGameType() {
        return mGameType;
    }


    public boolean battleRunning() {
        return mBattleRunning;
    }

    public void forfeit() {
        getShowdownService().sendRoomCommand(mRoomId, "forfeit", null);
    }

    public void sendSwitchDecision(int reqId, int who) {
        getShowdownService().sendRoomCommand(mRoomId, "switch", who + "|" + reqId);
    }

    public void sendChooseDecision(int reqId, int which) {
        //gen7randombattle-860621231|/choose move 1|3
        getShowdownService().sendRoomCommand(mRoomId, "move", which + "|" + reqId);
    }

    public void sendTimerCommand(boolean on) {
        getShowdownService().sendRoomCommand(mRoomId, "timer", on ? "on" : "off");
    }

    private String myUsername() {
        return getShowdownService().getSharedData("username");
    }

    @Override
    public void onHandleMessage(MessageIterator message) {
        int index = message.getIndex();
        super.onHandleMessage(message);
        message.moveTo(index);

        String command = message.next();
        if (command.charAt(0) == '-')
            handleMinorActionCommand(command.substring(1), message);
        else
            handleRegularCommand(command, message);
    }

    private void handleRegularCommand(String command, MessageIterator message) {
        switch (command) {
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
                onPrintText(Utils.boldText(message.next()));
                break;
            case "rated":
                onPrintText(Utils.tagText("Rated battle"));
                break;
            case "rule":
                onPrintText(Utils.italicText(message.next()));
                break;
            case "clearpoke":
                onPreviewStarted();
                break;
            case "poke":

                break;
            case "teampreview":

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
                mBattleRunning = false;
                onBattleEnded();
                printMajorActionText(message.nextTillEnd() + " won the battle !");
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
    private void handleMove(MessageIterator message) {
        String rawId = message.next();
        final PokemonId sourcePoke = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String moveName = message.next();
        rawId = message.next();
        final PokemonId targetPoke = rawId.length() > 0 ? PokemonId.fromRawId(getPlayer(rawId), rawId) : null;

        final boolean shouldAnim;
        if (message.hasNext()) {
            String next = message.next();
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

    private void handlePlayer(MessageIterator message) {
        String playerId = message.next();
        if (!message.hasNext())
            return;
        String username = message.next();

        if (playerId.contains("1"))
            mP1Username = username;
        else
            mP2Username = username;

        if (mP1Username != null && mP2Username != null)
            onPlayerInit(Player.TRAINER.username(mP1Username, mP2Username, myUsername()),
                    Player.FOE.username(mP1Username, mP2Username, myUsername()));

    }

    private void handleFaint(MessageIterator message) {
        String rawId = message.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        mActionQueue.enqueueMajorAction(new Runnable() {
            @Override
            public void run() {
                onFaint(pokemonId);
                printMajorActionText(mBattleTextBuilder.faint(pokemonId));
            }
        });
    }

    private void handleTeamSize(MessageIterator message) {
        String rawId = message.next();
        int count = Integer.parseInt(message.next());
        Player player = getPlayer(rawId);
        onTeamSize(player, count);
    }

    private void handleGameType(MessageIterator message) {
        switch (message.next().charAt(0)) {
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

    private void handleSwitch(MessageIterator message) {
        String msg = message.nextTillEnd();
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

    private void handleDrag(MessageIterator message) {
        String msg = message.nextTillEnd();
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

    private void handleTurn(MessageIterator message) {
        final Spannable spannable = new SpannableString("\n — Turn " + message.next() + " — ");
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

    private void handleRequest(MessageIterator message) {
        String rawJson = message.nextTillEnd();
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

    private void handleInactive(MessageIterator message, boolean on) {
        onTimerEnabled(on);
        final String text = message.next();
        if (text.startsWith("Time left:"))
            return;
        mActionQueue.enqueueAction(new Runnable() {
            @Override
            public void run() {
                printInactiveText(text);
            }
        });
    }

    private void handleMinorActionCommand(String command, final MessageIterator message) {
        switch (command) {
            case "message":
                mActionQueue.enqueueMinorAction(new Runnable() {
                    @Override
                    public void run() {
                        onPrintText(message.nextTillEnd());
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

                break;
            case "hint":
                mActionQueue.enqueueMinorAction(new Runnable() {
                    @Override
                    public void run() {
                        printMinorActionText("(" + message.next() + ")");
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
        }
    }

    private void handleAbility(final MessageIterator message, final boolean start) {
        String rawId = message.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String ability = message.hasNext() ? message.next() : null;
        String extra = message.hasNext() ? message.next() : null;
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

    private void handleVolatileStatusStart(MessageIterator message, final boolean start) {
        String rawId = message.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        if (!id.isInBattle) return;
        final String effect = message.next();
        String of = message.hasNext() ? message.next() : null;

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

    private void handleFail(MessageIterator message) {
        String rawId = message.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        String action = message.hasNext() ? message.next() : null;
        final Spanned text = mBattleTextBuilder.fail(pokemonId, action);
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onDisplayBattleToast(pokemonId, "Failed", Color.GRAY);
                printMinorActionText(text);
            }
        });
    }

    private void handleMiss(MessageIterator message) {
        String rawId = message.next();
        final PokemonId pokemonId = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final PokemonId targetPokeId;
        if (message.hasNext()) {
            rawId = message.hasNext() ? message.next() : null;
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

    private void handleHealthChange(MessageIterator message, boolean damage) {
        int index = message.getIndex();
        try {
            String rawId = message.next();
            final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
            String rawCondition = message.next();
            final Condition condition = new Condition(rawCondition);

            String mainKey = damage ? "damage" : "heal";
            String from = null;
            PokemonId of = null;
            if (message.hasNext()) from = message.next();
            if (message.hasNext()) {
                rawId = message.next();
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
            message.moveTo(index);
            printInactiveText(String.format("Could not handle message (%s)\n%s", e.toString(), message.nextTillEnd()));
        }
    }

    private void handleStatus(MessageIterator message, final boolean healed) {
        String rawId = message.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);
        final String status = message.next();
        final String action = message.hasNext() ? message.next() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatusChanged(id, healed ? null : status);
                printMinorActionText(mBattleTextBuilder.status(id, status, !healed, action));
            }
        });
    }

    private void handleStatChange(MessageIterator message, final boolean boost) {
        // POKEMON|STAT|AMOUNT
        String rawId = message.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final String stat = message.next();
        final int amount = (boost ? 1 : -1) * Integer.parseInt(message.next());

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatChanged(id, stat, amount, false);
                printMinorActionText(mBattleTextBuilder.statModifier(id, stat, amount));
            }
        });
    }

    private void handleSetBoost(MessageIterator message) {
        String rawId = message.next();
        final PokemonId id = PokemonId.fromRawId(getPlayer(rawId), rawId);

        final String stat = message.next();
        final int amount = Integer.parseInt(message.next());

        final String from = message.hasNext() ? message.next() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onStatChanged(id, stat, amount, true);
                printMinorActionText(mBattleTextBuilder.statModifierSet(id, stat, amount, from));
            }
        });
    }

    private void handleWeather(MessageIterator message) {
        final String weather = message.next();
        final String action = message.hasNext() ? message.next() : null;

        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                Spanned text = mBattleTextBuilder.weather(weather, action);
                if (text != null)
                    printMinorActionText(text);

                if (action == null || !action.contains("upkeep"))
                    onWeatherChanged(weather);
            }
        });
    }

    private void handleSide(MessageIterator message, final boolean start) {
        final Player player = getPlayer(message.next());
        final String side = message.next();
        mActionQueue.enqueueMinorAction(new Runnable() {
            @Override
            public void run() {
                onSideChanged(player, side, start);
                Spanned text = mBattleTextBuilder.side(player, side, start);
                printMinorActionText(text);
            }
        });
    }

    private void handleMoveEffect(MessageIterator message, final String type) {
        String rawId = message.next();
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
