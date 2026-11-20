package dev.ollasland.cinematics.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Registry-powered selection: also discovers mobs added by other installed mods. */
public class MobPickerScreen extends Screen {
    private final SceneEditorScreen editor;
    private final List<String> all=new ArrayList<>();
    private final List<String> filtered=new ArrayList<>();
    private EditBox search;
    private int page;
    public MobPickerScreen(SceneEditorScreen editor) {
        super(Component.literal("Seleccionar mob"));this.editor=editor;
        for(EntityType<?> type:ForgeRegistries.ENTITY_TYPES.getValues()) {
            if(type.getCategory()==MobCategory.MISC || type==EntityType.PLAYER) continue;
            var key=ForgeRegistries.ENTITY_TYPES.getKey(type);
            if(key!=null) all.add(key.toString());
        }
        all.sort(Comparator.naturalOrder());
        filtered.addAll(all);
    }
    @Override protected void init() {
        search=new EditBox(font,20,24,width-40,20,Component.literal("Buscar ID del mob"));
        search.setMaxLength(90);
        search.setResponder(s->{page=0;filter(s);refresh();});
        addRenderableWidget(search);
        refresh();
    }
    private void filter(String term) {
        filtered.clear();
        for(String id:all) if(id.toLowerCase(java.util.Locale.ROOT).contains(term.toLowerCase(java.util.Locale.ROOT))) filtered.add(id);
    }
    private void refresh() {
        clearWidgets();addRenderableWidget(search);
        int rows=Math.max(2,Math.min(9,(height-92)/23));
        for(int i=0;i<rows;i++) {
            int index=page*rows+i;
            if(index>=filtered.size()) break;
            String id=filtered.get(index);
            addRenderableWidget(Button.builder(Component.literal(id),b->{editor.addMob(id);minecraft.setScreen(editor);})
                .bounds(20,52+i*23,width-40,20).build());
        }
        int maxPage=Math.max(0,(filtered.size()-1)/rows);
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);refresh();})
            .bounds(20,height-30,35,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->{page=Math.min(maxPage,page+1);refresh();})
            .bounds(59,height-30,35,20).build());
        addRenderableWidget(Button.builder(Component.literal("Volver"),b->minecraft.setScreen(editor))
            .bounds(width-105,height-30,85,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt) {
        renderBackground(g);
        g.drawString(font,"Mobs de Minecraft y de mods instalados: "+filtered.size(),20,8,0xFFFFFFFF);
        super.render(g,mx,my,pt);
    }
    @Override public void onClose() {minecraft.setScreen(editor);}
}
