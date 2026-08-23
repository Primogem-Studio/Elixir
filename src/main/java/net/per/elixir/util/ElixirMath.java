package net.per.elixir.util;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.per.elixir.registry.data.Material;

import java.util.function.DoubleUnaryOperator;

import static net.per.elixir.ElixirConfig.pharmaLimited;

public final class ElixirMath {
    private ElixirMath() {
    }

    public static int rawPharm(int pharma, float exp, double s) {
        return (int) (pharma + (exp * 0.01 * pharma) + (pharma * (Math.min(s * 0.01, 2))));
    }

    public static int finalPharm(Holder<Material> off, int raw) {
        return Mth.clamp(ElixirHelper.calc(off.value(), raw, off.value().base()), -pharmaLimited, pharmaLimited);
    }

    public static int predictPharm(Holder<Material> off, int pharma, float exp, double s) {
        return finalPharm(off, rawPharm(pharma, exp, s));
    }

    public static double findPharmZero(Holder<Material> off, int pharma, float exp, double lo, double hi, boolean covered) {
        DoubleUnaryOperator f = s -> predictPharm(off, pharma, exp, covered ? s : s / 2);
        var flo = f.applyAsDouble(lo);
        var fhi = f.applyAsDouble(hi);
        if ((flo < 0) == (fhi < 0)) return flo >= 0 ? lo : hi;
        for (int i = 0; i < 40; i++) {
            var mid = (lo + hi) / 2;
            var fm = f.applyAsDouble(mid);
            if ((fm < 0) == (flo < 0)) lo = mid;
            else hi = mid;
        }
        return (lo + hi) / 2;
    }
}
