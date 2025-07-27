package vrpg.spellbook;

import java.util.Map;

public class Spell {
    public Map<String, String> localized;
    //todo support chinese
    //todo check mc's language code, use the same one
    public String action;
    public int duration;
    public int statusEffectLevel;
    public String statusEffectType;
}
