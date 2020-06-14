package com.majeur.psclient.model.battle;

import androidx.annotation.Nullable;
import com.majeur.psclient.model.common.Colors;

public class VolatileStatus {

    public final String id;
    public final String label;
    public final int color;

    private VolatileStatus(String id, @Nullable String label, int color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public static VolatileStatus getForId(String name) {
        switch (name) {
            case "dynamax":
                return new VolatileStatus(name, "Dynamaxed", Colors.VOLATILE_GOOD);
            case "throatchop":
                return new VolatileStatus(name, "Throat Chop", Colors.VOLATILE_BAD);
            case "powertrick":
                return new VolatileStatus(name, "Power Trick", Colors.VOLATILE_NEUTRAL);
            case "foresight":
            case "miracleeye":
                return new VolatileStatus(name, "Identified", Colors.VOLATILE_BAD);
            case "telekinesis":
                return new VolatileStatus(name, "Telekinesis", Colors.VOLATILE_NEUTRAL);
            case "transform":
                return new VolatileStatus(name, "Transformed", Colors.VOLATILE_NEUTRAL);
            case "confusion":
                return new VolatileStatus(name, "Confused", Colors.VOLATILE_BAD);
            case "healblock":
                return new VolatileStatus(name, "Heal Block", Colors.VOLATILE_BAD);
            case "yawn":
                return new VolatileStatus(name, "Drowsy", Colors.VOLATILE_BAD);
            case "taunt":
                return new VolatileStatus(name, "Taunted", Colors.VOLATILE_BAD);
            case "imprison":
                return new VolatileStatus(name, "Imprisoning", Colors.VOLATILE_GOOD);
            case "disable":
                return new VolatileStatus(name, "Disabled", Colors.VOLATILE_BAD);
            case "embargo":
                return new VolatileStatus(name, "Embargo", Colors.VOLATILE_BAD);
            case "torment":
                return new VolatileStatus(name, "Tormented", Colors.VOLATILE_BAD);
            case "ingrain":
                return new VolatileStatus(name, "Ingrained", Colors.VOLATILE_GOOD);
            case "aquaring":
                return new VolatileStatus(name, "Aqua Ring", Colors.VOLATILE_GOOD);
            case "stockpile1":
                return new VolatileStatus("stockpile", "Stockpile", Colors.VOLATILE_GOOD);
            case "stockpile2":
                return new VolatileStatus("stockpile", "Stockpile×2", Colors.VOLATILE_GOOD);
            case "stockpile3":
                return new VolatileStatus("stockpile", "Stockpile×3", Colors.VOLATILE_GOOD);
            case "perish0":
                return new VolatileStatus("perish", null, Colors.VOLATILE_BAD);
            case "perish1":
                return new VolatileStatus("perish", "Perish next turn", Colors.VOLATILE_BAD);
            case "perish2":
                return new VolatileStatus("perish", "Perish in 2", Colors.VOLATILE_BAD);
            case "perish3":
                return new VolatileStatus("perish", "Perish in 3", Colors.VOLATILE_BAD);
            case "encore":
                return new VolatileStatus(name, "Encored", Colors.VOLATILE_BAD);
            case "bide":
                return new VolatileStatus(name, "Bide", Colors.VOLATILE_GOOD);
            case "attract":
                return new VolatileStatus(name, "Attracted", Colors.VOLATILE_BAD);
            case "autotomize":
                return new VolatileStatus(name, "Lightened", Colors.VOLATILE_GOOD);
            case "focusenergy":
                return new VolatileStatus(name, "+Crit rate", Colors.VOLATILE_GOOD);
            case "curse":
                return new VolatileStatus(name, "Cursed", Colors.VOLATILE_BAD);
            case "nightmare":
                return new VolatileStatus(name, "Nightmare", Colors.VOLATILE_BAD);
            case "magnetrise":
                return new VolatileStatus(name, "Magnet Rise", Colors.VOLATILE_GOOD);
            case "smackdown":
                return new VolatileStatus(name, "Smacked Down", Colors.VOLATILE_BAD);
            case "substitute":
                return new VolatileStatus(name, "Substitute", Colors.VOLATILE_NEUTRAL);
            case "lightscreen":
                return new VolatileStatus(name, "Light Screen", Colors.VOLATILE_GOOD);
            case "reflect":
                return new VolatileStatus(name, "Reflect", Colors.VOLATILE_GOOD);
            case "flashfire":
                return new VolatileStatus(name, "Flash Fire", Colors.VOLATILE_GOOD);
            case "airballoon":
                return new VolatileStatus(name, "Balloon", Colors.VOLATILE_GOOD);
            case "leechseed":
                return new VolatileStatus(name, "Leech Seed", Colors.VOLATILE_BAD);
            case "slowstart":
                return new VolatileStatus(name, "Slow Start", Colors.VOLATILE_BAD);
            case "noretreat":
                return new VolatileStatus(name, "No Retreat", Colors.VOLATILE_BAD);
            case "octolock":
                return new VolatileStatus(name, "Octolock", Colors.VOLATILE_BAD);
            case "mimic":
                return new VolatileStatus(name, "Mimic", Colors.VOLATILE_GOOD);
            case "watersport":
                return new VolatileStatus(name, "Water Sport", Colors.VOLATILE_GOOD);
            case "mudsport":
                return new VolatileStatus(name, "Mud Sport", Colors.VOLATILE_GOOD);
            case "uproar":
                return new VolatileStatus(name, "Uproar", Colors.VOLATILE_NEUTRAL);
            case "rage":
                return new VolatileStatus(name, "Rage", Colors.VOLATILE_NEUTRAL);
            case "roost":
                return new VolatileStatus(name, "Landed", Colors.VOLATILE_NEUTRAL);
            case "protect":
                return new VolatileStatus(name, "Protect", Colors.VOLATILE_GOOD);
            case "quickguard":
                return new VolatileStatus(name, "Quick Guard", Colors.VOLATILE_GOOD);
            case "wideguard":
                return new VolatileStatus(name, "Wide Guard", Colors.VOLATILE_GOOD);
            case "craftyshield":
                return new VolatileStatus(name, "Crafty Shield", Colors.VOLATILE_GOOD);
            case "matblock":
                return new VolatileStatus(name, "Mat Block", Colors.VOLATILE_GOOD);
            case "maxguard":
                return new VolatileStatus(name, "Max Guard", Colors.VOLATILE_GOOD);
            case "helpinghand":
                return new VolatileStatus(name, "Helping Hand", Colors.VOLATILE_GOOD);
            case "magiccoat":
                return new VolatileStatus(name, "Magic Coat", Colors.VOLATILE_GOOD);
            case "destinybond":
                return new VolatileStatus(name, "Destiny Bond", Colors.VOLATILE_GOOD);
            case "snatch":
                return new VolatileStatus(name, "Snatch", Colors.VOLATILE_GOOD);
            case "grudge":
                return new VolatileStatus(name, "Grudge", Colors.VOLATILE_GOOD);
            case "charge":
                return new VolatileStatus(name, "Charge", Colors.VOLATILE_GOOD);
            case "endure":
                return new VolatileStatus(name, "Endure", Colors.VOLATILE_GOOD);
            case "focuspunch":
                return new VolatileStatus(name, "Focusing", Colors.VOLATILE_NEUTRAL);
            case "shelltrap":
                return new VolatileStatus(name, "Trap set", Colors.VOLATILE_NEUTRAL);
            case "powder":
                return new VolatileStatus(name, "Powder", Colors.VOLATILE_BAD);
            case "electrify":
                return new VolatileStatus(name, "Electrify", Colors.VOLATILE_BAD);
            case "ragepowder":
                return new VolatileStatus(name, "Rage Powder", Colors.VOLATILE_GOOD);
            case "followme":
                return new VolatileStatus(name, "Follow Me", Colors.VOLATILE_GOOD);
            case "instruct":
                return new VolatileStatus(name, "Instruct", Colors.VOLATILE_NEUTRAL);
            case "beakblast":
                return new VolatileStatus(name, "Beak Blast", Colors.VOLATILE_NEUTRAL);
            case "laserfocus":
                return new VolatileStatus(name, "Laser Focus", Colors.VOLATILE_GOOD);
            case "spotlight":
                return new VolatileStatus(name, "Spotlight", Colors.VOLATILE_NEUTRAL);
            case "bind":
                return new VolatileStatus(name, "Bind", Colors.VOLATILE_BAD);
            case "clamp":
                return new VolatileStatus(name, "Clamp", Colors.VOLATILE_BAD);
            case "firespin":
                return new VolatileStatus(name, "Fire Spin", Colors.VOLATILE_BAD);
            case "infestation":
                return new VolatileStatus(name, "Infestation", Colors.VOLATILE_BAD);
            case "magmastorm":
                return new VolatileStatus(name, "Magma Storm", Colors.VOLATILE_BAD);
            case "sandtomb":
                return new VolatileStatus(name, "Sand Tomb", Colors.VOLATILE_BAD);
            case "whirlpool":
                return new VolatileStatus(name, "Whirlpool", Colors.VOLATILE_BAD);
            case "wrap":
                return new VolatileStatus(name, "Wrap", Colors.VOLATILE_BAD);
            default:
                return null;
        }
    }
    
}
