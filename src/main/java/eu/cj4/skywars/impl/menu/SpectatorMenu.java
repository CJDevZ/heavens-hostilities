package eu.cj4.skywars.impl.menu;

import eu.cj4.skywars.impl.Skywars;
import eu.pb4.sgui.api.elements.ItemStackBuilder;
import eu.pb4.sgui.api.gui.HotbarGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SpectatorMenu extends HotbarGui {
    private static final ItemStack NEW_ROUND_ITEM = ItemStackBuilder.from(Items.PAPER.getDefaultInstance()).setItemName(Component.literal("Join New Round")).asStack();

    public SpectatorMenu(ServerPlayer player) {
        super(player);
        this.setSlot(7, NEW_ROUND_ITEM, () -> {
//            player.teleport(Skywars.joinNewGame(player, null));
        });
        this.open();
    }
}
