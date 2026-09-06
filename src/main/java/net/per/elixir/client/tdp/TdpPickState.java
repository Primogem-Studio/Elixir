package net.per.elixir.client.tdp;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.per.elixir.registry.data.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TdpPickState {
    private final boolean keepEmptyOff;
    private final List<Holder<Material>> allMains = new ArrayList<>();
    private final List<Holder<Material>> allOffs = new ArrayList<>();
    private final List<Holder<Material>> mains = new ArrayList<>();
    private final List<Holder<Material>> offs = new ArrayList<>();
    private final TdpEditField search;
    private String query = "";

    public TdpPickState(boolean keepEmptyOff) {
        this.keepEmptyOff = keepEmptyOff;
        search = new TdpEditField(170, 13);
        search.placeholder(Component.translatable("gui.elixir.tdp.search.hint"));
    }

    public TdpEditField search() {
        return search;
    }

    public void reload() {
        allMains.clear();
        allMains.addAll(TdpData.materials(true));
        allOffs.clear();
        for (var m : TdpData.materials(false)) {
            if (keepEmptyOff || !TdpData.isEmpty(m)) allOffs.add(m);
        }
        query = null;
        apply();
    }

    public void apply() {
        String q = search.text().trim().toLowerCase(Locale.ROOT);
        if (q.equals(query)) return;
        query = q;
        mains.clear();
        for (var m : allMains) {
            if (matches(m, q)) mains.add(m);
        }
        offs.clear();
        for (var m : allOffs) {
            if (matches(m, q)) offs.add(m);
        }
    }

    public List<Holder<Material>> mains() {
        return mains;
    }

    public List<Holder<Material>> offs() {
        return offs;
    }

    private static boolean matches(Holder<Material> m, String q) {
        if (q.isEmpty()) return true;
        return TdpData.name(m).getString().toLowerCase(Locale.ROOT).contains(q)
                || TdpData.description(m).getString().toLowerCase(Locale.ROOT).contains(q)
                || TdpData.id(m).toLowerCase(Locale.ROOT).contains(q);
    }
}
