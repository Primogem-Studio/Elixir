package net.per.elixir.compat.genshincraft;

import net.hackermdch.genshincraft.element.Element;
import net.hackermdch.genshincraft.element.ElementDamageSource;
import net.hackermdch.genshincraft.misc.TypeDamageSource;
import net.minecraft.world.damagesource.DamageSource;

public class DamageHelper {
    public static DamageSource type(DamageSource source, int type) {
        return switch (type) {
            case 0 -> new TypeDamageSource(source, TypeDamageSource.Type.PHYSICAL);
            case 1 -> new TypeDamageSource(source, TypeDamageSource.Type.QUANTUM);
            case 2 -> new TypeDamageSource(source, TypeDamageSource.Type.IMAGINARY);
            default -> source;
        };
    }

    public static DamageSource element(DamageSource source, int type, float qua) {
        return switch (type) {
            case 0 -> new ElementDamageSource(source, Element.fromType(Element.Type.Pyro, qua));
            case 1 -> new ElementDamageSource(source, Element.fromType(Element.Type.Hydro, qua));
            case 2 -> new ElementDamageSource(source, Element.fromType(Element.Type.Electro, qua));
            case 3 -> new ElementDamageSource(source, Element.fromType(Element.Type.Cryo, qua));
            case 4 -> new ElementDamageSource(source, Element.fromType(Element.Type.Dendro, qua));
            case 5 -> new ElementDamageSource(source, Element.fromType(Element.Type.Anemo, qua));
            case 7 -> new ElementDamageSource(source, Element.fromType(Element.Type.Geo, qua));
            default -> source;
        };
    }
}
